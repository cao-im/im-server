package com.caoim.imcore.client;

import com.caoim.imcore.common.Result;
import com.caoim.imcore.dto.AdminMessageVO;
import com.caoim.imcore.dto.AdminOverviewDTO;
import com.caoim.imcore.dto.PageResult;
import com.caoim.imcore.entity.Group;
import com.caoim.imcore.entity.SensitiveWord;
import com.caoim.imcore.entity.SystemConfig;
import com.caoim.imcore.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "im-admin-api", url = "${im.server.url}")
public interface AdminImClient {

    @GetMapping("/admin/user/page")
    Result<PageResult<User>> pageUsers(@RequestParam(value = "keyword", required = false) String keyword,
                                       @RequestParam(value = "page", defaultValue = "1") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size);

    @PutMapping("/admin/user/{id}/enabled")
    Result<Void> setUserEnabled(@PathVariable("id") Long id, @RequestParam("enabled") Integer enabled);

    @GetMapping("/admin/group/page")
    Result<PageResult<Group>> pageGroups(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "page", defaultValue = "1") int page,
                                         @RequestParam(value = "size", defaultValue = "20") int size);

    @PutMapping("/admin/group/{id}/dismiss")
    Result<Void> dismissGroup(@PathVariable("id") Long id);

    @GetMapping("/admin/message/page")
    Result<PageResult<AdminMessageVO>> pageMessages(@RequestParam(value = "userId", required = false) Long userId,
                                                    @RequestParam(value = "groupId", required = false) Long groupId,
                                                    @RequestParam(value = "keyword", required = false) String keyword,
                                                    @RequestParam(value = "startTime", required = false) String startTime,
                                                    @RequestParam(value = "endTime", required = false) String endTime,
                                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                                    @RequestParam(value = "size", defaultValue = "20") int size);

    @GetMapping("/admin/stats/overview")
    Result<AdminOverviewDTO> overview();

    @GetMapping("/admin/sensitive-word/page")
    Result<PageResult<SensitiveWord>> pageSensitiveWords(@RequestParam(value = "keyword", required = false) String keyword,
                                                         @RequestParam(value = "page", defaultValue = "1") int page,
                                                         @RequestParam(value = "size", defaultValue = "20") int size);

    @PostMapping("/admin/sensitive-word")
    Result<SensitiveWord> createSensitiveWord(@RequestBody Map<String, String> body);

    @PutMapping("/admin/sensitive-word/{id}/enabled")
    Result<Void> setSensitiveWordEnabled(@PathVariable("id") Long id, @RequestParam("enabled") Integer enabled);

    @DeleteMapping("/admin/sensitive-word/{id}")
    Result<Void> deleteSensitiveWord(@PathVariable("id") Long id);

    @GetMapping("/admin/config")
    Result<List<SystemConfig>> listConfig();

    @PutMapping("/admin/config")
    Result<Void> updateConfig(@RequestBody Map<String, String> values);
}
