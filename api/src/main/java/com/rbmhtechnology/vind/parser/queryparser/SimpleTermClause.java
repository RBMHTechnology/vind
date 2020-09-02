package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;

public class SimpleTermClause extends FieldClause {
    TermsLiteral value;

    public SimpleTermClause(boolean negated, String field, TermsLiteral value) {
        super(negated, field);
        this.value = value;
    }

    public TermsLiteral getValue() {
        return value;
    }

    public SimpleTermClause setValue(TermsLiteral value) {
        this.value = value;
        return this;
    }

    @Override
    public Filter toVindFilter(DocumentFactory factory) {
        final FieldDescriptor descriptor = factory.getField(this.getField());
        final Filter termFilter = (Filter)this.getValue().getValues().stream()
                .map(val -> Filter.eq(descriptor, val))
                .collect(Filter.AndCollector);
        if (isNegated()) {
            return Filter.not(termFilter);
        }
        return termFilter;
    }
}
