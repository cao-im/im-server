package com.caoim.imcore.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 离线消息拉取请求DTO
 */
@Data
public class OfflineMessageRequestDTO {
    /**
     * 起始时间戳（毫秒），0表示不限制时间
     */
    private Long since = 0L;

    /**
     * 起始消息ID，0表示不限制消息ID
     */
    private Long sinceMessageId = 0L;

    /**
     * 分页偏移量
     */
    private Integer offset = 0;

    /**
     * 每页数量（最大200）
     */
    private Integer limit = 50;
}
