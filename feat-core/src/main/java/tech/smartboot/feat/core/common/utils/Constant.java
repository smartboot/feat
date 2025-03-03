/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.core.common.utils;

import java.nio.charset.StandardCharsets;

public interface Constant {
    int WS_DEFAULT_MAX_FRAME_SIZE = (1 << 15) - 1;
    int WS_PLAY_LOAD_126 = 126;
    int WS_PLAY_LOAD_127 = 127;

    /**
     * Post 最大长度
     */
    int maxBodySize = 2 * 1024 * 1024;

    String SCHEMA_HTTP = "http";
    String SCHEMA_HTTPS = "https";

    String SCHEMA_WS = "ws";
    String SCHEMA_WSS = "wss";
    /**
     * Horizontal space
     */
    byte SP = 32;

    /**
     * Carriage return
     */
    byte CR = 13;

    /**
     * Line feed character
     */
    byte LF = 10;

    byte[] CRLF_BYTES = {Constant.CR, Constant.LF};

    byte[] CRLF_CRLF_BYTES = {Constant.CR, Constant.LF, Constant.CR, Constant.LF};


    byte[] CHUNKED_END_BYTES = "0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    byte[] EMPTY_BYTES = {};
}