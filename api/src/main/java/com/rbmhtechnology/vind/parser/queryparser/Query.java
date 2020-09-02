package com.rbmhtechnology.vind.parser.queryparser;

import java.util.ArrayList;

public class Query extends ArrayList<Clause> {
    private String text;

    public Query addText(String text) {
        this.text += text;
        return this;
    }

    public Query setText(String text ) {
        this.text = text;
        return this;
    }

    public String getText() {
        return text;
    }
}
