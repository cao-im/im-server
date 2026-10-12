package com.caoim.imserver.controller;

import com.caoim.imcore.common.Constants;
import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.AdminMessageVO;
import com.caoim.imcore.dto.AdminOverviewDTO;
import com.caoim.imcore.dto.PageResult;
import com.caoim.imcore.entity.Group;
import com.caoim.imcore.entity.SensitiveWord;
import com.caoim.imcore.entity.SystemConfig;
import com.caoim.imcore.entity.User;
import com.caoim.imcore.service.GroupService;
import com.caoim.imcore.service.ImAdminQueryService;
import com.caoim.imcore.service.SensitiveWordService;
import com.caoim.imcore.service.SystemConfigService;
import com.caoim.imcore.service.UserService;
import com.caoim.imserver.websocket.IMWebSocketHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin")
public class ImAdminController {

    private final ImAdminQueryService imAdminQueryService;
    private final UserService userService;
    private final GroupService groupService;
    private final SensitiveWordService sensitiveWordService;
    private final SystemConfigService systemConfigService;
    private final IMWebSocketHandler webSocketHandler;

    public ImAdminController(ImAdminQueryService imAdminQueryService,
                             UserService userService,
                             GroupService groupService,
                             SensitiveWordService sensitiveWordService,
                             SystemConfigService systemConfigService,
                             IMWebSocketHandler webSocketHandler) {
        this.imAdminQueryService = imAdminQueryService;
        this.userService = userService;
        this.groupService = groupService;
        this.sensitiveWordService = sensitiveWordService;
        this.systemConfigService = systemConfigService;
        this.webSocketHandler = webSocketHandler;
    }

    @GetMapping("/user/page")
    public Result<PageResult<User>> pageUsers(@RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "page", defaultValue = "1") int page,
                                              @RequestParam(value = "size", defaultValue = "20") int size) {
        PageResult<User> result = imAdminQueryService.pageUsers(keyword, page, size);
        Set<Long> onlineIds = webSocketHandler.liveOnlineUserIds();
        if (result.getRecords() != null) {
            for (User user : result.getRecords()) {
                boolean connected = onlineIds.contains(user.getId());
                if (!connected) {
                    user.setStatus(Constants.UserStatus.OFFLINE);
                } else if (user.getStatus() == null || user.getStatus() == Constants.UserStatus.OFFLINE) {
                    user.setStatus(Constants.UserStatus.ONLINE);
                }
            }
        }
        return Result.success(result);
    }

    @PutMapping("/user/{id}/enabled")
    public Result<Void> setUserEnabled(@PathVariable("id") Long id, @RequestParam("enabled") Integer enabled) {
        userService.setEnabled(id, enabled);
        if (enabled != null && enabled == 0) {
            webSocketHandler.disconnectUser(id);
        }
        return Result.success();
    }

    @GetMapping("/group/page")
    public Result<PageResult<Group>> pageGroups(@RequestParam(value = "keyword", required = false) String keyword,
                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.success(imAdminQueryService.pageGroups(keyword, page, size));
    }

    @PutMapping("/group/{id}/dismiss")
    public Result<Void> dismissGroup(@PathVariable("id") Long id) {
        groupService.adminDismiss(id);
        return Result.success();
    }

    @GetMapping("/message/page")
    public Result<PageResult<AdminMessageVO>> pageMessages(@RequestParam(value = "userId", required = false) Long userId,
                                                           @RequestParam(value = "groupId", required = false) Long groupId,
                                                           @RequestParam(value = "keyword", required = false) String keyword,
                                                           @RequestParam(value = "startTime", required = false) String startTime,
                                                           @RequestParam(value = "endTime", required = false) String endTime,
                                                           @RequestParam(value = "page", defaultValue = "1") int page,
                                                           @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.success(imAdminQueryService.pageMessages(userId, groupId, keyword, startTime, endTime, page, size));
    }

    @GetMapping("/stats/overview")
    public Result<AdminOverviewDTO> overview() {
        return Result.success(imAdminQueryService.overview());
    }

    @GetMapping("/sensitive-word/page")
    public Result<PageResult<SensitiveWord>> pageSensitiveWords(@RequestParam(value = "keyword", required = false) String keyword,
                                                                @RequestParam(value = "page", defaultValue = "1") int page,
                                                                @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.success(sensitiveWordService.page(keyword, page, size));
    }

    @PostMapping("/sensitive-word")
    public Result<SensitiveWord> createSensitiveWord(@RequestBody Map<String, String> body) {
        String word = body == null ? null : body.get("word");
        return Result.success(sensitiveWordService.create(word));
    }

    @PutMapping("/sensitive-word/{id}/enabled")
    public Result<Void> setSensitiveWordEnabled(@PathVariable("id") Long id, @RequestParam("enabled") Integer enabled) {
        sensitiveWordService.setEnabled(id, enabled);
        return Result.success();
    }

    @DeleteMapping("/sensitive-word/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable("id") Long id) {
        sensitiveWordService.delete(id);
        return Result.success();
    }

    @GetMapping("/config")
    public Result<List<SystemConfig>> listConfig() {
        return Result.success(systemConfigService.listAll());
    }

    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestBody Map<String, String> values) {
        systemConfigService.updateValues(values);
        return Result.success();
    }
}
