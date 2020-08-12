package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.api.ServiceProvider;
import com.rbmhtechnology.vind.elasticsearch.backend.client.ElasticVindClient;

public interface ElasticServerProvider extends ServiceProvider {
    ElasticVindClient getInstance();
    enum AuthTypes {
        NONE, BASIC, APIKEY;
    }
}
