package com.caoim.imcore.dto;

import lombok.Data;

@Data
public class AdminOverviewDTO {
    private long userCount;
    private long groupCount;
    private long todayMessageCount;
}
