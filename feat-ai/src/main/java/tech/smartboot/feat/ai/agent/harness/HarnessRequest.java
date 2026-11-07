/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 一次 harness 运行的入参。
 * <p>
 * {@code sessionId} 是事件流的聚合根：同一个 sessionId 的多次运行共享一条 transcript，
 * resume 与 replay 都基于它定位。调用方应保证同一会话内 sessionId 稳定。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class HarnessRequest {
    private final String sessionId;
    private final String input;
    private final Map<String, String> attributes;

    private HarnessRequest(String sessionId, String input, Map<String, String> attributes) {
        this.sessionId = sessionId;
        this.input = input;
        this.attributes = attributes == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, String>(attributes));
    }

    public static HarnessRequest of(String sessionId, String input) {
        return new HarnessRequest(sessionId, input, null);
    }

    public static HarnessRequest of(String sessionId, String input, Map<String, String> attributes) {
        return new HarnessRequest(sessionId, input, attributes);
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInput() {
        return input;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
