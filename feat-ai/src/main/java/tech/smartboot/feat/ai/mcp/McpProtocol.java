/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.mcp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MCP 协议版本与能力常量。
 * <p>
 * 同时保留多个受支持版本是为了平滑升级：客户端按 {@link #LATEST} 发起 initialize，
 * 若对端只支持旧版本会回写旧版本号，此时客户端仍可继续通信而不是直接断开。
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0
 */
public final class McpProtocol {
    public static final String V2025_06_18 = "2025-06-18";
    public static final String V2025_11_25 = "2025-11-25";

    /**
     * 本实现对外声明的最新协议版本。
     */
    public static final String LATEST = V2025_11_25;

    /**
     * 受支持的协议版本，按由新到旧排列。
     */
    public static final List<String> SUPPORTED = Collections.unmodifiableList(
            Arrays.asList(V2025_11_25, V2025_06_18));

    private McpProtocol() {
    }

    public static boolean isSupported(String version) {
        return version != null && SUPPORTED.contains(version);
    }
}
