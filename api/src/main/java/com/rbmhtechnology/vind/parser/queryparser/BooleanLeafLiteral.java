package com.rbmhtechnology.vind.parser.queryparser;


public class BooleanLeafLiteral extends BooleanLiteral {
    private final String value ;

    public BooleanLeafLiteral(String val) {
        value = val;
    }

    public String getValue() {
        return value;
    }
}
