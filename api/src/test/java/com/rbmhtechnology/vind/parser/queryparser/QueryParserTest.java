package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.filter.parser.FilterLuceneParser;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class QueryParserTest {

    @Test
    public void testParsings() throws ParseException {
        Query q = parse("some:test");
        assertEquals(1, q.size());
        assertEquals("test",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(0));

        q = parse("some:test text");
        assertEquals(1, q.size());
        assertEquals("text", q.getText());
        assertEquals("test",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(0));

        q = parse("some:test \"fulltext text\"");
        assertEquals("\"fulltext text\"", q.getText());

        q = parse("some:\"simple quoted test\"");
        assertEquals(1,q.size());
        assertEquals("\"simple quoted test\"",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(0));

        q = parse("topic: sports assettype: (video image)");
        assertEquals(2, q.size());
        assertEquals("sports",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(0));
        assertEquals("video",((TermsLiteral)((SimpleTermClause)q.get(1)).getValue()).getValues().get(0));

        q = parse("topic:( \"water sports\" \"formula 1\")");
        assertEquals(1, q.size());
        assertEquals("\"water sports\"",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(0));
        assertEquals("\"formula 1\"",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(1));

        q = parse("topic:( water sports \"formula 1\") text \"full Text\"");
        assertEquals(1, q.size());
        assertEquals("text \"full Text\"", q.getText());
        assertEquals("water",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(0));
        assertEquals("sports",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(1));
        assertEquals("\"formula 1\"",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(2));

        q = parse("(topic: water OR assettype: video)");
        assertEquals(1, q.size());
        assertEquals("OR",((BinaryBooleanClause)q.get(0)).getOp());
        assertEquals("water",((TermsLiteral)((SimpleTermClause)((BinaryBooleanClause)q.get(0)).getLeftClause()).getValue()).getValues().get(0));

        q = parse("(topic: water OR NOT(assettype: video))");
        assertEquals(1, q.size());
        assertEquals("OR",((BinaryBooleanClause)q.get(0)).getOp());
        assertEquals("water",((TermsLiteral)((SimpleTermClause)((BinaryBooleanClause)q.get(0)).getLeftClause()).getValue()).getValues().get(0));
        assertEquals("NOT",((UnaryBooleanClause)((BinaryBooleanClause)q.get(0)).getRightClause()).getOp());
        assertEquals("video",((TermsLiteral)((SimpleTermClause)((UnaryBooleanClause)((BinaryBooleanClause)q.get(0)).getRightClause()).getClause()).getValue()).getValues().get(0));

        q = parse("((topic: water AND athlete:\"Adam Ondra\") OR NOT(assettype: video)) \"fulltext text\"");
        assertEquals(1, q.size());
        assertEquals("\"fulltext text\"", q.getText());
        assertEquals("OR",((BinaryBooleanClause)q.get(0)).getOp());
        assertEquals("NOT",((UnaryBooleanClause)((BinaryBooleanClause)q.get(0)).getRightClause()).getOp());
        assertEquals("video",((TermsLiteral)((SimpleTermClause)((UnaryBooleanClause)((BinaryBooleanClause)q.get(0)).getRightClause()).getClause()).getValue()).getValues().get(0));

        q = parse("some:(test OR sample)");
        assertEquals(1, q.size());
        assertEquals("OR",((BinaryBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getOp());
        assertEquals("test",((BooleanLeafLiteral)((BinaryBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getLeftClause()).getValue());
        assertEquals("sample",((BooleanLeafLiteral)((BinaryBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getRightClause()).getValue());

        q = parse("some:(NOT test OR ( sample AND fake)))");
        assertEquals(1, q.size());
        assertEquals("OR",((BinaryBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getOp());
        assertEquals("NOT",((UnaryBooleanLiteral)((BinaryBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getLeftClause()).getOp());
        assertEquals("sample",((BooleanLeafLiteral)((BinaryBooleanLiteral)((BinaryBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getRightClause()).getLeftClause()).getValue());

        q = parse("some: [ 23 TO 56 ]");
        assertEquals(1, q.size());
        assertEquals("23",((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getFrom());
        assertEquals("56",((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getTo());
    }


    @Test
    public void testFilterSerializer() throws IOException {

        final VindQueryParser filterLuceneParser = new VindQueryParser();
        final FieldDescriptor<String> customMetadata = new FieldDescriptorBuilder<>()
                .setFacet(true)
                .buildTextField("customMetadata");

        final FieldDescriptor<String> athlete = new FieldDescriptorBuilder<>()
                .setFacet(true)
                .buildTextField("athlete");

        final FieldDescriptor<String> assetType = new FieldDescriptorBuilder<>()
                .setFacet(true)
                .buildTextField("assettype");

        final DocumentFactory testDocFactory = new DocumentFactoryBuilder("testDoc")
                .addField(customMetadata, assetType, athlete)
                .build();

        FulltextSearch vindFilter = filterLuceneParser
                .parse(
                        "+customMetadata:(\"coveragedb=true\" AND NOT \"cloudTranscoding=true\")  "
                        , testDocFactory);
        assertEquals("AndFilter",vindFilter.getFilter().getType());

        vindFilter = filterLuceneParser
                .parse(
                        "+customMetadata:((\"meppGraph=true\" OR \"coveragedb=true\") AND NOT \"cloudTranscoding=true\")  "
                        , testDocFactory);
        assertEquals("AndFilter",vindFilter.getFilter().getType());


        vindFilter = filterLuceneParser
                .parse(
                        "+customMetadata:((\"meppGraph=true\" OR \"coveragedb=true\") AND NOT ( \"netStorage=true\" AND \"cloudTranscoding=true\"))  "

                        , testDocFactory);
        assertEquals("AndFilter",vindFilter.getFilter().getType());

        vindFilter = filterLuceneParser
                .parse(
                        "((customMetadata: water AND athlete:\"Adam Ondra\") OR NOT(assettype: video))"

                        , testDocFactory);
        assertEquals("OrFilter",vindFilter.getFilter().getType());

    }

    private Query parse(String s) throws ParseException {
        QueryParser parser = new QueryParser(toStream(s), StandardCharsets.UTF_8);
        return parser.run();
    }

    private InputStream toStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

}
