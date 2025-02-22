package tech.smartboot.feat.ai.test;

import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.embedding.ModelVendor;
import tech.smartboot.feat.ai.vector.Document;
import tech.smartboot.feat.ai.vector.expression.Expression;
import tech.smartboot.feat.ai.vector.milvus.Collection;
import tech.smartboot.feat.ai.vector.milvus.Milvus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MilvusTest {
    private Milvus milvus;

    @Before
    public void init() {
        milvus = new Milvus("http://localhost:19530", opt -> opt.debug(true).embeddingModel(FeatAI.embedding(embedOpt -> {
            embedOpt.baseUrl("http://localhost:11434/v1").model(ModelVendor.Ollama.nomic_embed_text).debug(true);
        })));
    }

    @Test
    public void testMilvus() {
        milvus.createCollection("test_collection");
    }

    @Test
    public void testListDatabase() {
        List<String> list = milvus.listDatabase();
        System.out.println(list);
    }

    @Test
    public void testGetCollections() {
        milvus.getCollections();
        milvus.getCollections("default");
    }

    @Test
    public void testDelete() {
        Collection collection = milvus.getCollection("test_collection");

        Document document = new Document();
        document.setDocument("a");
        Map<String, String> map = new HashMap<>();
        map.put("name", "a");
        document.setMetadata(map);
        collection.add(document);

        collection.delete(1);

        Expression expression = Expression.of("id").eq(1);
        Expression expression1 = Expression.of("name").eq("a");
        Expression expression2 = Expression.of("name").eq("b");
        collection.delete(expression.and(expression1.or(expression2)));
    }
}
