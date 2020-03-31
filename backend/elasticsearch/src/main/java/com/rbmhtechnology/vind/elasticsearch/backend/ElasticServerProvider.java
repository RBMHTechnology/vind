package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.api.ServiceProvider;

public interface ElasticServerProvider extends ServiceProvider {
    ElasticVindClient getInstance();
}
