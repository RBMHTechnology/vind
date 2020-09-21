package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.Objects;

public class SimpleTermClause extends FieldClause {
     private SimpleLiteral value;

    public SimpleTermClause(boolean negated, String field, SimpleLiteral value) {
        super(negated, field);
        this.value = value;
    }

    public SimpleLiteral getValue() {
        return value;
    }

    public SimpleTermClause setValue(TermsLiteral value) {
        this.value = value;
        return this;
    }

    @Override
    public Filter toVindFilter(DocumentFactory factory) {
        final FieldDescriptor descriptor = factory.getField(this.getField());
        if (Objects.isNull(descriptor)) {
            throw new SearchServerException("Field [" + this.getField() + "] is not part of document factory " + factory.getType());
        }
        final Filter termFilter = this.value.toVindFilter(descriptor);
        if (isNegated()) {
            return Filter.not(termFilter);
        }
        return termFilter;
    }

    @Override
    public String toString() {
        return
                isNegated()? "-" : "" +
                        this.getField() + ":(" +
                        this.getValue().toString() +
                        ")";
    }
}
