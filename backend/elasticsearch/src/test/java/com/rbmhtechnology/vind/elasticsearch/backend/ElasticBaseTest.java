package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.elasticsearch.backend.client.ElasticVindClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;

public class ElasticBaseTest {
    protected static ElasticsearchContainer container;
    protected static ElasticVindClient client;
    protected static SearchServer server;


    @BeforeClass
    public static void setUp() throws IOException {
        // Create the elasticsearch container.
        container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.6.1");

        // Start the container. This step might take some time...
        container.start();

        client = new ElasticVindClient.Builder("http://" + container.getHttpHostAddress())
                .setDefaultIndex("vind")
                .build("elastic", "changeme");

        client.createIndex("vind");

        server = new ElasticSearchServer(client);

    }

    @AfterClass
    public static void cleanUp() throws IOException {
        //stop the client
        client.close();
        // Stop the container.
        container.stop();
    }

}
