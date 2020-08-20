package com.rbmhtechnology.vind.parser.queryparser;

import java.util.ArrayList;
import java.util.List;

public class ComplexTermClause extends Clause {

    private Query query;

    public ComplexTermClause(boolean negated, String field, Query query) {
        super(negated, field);
        this.query = query;
    }
}
