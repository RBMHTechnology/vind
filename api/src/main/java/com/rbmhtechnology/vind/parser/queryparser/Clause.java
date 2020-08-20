package com.rbmhtechnology.vind.parser.queryparser;

public abstract class Clause {

    private boolean negated = false;

    private String field = null;

    public Clause(boolean negated, String field) {
        this.negated = negated;
        this.field = field;
    }

    public boolean isNegated() {
        return negated;
    }

    public Clause setNegated(boolean negated) {
        this.negated = negated;
        return this;
    }
}
