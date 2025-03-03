/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.demo;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.ModelMeta;
import tech.smartboot.feat.ai.chat.ChatModel;

import java.io.IOException;

public class ModelMetaDemo {
    public static void main(String[] args) throws IOException {
        ChatModel chatModel = FeatAI.chatModel(opts -> opts.model(ModelMeta.GITEE_AI_DeepSeek_R1_Distill_Qwen_32B));
        chatModel.chat("你好，请自我介绍一下。", rsp -> {
            System.out.println("rsp: " + rsp.getContent());
            System.out.println("usage: " + rsp.getUsage());
            chatModel.chat("我对你说的上一句话是什么？", rsp2 -> {
                System.out.println("rsp2: " + rsp2.getContent());
            });
        });
    }
}
