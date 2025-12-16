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
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;

import java.util.Collection;

/**
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

    public abstract Session getSession(HttpRequest request, boolean create);

    public abstract void updateAccessTime(HttpRequest request);

    public final SessionOptions getOptions() {
        return sessionOptions;
    }

    protected String getSessionId(HttpRequest request) {
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

    protected void responseSessionCookie(HttpRequest request, String sessionId) {
        Cookie cookie = new Cookie(Session.DEFAULT_SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(getOptions().getMaxAge());
        request.getResponse().addCookie(cookie);
    }

    /**
     * 移除已存在的会话Cookie
     * <p>
     * 在本次响应中查找是否已经设置了会话Cookie，如果存在则将其移除，
     * 避免重复设置会话Cookie
     * </p>
     */
    public static void removeSessionCookie(HttpRequest request) {
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
