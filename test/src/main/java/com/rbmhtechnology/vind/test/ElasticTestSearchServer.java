package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.configure.SearchConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticTestSearchServer extends TestSearchServer {

    private ElasticsearchContainer container;

    @Override
    protected void before() throws Throwable {
        super.before();
        this.container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.6.1");
        container.start();
        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://" + container.getHttpHostAddress());
    }

    @Override
    protected void after() {
        if(this.container != null) {
            this.container.stop();
        }
        super.after();
    }
}
