package tech.smartboot.feat.test.ai;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.ai.vector.chroma.Chroma;

import java.util.HashMap;
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
        chroma.createCollection("my_collection", metadata);
    }
}
