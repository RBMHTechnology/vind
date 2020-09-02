package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;

public class ComplexTermClause extends FieldClause {

    private BooleanLiteral query;

    public ComplexTermClause(boolean negated, String field, BooleanLiteral query) {
        super(negated, field);
        this.query = query;
    }

    public BooleanLiteral getQuery() {
        return query;
    }

    @Override
    public Filter toVindFilter(DocumentFactory factory) {
        final FieldDescriptor descriptor = factory.getField(this.getField());
        final Filter complexFilter =  this.getQuery().toVindFilter(descriptor);

        if (isNegated()) {
            return Filter.not(complexFilter);
        }
        return complexFilter;
    }
}
