package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.elasticsearch.backend.ElasticVindClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;

public class ElasticTestSearchServer extends TestSearchServer {

    public static final String DEFAULT_COLLECTION_NAME = "vind";
    private ElasticsearchContainer container;

    @Override
    protected void start() throws RuntimeException {
        super.start();
        this.container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.6.1");
        container.start();
        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://" + container.getHttpHostAddress());
        SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, DEFAULT_COLLECTION_NAME);
        try {
            ((ElasticVindClient)getSearchServer().getBackend()).createIndex(DEFAULT_COLLECTION_NAME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void close() throws RuntimeException {
        if(this.container != null) {
            this.container.stop();
        }
        super.close();
    }

}
