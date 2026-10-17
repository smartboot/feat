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

import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.Session;
import tech.smartboot.feat.router.session.SessionManager;
import tech.smartboot.redisun.Redisun;

/**
 * 基于Redis的集群会话管理器
 * <p>
 * 仅负责会话的存储，Cookie的解析、去重与下发由基类模板方法统一管理
 * </p>
 *
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
    protected Session getSession(String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        // 更新过期时间，更新成功说明该会话存在
        int count = redisun.expire(sessionKey, sessionOptions.getTimeout());
        if (count == 1) {
            return new RedisSession(sessionId, sessionKey, sessionOptions.getTimeout(), redisun);
        }
        return null;
    }

    @Override
    protected Session createSession() {
        String sessionId = FeatUtils.createSessionId();
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        return new RedisSession(sessionId, sessionKey, sessionOptions.getTimeout(), redisun);
    }
}
