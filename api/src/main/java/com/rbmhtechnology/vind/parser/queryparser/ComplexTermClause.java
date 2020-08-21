package com.rbmhtechnology.vind.parser.queryparser;

public class ComplexTermClause extends Clause {

    private Query query;

    public ComplexTermClause(boolean negated, String field, Query query) {
        super(negated, field);
        this.query = query;
    }
}
