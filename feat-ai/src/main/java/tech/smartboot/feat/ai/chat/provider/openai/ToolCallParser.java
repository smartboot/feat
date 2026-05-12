package tech.smartboot.feat.ai.chat.provider.openai;

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.core.common.FeatUtils;

/**
 * OpenAI 工具调用解析器
 * <p>
 * 处理 OpenAI 特定的 tool_calls 格式，转换为通用 ToolCall 结构。
 * </p>
 *
 * <h3>解析逻辑：</h3>
 * <ol>
 *   <li>首次收到 tool_calls 分片时初始化</li>
 *   <li>累积 function.arguments 字符串</li>
 *   <li>最终转换为通用 ToolCall 格式</li>
 * </ol>
 *
 * @author Feat Team
 * @see OpenAiProvider OpenAI API 实现
 */
public class ToolCallParser {
    /**
     * 工具调用唯一标识
     */
    private String id;
    /**
     * 调用类型
     */
    private String type;
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
     * 获取调用类型
     *
     * @return 调用类型
     */
    public String getType() {
        return type;
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
     * 获取调用索引
     *
     * @return 调用索引
     */
    public int getIndex() {
        return index;
    }

    /**
     * 解析 toolCallObj 并更新内部状态
     *
     * @param toolCallObj OpenAI 格式的 tool_call JSON 对象
     */
    public void parse(JSONObject toolCallObj) {
        // 更新基础字段
        if (FeatUtils.isBlank(id)) {
            this.id = toolCallObj.getString("id");
        }
        if (FeatUtils.isBlank(type)) {
            this.type = toolCallObj.getString("type");
        }

        // 更新函数信息
        if (!toolCallObj.containsKey("function")) {
            return;
        }
        JSONObject functionObj = toolCallObj.getJSONObject("function");
        String functionName = functionObj.getString("name");
        if (FeatUtils.isNotBlank(functionName)) {
            this.name = functionName;
        }
        String functionArgs = functionObj.getString("arguments");
        if (FeatUtils.isNotBlank(functionArgs)) {
            argumentsBuilder.append(functionArgs);
        }
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
