package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

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

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {
        if (op.equals("NOT") || op.equals("-")) {
           return Filter.not(literal.toVindFilter(descriptor));
        } else throw new SearchServerException("Unsuported unary boolean operation '"+op+"' on field values");
    }
}
