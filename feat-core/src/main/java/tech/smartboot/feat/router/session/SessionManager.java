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

import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HeaderName;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;

import java.util.Collection;

/**
 * 会话管理器基类
 * <p>
 * 采用模板方法编排完整的会话获取流程：Cookie解析 → 会话查找 → 会话创建 → Cookie去重与下发。
 * 子类只需实现存储能力（{@link #getSession(String)}与{@link #createSession()}），
 * 不接触请求/响应对象——Cookie写入时机由框架统一保证，实现类无法遗漏、也无法污染响应。
 * </p>
 *
 * @author 三刀
 * @version v1.0 12/16/25
 */
public abstract class SessionManager {
    /**
     * 会话的配置选项
     * <p>
     * 包含会话相关的配置参数，如最大存活时间等
     * </p>
     */
    protected final SessionOptions sessionOptions = new SessionOptions();

    /**
     * 获取或创建会话
     * <p>
     * 模板方法：先从请求Cookie解析会话ID并查找，命中则直接返回；
     * 未命中且允许创建时调用{@link #createSession()}创建，
     * 并在当前响应中完成会话Cookie的去重与下发（仅此时刻操作当前响应是安全的）。
     * </p>
     *
     * @param request HTTP请求对象
     * @param create  未命中时是否创建新会话
     * @return 会话，未命中且不创建时返回null
     */
    public final Session getSession(HttpRequest request, boolean create) {
        String sessionId = getSessionId(request);
        if (FeatUtils.isNotBlank(sessionId)) {
            Session session = getSession(sessionId);
            if (session != null) {
                return session;
            }
        }
        if (!create) {
            return null;
        }
        Session session = createSession();
        //创建时刻的当前响应：先清理已存在的会话Cookie再下发新Cookie，确保仅一个有效会话Cookie
        removeSessionCookie(request);
        responseSessionCookie(request, session.getSessionId());
        return session;
    }

    /**
     * 按会话ID查找会话
     * <p>
     * 实现类负责存储查询，以及存储侧的访问时间刷新等逻辑
     * </p>
     *
     * @param sessionId 会话ID
     * @return 会话，不存在时返回null
     */
    protected abstract Session getSession(String sessionId);

    /**
     * 创建新会话
     * <p>
     * 注意：会话是长生命周期对象，而请求/响应对象随连接复用，
     * 实现返回的会话不得持有任何请求/响应引用，否则失效操作会污染其他请求的响应
     * </p>
     *
     * @return 新创建的会话
     */
    protected abstract Session createSession();

    public final SessionOptions getOptions() {
        return sessionOptions;
    }

    /**
     * 从请求Cookie中解析会话ID
     */
    private String getSessionId(HttpRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (Session.DEFAULT_SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 在当前响应中下发会话Cookie
     */
    private void responseSessionCookie(HttpRequest request, String sessionId) {
        Cookie cookie = new Cookie(Session.DEFAULT_SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(getOptions().getTimeout());
        request.getResponse().addCookie(cookie);
    }

    /**
     * 移除当前响应中已存在的会话Cookie
     * <p>
     * 在本次响应中查找是否已经设置了会话Cookie，如果存在则将其移除，
     * 避免重复设置会话Cookie
     * </p>
     */
    private static void removeSessionCookie(HttpRequest request) {
        Collection<String> preValues = request.getResponse().getHeaders(HeaderName.SET_COOKIE);
        //如果在本次请求中已经为session设置过Cookie了，那么需要将本次设置的Cookie移除掉
        if (FeatUtils.isNotEmpty(preValues)) {
            request.getResponse().setHeader(HeaderName.SET_COOKIE, null);
            preValues.forEach(preValue -> {
                if (!FeatUtils.startsWith(preValue, Session.DEFAULT_SESSION_COOKIE_NAME + "=")) {
                    request.getResponse().addHeader(HeaderName.SET_COOKIE, preValue);
                }
            });
        }
    }
}
