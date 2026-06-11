package com.caoim.imcore.common;

import java.security.SecureRandom;

/**
 * 雪花算法（Snowflake）ID 生成器 — 服务端实现
 *
 * 结构（64位）：
 * ┌────┬──────────────────┬─────────┬──────────┐
 * │符号 │   时间戳(41位)    │ 节点ID   │ 序列号    │
 * │ 1位 │                  │ (10位)   │ (12位)   │
 * └────┴──────────────────┴─────────┴──────────┘
 *
 * - 时间戳：毫秒级，可用约69年（从自定义纪元 2024-01-01 开始）
 * - 节点ID：支持1024个节点
 * - 序列号：每毫秒支持4096个ID
 */
public class SnowflakeIdGenerator {

    /** 自定义纪元：2024-01-01 00:00:00 UTC (毫秒) */
    private static final long EPOCH = 1704067200000L;

    private static final int WORKER_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1; // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4095

    private static final int WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    private static final SnowflakeIdGenerator INSTANCE = new SnowflakeIdGenerator();

    private SnowflakeIdGenerator() {
        // 使用随机数作为节点ID（0-1023）
        this.workerId = new SecureRandom().nextInt((int) MAX_WORKER_ID + 1);
    }

    public static SnowflakeIdGenerator getInstance() {
        return INSTANCE;
    }

    /**
     * 生成一个全局唯一的雪花ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        if (timestamp < lastTimestamp) {
            // 时钟回拨，等待追上
            timestamp = waitNextMillis(lastTimestamp);
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 判断一个 mid 是否看起来像合法的雪花ID
     */
    public static boolean isValidSnowflakeId(long mid) {
        return mid >= 100000000000000L; // 10^14
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
