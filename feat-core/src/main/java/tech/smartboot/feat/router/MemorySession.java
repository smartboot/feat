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
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.core.server.Session;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0
 */
class MemorySession implements Session {

    private final static byte[] DEFAULT_BYTES = ("feat:" + ServerOptions.VERSION).getBytes();
    private final static int maskLength = 4;
    private static final String MAGIC_NUMBER = "sf";
    private volatile int maxAge;
    private final String sessionId;
    private final HttpRequest request;
    private final Map<String, String> attributes = new HashMap<>();
    /**
     * 是否失效
     */
    private boolean invalid = false;

    MemorySession(HttpRequest request) {
        this.sessionId = createSessionId();
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
                if (!StringUtils.startsWith(preValue, DEFAULT_SESSION_COOKIE_NAME + "=")) {
                    request.getResponse().addHeader(HeaderName.SET_COOKIE, preValue);
                }
            });
        }
    }

    private String createSessionId() {
        Random random = new Random();
        //掩码+固定前缀+时间戳
        byte[] bytes = new byte[maskLength + DEFAULT_BYTES.length + Integer.BYTES];

        for (int i = 0; i < maskLength; ) {
            for (int rnd = random.nextInt(), n = Math.min(maskLength - i, Integer.SIZE / Byte.SIZE); n-- > 0; rnd >>= Byte.SIZE)
                bytes[i++] = (byte) rnd;
        }
        System.arraycopy(DEFAULT_BYTES, 0, bytes, maskLength, DEFAULT_BYTES.length);
        //将System.nanoTime()填充至bytes后四字节
        int time = (int) System.nanoTime();
        bytes[maskLength + DEFAULT_BYTES.length] = (byte) (time >>> 24);
        bytes[maskLength + DEFAULT_BYTES.length + 1] = (byte) (time >>> 16);
        bytes[maskLength + DEFAULT_BYTES.length + 2] = (byte) (time >>> 8);
        bytes[maskLength + DEFAULT_BYTES.length + 3] = (byte) (time);

        for (int i = maskLength; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ bytes[i % maskLength]);
        }

        return MAGIC_NUMBER + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
