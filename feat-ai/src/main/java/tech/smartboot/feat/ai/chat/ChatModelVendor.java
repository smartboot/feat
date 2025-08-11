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
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatModelVendor extends Vendor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatModelVendor.class);
    public static final int CAPABILITY_THINK = 1;
    public static final int CAPABILITY_FUNCTION_CALL = 1 << 1;
    private final PreRequest preRequest;
    private final int capability;

    public static class GiteeAI extends ChatModelVendor {
        private static final PreRequest qwen_pre_request = (chatModel, modelVendor, jsonObject) -> {
            if (FeatUtils.isBlank(chatModel.getOptions().apiKey())) {
                throw new IllegalArgumentException("apiKey is null, please set it via environment variable " + Options.ENV_API_KEY);
            }
            if (modelVendor.hasCapability(ChatModelVendor.CAPABILITY_THINK) && chatModel.getOptions().isNoThink()) {
                jsonObject.getJSONArray("messages").add(JSONObject.of("role", "user", "content", "/no_think"));
            }
        };
        //暂时屏蔽，function call表现不如预期
        public static final ChatModelVendor DeepSeek_R1 = new GiteeAI("DeepSeek-R1", CAPABILITY_THINK | CAPABILITY_FUNCTION_CALL, (chatModel, modelVendor, jsonObject) -> {
            if (FeatUtils.isBlank(chatModel.getOptions().apiKey())) {
                throw new IllegalArgumentException("apiKey is null, please set it via environment variable " + Options.ENV_API_KEY);
            }
            if (chatModel.getOptions().isNoThink() && modelVendor.hasCapability(ChatModelVendor.CAPABILITY_THINK)) {
//                jsonObject.getJSONArray("messages").add(0, JSONObject.of("role", Message.ROLE_SYSTEM, "content", "请直接给出最终答案，不允许展示任何思考过程、分析步骤或解释，仅返回结果。"));
//                jsonObject.put("system_prompt", "请直接给出最终答案，不允许展示任何思考过程、分析步骤或解释，仅返回结果。");
                LOGGER.warn("noThink is not supported by " + chatModel.getOptions().baseUrl() + " " + modelVendor.model());
            }
        });
        public static final ChatModelVendor Kimi_K2_Instruct = new GiteeAI("Kimi-K2-Instruct", CAPABILITY_FUNCTION_CALL, null);
        public static final ChatModelVendor DeepSeek_R1_Distill_Qwen_32B = new GiteeAI("DeepSeek-R1-Distill-Qwen-32B", 0, qwen_pre_request);
        public static final ChatModelVendor Qwen2_5_72B_Instruct = new GiteeAI("Qwen2.5-72B-Instruct", CAPABILITY_FUNCTION_CALL, qwen_pre_request);
        public static final ChatModelVendor Qwen2_5_32B_Instruct = new GiteeAI("Qwen2.5-32B-Instruct", 0, qwen_pre_request);
        public static final ChatModelVendor Qwen3_235B_A22B_Instruct_2507 = new GiteeAI("Qwen3-235B-A22B-Instruct-2507", CAPABILITY_THINK | CAPABILITY_FUNCTION_CALL, qwen_pre_request);
        public static final ChatModelVendor Qwen3_4B = new GiteeAI("Qwen3-4B", CAPABILITY_THINK, qwen_pre_request);

        GiteeAI(String model, int capability, PreRequest request) {
            super("https://ai.gitee.com/v1/", model, capability, request);
        }
    }

    public static class Ollama extends ChatModelVendor {
        private static final PreRequest qwen_pre_request = new PreRequest() {
            @Override
            public void preRequest(ChatModel chatModel, ChatModelVendor modelVendor, JSONObject jsonObject) {
                if (chatModel.getOptions().getModel().hasCapability(ChatModelVendor.CAPABILITY_THINK) && chatModel.getOptions().isNoThink()) {
                    jsonObject.getJSONArray("messages").add(JSONObject.of("role", "user", "content", "/no_think"));
                }
            }
        };
        public static final ChatModelVendor Qwen2_5_05B = new Ollama("qwen2.5:0.5b", 0, qwen_pre_request);
        public static final ChatModelVendor Qwen2_5_3B = new Ollama("qwen2.5:3b", 0, qwen_pre_request);

        public static final ChatModelVendor Qwen3_06B = new Ollama("qwen3:0.6b", CAPABILITY_THINK, qwen_pre_request);

        public static final ChatModelVendor Deepseek_r1_1_5B = new Ollama("deepseek-r1:1.5b", 0);
        public static final ChatModelVendor Deepseek_r1_7B = new Ollama("deepseek-r1:7b", CAPABILITY_THINK);

        Ollama(String model, int capability) {
            this(model, capability, null);
        }

        Ollama(String model, int capability, PreRequest request) {
            super("http://localhost:11434/v1", model, capability, request);
        }
    }

    ChatModelVendor(String baseUrl, String model, int capability, PreRequest request) {
        super(baseUrl, model);
        this.preRequest = request;
        this.capability = capability;
    }

    PreRequest getPreRequest() {
        return preRequest;
    }

    public boolean hasCapability(int capability) {
        return (this.capability & capability) != 0;
    }
}
