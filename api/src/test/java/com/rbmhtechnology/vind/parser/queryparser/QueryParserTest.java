package com.rbmhtechnology.vind.parser.queryparser;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {

    @Test
    public void testParsings() throws ParseException {
        Query q = parse("some:\"simple quoted test\"");
        assertEquals(1,q.size());
        assertEquals("\"simple quoted test\"",((SimpleTermClause)q.get(0)).getValue().getValues().get(0));

        q = parse("some:test");
        assertEquals(1, q.size());
        assertEquals("test",((SimpleTermClause)q.get(0)).getValue().getValues().get(0));

        q = parse("topic: sports assettype: video image");
        assertEquals(2, q.size());
        assertEquals("sports",((SimpleTermClause)q.get(0)).getValue().getValues().get(0));
        assertEquals("video",((SimpleTermClause)q.get(1)).getValue().getValues().get(0));

        q = parse("topic: \"water sports\" \"formula 1\"");
        assertEquals(1, q.size());
        assertEquals("\"water sports\"",((SimpleTermClause)q.get(0)).getValue().getValues().get(0));
        assertEquals("\"formula 1\"",((SimpleTermClause)q.get(0)).getValue().getValues().get(1));
    }

    private Query parse(String s) throws ParseException {
        QueryParser parser = new QueryParser(toStream(s), StandardCharsets.UTF_8);
        return parser.run();
    }

    private InputStream toStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }


}
