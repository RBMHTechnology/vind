package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.datemath.DateMathParser;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

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
        assertEquals("OR",((MultiBooleanClause)q.get(0)).getOps().get(0));
        assertEquals("water",((TermsLiteral)((SimpleTermClause)((MultiBooleanClause)q.get(0)).getClauses().get(0)).getValue()).getValues().get(0));

        q = parse("(topic: water OR NOT(assettype: video))");
        assertEquals(1, q.size());
        assertEquals("OR",((MultiBooleanClause)q.get(0)).getOps().get(0));
        assertEquals("water",((TermsLiteral)((SimpleTermClause)((MultiBooleanClause)q.get(0)).getClauses().get(0)).getValue()).getValues().get(0));
        assertEquals("NOT",((UnaryBooleanClause)((MultiBooleanClause)q.get(0)).getClauses().get(1)).getOp());
        assertEquals("video",((TermsLiteral)((SimpleTermClause)((UnaryBooleanClause)((MultiBooleanClause)q.get(0)).getClauses().get(1)).getClause()).getValue()).getValues().get(0));

        q = parse("((topic: water AND athlete:\"Adam Ondra\") OR NOT(assettype: video)) \"fulltext text\"");
        assertEquals(1, q.size());
        assertEquals("\"fulltext text\"", q.getText());
        assertEquals("OR",((MultiBooleanClause)q.get(0)).getOps().get(0));
        assertEquals("NOT",((UnaryBooleanClause)((MultiBooleanClause)q.get(0)).getClauses().get(1)).getOp());
        assertEquals("video",((TermsLiteral)((SimpleTermClause)((UnaryBooleanClause)((MultiBooleanClause)q.get(0)).getClauses().get(1)).getClause()).getValue()).getValues().get(0));

        q = parse("some:(test OR sample)");
        assertEquals(1, q.size());
        assertEquals("OR",((MultiBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getOps().get(0));
        assertEquals("test",((BooleanLeafLiteral)((MultiBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getClauses().get(0)).getValue());
        assertEquals("sample",((BooleanLeafLiteral)((MultiBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getClauses().get(1)).getValue());

        q = parse("some:(NOT test OR ( sample AND fake)))");
        assertEquals(1, q.size());
        assertEquals("OR",((MultiBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getOps().get(0));
        assertEquals("NOT",((UnaryBooleanLiteral)((MultiBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getClauses().get(0)).getOp());
        assertEquals("sample",((BooleanLeafLiteral)((MultiBooleanLiteral)((MultiBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getClauses().get(1)).getClauses().get(0)).getValue());

        q = parse("some:(NOT test OR sample AND fake)");
        assertEquals(1, q.size());
        assertEquals("OR",((MultiBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getOps().get(0));
        assertEquals("NOT",((UnaryBooleanLiteral)((MultiBooleanLiteral)((ComplexTermClause)q.get(0)).getQuery()).getClauses().get(0)).getOp());
    }

    @Test
    public void testRangeParsings() throws ParseException {
        Query q = parse("field: [23 TO 56 ]");
        assertEquals(1, q.size());
        assertEquals(23,((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getFrom());
        assertEquals(56,((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getTo());

        q = parse("field: [23.0 TO 56.45 ]");
        assertEquals(1, q.size());
        assertEquals(23.0F,((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getFrom());
        assertEquals(56.45F,((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getTo());

        q = parse("field: [03-02-1999 TO 09-12-2060 ]");
        assertEquals(1, q.size());
        assertEquals(new DateMathParser().parseMath("03-02-1999").toString(),((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getFrom().toString());
        assertEquals(new DateMathParser().parseMath("09-12-2060").toString(),((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getTo().toString());

        q = parse("field: [03-02-1999 TO * ]");
        assertEquals(1, q.size());
        assertEquals(new DateMathParser().parseMath("03-02-1999").toString(),((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getFrom().toString());
        assertEquals(null,((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getTo());

        q = parse("field: [* TO 56 ]");
        assertEquals(1, q.size());
        assertEquals(null,((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getFrom());
        assertEquals(56,((RangeLiteral)((SimpleTermClause)q.get(0)).getValue()).getTo());
    }

    @Test
    public void testDotFieldNameParsings() throws ParseException {
        Query q = parse("some.field:test");
        assertEquals(1, q.size());
        assertEquals("test",((TermsLiteral)((SimpleTermClause)q.get(0)).getValue()).getValues().get(0));
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

        final FieldDescriptor<Number> yearName = new FieldDescriptorBuilder<>()
                .setFacet(true)
                .buildNumericField("year.name");

        final FieldDescriptor<ZonedDateTime> fromDate = new FieldDescriptorBuilder<>()
                .setFacet(true)
                .buildDateField("fromDate");

        final FieldDescriptor<ZonedDateTime> toDate = new FieldDescriptorBuilder<>()
                .setFacet(true)
                .buildDateField("toDate");

        final DocumentFactory testDocFactory = new DocumentFactoryBuilder("testDoc")
                .addField(customMetadata, assetType, athlete, yearName, fromDate, toDate)
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

        vindFilter = filterLuceneParser
                .parse(
                        "(assettype:Season AND year.name:2017)"

                        , testDocFactory);
        assertEquals("AndFilter",vindFilter.getFilter().getType());

        vindFilter = filterLuceneParser
                .parse(
                        "(customMetadata: water AND athlete:\"Adam Ondra\" OR NOT(assettype: video))"

                        , testDocFactory);
        assertEquals("OrFilter",vindFilter.getFilter().getType());

        vindFilter = filterLuceneParser
                .parse(
                        "(customMetadata: water AND (athlete:\"Adam Ondra\" OR NOT(assettype: video)))"

                        , testDocFactory);
        assertEquals("AndFilter",vindFilter.getFilter().getType());

        vindFilter = filterLuceneParser
                .parse(
                        "(fromDate:[01-01-2010 TO 10-03-2020] AND toDate:[* TO 2020-01-01])"

                        , testDocFactory);
        assertEquals("AndFilter",vindFilter.getFilter().getType());

        vindFilter = filterLuceneParser
                .parse(
                        "customMetadata:(\"resourceType=derivative\" OR \"resourceGroup=other\" AND \"contentType=video\" AND \"videri=true\")"
                        , testDocFactory);
        assertEquals("OrFilter",vindFilter.getFilter().getType());

        vindFilter = filterLuceneParser
                .parse(
                        "full text search AND some OR \"quoted string\""
                        , testDocFactory);
        assertEquals("full text search AND some OR \"quoted string\"",vindFilter.getSearchString());

        vindFilter = filterLuceneParser
                .parse(
                        "customMetadata:(\"resourceType=derivative\" AND \"contentType=video\" AND ((\"resourceGroup=other\" AND NOT \"derivativeType=AX-1N9BK55DD1111\") OR (\"derivativeType=AX-23JPRQ73S1W11\")))"
                        , testDocFactory);
        assertEquals("*",vindFilter.getSearchString());

        vindFilter = filterLuceneParser
                .parse(
                        "(customMetadata:CoverageDBProject AND year.name:[01-08-2020 TO 01-08-2020]) fulltext"
                        , testDocFactory);
        assertEquals("fulltext",vindFilter.getSearchString());

        vindFilter = filterLuceneParser
                .parse(
                        "(customMetadata:MediaPlanetProject or customMetadata:Event)"
                        , testDocFactory);
        assertEquals("*",vindFilter.getSearchString());

        vindFilter = filterLuceneParser
                .parse(
                        "customMetadata:((\"resourceType=mezzanine\" OR \"resourceType=essence\") AND \"cloudTranscoding_normal=true\")"
                        , testDocFactory);
        assertEquals("*",vindFilter.getSearchString());
        assertEquals(2,((Filter.AndFilter)vindFilter.getFilter()).getChildren().size());
        assertEquals("cloudTranscoding_normal=true",((Filter.DescriptorFilter)((Filter.AndFilter)vindFilter.getFilter()).getChildren().toArray()[1]).getTerm());


        //(type:MediaPlanetProject or type:Event)
        //customMetadata:((\"resourceType=mezzanine\" OR \"resourceType=essence\") AND \"cloudTranscoding_normal=true\")

    }

    private Query parse(String s) throws ParseException {
        QueryParser parser = new QueryParser(toStream(s), StandardCharsets.UTF_8);
        return parser.run();
    }

    private InputStream toStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

}
