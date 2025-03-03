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

import com.alibaba.fastjson2.JSONObject;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.embedding.EmbeddingModel;
import tech.smartboot.feat.ai.embedding.ModelVendor;
import tech.smartboot.feat.ai.vector.ChromaVectorStore;
import tech.smartboot.feat.ai.vector.Document;
import tech.smartboot.feat.ai.vector.SearchRequest;
import tech.smartboot.feat.ai.vector.VectorStore;
import tech.smartboot.feat.ai.vector.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Document document = new Document();
            document.setId(i + "");
            Map<String, String> metadata = new HashMap<>();
            metadata.put("name", "sandao" + i);
            metadata.put("age", String.valueOf(i));
            document.setMetadata(metadata);
            document.setDocument("hello world" + i);
            documents.add(document);
        }
        vectorStore.add(documents);


        Expression a = Expression.of("name").eq("sandao0");
        Expression b = Expression.of("age").eq("0");

        SearchRequest request = new SearchRequest();
        request.setQuery("hello world");
//        request.setTopK(1);
        request.setExpression(a.and(b));
        vectorStore.similaritySearch(request);
    }


    @Test
    public void testExpression() {
        Expression a = Expression.parse("name == '1'");
        JSONObject object = new JSONObject();
        a.build(object, ChromaVectorStore.convert);
        System.out.println(object);
    }
}
