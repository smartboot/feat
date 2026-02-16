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
 * 聊天模型供应商类，继承自Vendor，用于定义不同的AI模型供应商及其能力
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChatModelVendor extends Vendor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatModelVendor.class);

    /**
     * 思考能力标识
     */
    public static final int CAPABILITY_THINK = 1;

    /**
     * 函数调用能力标识
     */
    public static final int CAPABILITY_FUNCTION_CALL = 1 << 1;

    /**
     * 请求预处理器
     */
    private final PreRequest preRequest;

    /**
     * 能力标识，通过位运算组合不同能力
     */
    private final int capability;

    /**
     * GiteeAI模型供应商内部类
     */
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
        /**
         * DeepSeek-R1模型
         */
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

        /**
         * DeepSeek-V3.2模型
         */
        public static final ChatModelVendor DeepSeek_V32 = new GiteeAI("DeepSeek-V3.2", CAPABILITY_THINK | CAPABILITY_FUNCTION_CALL, (chatModel, modelVendor, jsonObject) -> {
            if (FeatUtils.isBlank(chatModel.getOptions().apiKey())) {
                throw new IllegalArgumentException("apiKey is null, please set it via environment variable " + Options.ENV_API_KEY);
            }
            if (chatModel.getOptions().isNoThink() && modelVendor.hasCapability(ChatModelVendor.CAPABILITY_THINK)) {
                LOGGER.warn("noThink is not supported by " + chatModel.getOptions().baseUrl() + " " + modelVendor.model());
            }
        });

        /**
         * DeepSeek-V3_1模型
         */
        public static final ChatModelVendor DeepSeek_V31 = new GiteeAI("DeepSeek-V3_1", CAPABILITY_THINK | CAPABILITY_FUNCTION_CALL, (chatModel, modelVendor, jsonObject) -> {
            if (FeatUtils.isBlank(chatModel.getOptions().apiKey())) {
                throw new IllegalArgumentException("apiKey is null, please set it via environment variable " + Options.ENV_API_KEY);
            }
            if (chatModel.getOptions().isNoThink() && modelVendor.hasCapability(ChatModelVendor.CAPABILITY_THINK)) {
                LOGGER.warn("noThink is not supported by " + chatModel.getOptions().baseUrl() + " " + modelVendor.model());
            }
        });

        /**
         * Kimi-K2-Instruct模型
         */
        public static final ChatModelVendor Kimi_K2_Instruct = new GiteeAI("Kimi-K2-Instruct", CAPABILITY_FUNCTION_CALL, null);

        /**
         * Kimi-K2.5-Instruct模型
         */
        public static final ChatModelVendor Kimi_K25_Instruct = new GiteeAI("Kimi-K2.5", CAPABILITY_THINK | CAPABILITY_FUNCTION_CALL, null);

        /**
         * DeepSeek-R1-Distill-Qwen-32B模型
         */
        public static final ChatModelVendor DeepSeek_R1_Distill_Qwen_32B = new GiteeAI("DeepSeek-R1-Distill-Qwen-32B", 0, qwen_pre_request);

        /**
         * Qwen2.5-72B-Instruct模型
         */
        public static final ChatModelVendor Qwen2_5_72B_Instruct = new GiteeAI("Qwen2.5-72B-Instruct", CAPABILITY_FUNCTION_CALL, qwen_pre_request);

        /**
         * Qwen2.5-32B-Instruct模型
         */
        public static final ChatModelVendor Qwen2_5_32B_Instruct = new GiteeAI("Qwen2.5-32B-Instruct", 0, qwen_pre_request);

        /**
         * Qwen3-235B-A22B-Instruct-2507模型
         */
        public static final ChatModelVendor Qwen3_235B_A22B_Instruct_2507 = new GiteeAI("Qwen3-235B-A22B-Instruct-2507", CAPABILITY_THINK | CAPABILITY_FUNCTION_CALL, qwen_pre_request);

        /**
         * Qwen3-4B模型
         */
        public static final ChatModelVendor Qwen3_4B = new GiteeAI("Qwen3-4B", CAPABILITY_THINK, qwen_pre_request);

        /**
         * Qwen3-8B模型
         */
        public static final ChatModelVendor Qwen3_8B = new GiteeAI("Qwen3-8B", CAPABILITY_THINK, qwen_pre_request);

        /**
         * Qwen3-32B模型
         */
        public static final ChatModelVendor Qwen3_32B = new GiteeAI("Qwen3-32B", CAPABILITY_THINK, qwen_pre_request);

        /**
         * GiteeAI构造函数
         *
         * @param model      模型名称
         * @param capability 能力标识
         * @param request    预处理器
         */
        GiteeAI(String model, int capability, PreRequest request) {
            super("https://ai.gitee.com/v1/", model, capability, request);
        }
    }

    /**
     * Ollama模型供应商内部类
     */
    public static class Ollama extends ChatModelVendor {
        private static final PreRequest qwen_pre_request = new PreRequest() {
            @Override
            public void preRequest(ChatModel chatModel, ChatModelVendor modelVendor, JSONObject jsonObject) {
                if (chatModel.getOptions().getModel().hasCapability(ChatModelVendor.CAPABILITY_THINK) && chatModel.getOptions().isNoThink()) {
                    jsonObject.getJSONArray("messages").add(JSONObject.of("role", "user", "content", "/no_think"));
                }
            }
        };

        /**
         * Qwen2.5 0.5B模型
         */
        public static final ChatModelVendor Qwen2_5_05B = new Ollama("qwen2.5:0.5b", 0, qwen_pre_request);

        /**
         * Qwen2.5 3B模型
         */
        public static final ChatModelVendor Qwen2_5_3B = new Ollama("qwen2.5:3b", 0, qwen_pre_request);

        /**
         * Qwen3 0.6B模型
         */
        public static final ChatModelVendor Qwen3_06B = new Ollama("qwen3:0.6b", CAPABILITY_THINK, qwen_pre_request);

        /**
         * Deepseek-r1 1.5B模型
         */
        public static final ChatModelVendor Deepseek_r1_1_5B = new Ollama("deepseek-r1:1.5b", 0);

        /**
         * Deepseek-r1 7B模型
         */
        public static final ChatModelVendor Deepseek_r1_7B = new Ollama("deepseek-r1:7b", CAPABILITY_THINK);

        /**
         * Ollama构造函数
         *
         * @param model      模型名称
         * @param capability 能力标识
         */
        Ollama(String model, int capability) {
            this(model, capability, null);
        }

        /**
         * Ollama构造函数
         *
         * @param model      模型名称
         * @param capability 能力标识
         * @param request    预处理器
         */
        Ollama(String model, int capability, PreRequest request) {
            super("http://localhost:11434/v1", model, capability, request);
        }
    }

    /**
     * ChatModelVendor构造函数
     *
     * @param baseUrl    基础URL
     * @param model      模型名称
     * @param capability 能力标识
     * @param request    预处理器
     */
    ChatModelVendor(String baseUrl, String model, int capability, PreRequest request) {
        super(baseUrl, model);
        this.preRequest = request;
        this.capability = capability;
    }

    public ChatModelVendor(String baseUrl, String model) {
        this(baseUrl, model, 0, null);
    }

    public ChatModelVendor(String model) {
        this(null, model, 0, null);
    }

    /**
     * 获取请求预处理器
     *
     * @return 请求预处理器
     */
    PreRequest getPreRequest() {
        return preRequest;
    }

    /**
     * 检查是否具有指定能力
     *
     * @param capability 能力标识
     * @return 如果具有指定能力返回true，否则返回false
     */
    public boolean hasCapability(int capability) {
        return (this.capability & capability) != 0;
    }
}