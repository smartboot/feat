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
import tech.smartboot.feat.core.common.enums.HeaderNameEnum;
import tech.smartboot.feat.core.common.utils.CollectionUtils;
import tech.smartboot.feat.core.common.utils.StringUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.ServerOptions;
import tech.smartboot.feat.router.Router;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author 三刀(zhengjunweimail @ 163.com)
 * @version v1.0
 */
public class Session {
    public static final String DEFAULT_SESSION_COOKIE_NAME = "FEAT_SESSION";
    private final static byte[] DEFAULT_BYTES = ("feat:" + ServerOptions.VERSION).getBytes();
    private final static int maskLength = 4;
    private static final String MAGIC_NUMBER = "sf";
    private final long creationTime = System.currentTimeMillis();
    private final String sessionId;
    private final HttpRequest request;
    private final Map<String, Object> attributes = new HashMap<>();

    public Session(Router router, HttpRequest request) {
        this.sessionId = createSessionId();
        this.request = request;
        Cookie cookie = new Cookie(DEFAULT_SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(1800);
        request.getResponse().addCookie(cookie);
    }

    public String getSessionId() {
        return sessionId;
    }

    public synchronized void put(String key, Object value) {
        attributes.put(key, value);
    }

    public synchronized Object get(String key) {
        return attributes.get(key);
    }

    /**
     * 将当前Session失效掉
     */
    public void invalidate() {
        Collection<String> preValues = request.getResponse().getHeaders(HeaderNameEnum.SET_COOKIE.getName());
        //如果在本次请求中已经为session设置过Cookie了，那么需要将本次设置的Cookie移除掉
        if (CollectionUtils.isNotEmpty(preValues)) {
            request.getResponse().setHeader(HeaderNameEnum.SET_COOKIE.getName(), null);
            preValues.forEach(preValue -> {
                if (!StringUtils.startsWith(preValue, DEFAULT_SESSION_COOKIE_NAME + "=")) {
                    request.getResponse().addHeader(HeaderNameEnum.SET_COOKIE.getName(), preValue);
                }
            });
        }
        Cookie cookie = new Cookie(DEFAULT_SESSION_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        request.getResponse().addCookie(cookie);
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
