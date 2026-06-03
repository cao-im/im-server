package com.caoim.imcore.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.ContactDTO;
import com.caoim.imcore.dto.GroupCreateDTO;
import com.caoim.imcore.dto.LoginDTO;
import com.caoim.imcore.dto.MessageSendDTO;
import com.caoim.imcore.dto.RegisterDTO;
import com.caoim.imcore.entity.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "im-server", url = "${im.server.url}")
public interface ImFeignClient {

    // ==================== 健康检查接口 ====================

    @GetMapping("/health/check")
    Result<Map<String, Object>> healthCheck();

    @GetMapping("/health/ping")
    Result<String> ping();

    @GetMapping("/health/port-info")
    Result<Map<String, Object>> getPortInfo();

    // ==================== 用户接口 ====================

    @PostMapping("/user/register")
    Result<Map<String, Object>> registerUser(@RequestBody RegisterDTO dto);

    @PostMapping("/user/login")
    Result<Map<String, Object>> loginUser(@RequestBody LoginDTO dto);

    @PostMapping("/user/refresh-token")
    Result<Map<String, Object>> refreshImToken(@RequestBody Map<String, String> body);

    @GetMapping("/user/info/{userId}")
    Result<User> getUserInfo(@PathVariable("userId") Long userId);

    @PutMapping("/user/status")
    Result<Void> updateStatus(@RequestParam("userId") Long userId, @RequestParam("status") Integer status);

    // ==================== 消息接口 ====================

    @PostMapping("/message/send")
    Result<Message> sendMessage(@RequestBody MessageSendDTO dto);

    @GetMapping("/message/private/{targetId}")
    Result<Page<Message>> getPrivateMessages(
            @RequestParam("userId") Long userId,
            @PathVariable("targetId") Long targetId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);

    @GetMapping("/message/group/{groupId}")
    Result<Page<Message>> getGroupMessages(
            @PathVariable("groupId") Long groupId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);

    @GetMapping("/message/unread/count")
    Result<Long> getUnreadCount(@RequestParam("userId") Long userId);

    // ==================== 群组接口 ====================

    @PostMapping("/group/create")
    Result<Group> createGroup(@RequestBody GroupCreateDTO dto);

    @GetMapping("/group/list")
    Result<List<Group>> getUserGroups(@RequestParam("userId") Long userId);

    // ==================== 好友接口 ====================

    @PostMapping("/friend/request")
    Result<Void> sendFriendRequest(@RequestParam("userId") Long userId, @RequestParam("friendId") Long friendId);

    @PostMapping("/friend/accept")
    Result<Void> acceptFriendRequest(@RequestParam("userId") Long userId, @RequestParam("friendId") Long friendId);

    @PostMapping("/friend/reject")
    Result<Void> rejectFriendRequest(@RequestParam("userId") Long userId, @RequestParam("friendId") Long friendId);

    @GetMapping("/contact/list")
    Result<List<ContactDTO>> getContacts(@RequestParam("userId") Long userId);

    @DeleteMapping("/contact/{contactId}")
    Result<Void> deleteContact(@RequestParam("userId") Long userId, @PathVariable("contactId") Long contactId);
}
