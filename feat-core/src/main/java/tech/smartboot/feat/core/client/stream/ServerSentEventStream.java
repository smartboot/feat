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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public abstract class ServerSentEventStream implements Stream {
    private static final int STATE_COLON = 1;
    private static final int STATE_COLON_RIGHT_TRIM = 2;
    private static final int STATE_LF = 3;
    private static final int STATE_END_CHECK = 4;
    public static final String DATA = "data";
    public static final String EVENT = "event";
    public static final String ID = "id";
    public static final String RETRY = "retry";
    public static final String DEFAULT_EVENT = "message";
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final Map<String, String> event = new HashMap<>();

    @Override
    public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
        if (baos.size() > 0) {
            baos.write(bytes);
            bytes = baos.toByteArray();
            baos.reset();
        }
        int keyPos = 0;
        int colonPos = -1;
        int valuePos = -1;

        int state = STATE_COLON;

        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            switch (state) {
                case STATE_COLON:
                    if (b == ':') {
                        colonPos = i;
                        state = STATE_COLON_RIGHT_TRIM;
                    }
                    break;
                case STATE_COLON_RIGHT_TRIM:
                    if (b != ' ') {
                        valuePos = i;
                        state = STATE_LF;
                    }
                    if (b != '\n') { //空value的情况
                        break;
                    }
                case STATE_LF:
                    if (b == '\n') {
                        event.put(new String(bytes, keyPos, colonPos - keyPos), new String(bytes, valuePos, i - valuePos));
                        keyPos = i + 1;
                        state = STATE_END_CHECK;
                    }
                    break;
                case STATE_END_CHECK:
                    if (b == '\n') {
                        keyPos = i + 1;
                        //结束
                        onEvent(response, event);
                        event.clear();
                    } else if (b == ':') { // comment
                        colonPos = i;
                        state = STATE_COLON_RIGHT_TRIM;
                    } else {
                        state = STATE_COLON;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + state);
            }
        }

        if (keyPos < bytes.length) {
            baos.write(bytes, keyPos, bytes.length - keyPos);
        }
    }

    public abstract void onEvent(HttpResponse httpResponse, Map<String, String> event) throws IOException;
}
