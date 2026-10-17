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

import io.github.smartboot.socket.timer.HashedWheelTimer;
import io.github.smartboot.socket.timer.TimerTask;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于本地内存的会话管理器
 * <p>
 * 仅负责会话的存储与超时调度，Cookie的解析、去重与下发由基类模板方法统一管理
 * </p>
 *
 * @author 三刀
 * @version v1.0 12/16/25
 */
public class LocalSessionManager extends SessionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalSessionManager.class);
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


    @Override
    protected Session getSession(String sessionId) {
        MemorySession session = sessions.get(sessionId);
        if (session != null) {
            //刷新最近访问时间
            session.setLatestAccessTime(FeatUtils.currentTime().getTime());
        }
        return session;
    }

    @Override
    protected Session createSession() {
        MemorySession session = new MemorySession() {
            /**
             * 定时任务实例
             * <p>
             * 用于管理会话超时的定时任务
             * </p>
             */
            private TimerTask timerTask;

            /**
             * 更新超时任务
             * <p>
             * 取消当前超时任务并根据会话最大存活时间重新设置新的超时任务
             * </p>
             */
            synchronized void updateTimeoutTask() {
                pauseTimeoutTask();
                if (getTimeout() <= 0) {
                    return;
                }
                timerTask = timer.schedule(new Runnable() {
                    @Override
                    public void run() {
                        //getTimeout为纯读取（不刷新最近访问时间），
                        //若此处刷新访问时间，剩余时间将永远等于完整超时值，会话永不失效
                        long remainingTime = getTimeout() * 1000L - (FeatUtils.currentTime().getTime() - getLatestAccessTime());
                        if (remainingTime > 0) {
                            timerTask = timer.schedule(this, remainingTime, TimeUnit.MILLISECONDS);
                        } else {
                            LOGGER.info("sessionId:{} has be expired, lastAccessedTime:{} ,maxInactiveInterval:{}", getSessionId());
                            invalidate();
                        }
                    }
                }, getTimeout(), TimeUnit.SECONDS);
            }

            @Override
            public void setTimeout(int maxAge) {
                super.setTimeout(maxAge);
                updateTimeoutTask();
            }

            /**
             * 暂停超时任务
             * <p>
             * 取消当前的超时定时任务
             * </p>
             */
            void pauseTimeoutTask() {
                if (timerTask != null) {
                    timerTask.cancel();
                    timerTask = null;
                }
            }

            @Override
            public void invalidate() {
                pauseTimeoutTask();
                sessions.remove(getSessionId());
                super.invalidate();
            }
        };
        //设置超时并启动超时调度
        session.setTimeout(sessionOptions.getTimeout());
        sessions.put(session.getSessionId(), session);
        return session;
    }
}
