package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

public abstract class BooleanLiteral implements Literal{
    public abstract Filter toVindFilter(FieldDescriptor descriptor);
}
