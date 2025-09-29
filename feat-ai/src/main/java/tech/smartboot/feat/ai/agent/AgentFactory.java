/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.memory.AgentMemory;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatModelVendor;

import java.util.function.Consumer;

/**
 * Agent工厂类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class AgentFactory {
    
    /**
     * 创建一个基于指定模型的简单Agent
     *
     * @param name   Agent名称
     * @param vendor 模型供应商
     * @return SimpleAgent实例
     */
    public static SimpleAgent createSimpleAgent(String name, ChatModelVendor vendor) {
        ChatModel chatModel = FeatAI.chatModel(options -> options.model(vendor));
        return new SimpleAgent(name, chatModel);
    }
    
    /**
     * 创建一个基于指定模型的简单Agent
     *
     * @param name     Agent名称
     * @param consumer ChatModel配置消费者
     * @return SimpleAgent实例
     */
    public static SimpleAgent createSimpleAgent(String name, Consumer<ChatModel> consumer) {
        ChatModel chatModel = FeatAI.chatModel(options -> options.model(ChatModelVendor.GiteeAI.Qwen3_235B_A22B_Instruct_2507));
        return new SimpleAgent(name, chatModel);
    }
    
    /**
     * 创建一个自定义Agent
     *
     * @param name   Agent名称
     * @param vendor 模型供应商
     * @param memory Agent记忆
     * @return SimpleAgent实例
     */
    public static SimpleAgent createSimpleAgent(String name, ChatModelVendor vendor, AgentMemory memory) {
        ChatModel chatModel = FeatAI.chatModel(options -> options.model(vendor));
        return new SimpleAgent(name, chatModel, memory);
    }
}