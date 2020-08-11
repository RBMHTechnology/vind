package com.rbmhtechnology.vind.api.query.inverseSearch;

import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

public abstract class InverseSearchQueryFactory {

    public static final String PERCOLATOR_FIELD = "query";
    private static final String BINARY_QUERY_FIELD_NAME = "query_obj";

    public static final SingleValueFieldDescriptor.BinaryFieldDescriptor<ByteBuffer> BINARY_QUERY_FIELD =
            new FieldDescriptorBuilder().buildBinaryField(BINARY_QUERY_FIELD_NAME);

    static final DocumentFactoryBuilder queryDocumentFactory = new DocumentFactoryBuilder("_percolator_query_")
            .addField(BINARY_QUERY_FIELD);

    public static DocumentFactory getQueryFactory() {
        return queryDocumentFactory.build();
    }

    public static DocumentFactory getQueryFactory(FieldDescriptor<?> ... fields) {
        Optional.ofNullable(fields)
                .ifPresent(fieldDescriptors -> Arrays.asList(fields).forEach(queryDocumentFactory::addField));
        return queryDocumentFactory.build();
    }
}
