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

import com.alibaba.fastjson2.JSONObject;
import tech.smartboot.feat.ai.Vendor;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatModelVendor extends Vendor {
    private final PreRequest preRequest;
    private final boolean thinkSupport;
    private final boolean functionCallSupport;

    public static class GiteeAI extends ChatModelVendor {
        public static final ChatModelVendor DeepSeek_R1 = new GiteeAI("DeepSeek-R1", false, true);
        public static final ChatModelVendor DeepSeek_R1_Distill_Qwen_32B = new GiteeAI("DeepSeek-R1-Distill-Qwen-32B", false, false);
        public static final ChatModelVendor Qwen2_5_72B_Instruct = new GiteeAI("Qwen2.5-72B-Instruct", false, true);
        public static final ChatModelVendor Qwen2_5_32B_Instruct = new GiteeAI("Qwen2.5-32B-Instruct", false, false);
        public static final ChatModelVendor Qwen3_235B_A22B_Instruct_2507 = new GiteeAI("Qwen3-235B-A22B-Instruct-2507", false, true);
        public static final ChatModelVendor Qwen3_4B = new GiteeAI("Qwen3-4B", true, false);


        GiteeAI(String model, boolean thinkSupport, boolean functionCallSupport) {
            super("https://ai.gitee.com/v1/", model, thinkSupport, functionCallSupport, (chatModel, jsonObject) -> {
                if (chatModel.getOptions().isNoThink()) {
                    jsonObject.getJSONArray("messages").add(JSONObject.of("role", "user", "content", "/no_think"));
                }
//            if (jsonObject.containsKey("tools")) {
//                jsonObject.remove("tool_choice");
//            }
            });
        }
    }

    public static class Ollama extends ChatModelVendor {
        public static final ChatModelVendor Qwen2_5_05B = new Ollama("qwen2.5:0.5b", false, false);
        public static final ChatModelVendor Qwen2_5_3B = new Ollama("qwen2.5:3b", false, false);

        public static final ChatModelVendor Qwen3_06B = new Ollama("qwen3:0.6b", true, false);


        Ollama(String model, boolean thinkSupport, boolean functionCallSupport) {
            super("http://localhost:11434/v1", model, thinkSupport, functionCallSupport, (chatModel, jsonObject) -> {
                if (thinkSupport && chatModel.getOptions().isNoThink()) {
                    jsonObject.getJSONArray("messages").add(JSONObject.of("role", "user", "content", "/no_think"));
                }
            });
        }
    }

    ChatModelVendor(String baseUrl, String model, boolean thinkSupport, boolean functionCallSupport, PreRequest request) {
        super(baseUrl, model);
        this.preRequest = request;
        this.thinkSupport = thinkSupport;
        this.functionCallSupport = functionCallSupport;
    }

    public PreRequest getPreRequest() {
        return preRequest;
    }
}
