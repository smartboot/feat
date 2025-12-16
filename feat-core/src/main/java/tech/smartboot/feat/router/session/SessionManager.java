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
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.Session;

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
}
