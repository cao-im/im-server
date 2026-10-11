package com.caoim.imserver.common;

import com.caoim.imcore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;

public class UserContext {

    private static JwtUtil jwtUtil;
    private static String internalToken;

    public static void setJwtUtil(JwtUtil util) {
        jwtUtil = util;
    }

    public static void setInternalToken(String token) {
        internalToken = token;
    }

    public static boolean isInternalCall(HttpServletRequest request) {
        if (internalToken == null || internalToken.isBlank()) {
            return false;
        }
        String header = request.getHeader("X-Internal-Token");
        return internalToken.equals(header);
    }

    /**
     * 客户端请求以 Token 里的用户为准。
     * 服务间调用带 X-Internal-Token 时，使用调用方传入的 userId。
     */
    public static Long resolveActor(HttpServletRequest request, Long explicitUserId) {
        if (isInternalCall(request)) {
            return explicitUserId;
        }
        return getCurrentUserId(request);
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
