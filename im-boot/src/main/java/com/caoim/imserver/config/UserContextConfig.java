package com.caoim.imserver.config;

import com.caoim.imserver.common.UserContext;
import com.caoim.imcore.util.JwtUtil;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class UserContextConfig {

    private final JwtUtil jwtUtil;

    public UserContextConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void init() {
        UserContext.setJwtUtil(jwtUtil);
    }
}
