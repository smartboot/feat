/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router.session;

import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;

import java.util.Collection;
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
    private volatile int maxAge;
    
    /**
     * 会话唯一标识符
     * <p>
     * 通过UUID算法生成的全局唯一标识符，用于标识一个特定的会话
     * </p>
     */
    private final String sessionId;
    
    /**
     * 关联的HTTP请求对象
     * <p>
     * 用于在会话创建和管理过程中操作HTTP响应，如设置Cookie等
     * </p>
     */
    private final HttpRequest request;
    
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

    /**
     * 构造一个新的内存会话实例
     * <p>
     * 在构造过程中会生成唯一的会话ID，并通过Cookie将该ID发送给客户端。
     * 同时会清理之前可能设置的会话Cookie，确保只有一个有效的会话Cookie存在。
     * </p>
     *
     * @param request 关联的HTTP请求对象
     */
    MemorySession(HttpRequest request) {
        this.sessionId = FeatUtils.createSessionId();
        this.request = request;
        removeSessionCookie();
        Cookie cookie = new Cookie(DEFAULT_SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(1800);
        request.getResponse().addCookie(cookie);
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
     *
     * @return 会话最大存活时间（秒）
     * @throws FeatException 如果会话已经失效则抛出异常
     */
    public int getMaxAge() {
        checkValid();
        return maxAge;
    }

    /**
     * 设置会话最大存活时间
     *
     * @param maxAge 会话最大存活时间（秒）
     * @throws FeatException 如果会话已经失效则抛出异常
     */
    public void setMaxAge(int maxAge) {
        checkValid();
        this.maxAge = maxAge;
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
    }

    /**
     * 将当前会话标记为失效
     * <p>
     * 失效会话会清除所有属性数据，并向客户端发送清除会话Cookie的指令
     * </p>
     */
    public void invalidate() {
        checkValid();
        removeSessionCookie();
        attributes.clear();
        Cookie cookie = new Cookie(DEFAULT_SESSION_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        request.getResponse().addCookie(cookie);
        invalid = true;
    }

    /**
     * 移除已存在的会话Cookie
     * <p>
     * 在本次响应中查找是否已经设置了会话Cookie，如果存在则将其移除，
     * 避免重复设置会话Cookie
     * </p>
     */
    private void removeSessionCookie() {
        Collection<String> preValues = request.getResponse().getHeaders(HeaderName.SET_COOKIE);
        //如果在本次请求中已经为session设置过Cookie了，那么需要将本次设置的Cookie移除掉
        if (FeatUtils.isNotEmpty(preValues)) {
            request.getResponse().setHeader(HeaderName.SET_COOKIE, null);
            preValues.forEach(preValue -> {
                if (!FeatUtils.startsWith(preValue, DEFAULT_SESSION_COOKIE_NAME + "=")) {
                    request.getResponse().addHeader(HeaderName.SET_COOKIE, preValue);
                }
            });
        }
    }
}