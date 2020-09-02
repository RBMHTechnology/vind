package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;

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

    @Override
    public Filter toVindFilter(DocumentFactory factory) {
        if (op.equals("AND")) {
            return new Filter.AndFilter(leftClause.toVindFilter(factory), rightClause.toVindFilter(factory));
        }
        if (op.equals("OR")) {
            return new Filter.OrFilter(leftClause.toVindFilter(factory), rightClause.toVindFilter(factory));
        } else {
            throw new SearchServerException("Unsuported binary boolean operation '"+op+"' on fields");
        }
    }
}
