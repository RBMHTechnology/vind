package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.model.value.LatLng;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DocumentUtil {

    public static Map<String, Object> createInputDocument(Document doc) {

        final Map<String, Object> jsonMap = new HashMap<>();
        //add fields
        doc.listFieldDescriptors()
                .values()
                .stream()
                .filter(doc::hasValue)
                //TODO: move again to an approach where we do not go through all the use cases but based on which flags the descriptor has set to true
                .forEach(descriptor ->
                        doc.getFieldContexts(descriptor).stream().forEach(context -> {
                                                    final String fieldName = FieldUtil.getFieldName(descriptor, context);
                                                    if (Objects.nonNull(fieldName)) {
                                                        final Object value = doc.getContextualizedValue(descriptor, context);
                                                        if(Objects.nonNull(value)) {
                                                            jsonMap.put(
                                                                    fieldName,
                                                                    toSolrJType(value)
                                                            );
                                                        }
                                                    }
                                                }
                                        ));

        //TODO: add subdocuments if implemented

        jsonMap.put(FieldUtil.ID, doc.getId());
        jsonMap.put(FieldUtil.TYPE, doc.getType());

        return jsonMap;
    }

    private static Object toSolrJType(Object value) {
        if(value!=null) {
            if(Object[].class.isAssignableFrom(value.getClass())){
                return toSolrJType(Arrays.asList((Object[])value));
            }
            if(Collection.class.isAssignableFrom(value.getClass())){
                return((Collection)value).stream()
                        .map(o -> toSolrJType(o))
                        .collect(Collectors.toList());
            }
            if(value instanceof ZonedDateTime) {
                return Date.from(((ZonedDateTime) value).toInstant());
            }
            if(value instanceof LatLng) {
                return value.toString();
            }
            if(value instanceof Date) {
                //noinspection RedundantCast
                return ((Date) value);
            }
        }
        return value;
    }
}
