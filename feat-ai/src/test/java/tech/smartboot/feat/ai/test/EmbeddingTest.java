/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.test;

import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.embedding.EmbeddingModelVendor;

import java.util.Arrays;
import java.util.List;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class EmbeddingTest {

    @Test
    public void testEmbedding1() {
        //调用Gitee AI的Embedding模型
        EmbeddingModel embeddingModel = FeatAI.embedding(opt -> opt.model(EmbeddingModelVendor.GiteeAI.bce_embedding_base_v1));
        float[] embed = embeddingModel.embed("你好！");
        System.out.println(Arrays.toString(embed));
    }

    @Test
    public void testEmbedding2() {
        //调用Gitee AI的Embedding模型
        EmbeddingModel embeddingModel = FeatAI.embedding(opt -> opt.model(EmbeddingModelVendor.GiteeAI.bge_large_zh_v1_5));
        List<float[]> embeds = embeddingModel.embed(Arrays.asList("Hello World", "你好"));
        embeds.forEach(embed -> System.out.println(Arrays.toString(embed)));
    }

    @Test
    public void testOllama() {
        //调用Ollama的Embedding模型
        EmbeddingModel embeddingModel = FeatAI.embedding(opt -> opt.model(EmbeddingModelVendor.Ollama.nomic_embed_text));
        List<float[]> embeds = embeddingModel.embed(Arrays.asList("Hello World", "你好"));
        embeds.forEach(embed -> System.out.println(Arrays.toString(embed)));
    }
}
