package com.caoim.imcore.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 离线消息拉取响应DTO
 */
@Data
public class OfflineMessageResponseDTO {
    /**
     * 响应类型：offline_messages
     */
    private String type = "offline_messages";

    /**
     * 消息列表
     */
    private List<Map<String, Object>> messages;

    /**
     * 当前批次返回的消息数量
     */
    private Integer count;

    /**
     * 符合条件的消息总数
     */
    private Long totalCount;

    /**
     * 是否还有更多消息
     */
    private Boolean hasMore;

    /**
     * 当前偏移量
     */
    private Integer offset;

    /**
     * 每页数量
     */
    private Integer limit;
}
