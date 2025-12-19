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

import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version v1.0 12/16/25
 */
public class LocalSessionManager extends SessionManager {
    /**
     * 所有的会话映射表
     * <p>
     * 通过会话ID映射到会话单元，用于管理和查找会话信息
     * </p>
     */
    private final Map<String, MemorySession> sessions = new ConcurrentHashMap<>();
    /**
     * 会话监听器,用于清理过期的会话
     * <p>
     * 使用时间轮算法定期检查和清理过期的会话，避免内存泄漏
     * </p>
     */
    private static final HashedWheelTimer timer = new HashedWheelTimer(r -> {
        Thread thread = new Thread(r, "feat-session-timer");
        thread.setDaemon(true);
        return thread;
    }, 1000, 64);


    /**
     * 获取或创建会话单元
     *
     * @param request HTTP请求对象
     * @param create  是否创建新会话的标志
     * @return 会话单元，如果不存在且不创建则返回null
     */
    public Session getSession(HttpRequest request, boolean create) {
        String sessionId = getSessionId(request);
        MemorySession session = null;
        if (FeatUtils.isNotBlank(sessionId)) {
            session = sessions.get(sessionId);
        }
        if (session != null) {
            session.updateTimeoutTask();
            return session;
        } else if (!create) {
            return null;
        }
        session = new MemorySession(request, timer) {
            @Override
            public void invalidate() {
                sessions.remove(getSessionId());
                super.invalidate();
            }
        };
        responseSessionCookie(request, session.getSessionId());
        session.setTimeout(sessionOptions.getTimeout());
        sessions.put(session.getSessionId(), session);
        return session;
    }
}
