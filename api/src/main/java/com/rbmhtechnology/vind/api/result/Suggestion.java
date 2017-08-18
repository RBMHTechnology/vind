package com.rbmhtechnology.vind.api.result;

/**
 */
public class Suggestion {

    private String field, value, displayValue;

    public Suggestion(String field, String value, String displayValue) {
        this.field = field;
        this.value = value;
        this.displayValue = displayValue;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}
