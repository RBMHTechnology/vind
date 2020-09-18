package com.rbmhtechnology.vind.parser.queryparser;

import com.google.common.base.Strings;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TermsLiteral  extends SimpleLiteral{
    private final List<String> values = new ArrayList<>();

    public TermsLiteral add(String val) {
        values.add(val);
        return this;
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return this.values.stream()
                .filter(val -> !Strings.isNullOrEmpty(val)).collect(Collectors.joining(" "));
    }

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {
        final Filter termFilter = (Filter)this.getValues().stream()
                .map(val -> Filter.eq(descriptor, val.replaceAll("\\\"", "")))
                .collect(Filter.OrCollector); //TODO: default OR, should be able to change

        return termFilter;
    }
}
