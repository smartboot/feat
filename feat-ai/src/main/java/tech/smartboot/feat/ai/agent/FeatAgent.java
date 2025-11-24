package tech.smartboot.feat.ai.agent;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.memory.AgentMemory;
import tech.smartboot.feat.ai.agent.tool.ToolExecutionManager;
import tech.smartboot.feat.ai.agent.tool.ToolExecutor;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.ResponseMessage;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    /**
     * Agent状态
     */
    protected AgentState state = AgentState.IDLE;

    @Override
    public void addTool(ToolExecutor executor) {
        toolExecutors.put(executor.getName(), executor);
        toolExecutionManager.addToolExecutor(executor.getName(), executor);
        logger.info("添加工具执行器: " + executor.getName());
    }


    public void callStream(List<Message> messages, StreamResponseCallback callback) {
        // 创建ChatModel实例
        ChatModel model = FeatAI.chatModel(chatOptions ->
                chatOptions
                        .noThink(true)
                        .debug(false)
                        .system(options.systemPrompt())
                        .model(options.getVendor()));
        model.chatStream(messages, callback);
    }

    public CompletableFuture<ResponseMessage> call(List<Message> messages) {
        // 创建ChatModel实例
        ChatModel model = FeatAI.chatModel(chatOptions ->
                chatOptions
                        .noThink(true)
                        .debug(false)
                        .system(options.systemPrompt())
                        .model(options.getVendor()));
        return model.chat(messages);
    }

    public AgentMemory getMemory() {
        return options.getMemory();
    }

    /**
     * 获取Agent当前状态
     *
     * @return Agent状态
     */
    public AgentState getState() {
        return state;
    }

    /**
     * 设置Agent状态
     *
     * @param state 新状态
     */
    protected void setState(AgentState state) {
        this.state = state;
        logger.info("Agent状态变更: " + state);
    }
}