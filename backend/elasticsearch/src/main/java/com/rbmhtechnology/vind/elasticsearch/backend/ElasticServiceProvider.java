package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.api.ServiceProvider;

public interface ElasticServiceProvider extends ServiceProvider {
    ElasticSearchServer getInstance();
}
