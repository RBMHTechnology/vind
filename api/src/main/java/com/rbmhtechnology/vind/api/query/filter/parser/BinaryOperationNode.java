package com.rbmhtechnology.vind.api.query.filter.parser;

import com.rbmhtechnology.vind.api.query.filter.Filter;

public class BinaryOperationNode implements Node {
    final private Node leftClause;
    final private Node rightClause;
    private final Operator operator;

    public BinaryOperationNode(Node leftClause, Node rightClause, Operator operator) {
        this.leftClause = leftClause;
        this.rightClause = rightClause;
        this.operator = operator;
    }

    @Override
    public Filter eval(String field) {
        switch (operator){
            case OR:
                return Filter.or(leftClause.eval(field),rightClause.eval(field));
            case AND:
                return Filter.and(leftClause.eval(field),rightClause.eval(field));
            default:
                throw new RuntimeException("Error parsing lucene filter: unsupported operator.");
        }
    }
    public enum Operator {
        AND,OR;
    }
}
