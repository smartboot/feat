/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.chat;

import tech.smartboot.feat.ai.Vendor;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public abstract class ChatModelVendor extends Vendor {

    public static class GiteeAI extends ChatModelVendor {
        public static final ChatModelVendor DeepSeek_R1 = new GiteeAI("DeepSeek-R1");
        public static final ChatModelVendor DeepSeek_R1_Distill_Qwen_32B = new GiteeAI("DeepSeek-R1-Distill-Qwen-32B");
        public static final ChatModelVendor Qwen2_5_72B_Instruct = new GiteeAI("Qwen2.5-72B-Instruct");
        public static final ChatModelVendor Qwen2_5_32B_Instruct = new GiteeAI("Qwen2.5-32B-Instruct");
        public static final ChatModelVendor Qwen3_4B = new GiteeAI("Qwen3-4B");

        GiteeAI(String model) {
            super("https://ai.gitee.com/v1/", model);
        }
    }

    public static class Ollama extends ChatModelVendor {
        public static final ChatModelVendor Qwen2_5_05B = new Ollama("qwen2.5:0.5b");
        public static final ChatModelVendor Qwen2_5_3B = new Ollama("qwen2.5:3b");

        Ollama(String model) {
            super("http://localhost:11434/v1", model);
        }
    }

    ChatModelVendor(String baseUrl, String model) {
        super(baseUrl, model);
    }
}
