/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.session;

import tech.smartboot.feat.core.server.Session;
import tech.smartboot.redisun.Redisun;

/**
 * 基于Redis的会话实现
 * <p>
 * 注意：会话是长生命周期对象，不持有任何请求/响应引用（其随连接复用），
 * 失效仅清理Redis中的存储，Cookie清理由客户端携带失效ID再次请求时自愈
 * </p>
 *
 * @author 三刀
 * @version v1.0 12/16/25
 */
public class RedisSession implements Session {
    private final String sessionId;
    private final Redisun redisun;
    private final String sessionKey;
    private int maxAge;

    public RedisSession(String sessionId, String sessionKey, int maxAge, Redisun redisun) {
        this.sessionId = sessionId;
        this.sessionKey = sessionKey;
        this.maxAge = maxAge;
        this.redisun = redisun;
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
        //仅清理存储，不操作响应对象（其随连接复用，写入会污染无关请求的响应）
        redisun.del(sessionKey);
    }
}