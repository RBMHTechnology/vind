package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.elasticsearch.backend.ElasticVindClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticTestSearchServer extends TestSearchServer {

    public static final String DEFAULT_COLLECTION_NAME = "vind";
    private ElasticsearchContainer container;

    @Override
    protected void before() throws Throwable {
        super.before();
        this.container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.6.1");
        container.start();
        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://" + container.getHttpHostAddress());
        SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, DEFAULT_COLLECTION_NAME);
        ((ElasticVindClient)getSearchServer().getBackend()).createIndex(DEFAULT_COLLECTION_NAME);
    }

    @Override
    protected void after() {
        if(this.container != null) {
            this.container.stop();
        }
        super.after();
    }

}
