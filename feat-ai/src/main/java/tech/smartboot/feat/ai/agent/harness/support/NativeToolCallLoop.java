/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 */

package tech.smartboot.feat.ai.agent.harness.support;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.agent.harness.AgentLoop;
import tech.smartboot.feat.ai.agent.harness.Step;
import tech.smartboot.feat.ai.agent.harness.StepContext;
import tech.smartboot.feat.ai.agent.harness.ToolSpec;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.Tool;
import tech.smartboot.feat.ai.chat.entity.ToolCall;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 原生 tool_calls 循环：推荐的默认协议实现。
 * <p>
 * 直接复用 {@code ChatModel.chat(List<Message>, List<Tool>)}（feat-ai 早已具备），
 * 结构由 provider 保证，不再从文本里"抠 JSON"，因此没有文本协议的解析失败率问题。
 * </p>
 * <p>
 * 已知限制（P1 补齐）：
 * <ul>
 *     <li>一轮返回多个 tool_call 时只消费第一个——并行工具调用需要 harness 侧支持批处理步；</li>
 *     <li>国产兼容网关若返回缺 {@code id} 的 tool_calls，需要降级到 {@code TextReActLoop}，
 *     降级判定放在本类内部，并写入 {@code PROTOCOL_FALLBACK} 事件。</li>
 * </ul>
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v0.1.0
 */
public class NativeToolCallLoop implements AgentLoop {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeToolCallLoop.class.getName());

    private final ChatOptions chatOptions;

    /**
     * schema 还原结果缓存：同一份 schema 每一步都会用到，没必要重复解析
     */
    private final Map<String, Tool> schemaCache = new HashMap<String, Tool>();

    public NativeToolCallLoop(Consumer<ChatOptions> customizer) {
        this.chatOptions = new ChatOptions();
        if (customizer != null) {
            customizer.accept(chatOptions);
        }
    }

    @Override
    public String name() {
        return "native-tool-call";
    }

    @Override
    public boolean nativeToolCalling() {
        return true;
    }

    @Override
    public CompletableFuture<Step> next(StepContext context) {
        if (chatOptions.getSystem() == null && context.getOptions().getSystemPrompt() != null) {
            chatOptions.system(context.getOptions().getSystemPrompt());
        }
        List<Message> window = context.getWindow();
        List<Tool> tools = toTools(context.getTools());
        ChatModel model = new ChatModel(chatOptions);

        return model.chat(window, tools).thenApply(response -> {
            if (!response.isSuccess()) {
                throw new FeatException("模型调用失败：" + response.getError());
            }
            return toStep(response);
        });
    }

    private Step toStep(ChatResponse response) {
        List<ToolCall> toolCalls = response.getToolCalls();
        if (toolCalls == null || toolCalls.isEmpty()) {
            return Step.finalAnswer(response.getReasoningContent(), response.getContent(), response.getUsage());
        }
        ToolCall toolCall = toolCalls.get(0);
        JSONObject arguments;
        try {
            arguments = toolCall.getArguments() == null || toolCall.getArguments().isEmpty()
                    ? new JSONObject()
                    : JSONObject.parseObject(toolCall.getArguments());
        } catch (Exception e) {
            // 参数不是合法 JSON：交给上层以 INVALID_ARGUMENT 回喂模型，而不是炸掉整个 run
            arguments = new JSONObject();
            arguments.put("__raw__", toolCall.getArguments());
        }
        return Step.toolCall(toolCall.getId(), toolCall.getName(), arguments,
                response.getReasoningContent(), response.getUsage());
    }

    private List<Tool> toTools(List<ToolSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Tool> tools = new ArrayList<Tool>(specs.size());
        for (ToolSpec spec : specs) {
            tools.add(toNativeTool(spec));
        }
        return tools;
    }

    /**
     * 还原工具的原生描述。
     * <p>
     * 优先用显式提供的 {@link ToolSpec#getNativeTool()}；只声明了 {@code parametersSchema}
     * 时必须把 schema 里的参数定义还原出来——否则模型只看到工具名和一句描述，参数全靠猜，
     * 原生协议"结构可靠"的优势就丢了一半。这类缺失不会报错，只会表现为模型反复传错参数，
     * 是最难排查的一类问题。
     * </p>
     */
    private Tool toNativeTool(ToolSpec spec) {
        if (spec.getNativeTool() != null) {
            return spec.getNativeTool();
        }
        Tool cached = schemaCache.get(spec.getName());
        if (cached != null) {
            return cached;
        }
        Tool tool = buildFromSchema(spec);
        schemaCache.put(spec.getName(), tool);
        return tool;
    }

    private Tool buildFromSchema(ToolSpec spec) {
        Tool tool = Tool.of(spec.getName()).description(spec.getDescription());
        String schemaText = spec.getParametersSchema();
        if (schemaText == null || schemaText.isEmpty()) {
            return tool;
        }
        try {
            JSONObject schema = JSONObject.parseObject(schemaText);
            JSONObject properties = schema.getJSONObject("properties");
            if (properties == null || properties.isEmpty()) {
                return tool;
            }
            Set<String> required = new HashSet<String>();
            JSONArray requiredArray = schema.getJSONArray("required");
            if (requiredArray != null) {
                for (int i = 0; i < requiredArray.size(); i++) {
                    required.add(requiredArray.getString(i));
                }
            }
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                JSONObject property = (JSONObject) entry.getValue();
                // 类型原样透传：provider 需要的是合法 JSON Schema 类型，不要擅自归一化
                String type = property.getString("type");
                tool.addParam(entry.getKey(), property.getString("description"),
                        type == null ? Tool.ParameterProperty.TYPE_STRING : type,
                        required.contains(entry.getKey()));
            }
        } catch (Exception e) {
            // schema 本身不合法：退回"只有名字和描述"，至少不阻断运行
            LOGGER.warn("工具 {} 的 parametersSchema 解析失败，参数定义将缺失：{}",
                    spec.getName(), e.getMessage());
        }
        return tool;
    }
}
