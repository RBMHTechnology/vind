package com.rbmhtechnology.vind.api.query.filter.parser;

import com.rbmhtechnology.vind.api.query.filter.Filter;

public class NotNode implements Node {
    private final Node subject;

    public NotNode(Node subject) {
        this.subject = subject;
    }

    @Override
    public Filter eval(String field) {
        return Filter.not(subject.eval(field));
    }
}
