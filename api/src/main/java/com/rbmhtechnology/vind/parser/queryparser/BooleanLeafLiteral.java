package com.rbmhtechnology.vind.parser.queryparser;


import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

public class BooleanLeafLiteral extends BooleanLiteral {
    private final String value ;

    public BooleanLeafLiteral(String val) {
        value = val;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {
        //TODO casting to descriptor type
        return Filter.eq(descriptor, value);
    }
}
