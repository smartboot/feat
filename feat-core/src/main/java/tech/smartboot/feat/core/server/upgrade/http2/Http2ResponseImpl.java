/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: HttpResponseImpl.java
 * Date: 2021-02-07
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.upgrade.http2;

import tech.smartboot.feat.core.common.Cookie;
import tech.smartboot.feat.core.server.impl.AbstractResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀
 * @version V1.0 , 2018/2/3
 */
public class Http2ResponseImpl extends AbstractResponse {
    private List<Cookie> cookies = Collections.emptyList();

    public Http2ResponseImpl(int streamId, Http2Endpoint httpRequest, boolean push) {
        outputStream = new Http2OutputStream(streamId, httpRequest, this, push);
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    @Override
    public void addCookie(Cookie cookie) {
        super.addCookie(cookie);
        List<Cookie> emptyList = Collections.emptyList();
        if (cookies == emptyList) {
            cookies = new ArrayList<>();
        }
        cookies.add(cookie);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closed = true;
        }
    }
}
