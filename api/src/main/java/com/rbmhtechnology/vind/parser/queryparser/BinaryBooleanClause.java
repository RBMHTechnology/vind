package com.rbmhtechnology.vind.parser.queryparser;

public class BinaryBooleanClause extends BooleanClause{
    private final String op;
    private final Clause leftClause;
    private final Clause rightClause;

    public BinaryBooleanClause(String op, Clause leftClause, Clause rightClause){
        this.op = op;
        this.leftClause = leftClause;
        this.rightClause = rightClause;
    }

    public String getOp() {
        return op;
    }

    public Clause getLeftClause() {
        return leftClause;
    }

    public Clause getRightClause() {
        return rightClause;
    }
}
