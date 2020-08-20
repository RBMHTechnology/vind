package com.rbmhtechnology.vind.parser.queryparser;

public class SimpleTermClause extends Clause {
    String value;

    public SimpleTermClause(boolean negated, String field, String value) {
        super(negated, field);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public SimpleTermClause setValue(String value) {
        this.value = value;
        return this;
    }
}
