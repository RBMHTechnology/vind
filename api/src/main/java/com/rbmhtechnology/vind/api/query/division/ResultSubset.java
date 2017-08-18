package com.rbmhtechnology.vind.api.query.division;

/**
 * Created by fonso on 31.03.17.
 */
public abstract class ResultSubset {

    protected DivisionType type;
    public abstract ResultSubset copy();

    public DivisionType getType() {
        return type;
    }

    public enum DivisionType {
        slice, page
    }
}
