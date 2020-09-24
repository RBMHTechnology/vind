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

        Filter baseFilter = null;
        for (int i = 0 ; i < ops.size();i++ ) {
            final String op = ops.get(i);
            if (op.equals("AND")) {
                if ( i == 0) {
                    baseFilter  = new Filter.AndFilter(clauses.get(i).toVindFilter(factory), clauses.get(i+1).toVindFilter(factory));
                } else {
                    if ("AndFilter".equals(baseFilter.getType())){
                        ((Filter.AndFilter) baseFilter).getChildren().add(clauses.get(i+1).toVindFilter(factory));

                    } else {
                        final HashSet<Filter> orChildren = (HashSet<Filter>) ((Filter.OrFilter) baseFilter).getChildren();
                        orChildren.remove(clauses.get(i).toVindFilter(factory));
                        orChildren.add(new Filter.AndFilter(clauses.get(i).toVindFilter(factory), clauses.get(i+1).toVindFilter(factory)));
                    }
                }

            }else if (op.equals("OR")) {
                if ( i == 0) {
                    baseFilter =  new Filter.OrFilter(clauses.get(i).toVindFilter(factory), clauses.get(i+1).toVindFilter(factory));
                } else {
                    if ("OrFilter".equals(baseFilter.getType())) {
                        ((Filter.OrFilter) baseFilter).getChildren().add(clauses.get(i + 1).toVindFilter(factory));

                    } else {
                        final Filter.AndFilter leftClause = (Filter.AndFilter) baseFilter;
                        baseFilter = new Filter.OrFilter(leftClause, clauses.get(i + 1).toVindFilter(factory));
                    }
                }
            } else {
                throw new SearchServerException("Unsuported binary boolean operation '"+op+"' on fields");
            }
        }
        return baseFilter;
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
