package com.rbmhtechnology.vind.parser.queryparser;

public class SimpleTermClause extends Clause {
    Literal value;

    public SimpleTermClause(boolean negated, String field, Literal value) {
        super(negated, field);
        this.value = value;
    }

    public Literal getValue() {
        return value;
    }

    public SimpleTermClause setValue(Literal value) {
        this.value = value;
        return this;
    }
}
