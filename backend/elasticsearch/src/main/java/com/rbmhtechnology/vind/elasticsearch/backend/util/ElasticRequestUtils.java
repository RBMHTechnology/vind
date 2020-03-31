package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.api.query.update.Update;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.ScriptQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.script.Script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ElasticRequestUtils {

    public static IndexRequest getIndexRequest(String index, Map<String,Object> jsonMap) {
        return new IndexRequest(index)
                .id(jsonMap.get(FieldUtil.ID).toString())
                .source(jsonMap);
    }

    public static UpdateRequest getUpdateRequest(String index, String id, Map<String,Object> partialDocMap) {
        final UpdateRequest request = new UpdateRequest(index, id);
        request.script(ScriptQueryBuilder.)doc(partialDocMap);
        return request;
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
    public static DeleteByQueryRequest getDeleteByQueryRequest(String index, QueryBuilder query) {

        final DeleteByQueryRequest request =
                new DeleteByQueryRequest(index);
        request.setQuery(query);
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
    public String parseToScript(String field, Map<String,Object> updateOps) {
        updateOps.entrySet().stream()
                .map( entry -> {
                    switch (entry.getKey()) {
                        case "add":
                            return "ctx._source." + field + ".add(\""+ entry.getValue().toString() +"\")";
                        case "set":
                            return "ctx._source." + field + " = \""+ entry.getValue().toString() +"\"" ;
                        case "inc":
                            return "ctx._source." + field + "+= "+ entry.getValue().toString();
                        case "remove":
                            return "ctx._source." + field + ".removeAll(\"" + entry.getValue().toString() + "\")";
                        case "removeregex":
                            return "ctx._source." + field + " = "+ entry.getValue().toString() ;
                        default:
                            return "";
                    }
                })
        .collect(Collecto);

    }


}
