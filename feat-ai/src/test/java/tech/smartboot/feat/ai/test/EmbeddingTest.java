package tech.smartboot.feat.ai.test;

import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.embedding.ModelVendor;

import java.util.Arrays;
import java.util.List;

public class EmbeddingTest {

    @Test
    public void testEmbedding1() {
        //调用Gitee AI的Embedding模型
        EmbeddingModel embeddingModel = FeatAI.embedding(opt -> opt.model(ModelVendor.GITEE_AI_BCE_BASE_V1));
        float[] embed = embeddingModel.embed("你好！");
        System.out.println(Arrays.toString(embed));
    }

    @Test
    public void testEmbedding2() {
        //调用Gitee AI的Embedding模型
        EmbeddingModel embeddingModel = FeatAI.embedding(opt -> opt.model(ModelVendor.GITEE_AI_BGE_LARGE_ZH_V1_5));
        List<float[]> embeds = embeddingModel.embed(Arrays.asList("Hello World", "你好"));
        embeds.forEach(embed -> System.out.println(Arrays.toString(embed)));
    }

    @Test
    public void testOllama() {
        //调用Ollama的Embedding模型
        EmbeddingModel embeddingModel = FeatAI.embedding(opt -> opt.baseUrl("http://localhost:11434/v1").model(ModelVendor.Ollama.nomic_embed_text));
        List<float[]> embeds = embeddingModel.embed(Arrays.asList("Hello World", "你好"));
        embeds.forEach(embed -> System.out.println(Arrays.toString(embed)));
    }
}
