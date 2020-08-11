package com.rbmhtechnology.vind.api.query.filter.parser;

import com.rbmhtechnology.vind.api.query.filter.Filter;

public class LiteralNode implements Node {

    private final String value;

    public LiteralNode(String value) {
        this.value = value;
    }
    @Override
    public Filter eval(String field) {
        return Filter.eq(field, value);
    }
}
