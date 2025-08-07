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

import tech.smartboot.feat.ai.Vendor;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public abstract class EmbeddingModelVendor extends Vendor {


    EmbeddingModelVendor(String baseUrl, String model) {
        super(baseUrl, model);
    }


    public static class GiteeAI extends EmbeddingModelVendor {
        /**
         * Qwen3‑Embedding‑8B 是 Qwen 系列推出的大规模嵌入模型，专注于生成高质量、多语言及代码向量，支持多种下游任务中的语义匹配与信息检索需求。
         */
        public static final EmbeddingModelVendor Qwen3_Embedding_8B = new GiteeAI("Qwen3-Embedding-8B");
        /**
         * Qwen3-Embedding-4B 是一款多语言、多模态任务通用的高性能文本和代码嵌入模型，适用于语义搜索、跨语言检索和信息匹配等场景。
         */
        public static final EmbeddingModelVendor Qwen3_Embedding_4B = new GiteeAI("Qwen3-Embedding-4B");

        /**
         * Qwen3‑Embedding‑0.6B 是一款多语言文本嵌入模型，支持用户自定义输出维度，擅长跨语言和代码检索任务，并与同系列重排序模型联合使用可通过指令微调以提升特定场景表现
         */
        public static final EmbeddingModelVendor Qwen3_Embedding_06B = new GiteeAI("Qwen3-Embedding-0.6B");

        public static final EmbeddingModelVendor bce_embedding_base_v1 = new GiteeAI("bce-embedding-base_v1");
        public static final EmbeddingModelVendor bge_small_zh_v1_5 = new GiteeAI("bge-small-zh-v1.5");
        public static final EmbeddingModelVendor bge_large_zh_v1_5 = new GiteeAI("bge-large-zh-v1.5");
        public static final EmbeddingModelVendor BGE_M3 = new GiteeAI("bge-m3");

        GiteeAI(String model) {
            super("https://ai.gitee.com/v1/", model);
        }
    }

    /**
     * Ollama 模型
     */
    public static class Ollama extends EmbeddingModelVendor {
        /**
         * A high-performing open embedding model with a large token context window.
         * <p>
         * nomic-embed-text 是一个大上下文长度的文本编码器，
         * 它超越了 OpenAItext-embedding-ada-002 并 text-embedding-3-small 短上下文任务和长上下文任务的性能。
         */
        public static final EmbeddingModelVendor nomic_embed_text = new Ollama("nomic-embed-text");

        /**
         * State-of-the-art large embedding model from mixedbread.ai
         * <p>
         * 截至 2024 年 3 月，该模型在 MTEB 上归档了 Bert 大尺寸模型的 SOTA 性能。
         * 它优于商业模型，如 OpenAI 的 text-embedding-3-large 模型，并与模型 20 倍的性能相匹配。
         * <p>
         * mxbae-embed-size 在没有 MTEB 数据重叠的情况下进行了训练，这表明该模型在多个领域、任务和文本长度上具有良好的泛化性。
         */
        public static final EmbeddingModelVendor mxbai_embed_large = new Ollama("mxbai-embed-large");

        /**
         * A suite of text embedding models by Snowflake, optimized for performance.
         */
        public static final EmbeddingModelVendor snowflake_arctic_embed = new Ollama("snowflake-arctic-embed");

        Ollama(String model) {
            super("http://localhost:11434/v1", model);
        }
    }
}
