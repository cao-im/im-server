package com.caoim.imserver.common;

import com.caoim.imcore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

public class UserContext {

    private static JwtUtil jwtUtil;

    public static void setJwtUtil(JwtUtil util) {
        jwtUtil = util;
    }

    public static Long getCurrentUserId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getCurrentUsername(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return jwtUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
