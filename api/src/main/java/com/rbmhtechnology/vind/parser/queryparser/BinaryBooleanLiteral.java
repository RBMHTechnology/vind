package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BinaryBooleanLiteral extends BooleanLiteral{
    private final List<String> ops = new ArrayList<>();
    private final List<BooleanLiteral> clauses = new ArrayList<>();

    public BinaryBooleanLiteral(String op, BooleanLiteral leftClause, BooleanLiteral rightClause){
        this.ops.add(op);
        this.clauses.add(leftClause);
        this.clauses.add(rightClause);
    }

    public void addClause(String op, BooleanLiteral rightClause) {
        this.ops.add(op) ;
        this.clauses.add(rightClause);
    }

    public List<String> getOps() {
        return ops;
    }
    public List<BooleanLiteral> getClauses() {
        return clauses;
    }

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {
        final List<Filter> sortedFilters = new ArrayList<>();
        for (int i = 0; i < ops.size(); i++) {
            final String op = ops.get(i);
            if (op.equals("AND")) {
                if (i == 0) {
                    sortedFilters.add(new Filter.AndFilter(clauses.get(i).toVindFilter(descriptor), clauses.get(i + 1).toVindFilter(descriptor)));
                } else {
                    final Filter lasClause = sortedFilters.toArray(new Filter[sortedFilters.size()])[sortedFilters.size()-1];
                    sortedFilters.remove(lasClause);
                    sortedFilters.add(new Filter.AndFilter(lasClause, clauses.get(i + 1).toVindFilter(descriptor)));
                }

            } else if (op.equals("OR")) {
                if (i == 0) {
                    sortedFilters.add(clauses.get(i).toVindFilter(descriptor));
                }
                sortedFilters.add(clauses.get(i + 1).toVindFilter(descriptor));
            } else {
                throw new SearchServerException("Unsuported binary boolean operation '" + op + "' on field values");
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
