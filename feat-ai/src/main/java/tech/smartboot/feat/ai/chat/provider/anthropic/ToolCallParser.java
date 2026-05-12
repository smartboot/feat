package tech.smartboot.feat.ai.chat.provider.anthropic;

import tech.smartboot.feat.ai.chat.entity.ToolCall;

/**
 * Anthropic 工具调用解析器
 * <p>
 * 处理 Anthropic 特定的 tool_use 格式，转换为通用 ToolCall 结构。
 * </p>
 *
 * <h3>解析逻辑：</h3>
 * <ol>
 *   <li>content_block_start 事件：从 content_block 字段提取 id 和 name</li>
 *   <li>content_block_delta 事件：从 delta.partial_json 累积参数</li>
 *   <li>最终转换为通用 ToolCall 格式</li>
 * </ol>
 *
 * @author Feat Team
 * @see AnthropicProvider Anthropic API 实现
 */
public class ToolCallParser {
    /**
     * 工具调用唯一标识
     */
    private String id;
    /**
     * 函数名称
     */
    private String name;
    /**
     * 参数累积器
     */
    private final StringBuilder argumentsBuilder = new StringBuilder();
    /**
     * 调用索引
     */
    private final int index;

    /**
     * 构造工具调用解析器
     *
     * @param index 调用索引
     */
    public ToolCallParser(int index) {
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
     * 获取函数名称
     *
     * @return 函数名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置函数名称
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
     *
     * @param partialJson 参数 JSON 片段
     */
    public void appendArguments(String partialJson) {
        argumentsBuilder.append(partialJson);
    }

    /**
     * 转换为通用 ToolCall 结构
     *
     * @return 通用 ToolCall 对象
     */
    public ToolCall toToolCall() {
        ToolCall toolCall = new ToolCall();
        toolCall.setIndex(index);
        toolCall.setId(id);
        toolCall.setName(name);
        if (argumentsBuilder.length() > 0) {
            toolCall.setArguments(argumentsBuilder.toString());
        } else {
            toolCall.setArguments("{}");
        }
        return toolCall;
    }
}
