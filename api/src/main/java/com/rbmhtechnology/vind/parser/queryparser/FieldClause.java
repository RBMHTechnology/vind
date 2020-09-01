package com.rbmhtechnology.vind.parser.queryparser;

public abstract class FieldClause  implements Clause{

    private boolean negated = false;

    private String field = null;

    public FieldClause(boolean negated, String field) {
        this.negated = negated;
        this.field = field;
    }

    public boolean isNegated() {
        return negated;
    }

    public FieldClause setNegated(boolean negated) {
        this.negated = negated;
        return this;
    }
}
