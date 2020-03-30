package com.rbmhtechnology.vind.elasticsearch.backend.util;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
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
import java.util.List;
import java.util.Map;

public class ElasticRequestUtils {

    public static IndexRequest getIndexRequest(String index, Map<String,Object> jsonMap) {
        return new IndexRequest(index)
                .id(jsonMap.get(FieldUtil.ID).toString())
                .source(jsonMap);
    }

    public static GetRequest getRealTimeGetRequest(String index, String docId) {
        return new GetRequest(index, docId);
    }

    public static MultiGetRequest getRealTimeGetRequest(String index, List<String> docIds) {
        final MultiGetRequest request = new MultiGetRequest();
        docIds.forEach(id -> request.add(new MultiGetRequest.Item(index, id)));
        return request;
    }


    public static DeleteRequest getDeleteRequest(String index, String docId) {
        return new DeleteRequest(index, docId);
    }

    public static CreateIndexRequest getCreateIndexRequest(String index) {
        final CreateIndexRequest request = new CreateIndexRequest(index);
        request.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1)
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
