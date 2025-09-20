/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.sse;

/**
 * SSE心跳检测配置
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class HeartbeatConfig {
    
    /**
     * 是否启用心跳检测
     */
    private boolean enabled = false;
    
    /**
     * 心跳间隔(毫秒)
     */
    private long interval = 30000;
    
    /**
     * 心跳超时(毫秒)
     */
    private long timeout = 5000;
    
    /**
     * 最大丢失心跳次数
     */
    private int maxMissed = 3;

    public HeartbeatConfig() {
    }

    public HeartbeatConfig(boolean enabled, long interval, long timeout, int maxMissed) {
        this.enabled = enabled;
        this.interval = interval;
        this.timeout = timeout;
        this.maxMissed = maxMissed;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public HeartbeatConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public long getInterval() {
        return interval;
    }

    public HeartbeatConfig setInterval(long interval) {
        this.interval = interval;
        return this;
    }

    public long getTimeout() {
        return timeout;
    }

    public HeartbeatConfig setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getMaxMissed() {
        return maxMissed;
    }

    public HeartbeatConfig setMaxMissed(int maxMissed) {
        this.maxMissed = maxMissed;
        return this;
    }
    
    /**
     * 创建默认心跳配置（关闭状态）
     * 
     * @return 默认心跳配置
     */
    public static HeartbeatConfig disabled() {
        return new HeartbeatConfig();
    }
    
    /**
     * 创建启用的心跳配置
     * 
     * @return 启用的心跳配置
     */
    public static HeartbeatConfig enabled() {
        return new HeartbeatConfig(true, 30000, 5000, 3);
    }
    
    /**
     * 创建自定义心跳配置
     * 
     * @param interval 心跳间隔(毫秒)
     * @return 心跳配置
     */
    public static HeartbeatConfig custom(long interval) {
        return new HeartbeatConfig(true, interval, 5000, 3);
    }
}