package com.rbmhtechnology.vind.parser.queryparser;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class QueryParserTest {

    @Test
    public void testParsings() throws ParseException {
        parse("some:test");
    }

    private Query parse(String s) throws ParseException {
        QueryParser parser = new QueryParser(toStream(s), StandardCharsets.UTF_8);
        return null;
    }

    private InputStream toStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }


}
