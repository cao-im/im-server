package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.UserMapper;
import com.caoim.imcore.dto.LoginDTO;
import com.caoim.imcore.dto.RegisterDTO;
import com.caoim.imcore.dto.UserSearchDTO;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long expiration;

    public Map<String, Object> register(RegisterDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setStatus(Constants.UserStatus.OFFLINE);
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", expiration / 1000);
        result.put("user", getUserInfo(user.getId()));
        return result;
    }

    public Map<String, Object> login(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        user.setStatus(Constants.UserStatus.ONLINE);
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("refreshToken", refreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", expiration / 1000);
        result.put("user", getUserInfo(user.getId()));
        return result;
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID.getCode(), "无效的Refresh Token");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        String newToken = jwtUtil.generateToken(userId, username);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);
        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("refreshToken", newRefreshToken);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", expiration / 1000);
        return result;
    }

    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    public User findByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    public void updateStatus(Long userId, Integer status) {
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public boolean existsById(Long userId) {
        if (userId == null) return false;
        return userMapper.selectById(userId) != null;
    }

    public List<User> searchUsers(String keyword, Long currentUserId, String currentUsername) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            if (currentUserId != null) {
                User currentUser = userMapper.selectById(currentUserId);
                if (currentUser != null) {
                    currentUsername = currentUser.getUsername();
                }
            }
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .like(User::getUsername, keyword)
                .or()
                .like(User::getNickname, keyword)
        );
        if (currentUsername != null && !currentUsername.isEmpty()) {
            wrapper.ne(User::getUsername, currentUsername);
        }

        return userMapper.selectList(wrapper);
    }
}
