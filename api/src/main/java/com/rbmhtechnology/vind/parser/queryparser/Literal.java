package com.rbmhtechnology.vind.parser.queryparser;

import java.util.ArrayList;
import java.util.List;

public class Literal {
    private final List<String> values = new ArrayList<>();

    public Literal add(String val) {
        values.add(val);
        return this;
    }

    public List<String> getValues() {
        return values;
    }
}
