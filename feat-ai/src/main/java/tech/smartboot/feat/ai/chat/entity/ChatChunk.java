/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.entity;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * 流式响应块类，表示AI模型流式输出中的一个增量数据块
 * <p>
 * 与 {@link ChatStreamListener#onStreamResponse(String)} 只暴露文本增量不同，
 * {@code ChatChunk} 携带了上游数据帧的完整元数据（id、model、finish_reason、
 * tool_calls 增量、usage 等），供网关、代理等场景原样转发，无需自行补齐字段。
 * </p>
 *
 * <h3>字段来源（OpenAI chunk 结构）：</h3>
 * <ul>
 *   <li>{@code id} / {@code model} / {@code finish_reason}：chunk 顶层字段</li>
 *   <li>{@code content} / {@code role} / {@code tool_calls}：choices[0].delta 内的字段</li>
 *   <li>{@code usage}：stream_options.include_usage 开启时随最后一个 chunk 返回</li>
 * </ul>
 *
 * <h3>Anthropic 映射：</h3>
 * <ul>
 *   <li>message_start 携带 id 与输入 token 用量</li>
 *   <li>content_block_delta 映射为 content / reasoningContent / toolCalls 增量</li>
 *   <li>message_delta 携带 finish_reason（由 stop_reason 映射）与输出 token 用量</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 * @see ChatStreamListener#onChunk(ChatChunk)
 */
public class ChatChunk {
    /**
     * 数据块标识，同一流式响应内保持不变
     */
    private String id;
    /**
     * 模型名称
     */
    private String model;
    /**
     * 消息角色，通常出现在首个数据块（如 assistant）
     */
    private String role;
    /**
     * 文本内容增量
     */
    private String content;
    /**
     * 推理内容增量
     */
    @JSONField(name = "reasoning_content")
    private String reasoningContent;
    /**
     * 工具调用增量，包含 index/id/name/参数片段等分片信息
     */
    @JSONField(name = "tool_calls")
    private List<ToolCall> toolCalls;
    /**
     * 结束原因（如 stop、tool_calls、length），通常出现在最后一个内容块
     */
    @JSONField(name = "finish_reason")
    private String finishReason;
    /**
     * Token 用量统计，上游在流末尾返回（OpenAI 需开启 include_usage）
     */
    private Usage usage;

    /**
     * 获取数据块标识
     *
     * @return 数据块标识
     */
    public String getId() {
        return id;
    }

    /**
     * 设置数据块标识
     *
     * @param id 数据块标识
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }

    /**
     * 设置模型名称
     *
     * @param model 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 获取消息角色
     *
     * @return 消息角色
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置消息角色
     *
     * @param role 消息角色
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取文本内容增量
     *
     * @return 文本内容增量
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置文本内容增量
     *
     * @param content 文本内容增量
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取推理内容增量
     *
     * @return 推理内容增量
     */
    public String getReasoningContent() {
        return reasoningContent;
    }

    /**
     * 设置推理内容增量
     *
     * @param reasoningContent 推理内容增量
     */
    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }

    /**
     * 获取工具调用增量列表
     *
     * @return 工具调用增量列表
     */
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    /**
     * 设置工具调用增量列表
     *
     * @param toolCalls 工具调用增量列表
     */
    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    /**
     * 获取结束原因
     *
     * @return 结束原因
     */
    public String getFinishReason() {
        return finishReason;
    }

    /**
     * 设置结束原因
     *
     * @param finishReason 结束原因
     */
    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    /**
     * 获取Token用量统计
     *
     * @return Token用量统计
     */
    public Usage getUsage() {
        return usage;
    }

    /**
     * 设置Token用量统计
     *
     * @param usage Token用量统计
     */
    public void setUsage(Usage usage) {
        this.usage = usage;
    }
}
