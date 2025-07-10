/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.router;

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
 * @author 三刀(zhengjunweimail@163.com)
 * @version v1.0
 */
class MemorySession implements Session {
    private volatile int maxAge;
    private final String sessionId;
    private final HttpRequest request;
    private final Map<String, String> attributes = new HashMap<>();
    /**
     * 是否失效
     */
    private boolean invalid = false;

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

    public String getSessionId() {
        return sessionId;
    }

    public synchronized void put(String key, String value) {
        checkValid();
        attributes.put(key, value);
    }

    public synchronized String get(String key) {
        checkValid();
        return attributes.get(key);
    }

    public int getMaxAge() {
        checkValid();
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        checkValid();
        this.maxAge = maxAge;
    }

    private void checkValid() {
        if (invalid) {
            throw new FeatException("Session is invalid");
        }
    }

    /**
     * 将当前Session失效掉
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
