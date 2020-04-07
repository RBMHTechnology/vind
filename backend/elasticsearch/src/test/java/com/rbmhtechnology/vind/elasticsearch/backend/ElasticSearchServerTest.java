package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.IndexResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import com.rbmhtechnology.vind.model.value.LatLng;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ElasticSearchServerTest extends ElasticBaseTest {


    @Test
    public void indexTest(){
        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("TestDoc");

        final FieldDescriptor descriptor = new FieldDescriptorBuilder().setFacet(true).buildTextField("title");
        docFactoryBuilder.addField(descriptor);
        final DocumentFactory documents = docFactoryBuilder.build();
        final Document doc1 = documents.createDoc("AA-2X3451")
                .setValue(descriptor, "The last ascent of man");

        final Document doc2 = documents.createDoc("AA-2X6891")
                .setValue(descriptor, "Dawn of humanity: the COVID-19 chronicles");
        final IndexResult indexResult = server.index(doc1,doc2);
        assertNotNull(indexResult);
    }

    @Test
    public void fullTextSearchTest(){
        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("TestDoc");

        final FieldDescriptor descriptor = new FieldDescriptorBuilder()
                .setFacet(true)
                .setFullText(true)
                .buildTextField("title");
        docFactoryBuilder.addField(descriptor);
        final DocumentFactory documents = docFactoryBuilder.build();
        final Document doc1 = documents.createDoc("AA-2X3451")
                .setValue(descriptor, "The last ascent of man");

        final Document doc2 = documents.createDoc("AA-2X6891")
                .setValue(descriptor, "Dawn of humanity: the COVID-19 chronicles");
        server.index(doc1,doc2);

        SearchResult searchResult = server.execute(Search.fulltext("evanescense"), documents);
        assertNotNull(searchResult);
        assertEquals(0, searchResult.getNumOfResults());

        searchResult = server.execute(Search.fulltext(), documents);
        assertNotNull(searchResult);
        assertEquals(2, searchResult.getNumOfResults());

        searchResult = server.execute(Search.fulltext("dawn"), documents);
        assertNotNull(searchResult);
        assertEquals(1, searchResult.getNumOfResults());
    }

    @Test
    public void filterSearchTest(){
        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("TestDoc");

        final FieldDescriptor title = new FieldDescriptorBuilder()
                .setFacet(true)
                .setFullText(true)
                .buildTextField("title");

        final FieldDescriptor description = new FieldDescriptorBuilder()
                .setFacet(true)
                .setFullText(true)
                .buildTextField("description");

        final MultiValueFieldDescriptor.TextFieldDescriptor<String> tags = new FieldDescriptorBuilder()
                .setFacet(true)
                .setFullText(true)
                .buildMultivaluedTextField("tags");

        final MultiValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime>  created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedDateField("created");

        final MultiValueFieldDescriptor.UtilDateFieldDescriptor<Date> published = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedUtilDateField("published");

        final SingleValueFieldDescriptor.NumericFieldDescriptor<Number> rating = new FieldDescriptorBuilder()
                .buildNumericField("rating");

        final SingleValueFieldDescriptor.LocationFieldDescriptor<LatLng> location = new FieldDescriptorBuilder()
                .buildLocationField("location");


        docFactoryBuilder
                .addField(title, description, tags, created, published, rating, location);

        final DocumentFactory documents = docFactoryBuilder.build();
        final Document doc1 = documents.createDoc("AA-2X3451")
                .setValue(title, "The last ascent of man")
                .setValues(tags, "climbing", "pandemia")
                .setValue(rating, 9.5)
                .setValue(created, ZonedDateTime.now())
                .setValue(published, new Date());

        final LatLng wuhan = new LatLng(30.583332,114.283333);

        final Document doc2 = documents.createDoc("AA-2X6891")
                .setValue(title, "Dawn of humanity: the COVID-19 chronicles")
                .setValues(tags, "pandemia")
                .setValue(description,"Earth year 2020; a new breed of virus born within the rural China spreads " +
                        "around the world decimating humanity.")
                .setValue(rating, 9.9)
                .setValue(location, wuhan)
                .setValue(created, ZonedDateTime.now())
                .setValue(published, new Date());

        server.index(doc1,doc2);

        SearchResult searchResult = server.execute(Search.fulltext(), documents);
        assertNotNull(searchResult);
        assertEquals(2, searchResult.getNumOfResults());

        searchResult = server.execute(Search.fulltext()
                .filter(Filter.and(description.isNotEmpty(),rating.greaterThan(9.5))), documents);
        assertNotNull(searchResult);
        assertEquals(1, searchResult.getNumOfResults());

        searchResult = server.execute(Search.fulltext()
                .filter(Filter.or(tags.prefix("climb")
                        ,rating.greaterThan(9.5))), documents);
        assertNotNull(searchResult);
        assertEquals(2, searchResult.getNumOfResults());

        searchResult = server.execute(Search.fulltext()
                .filter(Filter.not(created.between(ZonedDateTime.now().minusDays(1),ZonedDateTime.now().plusDays(1)))), documents);
        assertNotNull(searchResult);
        assertEquals(0, searchResult.getNumOfResults());

        final LatLng salzburg = new LatLng(47.811195, 13.033229);

        searchResult = server.execute(Search.fulltext()
                        .filter(location.withinCircle(salzburg,10000))
        , documents);

        assertNotNull(searchResult);
        assertEquals(0, searchResult.getNumOfResults());

        final LatLng chinaUpperLeftCorner = new LatLng(53.4588044297, 73.6753792663 );
        final LatLng chinaLowerRightCorner = new LatLng(18.197700914,  135.026311477);
        searchResult = server.execute(Search.fulltext()
                .filter(location.withinBBox(chinaUpperLeftCorner, chinaLowerRightCorner)), documents);
        assertNotNull(searchResult);
        assertEquals(1, searchResult.getNumOfResults());
    }

    @Test
    public void facetSearchTest() {
        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("TestDoc");
        final FieldDescriptor descriptor = new FieldDescriptorBuilder()
                .setFacet(true)
                .setFullText(true)
                .buildTextField("title");
        final SingleValueFieldDescriptor.NumericFieldDescriptor<Double> rating = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildNumericField("rating", Double.class);

        final SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> creationDate = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        docFactoryBuilder.addField(descriptor, rating, creationDate);
        final DocumentFactory documents = docFactoryBuilder.build();
        final Document doc1 = documents.createDoc("AA-2X3451")
                .setValue(descriptor, "The last ascent of man")
                .setValue(rating, 7.8)
                .setValue(creationDate, ZonedDateTime.now());

        final Document doc2 = documents.createDoc("AA-2X6891")
                .setValue(descriptor, "Dawn of humanity: the COVID-19 chronicles")
                .setValue(rating, 9.2)
                .setValue(creationDate, ZonedDateTime.now());

        server.index(doc1,doc2);

        final DateMathExpression yesterday =  new DateMathExpression().sub(2, DateMathExpression.TimeUnit.DAY);
        final DateMathExpression tomorrow = new DateMathExpression().add(2, DateMathExpression.TimeUnit.DAY);
        SearchResult searchResult = server.execute(Search.fulltext()
                        .facet(descriptor)
                        .facet(new Facet.TypeFacet())
                        .facet(new Facet.QueryFacet("Awesome_rating", rating.greaterThan(9)))
                        .facet(new Facet.NumericRangeFacet("rating",rating,6,9,1))
                        .facet(new Facet.DateRangeFacet.DateMathRangeFacet("created", creationDate, yesterday, tomorrow, Duration.ofDays(1l)))
                        .facet(new Facet.IntervalFacet.DateIntervalFacet.ZoneDateTimeIntervalFacet(
                                "interval",
                                creationDate,
                                new Interval.ZonedDateTimeInterval("yesterday", ZonedDateTime.now().minusDays(1).truncatedTo(ChronoUnit.DAYS),ZonedDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS))))
                , documents);
        assertNotNull(searchResult);
        assertEquals(2, searchResult.getNumOfResults());
    }

    @Test
    public void realTimeGetTest(){
        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("TestDoc");

        final FieldDescriptor descriptor = new FieldDescriptorBuilder().setFacet(true).buildTextField("title");
        docFactoryBuilder.addField(descriptor);
        final DocumentFactory documents = docFactoryBuilder.build();
        final Document doc1 = documents.createDoc("AA-2X3451")
                .setValue(descriptor, "The last ascent of man");

        final Document doc2 = documents.createDoc("AA-2X6891")
                .setValue(descriptor, "Dawn of humanity: the COVID-19 chronicles");
        server.index(doc1,doc2);

        final GetResult result = server.execute(new RealTimeGet().get("AA-2X3451", "AA-2X6891"), documents);

        assertNotNull(result);
        assertEquals(2, result.getNumOfResults());
        assertTrue(result.getResults().get(0).hasField(descriptor.getName()));
    }
}
