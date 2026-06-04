package com.caoim.imserver.controller;

import com.caoim.imcore.common.BusinessException;
import com.caoim.imcore.common.Result;
import com.caoim.imserver.common.UserContext;
import com.caoim.imcore.dto.LoginDTO;
import com.caoim.imcore.dto.RegisterDTO;
import com.caoim.imcore.dto.UpdateProfileDTO;
import com.caoim.imcore.dto.UserSearchDTO;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @Operation(summary = "刷新Token", description = "使用RefreshToken获取新的AccessToken和RefreshToken")
    @PostMapping("/refresh-token")
    public Result<Map<String, Object>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Result.error(400, "缺少refreshToken参数");
        }
        try {
            return Result.success(userService.refreshToken(refreshToken));
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public Result<User> getCurrentUserInfo(HttpServletRequest request) {
        Long userId = UserContext.getCurrentUserId(request);
        if (userId == null) {
            return Result.error(401, "未认证或Token无效");
        }
        return Result.success(userService.getUserInfo(userId));
    }

    @Operation(summary = "获取用户信息(通过ID)")
    @GetMapping("/info/{userId}")
    public Result<User> getUserInfo(@PathVariable Long userId) {
        return Result.success(userService.getUserInfo(userId));
    }

    @Operation(summary = "更新在线状态")
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam("userId") Long userId, @RequestParam("status") Integer status) {
        userService.updateStatus(userId, status);
        return Result.success();
    }

    @Operation(summary = "更新个人资料", description = "支持修改昵称、头像、签名、性别、生日、地区、电话、邮箱。内部服务间调用可通过userId参数指定用户")
    @PutMapping("/profile")
    public Result<User> updateProfile(
            @RequestBody UpdateProfileDTO dto,
            @RequestParam(value = "userId", required = false) Long requestUserId,
            HttpServletRequest request) {
        Long userId;
        if (requestUserId != null) {
            // 内部服务间调用：通过参数传递 userId
            userId = requestUserId;
        } else {
            // 外部客户端调用：从 Token 中解析
            userId = UserContext.getCurrentUserId(request);
            if (userId == null) {
                return Result.error(401, "未认证或Token无效");
            }
        }
        try {
            User updatedUser = userService.updateProfile(userId, dto);
            return Result.success(updatedUser);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "搜索用户")
    @GetMapping("/search")
    public Result<List<UserSearchDTO>> searchUsers(
            @RequestParam("keyword") String keyword,
            HttpServletRequest request) {
        Long currentUserId = UserContext.getCurrentUserId(request);
        String currentUsername = UserContext.getCurrentUsername(request);
        
        List<User> users = userService.searchUsers(keyword, currentUserId, currentUsername);
        List<UserSearchDTO> result = new java.util.ArrayList<>();
        for (User user : users) {
            result.add(UserSearchDTO.fromEntity(user));
        }
        return Result.success(result);
    }
}
