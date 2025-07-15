/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.client.stream;

import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.impl.HttpResponseImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class StringStream implements Stream {
    private final ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();


    @Override
    public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
        try {
            bodyStream.write(bytes);
            if (end) {
                ((HttpResponseImpl) response).setBody(bodyStream.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
