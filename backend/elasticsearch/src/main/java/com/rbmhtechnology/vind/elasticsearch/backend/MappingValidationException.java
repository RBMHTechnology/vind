package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.SearchServerException;

public class MappingValidationException extends SearchServerException {
    public MappingValidationException(String m) {
        super(m);
    }

    public MappingValidationException(String m, Throwable t) {
        super(m, t);
    }
}
