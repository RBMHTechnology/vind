package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.rbmhtechnology.vind.SearchServerException;

import java.util.Base64;
import java.util.stream.StreamSupport;


public class CursorUtils   {
    private static ObjectMapper mapper = new ObjectMapper();

    public static String serializeSearchAfter(Object[] searchAfter) {
        final ArrayNode arrayNode = mapper.createArrayNode();
        for( Object item : searchAfter) {
            arrayNode.add(mapper.valueToTree(item));
        }
        return arrayNode.toString();
    }

    public static Object[] deserializeSearchAfter(String searchAfter) {
        try {
            final ArrayNode arrayNode = (ArrayNode)mapper.readTree(searchAfter);
            return StreamSupport.stream(arrayNode.spliterator(), false)
                    .map(node -> {
                        try {
                            return mapper.treeToValue(node,Object.class);
                        } catch (JsonProcessingException e) {
                            throw new SearchServerException("Error deserializing search after values: " + e.getMessage(),e);
                        }
                    })
                    .toArray();

        } catch (JsonProcessingException e) {
            throw new SearchServerException("Error deserializing search after values: " + e.getMessage(),e);
        }
    }

    public static String toSearchAfterCursor(Object[] values) {
        return Base64.getEncoder().encodeToString(serializeSearchAfter(values).getBytes());
    }

    public static Object[] fromSearchAfterCursor(String searchAfter) {
        return deserializeSearchAfter(new String( Base64.getDecoder().decode(searchAfter)));
    }

}
