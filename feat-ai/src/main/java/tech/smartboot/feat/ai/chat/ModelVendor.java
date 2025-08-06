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

import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ModelVendor {
    private static final Map<String, Map<String, ModelVendor>> modelMetas = new HashMap<>();

    public interface GiteeAI {
        String BASE_URL = "https://ai.gitee.com/v1/";
        ModelVendor DeepSeek_R1 = new ModelVendor(BASE_URL, "DeepSeek-R1", false);
        ModelVendor DeepSeek_R1_Distill_Qwen_32B = new ModelVendor(BASE_URL, "DeepSeek-R1-Distill-Qwen-32B", false);
        ModelVendor Qwen2_5_72B_Instruct = new ModelVendor(BASE_URL, "Qwen2.5-72B-Instruct", true);
        ModelVendor Qwen2_5_32B_Instruct = new ModelVendor(BASE_URL, "Qwen2.5-32B-Instruct", false);
        ModelVendor Qwen3_4B = new ModelVendor(BASE_URL, "Qwen3-4B", false);
    }

    public interface Ollama {
        String BASE_URL = "http://localhost:11434/v1";
        ModelVendor Qwen2_5_05B = new ModelVendor(BASE_URL, "qwen2.5:0.5b", false);
        ModelVendor Qwen2_5_3B = new ModelVendor(BASE_URL, "qwen2.5:3b", false);
    }


    /**
     * 服务商
     */
    private final String baseUrl;
    /**
     * 模型名称
     */
    private final String model;
    /**
     * 是否支持工具调用
     */
    private final boolean toolSupport;

    ModelVendor(String baseUrl, String model, boolean toolSupport) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.toolSupport = toolSupport;
        modelMetas.computeIfAbsent(baseUrl, k -> new HashMap<>()).putIfAbsent(model, this);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }
}
