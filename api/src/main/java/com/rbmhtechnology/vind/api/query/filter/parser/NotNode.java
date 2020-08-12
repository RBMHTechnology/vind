package com.rbmhtechnology.vind.api.query.filter.parser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

public class NotNode implements Node {
    private final Node subject;

    public NotNode(Node subject) {
        this.subject = subject;
    }

    @Override
    public Filter eval(FieldDescriptor field) {
        return Filter.not(subject.eval(field));
    }
}
