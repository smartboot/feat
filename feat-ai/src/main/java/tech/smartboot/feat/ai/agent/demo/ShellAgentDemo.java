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
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.chat.entity.StreamResponseCallback;

import java.util.concurrent.ExecutionException;

/**
 * @author 三刀
 * @version v1.0 9/30/25
 */
public class ShellAgentDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        FeatAgent agent = new FeatAgent(options -> {
            options.name("")
                    .model(ChatModelVendor.GiteeAI.Qwen3_32B)
                    .prompt("你是一个编程高手");
        });
        agent.addTool(new ShellInputTool());
        agent.execute("开发一个五子棋游戏", (StreamResponseCallback) System.out::print);
    }
}
