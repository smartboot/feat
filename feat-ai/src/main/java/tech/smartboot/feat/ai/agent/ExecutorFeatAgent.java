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

import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;
import tech.smartboot.feat.ai.chat.prompt.Prompt;
import tech.smartboot.feat.ai.chat.prompt.PromptTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 11/16/25
 */
public class ExecutorFeatAgent extends FeatAgent {
    @Override
    public String execute(String input) {
        Prompt prompt = new Prompt(PromptTemplate.loadPrompt("feat_agent_planner.tpl"));
        Map<String, String> data = new HashMap<>();
        data.put("date", new Date().toString());
        data.put("history_dialogue", input);
        options.setSystemPrompt(prompt.prompt(data));
        callStream(null, new StreamResponseCallback() {
            @Override
            public void onStreamResponse(String content) {
                System.out.print(content);
            }
        });
        return "";
    }
}
