package com.rbmhtechnology.vind.parser.queryparser;

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
}
