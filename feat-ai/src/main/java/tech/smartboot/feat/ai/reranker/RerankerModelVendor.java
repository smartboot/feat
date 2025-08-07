/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.reranker;

import tech.smartboot.feat.ai.Vendor;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public abstract class RerankerModelVendor extends Vendor {


    public RerankerModelVendor(String vendor, String model) {
        super(vendor, model);
    }


    public static class GiteeAI extends RerankerModelVendor {
        /**
         * Qwen3-Reranker-8B 是 Qwen 系列 reranker 模型中规模最大、性能最强的版本，采用基于 Transformer 的 Decoder-only 架构，拥有约 80 亿参数。
         * 该模型专注于对检索系统中候选结果进行精细排序，适用于跨语言、多领域的文本和代码排序任务。
         */
        public static final RerankerModelVendor Qwen3_Reranker_8B = new GiteeAI("Qwen3-Reranker-8B");
        /**
         * Qwen3‑Reranker‑4B 是Qwen团队推出的一款基于大型（≈40亿参数）Decoder-only Transformer，专为跨语言和多域的信息检索排序任务优化的高性能reranker模型。
         */
        public static final RerankerModelVendor Qwen3_Reranker_4B = new GiteeAI("Qwen3-Reranker-4B");

        /**
         * Qwen3-Reranker-0.6B 是由 Qwen 团队开发的一款轻量级重排序模型，基于 transformer 架构，专为信息检索任务中的文档或答案排序优化而设计。
         */
        public static final RerankerModelVendor Qwen3_Reranker_06B = new GiteeAI("Qwen3-Reranker-0.6B");

        public GiteeAI(String model) {
            super("https://ai.gitee.com/v1/", model);
        }
    }
}
