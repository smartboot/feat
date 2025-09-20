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
 * SSE重连策略配置
 * 
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class RetryPolicy {
    
    /**
     * 最大重连次数，-1表示无限重连
     */
    private int maxRetries = 5;
    
    /**
     * 初始重连延迟(毫秒)
     */
    private long initialDelay = 1000;
    
    /**
     * 最大重连延迟(毫秒)
     */
    private long maxDelay = 30000;
    
    /**
     * 退避倍数
     */
    private double backoffMultiplier = 1.5;
    
    /**
     * 错误时是否重连
     */
    private boolean retryOnError = true;

    public RetryPolicy() {
    }

    public RetryPolicy(int maxRetries, long initialDelay, long maxDelay, double backoffMultiplier, boolean retryOnError) {
        this.maxRetries = maxRetries;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.backoffMultiplier = backoffMultiplier;
        this.retryOnError = retryOnError;
    }

    /**
     * 计算下次重连延迟时间
     * 
     * @param retryCount 当前重试次数
     * @return 延迟时间(毫秒)
     */
    public long calculateDelay(int retryCount) {
        if (retryCount <= 0) {
            return initialDelay;
        }
        long delay = (long) (initialDelay * Math.pow(backoffMultiplier, retryCount));
        return Math.min(delay, maxDelay);
    }
    
    /**
     * 检查是否允许重连
     * 
     * @param retryCount 当前重试次数
     * @return 是否允许重连
     */
    public boolean shouldRetry(int retryCount) {
        return maxRetries == -1 || retryCount < maxRetries;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public RetryPolicy setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public RetryPolicy setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
        return this;
    }

    public long getMaxDelay() {
        return maxDelay;
    }

    public RetryPolicy setMaxDelay(long maxDelay) {
        this.maxDelay = maxDelay;
        return this;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public RetryPolicy setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
        return this;
    }

    public boolean isRetryOnError() {
        return retryOnError;
    }

    public RetryPolicy setRetryOnError(boolean retryOnError) {
        this.retryOnError = retryOnError;
        return this;
    }

    /**
     * 创建默认重连策略
     * 
     * @return 默认重连策略
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy();
    }
    
    /**
     * 创建无重连策略
     * 
     * @return 无重连策略
     */
    public static RetryPolicy noRetry() {
        return new RetryPolicy(0, 0, 0, 1.0, false);
    }
    
    /**
     * 创建无限重连策略
     * 
     * @return 无限重连策略
     */
    public static RetryPolicy infiniteRetry() {
        return new RetryPolicy(-1, 1000, 30000, 1.5, true);
    }
}