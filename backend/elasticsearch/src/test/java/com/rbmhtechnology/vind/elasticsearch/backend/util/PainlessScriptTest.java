package com.rbmhtechnology.vind.elasticsearch.backend.util;

import org.junit.Test;

import static com.rbmhtechnology.vind.elasticsearch.backend.util.PainlessScript.Statement.getStringPredicate;
import static org.junit.Assert.assertEquals;

public class PainlessScriptTest {

    @Test
    public void testGetPredicateString() {
        final String[] predicate = {"tradition", "survival"};
        String stringPredicate = getStringPredicate(predicate, String.class);
        assertEquals("['tradition', 'survival']", stringPredicate);
    }
}
