package com.rbmhtechnology.vind.elasticsearch.backend;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;

public class ElasticBaseTest {
    protected static ElasticsearchContainer container;
    protected static ElasticVindClient client;

    @BeforeClass
    public static void setUp() throws IOException {
        // Create the elasticsearch container.
        container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.6.1");

        // Start the container. This step might take some time...
        container.start();

        client = new ElasticVindClient.Builder("http://" + container.getHttpHostAddress())
                .setDefaultIndex("vind-test")
                .build("elastic", "changeme");

        client.createIndex("vind-test");

//// Do whatever you want with the rest client ...
//        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "changeme"));
//        RestClient restClient = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
//                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
//                .build();
//        Response response = restClient.performRequest(new Request("GET", "/"));
//
//// ... or the transport client
//        TransportAddress transportAddress = new TransportAddress(container.getTcpHost());
//        Settings settings = Settings.builder().put("cluster.name", "docker-cluster").build();
//        TransportClient transportClient = new PreBuiltTransportClient(settings)
//                .addTransportAddress(transportAddress);
//        ClusterHealthResponse healths = transportClient.admin().cluster().prepareHealth().get();

    }

    @AfterClass
    public static void cleanUp() throws IOException {
        //stop the client
        client.close();
        // Stop the container.
        container.stop();
    }

}
