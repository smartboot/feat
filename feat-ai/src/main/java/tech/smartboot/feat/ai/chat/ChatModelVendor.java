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
import tech.smartboot.feat.ai.Options;
import tech.smartboot.feat.ai.Vendor;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.core.common.FeatUtils;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatModelVendor extends Vendor {
    private final PreRequest preRequest;
    private final boolean thinkSupport;
    private final boolean functionCallSupport;

    public static class GiteeAI extends ChatModelVendor {
        //暂时屏蔽，function call表现不如预期
        public static final ChatModelVendor DeepSeek_R1 = new GiteeAI("DeepSeek-R1", true, true, new PreRequest() {
            @Override
            public void preRequest(ChatModel chatModel, JSONObject jsonObject) {
                if (FeatUtils.isBlank(chatModel.getOptions().apiKey())) {
                    throw new IllegalArgumentException("apiKey is null, please set it via environment variable " + Options.ENV_API_KEY);
                }
                if (chatModel.getOptions().isNoThink()) {
                    jsonObject.getJSONArray("messages").add(0,JSONObject.of("role", Message.ROLE_SYSTEM, "content", "请直接给出最终答案，不允许展示任何思考过程、分析步骤或解释，仅返回结果。"));
                }
            }
        });
        public static final ChatModelVendor Kimi_K2_Instruct = new GiteeAI("Kimi-K2-Instruct", false, true);
//        public static final ChatModelVendor ERNIE_X1_Turbo = new GiteeAI("ERNIE-X1-Turbo", false, true, new PreRequest() {
//            @Override
//            public void preRequest(ChatModel chatModel, JSONObject jsonObject) {
//                if (chatModel.getOptions().isNoThink()) {
//                    jsonObject.getJSONArray("messages").add(0, JSONObject.of("role", Message.ROLE_SYSTEM, "content", "请直接给出最终答案，无需任何分析、解释或思考过程，保持回答简洁明了。"));
//                }
//            }
//        });
        public static final ChatModelVendor DeepSeek_R1_Distill_Qwen_32B = new GiteeAI("DeepSeek-R1-Distill-Qwen-32B", false, false);
        public static final ChatModelVendor Qwen2_5_72B_Instruct = new GiteeAI("Qwen2.5-72B-Instruct", false, true);
        public static final ChatModelVendor Qwen2_5_32B_Instruct = new GiteeAI("Qwen2.5-32B-Instruct", false, false);
        public static final ChatModelVendor Qwen3_235B_A22B_Instruct_2507 = new GiteeAI("Qwen3-235B-A22B-Instruct-2507", false, true);
        public static final ChatModelVendor Qwen3_4B = new GiteeAI("Qwen3-4B", true, false);

        GiteeAI(String model, boolean thinkSupport, boolean functionCallSupport, PreRequest request) {
            super("https://ai.gitee.com/v1/", model, thinkSupport, functionCallSupport, request);
        }

        GiteeAI(String model, boolean thinkSupport, boolean functionCallSupport) {
            this(model, thinkSupport, functionCallSupport, (chatModel, jsonObject) -> {
                if (FeatUtils.isBlank(chatModel.getOptions().apiKey())) {
                    throw new IllegalArgumentException("apiKey is null, please set it via environment variable " + Options.ENV_API_KEY);
                }
                if (chatModel.getOptions().isNoThink()) {
                    jsonObject.getJSONArray("messages").add(JSONObject.of("role", "user", "content", "/no_think"));
                }
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
