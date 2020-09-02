package com.rbmhtechnology.vind.parser.queryparser;

public class BinaryBooleanLiteral extends BooleanLiteral{
    private final String op;
    private final BooleanLiteral leftClause;
    private final BooleanLiteral rightClause;

    public BinaryBooleanLiteral(String op, BooleanLiteral leftClause, BooleanLiteral rightClause){
        this.op = op;
        this.leftClause = leftClause;
        this.rightClause = rightClause;
    }

    public String getOp() {
        return op;
    }

    public BooleanLiteral getLeftClause() {
        return leftClause;
    }

    public BooleanLiteral getRightClause() {
        return rightClause;
    }
}
