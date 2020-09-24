package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BinaryBooleanClause extends BooleanClause{
    private final List<String> ops = new ArrayList<>();
    private final List<Clause> clauses = new ArrayList<>();

    public BinaryBooleanClause(String op, Clause leftClause, Clause rightClause){
        this.ops.add(op) ;
        this.clauses.add(leftClause);
        this.clauses.add(rightClause);
    }

    public void addClause(String op, Clause rightClause) {
        this.ops.add(op) ;
        this.clauses.add(rightClause);
    }

    public List<String> getOps() {
        return ops;
    }

    public List<Clause> getClauses() {
        return clauses;
    }


    @Override
    public Filter toVindFilter(DocumentFactory factory) {
        final List<Filter> sortedFilters = new ArrayList<>();
        for (int i = 0; i < ops.size(); i++) {
            final String op = ops.get(i);
            if (op.equals("AND")) {
                if (i == 0) {
                    sortedFilters.add(new Filter.AndFilter(clauses.get(i).toVindFilter(factory), clauses.get(i + 1).toVindFilter(factory)));
                } else {
                    final Filter lasClause = sortedFilters.toArray(new Filter[sortedFilters.size()])[sortedFilters.size()-1];
                    sortedFilters.remove(lasClause);
                    sortedFilters.add(new Filter.AndFilter(lasClause, clauses.get(i + 1).toVindFilter(factory)));
                }
            } else if (op.equals("OR")) {
                if (i == 0) {
                    sortedFilters.add(clauses.get(i).toVindFilter(factory));
                }
                sortedFilters.add(clauses.get(i + 1).toVindFilter(factory));
            } else {
                throw new SearchServerException("Unsuported binary boolean operation '" + op + "' on fields");
            }
        }
        return sortedFilters.stream().collect(Filter.OrCollector);
    }

    @Override
    public String toString() {
        String booleanExp = "(";
        for (int i = 0 ; i < ops.size();i++ ) {
            if( i == 0){
                booleanExp = String.join(" ", booleanExp, clauses.get(i).toString());
            }
            booleanExp = String.join(" ", booleanExp, ops.get(i), clauses.get(i+1).toString());
        }
        return booleanExp + ")";
    }
}
