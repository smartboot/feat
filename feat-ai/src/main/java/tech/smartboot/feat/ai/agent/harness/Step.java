/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.chat.entity.Usage;

/**
 * 结构化的一步。
 * <p>
 * 相比 {@code ToolCaller} 的"thought/action/actionInput 三段字符串"，这里显式区分
 * {@link Type#FINAL}、{@link Type#TOOL_CALL}、{@link Type#THINK}，避免用哨兵值
 * （{@code ToolCaller.FINAL_ANSWER}）表达控制流，也让事件流可以直接序列化落盘。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public final class Step {

    public enum Type {
        /**
         * 最终答案，run 结束
         */
        FINAL,
        /**
         * 请求调用工具
         */
        TOOL_CALL,
        /**
         * 只有推理、没有行动也没有结论，通常意味着模型犹豫，需要 harness 纠偏
         */
        THINK
    }

    private final Type type;
    private final String thought;
    private final String content;
    private final String toolCallId;
    private final String toolName;
    private final JSONObject arguments;
    private final Usage usage;

    private Step(Type type, String thought, String content, String toolCallId,
                 String toolName, JSONObject arguments, Usage usage) {
        this.type = type;
        this.thought = thought;
        this.content = content;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.arguments = arguments;
        this.usage = usage;
    }

    public static Step finalAnswer(String thought, String content, Usage usage) {
        return new Step(Type.FINAL, thought, content, null, null, null, usage);
    }

    public static Step toolCall(String toolCallId, String toolName, JSONObject arguments, String thought, Usage usage) {
        return new Step(Type.TOOL_CALL, thought, null, toolCallId, toolName, arguments, usage);
    }

    public static Step think(String thought, Usage usage) {
        return new Step(Type.THINK, thought, null, null, null, null, usage);
    }

    public Step withUsage(Usage usage) {
        return new Step(type, thought, content, toolCallId, toolName, arguments, usage);
    }

    public Type getType() {
        return type;
    }

    public String getThought() {
        return thought;
    }

    public String getContent() {
        return content;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public JSONObject getArguments() {
        return arguments;
    }

    public Usage getUsage() {
        return usage;
    }

    public int promptTokens() {
        return usage == null ? 0 : usage.getPromptTokens();
    }

    public int totalTokens() {
        return usage == null ? 0 : usage.getTotalTokens();
    }
}
