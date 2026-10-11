package com.caoim.imserver.config;

import com.caoim.imserver.common.UserContext;
import com.caoim.imcore.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class UserContextConfig {

    private final JwtUtil jwtUtil;

    @Value("${im.internal-token:}")
    private String internalToken;

    public UserContextConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void init() {
        UserContext.setJwtUtil(jwtUtil);
        UserContext.setInternalToken(internalToken);
    }
}
