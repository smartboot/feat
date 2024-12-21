/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: AbstractDecoder.java
 * Date: 2021-06-10
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package tech.smartboot.feat.core.server.decode;

import tech.smartboot.feat.core.server.HttpServerConfiguration;
import tech.smartboot.feat.core.server.impl.Request;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2021/6/10
 */
public abstract class AbstractDecoder implements Decoder {
    private final HttpServerConfiguration configuration;

    private final AbstractDecoder wafDecoder;

    public AbstractDecoder(HttpServerConfiguration configuration) {
        this(configuration, null);
    }

    public AbstractDecoder(HttpServerConfiguration configuration, AbstractDecoder wafDecoder) {
        this.configuration = configuration;
        this.wafDecoder = wafDecoder;
    }

    @Override
    public final Decoder decode(ByteBuffer byteBuffer, Request request) {
        Decoder decoder = decode0(byteBuffer, request);
        if (wafDecoder == null || decoder == this) {
            return decoder;
        }
        Decoder waf = wafDecoder.decode0(byteBuffer, request);
        return waf == null ? decoder : waf;
    }

    protected abstract Decoder decode0(ByteBuffer byteBuffer, Request request);

    public HttpServerConfiguration getConfiguration() {
        return configuration;
    }
}
