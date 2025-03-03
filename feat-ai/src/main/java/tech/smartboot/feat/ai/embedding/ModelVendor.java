/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.embedding;

import tech.smartboot.feat.ai.Options;

import java.util.HashMap;
import java.util.Map;

public class ModelVendor {
    private static final Map<String, Map<String, ModelVendor>> modelMetas = new HashMap<>();
    public static final ModelVendor GITEE_AI_BCE_BASE_V1 = new ModelVendor(tech.smartboot.feat.ai.Options.AI_VENDOR_GITEE, "bce-embedding-base_v1");
    public static final ModelVendor GITEE_AI_BGE_SMALL_ZH_V1_5 = new ModelVendor(tech.smartboot.feat.ai.Options.AI_VENDOR_GITEE, "bge-small-zh-v1.5");
    public static final ModelVendor GITEE_AI_BGE_LARGE_ZH_V1_5 = new ModelVendor(tech.smartboot.feat.ai.Options.AI_VENDOR_GITEE, "bge-large-zh-v1.5");
    public static final ModelVendor GITEE_AI_BGE_M3 = new ModelVendor(Options.AI_VENDOR_GITEE, "bge-m3");

    /**
     * 服务商
     */
    private final String vendor;
    /**
     * 模型名称
     */
    private final String model;


    public ModelVendor(String vendor, String model) {
        this.vendor = vendor;
        this.model = model;
        modelMetas.computeIfAbsent(vendor, k -> new HashMap<>()).putIfAbsent(model, this);
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }


    public static ModelVendor get(String vendor, String model) {
        return modelMetas.getOrDefault(vendor, new HashMap<>()).getOrDefault(model, null);
    }

    /**
     *  Ollama 模型
     */
    public interface Ollama {
        /**
         * A high-performing open embedding model with a large token context window.
         *
         * nomic-embed-text 是一个大上下文长度的文本编码器，
         * 它超越了 OpenAItext-embedding-ada-002 并 text-embedding-3-small 短上下文任务和长上下文任务的性能。
         */
        String nomic_embed_text = "nomic-embed-text";

        /**
         * State-of-the-art large embedding model from mixedbread.ai
         *
         * 截至 2024 年 3 月，该模型在 MTEB 上归档了 Bert 大尺寸模型的 SOTA 性能。
         * 它优于商业模型，如 OpenAI 的 text-embedding-3-large 模型，并与模型 20 倍的性能相匹配。
         *
         * mxbae-embed-size 在没有 MTEB 数据重叠的情况下进行了训练，这表明该模型在多个领域、任务和文本长度上具有良好的泛化性。
         */
        String mxbai_embed_large = "mxbai-embed-large";

        /**
         * A suite of text embedding models by Snowflake, optimized for performance.
         */
        String snowflake_arctic_embed = "snowflake-arctic-embed";
    }
}
