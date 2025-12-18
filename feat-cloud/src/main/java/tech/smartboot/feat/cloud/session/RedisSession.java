/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.session;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.router.session.SessionManager;
import tech.smartboot.redisun.Redisun;

/**
 * @author 三刀
 * @version v1.0 12/16/25
 */
public class RedisSession implements Session {
    private final String sessionId;
    private final Redisun redisun;
    private final String sessionKey;
    private int maxAge = 30 * 60; // 默认30分钟
    private final HttpRequest request;

    public RedisSession(String sessionId, String sessionKey, HttpRequest request, Redisun redisun) {
        this.sessionId = sessionId;
        this.redisun = redisun;
        this.sessionKey = sessionKey;
        this.request = request;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void put(String key, String value) {
        redisun.hset(sessionKey, key, value);
        // 每次更新都会刷新过期时间
        redisun.expire(sessionKey, maxAge);
    }

    @Override
    public String get(String key) {
        // 每次读取都会刷新过期时间
        redisun.expire(sessionKey, maxAge);
        return redisun.hget(sessionKey, key);
    }

    @Override
    public int getTimeout() {
        return maxAge;
    }

    @Override
    public void setTimeout(int expiry) {
        this.maxAge = expiry;
        redisun.expire(sessionKey, maxAge);
    }

    @Override
    public void invalidate() {
        redisun.del(sessionKey);
        SessionManager.removeSessionCookie(request);
    }
}