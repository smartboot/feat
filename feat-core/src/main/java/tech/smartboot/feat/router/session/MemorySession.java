/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router.session;

import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.Session;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于内存的会话实现类
 * <p>
 * MemorySession提供了基于内存的会话管理功能，通过Cookie机制在客户端存储会话ID，
 * 在服务器端存储会话数据。支持会话属性的存储、获取和失效等操作。
 * </p>
 * <p>
 * 会话数据存储在服务器内存中，适用于单机部署场景。在分布式部署场景下，
 * 需要使用其他支持共享存储的会话实现。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
class MemorySession implements Session {

    /**
     * 会话的最大存活时间（秒）
     * <p>
     * 用于控制会话的超时时间，超过此时间未访问的会话将被自动清理
     * </p>
     */
    private volatile int timeout;

    /**
     * 会话唯一标识符
     * <p>
     * 通过UUID算法生成的全局唯一标识符，用于标识一个特定的会话
     * </p>
     */
    private final String sessionId;

    /**
     * 会话属性映射表
     * <p>
     * 存储会话中保存的所有键值对属性，键为属性名，值为属性值
     * </p>
     */
    private final Map<String, String> attributes = new HashMap<>();

    /**
     * 会话失效标志
     * <p>
     * 标识当前会话是否已经失效，失效的会话不能继续使用
     * </p>
     */
    private boolean invalid = false;

    private long latestAccessTime = FeatUtils.currentTime().getTime();

    /**
     * 构造一个新的内存会话实例
     * <p>
     * 注意：会话是长生命周期对象，而请求/响应对象随连接复用，
     * 会话不得持有任何请求引用，否则定时器过期或后续请求中的
     * invalidate调用会污染其他无关请求的响应。
     * 会话Cookie的清理与下发由SessionManager在创建时刻完成。
     * </p>
     */
    MemorySession() {
        this.sessionId = FeatUtils.createSessionId();
    }


    /**
     * 获取会话唯一标识符
     *
     * @return 会话ID字符串
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话属性
     * <p>
     * 将指定的键值对存储到会话中，如果已存在相同键的属性，则会覆盖原有值
     * </p>
     *
     * @param key   属性键
     * @param value 属性值
     * @throws FeatException 如果会话已经失效则抛出异常
     */
    public synchronized void put(String key, String value) {
        checkValid();
        attributes.put(key, value);
    }

    /**
     * 获取会话属性值
     * <p>
     * 根据指定的键从会话中获取对应的属性值
     * </p>
     *
     * @param key 属性键
     * @return 对应的属性值，如果不存在则返回null
     * @throws FeatException 如果会话已经失效则抛出异常
     */
    public synchronized String get(String key) {
        checkValid();
        return attributes.get(key);
    }

    /**
     * 获取会话最大存活时间
     * <p>
     * 纯配置读取：不校验有效性、不刷新最近访问时间。
     * 曾经由checkValid带来的访问时间刷新会导致超时任务计算剩余时间时
     * 会话永不失效，且内部调度依赖该纯读取语义。
     * </p>
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 设置会话最大存活时间
     *
     * @param maxAge 会话最大存活时间（秒）
     * @throws FeatException 如果会话已经失效则抛出异常
     */
    public void setTimeout(int maxAge) {
        checkValid();
        this.timeout = maxAge;
    }


    /**
     * 检查会话是否有效
     * <p>
     * 如果会话已经被标记为失效，则抛出异常
     * </p>
     *
     * @throws FeatException 当会话已经失效时抛出
     */
    private void checkValid() {
        if (invalid) {
            throw new FeatException("Session is invalid");
        }
        latestAccessTime = FeatUtils.currentTime().getTime();
    }

    public long getLatestAccessTime() {
        return latestAccessTime;
    }

    public void setLatestAccessTime(long latestAccessTime) {
        this.latestAccessTime = latestAccessTime;
    }

    /**
     * 将当前会话标记为失效
     * <p>
     * 仅清理服务端状态，不操作任何响应对象：响应随连接复用，从任意线程
     * （超时定时器、后续请求中的应用代码）写入都会污染其他无关请求的响应。
     * 客户端下次携带已失效的会话ID请求时，服务端将颁发新会话并下发新Cookie，
     * 旧Cookie亦会因其Max-Age在浏览器中自然过期。
     * </p>
     */
    public void invalidate() {
        if (invalid) {
            return;
        }
        attributes.clear();
        invalid = true;
    }
}