/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.cloud.mcp.client;

import tech.smartboot.feat.cloud.mcp.Request;
import tech.smartboot.feat.cloud.mcp.Response;

/**
 * @author 三刀
 * @version v1.0 7/9/25
 */
public class StreamableTransport extends Transport {
    public StreamableTransport(McpOptions options) {
        super(options);
    }

    @Override
    Response sendRequest(Request request) {
        return null;
    }
}
