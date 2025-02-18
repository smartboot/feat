package tech.smartboot.feat.test.ai;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.ai.vector.chroma.Chroma;
import tech.smartboot.feat.ai.vector.chroma.collection.Collection;
import tech.smartboot.feat.ai.vector.chroma.collection.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChromaTest {
    private Chroma chroma;

    @Before
    public void init() {
        chroma = new Chroma("http://localhost:8000", opt -> opt.debug(true));
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
        Collection collection = chroma.createCollection("my_collection_1", metadata);
        Assert.assertNotNull(collection);
    }

    @Test
    public void testGetCollections() {
        List<Collection> collections = chroma.getCollections(0, 10);
        collections.forEach(collection -> System.out.println(collection.getName()));
    }

    @Test
    public void testGetCollection() {
        Collection collection = chroma.getCollection("my_collection");
        Assert.assertNotNull(collection);
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Document document = new Document();
            document.setId(i + "");
            document.setMetadata(Collections.singletonMap("name", "sndao" + System.currentTimeMillis()));
            document.setDocument("hello world" + System.currentTimeMillis());
            documents.add(document);
        }


        collection.add(documents);


        collection.get();

        System.out.println("count: " + collection.count());

        collection.delete();

    }
}
