package tech.smartboot.feat.ai.agent;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.memory.AgentMemory;
import tech.smartboot.feat.ai.agent.tool.ToolExecutionManager;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Agent抽象实现类，基于ReAct（Reasoning + Acting）范式
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.1
 */
public abstract class FeatAgent implements Agent {
    protected final AgentOptions options = new AgentOptions();

    /**
     * 工具执行器映射
     */
    protected final Map<String, ToolExecutor> toolExecutors = new HashMap<>();

    /**
     * 工具执行管理器
     */
    protected final ToolExecutionManager toolExecutionManager = new ToolExecutionManager();

    /**
     * 日志记录器
     */
    protected static final Logger logger = LoggerFactory.getLogger(FeatAgent.class.getName());

    @Override
    public void addTool(ToolExecutor executor) {
        toolExecutors.put(executor.getName(), executor);
        toolExecutionManager.addToolExecutor(executor.getName(), executor);
        logger.info("添加工具执行器: " + executor.getName());
    }


    public void call(String input, StreamResponseCallback callback) {
        // 创建ChatModel实例
        ChatModel model = FeatAI.chatModel(chatOptions ->
                chatOptions
                        .noThink(true)
                        .debug(false)
                        .model(options.getVendor()));
        model.chatStream(input, callback);
    }

    /**
     * 获取ReAct模式的系统提示
     *
     * @return 系统提示字符串
     */
    private String getReActSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个名为 \"").append(options.getName()).append("\" 的AI助手，你的角色是 \"").append(options.getRoleName()).append("\"。\n")
                .append("你需要交替进行以下步骤来解决用户问题：\n")
                .append("1. Thought: 分析当前情况并制定行动计划\n")
                .append("2. Action: 使用工具执行操作，格式为 {{tool:tool_name(param1=\"value1\", param2=\"value2\")}}\n")
                .append("3. Observation: 观察工具执行结果并分析\n")
                .append("重复以上步骤直到问题得到解决。\n")
                .append("可用的工具包括：\n");

        for (ToolExecutor executor : toolExecutors.values()) {
            prompt.append("- ").append(executor.getName()).append(": ").append(executor.getDescription()).append("\n");
        }

        prompt.append("\n请严格按照指定格式调用工具，每次只能调用一个工具。");
        return prompt.toString();
    }


    public AgentMemory getMemory() {
        return options.getMemory();
    }

}