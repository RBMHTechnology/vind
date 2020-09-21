package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

public abstract class SimpleLiteral implements Literal{

    abstract Filter toVindFilter(FieldDescriptor descriptor);
}
