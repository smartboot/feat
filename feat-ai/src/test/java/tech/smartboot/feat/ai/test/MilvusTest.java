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

import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.embedding.EmbeddingModelVendor;
import tech.smartboot.feat.ai.vector.Document;
import tech.smartboot.feat.ai.vector.expression.Expression;
import tech.smartboot.feat.ai.vector.milvus.Collection;
import tech.smartboot.feat.ai.vector.milvus.Milvus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class MilvusTest {
    private Milvus milvus;

    @Before
    public void init() {
        milvus = new Milvus("http://localhost:19530", opt -> opt.debug(true).embeddingModel(FeatAI.embedding(embedOpt -> {
            embedOpt.baseUrl("http://localhost:11434/v1").model(EmbeddingModelVendor.Ollama.nomic_embed_text).debug(true);
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
