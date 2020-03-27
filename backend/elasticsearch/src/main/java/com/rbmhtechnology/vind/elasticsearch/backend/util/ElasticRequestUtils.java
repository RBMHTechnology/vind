package com.rbmhtechnology.vind.elasticsearch.backend.util;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ElasticRequestUtils {

    public static IndexRequest getIndexRequest(String defaultIndex, Map<String,Object> jsonMap) {
        return new IndexRequest(defaultIndex)
                .id(jsonMap.get(FieldUtil.ID).toString())
                .source(jsonMap);
    }

    public static CreateIndexRequest getCreateIndexRequest(String indexName) {
        final CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder()
                .put("index.number_of_shards", 3)
                .put("index.number_of_replicas", 2)
        );

        request.mapping(getDefaultMaping(), XContentType.JSON);
        return request;
    }

    private static String getDefaultMaping() {
        final Path mappingsFile = Paths.get(ElasticRequestUtils.class
                .getClassLoader().getResource("mappings.json").getPath());

        try {
            return new String(Files.readAllBytes(mappingsFile));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
