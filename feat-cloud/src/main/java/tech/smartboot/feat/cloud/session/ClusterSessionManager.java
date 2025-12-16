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

import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.router.session.SessionManager;
import tech.smartboot.redisun.Redisun;

/**
 * @author 三刀
 * @version v1.0 12/16/25
 */
public class ClusterSessionManager extends SessionManager {
    private final Redisun redisun;
    private static final String SESSION_KEY_PREFIX = "feat_session:";

    public ClusterSessionManager(Redisun redisun) {
        this.redisun = redisun;
    }

    @Override
    public Session getSession(HttpRequest request, boolean create) {
        String sessionId = getSessionId(request);

        // 如果存在sessionId，则尝试从Redis获取会话
        if (sessionId != null) {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            // 更新过期时间
            int count = redisun.expire(sessionKey, sessionOptions.getMaxAge());
            // 更新成功，说明存在该会话，返回RedisSession
            if (count == 1) {
                return new RedisSession(sessionId, sessionKey, request, redisun);
            }
        }

        // 如果不允许创建新会话，则返回null
        if (!create) {
            return null;
        }

        // 创建新的会话
        String newSessionId = FeatUtils.createSessionId();
        String sessionKey = SESSION_KEY_PREFIX + newSessionId;

        RedisSession redisSession = new RedisSession(newSessionId, sessionKey, request, redisun);
        removeSessionCookie(request);
        responseSessionCookie(request, newSessionId);
        return redisSession;
    }

    @Override
    public void updateAccessTime(HttpRequest request) {
        String sessionId = getSessionId(request);
        if (sessionId != null) {
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            redisun.expire(sessionKey, sessionOptions.getMaxAge());
        }
    }
}