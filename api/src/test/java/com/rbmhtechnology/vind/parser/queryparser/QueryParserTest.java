package com.rbmhtechnology.vind.parser.queryparser;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class QueryParserTest {

    @Test
    public void testParsings() throws ParseException {
        Query q = parse("some:test");
        System.out.println(q);
    }

    private Query parse(String s) throws ParseException {
        QueryParser parser = new QueryParser(toStream(s), StandardCharsets.UTF_8);
        return parser.run();
    }

    private InputStream toStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }


}
