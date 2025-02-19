package tech.smartboot.feat.test.ai;

import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.embedding.ModelVendor;

public class EmbeddingTest {
    private EmbeddingModel embeddingModel;

    @Before
    public void init() {
        embeddingModel = FeatAI.embedding(opt -> opt.debug(true).model(ModelVendor.GITEE_AI_BCE_BASE_V1));
    }

    @Test
    public void testEmbedding() {
        embeddingModel.embed("你好！");
    }
}
