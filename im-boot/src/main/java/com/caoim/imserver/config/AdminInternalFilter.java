package com.caoim.imserver.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminInternalFilter extends OncePerRequestFilter {

    private final String internalToken;

    public AdminInternalFilter(@Value("${im.internal-token:}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = request.getContextPath() + "/admin";
        return uri == null || !(uri.equals(prefix) || uri.startsWith(prefix + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("X-Internal-Token");
        if (internalToken == null || internalToken.isBlank() || !internalToken.equals(header)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"内部接口拒绝访问\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
