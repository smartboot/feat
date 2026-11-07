/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import com.alibaba.fastjson2.JSONObject;

/**
 * 执行事件（append-only）。
 * <p>
 * 事件流是 harness 的可观测底座：resume（重放重建状态）、replay（换模型对比）、
 * eval（直接算指标）三个能力都长在它上面，不需要应用额外埋点。
 * </p>
 * <p>
 * 设计上刻意保持"扁平 + JSONObject 载荷"：一行一条 JSONL，落盘即可用，
 * 不引入额外序列化框架，也便于用命令行工具直接检索。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class AgentEvent {

    public enum Type {
        RUN_STARTED,
        STEP_STARTED,
        MODEL_RESPONDED,
        TOOL_INVOKED,
        POLICY_DECIDED,
        TOOL_COMPLETED,
        CONTEXT_COMPACTED,
        /**
         * 发送给模型的前缀发生变化。只在变化时记录——"变化次数"就是缓存命中的反向指标
         */
        CONTEXT_ASSEMBLED,
        PROTOCOL_FALLBACK,
        VERIFY_DECIDED,
        RUN_FINISHED
    }

    private final Type type;
    private final String sessionId;
    private final int stepIndex;
    private final long timestamp;
    private final JSONObject payload;
    private long seq;

    public AgentEvent(Type type, String sessionId, int stepIndex, JSONObject payload) {
        this.type = type;
        this.sessionId = sessionId;
        this.stepIndex = stepIndex;
        this.timestamp = System.currentTimeMillis();
        this.payload = payload == null ? new JSONObject() : payload;
    }

    public static AgentEvent of(Type type, String sessionId, int stepIndex) {
        return new AgentEvent(type, sessionId, stepIndex, null);
    }

    public static AgentEvent of(Type type, String sessionId, int stepIndex, JSONObject payload) {
        return new AgentEvent(type, sessionId, stepIndex, payload);
    }

    public AgentEvent put(String key, Object value) {
        payload.put(key, value);
        return this;
    }

    public Type getType() {
        return type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public long getSeq() {
        return seq;
    }

    /**
     * 序号由 {@link SessionStore} 在追加时统一分配，保证同一会话内单调递增
     */
    public void setSeq(long seq) {
        this.seq = seq;
    }
}
