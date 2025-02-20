package tech.smartboot.feat.test.ai;

import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.embedding.ModelVendor;
import tech.smartboot.feat.ai.vector.Document;
import tech.smartboot.feat.ai.vector.VectorStore;
import tech.smartboot.feat.ai.vector.expression.Expression;
import tech.smartboot.feat.ai.vector.expression.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VectorStoreTest {
    private VectorStore vectorStore;

    @Before
    public void init() {
        EmbeddingModel embeddingModel = FeatAI.embedding(embedOpt -> {
            embedOpt.baseUrl("http://localhost:11434/v1").model(ModelVendor.Ollama.nomic_embed_text).debug(true);
        });
        vectorStore = VectorStore.chroma(opt -> {
            opt.setUrl("http://localhost:8000").collectionName("my_collection").debug(true).embeddingModel(embeddingModel);
        });
    }

    @Test
    public void testAddDocs() {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Document document = new Document();
            document.setId(i + "");
            document.setMetadata(Collections.singletonMap("name", "sndao" + System.nanoTime()));
            document.setDocument("hello world" + i);
            documents.add(document);
        }
        vectorStore.add(documents);
    }

    @Test
    public void testDelete() {
        String testId = "testDelete";
        Document document = new Document();
        document.setId(testId);
        document.setDocument("hello world");
        vectorStore.add(document);
        vectorStore.delete(testId);
    }

    @Test
    public void testQuery() {
        Filter filter = new Filter();
        Expression a = Expression.of("a").eq("b");
        Expression b = Expression.of("b").eq("c");
        vectorStore.similaritySearch(filter);
    }

}
