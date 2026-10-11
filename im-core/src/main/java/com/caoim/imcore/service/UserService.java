package com.caoim.imcore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.ErrorCode;
import com.caoim.imcore.dao.UserMapper;
import com.caoim.imcore.dto.LoginDTO;
import com.caoim.imcore.dto.RegisterDTO;
import com.caoim.imcore.dto.UpdateProfileDTO;
import com.caoim.imcore.dto.UserSearchDTO;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final SystemConfigService systemConfigService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.expiration}")
    private Long expiration;

    public Map<String, Object> register(RegisterDTO dto) {
        if (!systemConfigService.getBoolean(SystemConfigService.REGISTER_ENABLED, true)) {
            throw new BusinessException(ErrorCode.REGISTER_CLOSED);
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "密码不能为空");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setStatus(Constants.UserStatus.OFFLINE);
        user.setEnabled(1);
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
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_SET);
        }
        if (dto.getPassword() == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        assertEnabled(user);

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
        assertEnabled(user);

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

    /**
     * 仅在密码尚未写入时绑定。已有密码的账号不会被覆盖。
     */
    public void bindPasswordIfAbsent(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "用户名和密码不能为空");
        }
        User user = findByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            return;
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
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

    public void assertEnabled(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        assertEnabled(user);
    }

    public void setEnabled(Long userId, Integer enabled) {
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User user = getUserInfo(userId);
        user.setEnabled(enabled);
        if (enabled == 0) {
            user.setStatus(Constants.UserStatus.OFFLINE);
        }
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

    public User updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getSignature() != null) {
            user.setSignature(dto.getSignature());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null && !dto.getBirthday().isBlank()) {
            user.setBirthday(LocalDate.parse(dto.getBirthday()));
        }
        if (dto.getLocation() != null) {
            user.setLocation(dto.getLocation());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }

        userMapper.updateById(user);
        return user;
    }

    private void assertEnabled(User user) {
        if (user.getEnabled() != null && user.getEnabled() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
    }
}
