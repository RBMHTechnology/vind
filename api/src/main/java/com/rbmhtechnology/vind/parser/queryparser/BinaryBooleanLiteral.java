package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

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

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {
        if (op.equals("AND")) {
            return new Filter.AndFilter(leftClause.toVindFilter(descriptor), rightClause.toVindFilter(descriptor));
        }
        if (op.equals("OR")) {
            return new Filter.OrFilter(leftClause.toVindFilter(descriptor), rightClause.toVindFilter(descriptor));
        } else {
            throw new SearchServerException("Unsuported binary boolean operation '"+op+"' on field values");
        }
    }
    @Override
    public String toString() {
        return "(" + this.leftClause.toString()+ " " + this.op + " " + this.rightClause.toString() + ")";
    }

}
