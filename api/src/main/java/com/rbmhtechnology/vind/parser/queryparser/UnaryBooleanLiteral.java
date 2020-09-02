package com.rbmhtechnology.vind.parser.queryparser;

public class UnaryBooleanLiteral extends BooleanLiteral{
    private final String op;
    private final BooleanLiteral literal;

    public UnaryBooleanLiteral(String op, BooleanLiteral literal){
        this.op = op;
        this.literal = literal;
    }

    public String getOp() {
        return op;
    }

    public BooleanLiteral getLiteral() {
        return literal;
    }
}
