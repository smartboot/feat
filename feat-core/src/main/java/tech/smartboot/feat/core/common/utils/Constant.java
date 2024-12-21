/*******************************************************************************
 * Copyright (c) 2024, tech.smartboot. All rights reserved.
 * project name: feat
 * file name: Consts.java
 * Date: 2020-01-01
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

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

    /**
     * Colon ':'
     */
    byte COLON = 58;

    /**
     * Dash '-'
     */
    byte DASH = 45;


    /**
     * Horizontal space
     */
    char SP_CHAR = (char) SP;

    char COLON_CHAR = COLON;

    byte[] CRLF_BYTES = {Constant.CR, Constant.LF};

    byte[] CRLF_CRLF_BYTES = {Constant.CR, Constant.LF, Constant.CR, Constant.LF};

    String CRLF = "\r\n";

    byte[] CHUNKED_END_BYTES = "0\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    byte[] EMPTY_BYTES = {};
}