package com.rbmhtechnology.vind.parser.queryparser;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Query extends ArrayList<Clause> {
    private String text;

    public Query addText(String text) {
        this.text = Stream.of(this.text,text).filter(t -> !Strings.isNullOrEmpty(t)).collect(Collectors.joining(" "));
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
