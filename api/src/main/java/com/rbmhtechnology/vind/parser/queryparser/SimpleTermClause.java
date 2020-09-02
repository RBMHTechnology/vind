package com.rbmhtechnology.vind.parser.queryparser;

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
}
