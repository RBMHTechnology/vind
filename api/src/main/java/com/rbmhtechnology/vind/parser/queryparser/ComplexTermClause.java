package com.rbmhtechnology.vind.parser.queryparser;

public class ComplexTermClause extends FieldClause {

    private Clause query;

    public ComplexTermClause(boolean negated, String field, Clause query) {
        super(negated, field);
        this.query = query;
    }

    public Clause getQuery() {
        return query;
    }
}
