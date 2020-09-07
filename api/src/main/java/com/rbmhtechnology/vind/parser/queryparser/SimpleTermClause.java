package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.Objects;
import java.util.stream.Collectors;

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
        if (Objects.isNull(descriptor)) {
            throw new SearchServerException("Field [" + this.getField() + "] is not part of document factory " + factory.getType());
        }
        final Filter termFilter = (Filter)this.getValue().getValues().stream()
                .map(val -> Filter.eq(descriptor, val.replaceAll("\\\"", "")))
                .collect(Filter.OrCollector);
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
                        this.getValue().getValues().stream().collect(Collectors.joining(" ")) +
                        ")";
    }
}
