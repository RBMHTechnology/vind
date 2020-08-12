package com.rbmhtechnology.vind.api;

import com.google.common.collect.ImmutableSet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.filter.parser.FilterLuceneParser;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Set;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;
import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 04.07.16.
 */
public class FilterTest {

    @Test
    public void testAndFilters() {

        Filter f = Filter.AndFilter.fromSet(ImmutableSet.of(eq("a", "1"),eq("b","2"),eq("b","3")));

        Assert.assertTrue(f instanceof Filter.AndFilter);

        assertEquals(3, ((Filter.AndFilter)f).getChildren().size());

        Set<String> values = ImmutableSet.of("a","b","c","d");

        Filter filter = values.stream().map(v -> eq("cat",v)).collect(Filter.AndCollector);

        Assert.assertTrue(filter instanceof Filter.AndFilter);

        assertEquals(4, ((Filter.AndFilter)filter).getChildren().size());
    }

    @Test
    public void testFilterSerializer() throws IOException {

        final FilterLuceneParser filterLuceneParser = new FilterLuceneParser();
        final FieldDescriptor<String> customMetadata = new FieldDescriptorBuilder<>()
            .setFacet(true)
            .buildTextField("customMetadata");

        final DocumentFactory testDocFactory = new DocumentFactoryBuilder("testDoc")
                .addField(customMetadata)
                .build();

        Filter vindFilter = filterLuceneParser
                        .parse(
                                "+customMetadata:(\"coveragedb=true\" AND NOT \"cloudTranscoding=true\")  "
                                , testDocFactory);
        assertEquals("AndFilter",vindFilter.getType());

        vindFilter = filterLuceneParser
                .parse(
                        "+customMetadata:((\"meppGraph=true\" OR \"coveragedb=true\") AND NOT \"cloudTranscoding=true\")  "
                        , testDocFactory);
        assertEquals("AndFilter",vindFilter.getType());


        vindFilter = filterLuceneParser
                .parse(
                        "+customMetadata:((\"meppGraph=true\" OR \"coveragedb=true\") AND NOT ( \"netStorage=true\" AND \"cloudTranscoding=true\"))  "

                        , testDocFactory);
        assertEquals("AndFilter",vindFilter.getType());

     }

}
