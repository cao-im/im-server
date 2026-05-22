package com.caoim.imserver.controller;

import com.caoim.imcore.common.Result;
import com.caoim.imserver.config.PortBindingValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "健康检查")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Value("${server.port:8080}")
    private int serverPort;

    @Operation(summary = "IM服务健康检查")
    @GetMapping("/check")
    public Result<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "曹操IM (Cao-IM) Server");
        healthInfo.put("version", "1.0.0");
        healthInfo.put("timestamp", LocalDateTime.now().toString());
        healthInfo.put("description", "即时通讯核心服务运行正常");

        return Result.success(healthInfo);
    }

    @Operation(summary = "简易心跳检测")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }

    @Operation(summary = "获取服务端端口配置信息（SDK连接前必须调用）")
    @GetMapping("/port-info")
    public Result<Map<String, Object>> getPortInfo() {
        Map<String, Object> portInfo = new HashMap<>();
        portInfo.put("port", serverPort);
        portInfo.put("expectedPort", PortBindingValidator.getExpectedPort());
        portInfo.put("isPortLocked", serverPort == PortBindingValidator.getExpectedPort());
        portInfo.put("buildSignature", PortBindingValidator.getBuildSignature());
        portInfo.put("protocolVersion", PortBindingValidator.getProtocolVersion());
        portInfo.put("fingerprint", System.getProperty("im.server.fingerprint", "unknown"));
        portInfo.put("timestamp", LocalDateTime.now().toString());

        if (serverPort != PortBindingValidator.getExpectedPort()) {
            portInfo.put("warning", "⚠️ 端口已被修改！SDK客户端可能无法正常连接");
            portInfo.put("sdkRequiredPort", PortBindingValidator.getExpectedPort());
        }

        return Result.success(portInfo);
    }
}
