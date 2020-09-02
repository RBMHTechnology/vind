package com.rbmhtechnology.vind.parser.queryparser;

import java.util.ArrayList;
import java.util.List;

public class TermsLiteral implements Literal{
    private final List<String> values = new ArrayList<>();

    public TermsLiteral add(String val) {
        values.add(val);
        return this;
    }

    public List<String> getValues() {
        return values;
    }
}
