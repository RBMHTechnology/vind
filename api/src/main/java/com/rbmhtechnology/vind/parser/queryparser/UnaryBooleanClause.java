package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;

public class UnaryBooleanClause extends BooleanClause{
    private final String op;
    private final Clause clause;

    public UnaryBooleanClause(String op, Clause clause){
        this.op = op;
        this.clause = clause;
    }

    public String getOp() {
        return op;
    }

    public Clause getClause() {
        return clause;
    }

    @Override
    public Filter toVindFilter(DocumentFactory factory) {
        return Filter.not(clause.toVindFilter(factory));
    }

    @Override
    public String toString() {
        return this.op + " " +this.clause.toString();
    }
}
