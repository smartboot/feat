/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp.server.handler;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.mcp.McpException;
import tech.smartboot.feat.core.common.FeatUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * list 类方法的游标分页。
 * <p>
 * 规范要求 cursor 对客户端不透明，这里用 Base64 包装偏移量，客户端不应解析其内容。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
final class Pagination {
    private Pagination() {
    }

    /**
     * 解析请求中的 cursor，返回起始偏移量。cursor 缺省时从 0 开始。
     */
    static int offset(JSONObject jsonObject, int total) {
        JSONObject params = jsonObject.getJSONObject("params");
        String cursor = params == null ? null : params.getString("cursor");
        if (FeatUtils.isBlank(cursor)) {
            return 0;
        }
        int offset;
        try {
            offset = Integer.parseInt(new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new McpException(McpException.INVALID_PARAMS, "invalid cursor");
        }
        if (offset < 0 || offset > total) {
            throw new McpException(McpException.INVALID_PARAMS, "invalid cursor");
        }
        return offset;
    }

    /**
     * 计算当前页的结束位置，用 long 运算避免大 pageSize 下的整型溢出。
     */
    static int end(int offset, int pageSize, int total) {
        return (int) Math.min((long) offset + pageSize, total);
    }

    /**
     * 计算下一页游标，已到末页时返回 null（响应中不输出 nextCursor 字段）。
     */
    static String nextCursor(int end, int total) {
        if (end >= total) {
            return null;
        }
        return Base64.getEncoder().encodeToString(String.valueOf(end).getBytes(StandardCharsets.UTF_8));
    }
}
