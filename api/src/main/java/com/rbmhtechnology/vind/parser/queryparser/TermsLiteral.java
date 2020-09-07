package com.rbmhtechnology.vind.parser.queryparser;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TermsLiteral implements Literal{
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

}
