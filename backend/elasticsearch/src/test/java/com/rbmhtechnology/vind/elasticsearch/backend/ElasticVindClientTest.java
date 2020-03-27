package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.elasticsearch.backend.util.FieldUtil;
import org.elasticsearch.action.index.IndexResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ElasticVindClientTest  extends ElasticBaseTest{

    private  ElasticVindClient client;

    @Before
    public void prepare() {
        client = new ElasticVindClient.Builder("http://" + container.getHttpHostAddress())
                .setDefaultIndex("vind-test")
                .build("elastic", "changeme");
    }

    @Test
    public void testPing() throws IOException {
        assertTrue(client.ping());
    }

    @Test
    public void testAdd() throws IOException {

        final Map<String, Object> doc = new HashMap<>();
        doc.put("dynamic_string_title", "The last ascend of man");
        doc.put(FieldUtil.ID, "AA-2X3451");
        doc.put(FieldUtil.TYPE, "TestDoc");
        final IndexResponse indexResult = client.add(doc);
        assertNotNull(indexResult);
    }

    @After
    public void clean() {
        client.close();
    }
}
