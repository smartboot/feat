/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.agent.demo;

import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.agent.tool.ShellInputTool;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 9/30/25
 */
public class ShellAgentDemo {
    public static void main(String[] args) {
        FeatAgent agent = new FeatAgent(options -> {
            options.prompt(PromptTemplate.GENERAL_AGENT_PROMPT);
        });
        agent.addTool(new ShellInputTool());
        System.out.println(agent.getToolsPrompts());
        Map<String, String> input = new HashMap<>();
        input.put(PromptTemplate.PARAM_SYSTEM_PROMPT, "");
        input.put("query", "提供一份旅游攻略");
        input.put("tools","");
        agent.execute(input, new StreamResponseCallback() {
            @Override
            public void onStreamResponse(String content) {
                System.out.print(content);
            }
        });
    }
}
