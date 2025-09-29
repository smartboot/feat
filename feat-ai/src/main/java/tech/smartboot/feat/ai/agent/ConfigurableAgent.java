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

/**
 * 可配置的Agent实现
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ConfigurableAgent extends AbstractAgent {
    
    public ConfigurableAgent(AgentOptions options) {
        super(options.getName(), 
              FeatAI.chatModel(chatOptions -> chatOptions.model(options.getVendor())), 
              options.getMemory());
    }
    
    public ConfigurableAgent(String name, ChatModel chatModel, AgentMemory memory) {
        super(name, chatModel, memory);
    }
}