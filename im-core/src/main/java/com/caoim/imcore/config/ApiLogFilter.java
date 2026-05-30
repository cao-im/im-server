package com.caoim.imcore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "api-log")
@ConditionalOnProperty(name = "api-log.enabled", havingValue = "true", matchIfMissing = true)
public class ApiLogFilter implements Filter {

    private boolean enabled = true;
    private boolean logHeaders = true;
    private boolean logParams = true;
    private boolean logBody = true;
    private boolean logResponse = true;
    private int maxBodyLength = 2000;
    private List<String> excludePaths = new ArrayList<>();
    private List<String> sensitiveHeaders = Arrays.asList("authorization", "cookie", "set-cookie");

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLogHeaders(boolean logHeaders) {
        this.logHeaders = logHeaders;
    }

    public void setLogParams(boolean logParams) {
        this.logParams = logParams;
    }

    public void setLogBody(boolean logBody) {
        this.logBody = logBody;
    }

    public void setLogResponse(boolean logResponse) {
        this.logResponse = logResponse;
    }

    public void setMaxBodyLength(int maxBodyLength) {
        this.maxBodyLength = maxBodyLength;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public void setSensitiveHeaders(List<String> sensitiveHeaders) {
        this.sensitiveHeaders = sensitiveHeaders;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        if (shouldExclude(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        log.info("════════════════════════════════════════════════════");
        log.info("🌐 🔵 [API请求] [{}] {} {}", requestId, method, requestUri);
        log.info("📍 客户端IP: {}", getClientIp(httpRequest));
        log.info("⏱️ 请求时间: {}", new Date());

        if (logHeaders) {
            logRequestHeaders(httpRequest, requestId);
        }

        if (logParams) {
            logRequestParams(httpRequest, requestId);
        }

        if (logBody && shouldLogBody(method)) {
            logRequestBody(httpRequest, requestId);
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(httpResponse);

        try {
            chain.doFilter(request, wrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = wrapper.getStatus();

            log.info("");
            log.info("🟢 [响应完成] [{}] {} {} - 状态码: {} (耗时: {}ms)", 
                    requestId, method, requestUri, status, duration);
            
            if (logResponse && wrapper.getContentSize() > 0) {
                logResponseBody(wrapper, requestId);
            }
            
            log.info("════════════════════════════════════════════════════");
            log.info("");

            wrapper.copyToResponse();
        }
    }

    private boolean shouldExclude(String uri) {
        return excludePaths.stream().anyMatch(uri::contains);
    }

    private boolean shouldLogBody(String method) {
        return "POST".equalsIgnoreCase(method) || 
               "PUT".equalsIgnoreCase(method) || 
               "PATCH".equalsIgnoreCase(method);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty() && !"unknown".equalsIgnoreCase(xri)) {
            return xri;
        }
        return request.getRemoteAddr();
    }

    private void logRequestHeaders(HttpServletRequest request, String requestId) {
        log.info("┌─────────────── 请求头 (Request Headers) [{}] ───────────────", requestId);
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            String value = request.getHeader(headerName);
            if (sensitiveHeaders.stream().anyMatch(h -> h.equalsIgnoreCase(headerName))) {
                value = maskSensitiveValue(value);
            }
            log.info("│ {}: {}", headerName, value);
        });
        log.info("└────────────────────────────────────────────────────");
    }

    private void logRequestParams(HttpServletRequest request, String requestId) {
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            log.info("┌─────────────── 请求参数 (Query Parameters) [{}] ──────────", requestId);
            params.forEach((key, values) -> {
                log.info("│ {}: {}", key, String.join(", ", values));
            });
            log.info("└────────────────────────────────────────────────────");
        }
    }

    private void logRequestBody(HttpServletRequest request, String requestId) {
        try {
            String body = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            if (body != null && !body.trim().isEmpty()) {
                String formattedBody = formatJson(body);
                
                if (formattedBody.length() > maxBodyLength) {
                    formattedBody = formattedBody.substring(0, maxBodyLength) + "\n...(数据过长，已截取前" + maxBodyLength + "字符)";
                }
                
                log.info("┌─────────────── 请求体 (Request Body) [{}] ──────────────", requestId);
                String[] lines = formattedBody.split("\n");
                for (String line : lines) {
                    log.info("│ {}", line);
                }
                log.info("└────────────────────────────────────────────────────");
            }
        } catch (IOException e) {
            log.warn("[{}] 无法读取请求体: {}", requestId, e.getMessage());
        }
    }

    private void logResponseBody(ContentCachingResponseWrapper response, String requestId) {
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                String responseBody = new String(content, StandardCharsets.UTF_8);
                String formattedBody = formatJson(responseBody);
                
                if (formattedBody.length() > maxBodyLength) {
                    formattedBody = formattedBody.substring(0, maxBodyLength) + "\n...(数据过长，已截取前" + maxBodyLength + "字符)";
                }
                
                log.info("┌─────────────── 响应数据 (Response Body) [{}] ─────────────", requestId);
                String[] lines = formattedBody.split("\n");
                for (String line : lines) {
                    log.info("│ {}", line);
                }
                log.info("└────────────────────────────────────────────────────");
            }
        } catch (Exception e) {
            log.warn("[{}] 无法读取响应体: {}", requestId, e.getMessage());
        }
    }

    private String formatJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object obj = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return json;
        }
    }

    private String maskSensitiveValue(String value) {
        if (value == null || value.length() <= 10) {
            return "***";
        }
        if (value.startsWith("Bearer ")) {
            return "Bearer " + value.substring(7, Math.min(27, value.length())) + "...***";
        }
        return value.substring(0, 5) + "***" + value.substring(value.length() - 3);
    }

    public static class ContentCachingResponseWrapper extends HttpServletResponseWrapper {
        private byte[] content;

        public ContentCachingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return new DelegatingServletOutputStream(super.getOutputStream());
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return super.getWriter();
        }

        public byte[] getContentAsByteArray() {
            return content != null ? content : new byte[0];
        }

        public int getContentSize() {
            return content != null ? content.length : 0;
        }

        public void copyToResponse() throws IOException {
            if (content != null && content.length > 0) {
                getResponse().getOutputStream().write(content);
            }
        }

        private class DelegatingServletOutputStream extends ServletOutputStream {
            private final ServletOutputStream delegate;
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            public DelegatingServletOutputStream(ServletOutputStream delegate) {
                this.delegate = delegate;
            }

            @Override
            public void write(int b) throws IOException {
                buffer.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                buffer.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                content = buffer.toByteArray();
            }

            @Override
            public void close() throws IOException {
                content = buffer.toByteArray();
            }

            @Override
            public boolean isReady() {
                return delegate.isReady();
            }

            @Override
            public void setWriteListener(WriteListener listener) {
                delegate.setWriteListener(listener);
            }
        }
    }
}
