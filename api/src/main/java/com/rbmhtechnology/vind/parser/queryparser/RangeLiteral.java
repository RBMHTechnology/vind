package com.rbmhtechnology.vind.parser.queryparser;

public abstract class RangeLiteral extends SimpleLiteral{
    public static final String WILDCARD = "*";
    private Object from;
    private Object to;

    public abstract Object getFrom();

    public abstract Object getTo();

    @Override
    public String toString() {
        final String stringFrom = from != null? from.toString() : "*" ;
        final String stringTo = to != null? to.toString() : "*" ;
        return "[ " + stringFrom + " TO "+ stringTo + " ]";
    }


}
