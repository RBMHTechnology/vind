package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.result.IndexResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class ElasticSearchServerTest extends ElasticBaseTest {

    private SearchServer server;

    @Before
    public void init() throws IOException {
        final ElasticVindClient client = new ElasticVindClient.Builder("http://" + container.getHttpHostAddress())
                .setDefaultIndex("vind-test")
                .build("elastic", "changeme");

        client.createIndex("vind-test");
        server = new ElasticSearchServer(client);
    }

    @Test
    public void indexTest(){
        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("TestDoc");

        final FieldDescriptor descriptor = new FieldDescriptorBuilder().setFacet(true).buildTextField("title");
        docFactoryBuilder.addField(descriptor);
        final DocumentFactory documents = docFactoryBuilder.build();
        final Document doc1 = documents.createDoc("AA-2X3451")
                .setValue(descriptor, "The last ascent of man");

        final Document doc2 = documents.createDoc("AA-2X6891")
                .setValue(descriptor, "Dawn of humanity: the COVID-19 chronicles");
        final IndexResult indexResult = server.index(doc1,doc2);
        assertNotNull(indexResult);
    }
}
