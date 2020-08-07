package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.annotations.language.Language;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.api.query.facet.TermFacetOption;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearch;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.IndexResult;
import com.rbmhtechnology.vind.api.result.InverseSearchResult;
import com.rbmhtechnology.vind.api.result.PageResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.api.result.facet.RangeFacetResult;
import com.rbmhtechnology.vind.api.result.facet.TermFacetResult;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.ComplexFieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.InverseSearchQuery;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.NumericFieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValuedComplexField;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValuedComplexField;
import com.rbmhtechnology.vind.model.value.LatLng;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.rbmhtechnology.vind.api.query.datemath.DateMathExpression.TimeUnit.DAY;
import static com.rbmhtechnology.vind.api.query.datemath.DateMathExpression.TimeUnit.HOUR;
import static com.rbmhtechnology.vind.api.query.datemath.DateMathExpression.TimeUnit.MILLISECONDS;
import static com.rbmhtechnology.vind.api.query.facet.Facets.interval;
import static com.rbmhtechnology.vind.api.query.facet.Facets.pivot;
import static com.rbmhtechnology.vind.api.query.facet.Facets.query;
import static com.rbmhtechnology.vind.api.query.facet.Facets.range;
import static com.rbmhtechnology.vind.api.query.facet.Facets.stats;
import static com.rbmhtechnology.vind.api.query.facet.Facets.type;
import static com.rbmhtechnology.vind.api.query.filter.Filter.Scope;
import static com.rbmhtechnology.vind.api.query.filter.Filter.and;
import static com.rbmhtechnology.vind.api.query.filter.Filter.before;
import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;
import static com.rbmhtechnology.vind.api.query.filter.Filter.hasChildrenDocuments;
import static com.rbmhtechnology.vind.api.query.filter.Filter.not;
import static com.rbmhtechnology.vind.api.query.filter.Filter.or;
import static com.rbmhtechnology.vind.api.query.sort.Sort.asc;
import static com.rbmhtechnology.vind.api.query.sort.Sort.desc;
import static com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.DateFieldDescriptor;
import static com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.LocationFieldDescriptor;
import static com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.TextFieldDescriptor;
import static com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.UtilDateFieldDescriptor;
import static com.rbmhtechnology.vind.test.Backend.Elastic;
import static com.rbmhtechnology.vind.test.Backend.Solr;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 15.06.16.
 */
public class InverseSearchTest {

    @Rule
    public TestBackend testBackend = new TestBackend();

    @Test
    @RunWithBackend(Elastic)
    public void testInverseSearch() {
        final SearchServer server = testBackend.getSearchServer();
        final SingleValueFieldDescriptor.TextFieldDescriptor title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        final SingleValueFieldDescriptor.TextFieldDescriptor volume = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("volume");

        final DocumentFactory testDocsFactory = new DocumentFactoryBuilder("TestDocument")
                .addField(title)
                .addInverseSearchMetaField(volume)
                .build();

        // Reverse Search
        Document d1 = testDocsFactory.createDoc("1")
                .setValue(title, "Hello World");
        server.index(d1);

        final InverseSearchQuery inverseSearchQuery =
                testDocsFactory.createInverseSearchQuery("testQuery1", title.equals("Hello World"))
                    .setValue(volume,"volume1");
        final IndexResult indexResult =
                server.addInverseSearchQuery(inverseSearchQuery);

        InverseSearch inverseSearch = Search.inverseSearch(d1).setQueryFilter(volume.equals("volume1"));
        InverseSearchResult result = server.execute(inverseSearch, testDocsFactory);
        assertEquals(1, result.getNumOfResults());

        inverseSearch = Search.inverseSearch(d1).setQueryFilter(volume.equals("v1"));
        result = server.execute(inverseSearch, testDocsFactory);
        assertEquals(0, result.getNumOfResults());

    }
}
