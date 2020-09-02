package com.rbmhtechnology.vind.parser.queryparser;

public class ComplexTermClause extends FieldClause {

    private BooleanLiteral query;

    public ComplexTermClause(boolean negated, String field, BooleanLiteral query) {
        super(negated, field);
        this.query = query;
    }

    public BooleanLiteral getQuery() {
        return query;
    }
}
