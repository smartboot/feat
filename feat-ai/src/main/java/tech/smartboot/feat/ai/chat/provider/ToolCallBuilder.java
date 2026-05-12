/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *   without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat.provider;

import tech.smartboot.feat.ai.chat.entity.ToolRequest;

/**
 * 工具调用构建器
 * <p>
 * 用于逐步构建 {@link ToolRequest} 对象，特别适用于流式响应场景。
 * 各 Provider 使用此类累积工具调用的各个字段，最终构建成完整的 ToolCall 对象。
 * </p>
 *
 * <h3>设计要点：</h3>
 * <ul>
 *   <li><b>构建器模式</b>：逐步设置字段，最终通过 build 方法生成对象</li>
 *   <li><b>参数累积</b>：通过 {@link StringBuilder} 累积流式传输的参数片段</li>
 *   <li><b>延迟构建</b>：通过 {@link #toToolCall()} 方法统一构建最终对象</li>
 * </ul>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * // 创建构建器实例
 * ToolCallBuilder builder = new ToolCallBuilder(index);
 *
 * // 逐步设置字段
 * builder.setId("tool_123");
 * builder.setName("getWeather");
 * builder.appendArguments("{\"city\": \"Beijing\"}");
 *
 * // 构建通用 ToolCall
 * ToolCall toolCall = builder.toToolCall();
 * }</pre>
 *
 * @author Feat Team
 * @see ToolRequest 通用工具调用结构
 * @see Provider AI Provider 基类
 */
public class ToolCallBuilder {
    /**
     * 工具调用唯一标识
     */
    private String id;

    /**
     * 函数/工具名称
     */
    private String name;

    /**
     * 参数累积器
     * <p>用于累积流式传输的参数 JSON 片段</p>
     */
    private final StringBuilder argumentsBuilder = new StringBuilder();

    /**
     * 调用索引
     * <p>标识同一响应中多个工具调用的顺序</p>
     */
    private final int index;

    /**
     * 构造工具调用构建器
     *
     * @param index 调用索引
     */
    public ToolCallBuilder(int index) {
        this.index = index;
    }

    /**
     * 获取工具调用唯一标识
     *
     * @return 工具调用 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 设置工具调用唯一标识
     *
     * @param id 工具调用 ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取函数/工具名称
     *
     * @return 函数名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置函数/工具名称
     *
     * @param name 函数名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取调用索引
     *
     * @return 调用索引
     */
    public int getIndex() {
        return index;
    }

    /**
     * 追加参数片段
     * <p>用于流式响应中累积参数 JSON 片段</p>
     *
     * @param partialJson 参数 JSON 片段
     */
    public void appendArguments(String partialJson) {
        if (partialJson != null) {
            argumentsBuilder.append(partialJson);
        }
    }

    /**
     * 获取已累积的参数
     *
     * @return 累积的参数字符串，如果没有则返回 "{}"
     */
    protected String getArguments() {
        if (argumentsBuilder.length() > 0) {
            return argumentsBuilder.toString();
        }
        return "{}";
    }

    /**
     * 转换为通用 ToolCall 结构
     * <p>将解析器中的数据转换为统一的 ToolCall 对象</p>
     *
     * @return 通用 ToolCall 对象
     */
    public ToolRequest toToolCall() {
        ToolRequest toolCall = new ToolRequest();
        toolCall.setIndex(index);
        toolCall.setId(id);
        toolCall.setName(name);
        toolCall.setArguments(getArguments());
        return toolCall;
    }
}
