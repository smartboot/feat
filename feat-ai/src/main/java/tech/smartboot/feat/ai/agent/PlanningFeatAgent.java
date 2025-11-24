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

import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 11/15/25
 */
public class PlanningFeatAgent extends FeatAgent {

    @Override
    public String execute(String input) {
        Prompt prompt = new Prompt(PromptTemplate.loadPrompt("feat_agent_planner.tpl"));
        Map<String, String> data = new HashMap<>();
        data.put("date", new Date().toString());
        data.put("input", input);
//        data.put("sopPrompt", PromptTemplate.loadPrompt("feat_agent_executor.tpl"));
//        options.setSystemPrompt(prompt.prompt(data));

//        Prompt executorPrompt = new Prompt(PromptTemplate.loadPrompt("feat_agent_executor.tpl"));
//        Map<String, String> executorData = new HashMap<>();
//        executorData.put("input", input);

        List<Message> messages = new ArrayList<>();
        messages.add(Message.ofUser(prompt.prompt(data)));
//        messages.add(Message.ofUser(executorPrompt.prompt(executorData)));
        callStream(messages, new StreamResponseCallback() {
            @Override
            public void onStreamResponse(String content) {
                System.out.print(content);
            }
        });
        return "";
    }

    public static void main(String[] args) {
        PlanningFeatAgent agent = new PlanningFeatAgent();
        agent.execute("开发一个五子棋的游戏");
    }
}
