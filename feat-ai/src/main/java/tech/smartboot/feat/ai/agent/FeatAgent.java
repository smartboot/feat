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
                        .system(options.systemPrompt())
                        .model(options.getVendor()));
        model.chatStream(input, callback);
    }

    public AgentMemory getMemory() {
        return options.getMemory();
    }

}