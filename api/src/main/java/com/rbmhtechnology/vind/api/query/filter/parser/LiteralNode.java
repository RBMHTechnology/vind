package com.rbmhtechnology.vind.api.query.filter.parser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

public class LiteralNode implements Node {

    private final String value;

    public LiteralNode(String value) {
        this.value = value;
    }
    @Override
    public Filter eval(FieldDescriptor  field) {
        return Filter.eq(field, value);
    }
}
