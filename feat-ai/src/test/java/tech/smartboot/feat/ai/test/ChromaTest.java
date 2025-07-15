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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.embedding.ModelVendor;
import tech.smartboot.feat.ai.vector.Document;
import tech.smartboot.feat.ai.vector.chroma.Chroma;
import tech.smartboot.feat.ai.vector.chroma.Collection;
import tech.smartboot.feat.ai.vector.chroma.collection.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ChromaTest {
    private Chroma chroma;

    @Before
    public void init() {
        chroma = new Chroma("http://localhost:8000", opt -> opt.debug(true).embeddingModel(FeatAI.embedding(embedOpt -> {
            embedOpt.baseUrl("http://localhost:11434/v1").model(ModelVendor.Ollama.nomic_embed_text).debug(true);
        })));
    }

    @Test
    public void testVersion() {
        String version = chroma.version();
        System.out.println(version);
        Assert.assertEquals("0.6.3", version);
    }

    @Test
    public void testReset() {
        boolean result = chroma.rest();
        Assert.assertTrue(result);
    }

    @Test
    public void testCreateDatabase() {
        chroma.createDatabase("test");
    }

    @Test
    public void testDeleteDatabase() {
        chroma.deleteDatabase("test");
    }

    @Test
    public void testGetDatabase() {
        String database = chroma.getDatabase("test");
        System.out.println(database);
    }

    @Test
    public void testCreateCollection() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("name", "sndao");
        metadata.put("age", "12");
        Collection collection = chroma.createCollection("my_collection", metadata);
        Assert.assertNotNull(collection);
    }

    @Test
    public void testGetCollections() {
        List<Collection> collections = chroma.getCollections(0, 10);
        collections.forEach(collection -> System.out.println(collection.getName()));
    }


    @Test
    public void testEmbedding() {
        ChatModel chatModel = FeatAI.chatModel(opt -> opt.debug(true).model("nomic-embed-text")
                .baseUrl("http://localhost:11434/v1") // Ollama本地服务地址
        );
        chatModel.chat("aaa", resp -> {
            System.out.println(resp.getContent());
        });
    }

    @Test
    public void testGetCollection() {
        testCreateCollection();
        Collection collection = chroma.getCollection("my_collection");
        Assert.assertNotNull(collection);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Document document = new Document();
            document.setId(i + "");
            document.setMetadata(Collections.singletonMap("name", "sndao" + System.nanoTime()));
            document.setDocument("hello world" + i);
            documents.add(document);
        }


        collection.add(documents);


        collection.get();

        System.out.println("count: " + collection.count());


        Query query = new Query();
//        query.where("metadata_field", "is_equal_to_this");
//        query.whereDocument("$contains", "hello");

        query.setQueryTexts(Arrays.asList("hello world1"));
        query.setInclude(Arrays.asList("metadatas", "documents", "distances"));
//        query.setInclude(Arrays.asList("documents"));
        collection.query(query);

        collection.delete();

    }
}
