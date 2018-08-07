package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.annotations.language.Language;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.PageResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.api.result.facet.RangeFacetResult;
import com.rbmhtechnology.vind.api.result.facet.TermFacetResult;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.*;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.NumericFieldDescriptor;
import com.rbmhtechnology.vind.model.value.LatLng;
import com.rbmhtechnology.vind.utils.mam.FacetMapper;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.rbmhtechnology.vind.api.query.datemath.DateMathExpression.TimeUnit.*;
import static com.rbmhtechnology.vind.api.query.facet.Facets.*;
import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static com.rbmhtechnology.vind.api.query.sort.Sort.asc;
import static com.rbmhtechnology.vind.api.query.sort.Sort.desc;
import static com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.*;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.*;


/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 15.06.16.
 */
public class TestServerTest {

    @Rule
    public TestSearchServer testSearchServer = new TestSearchServer();

    @Test
    public void testEmbeddedSolr() {

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yesterday = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> modified = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("modified");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(created)
                .addField(category)
                .addField(modified)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, yesterday)
                .setValue(modified, new Date())
                .setValues(category, Arrays.asList(1L, 2L));

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello Friends")
                .setValue(created, now)
                .setValue(modified, new Date())
                .addValue(category, 4L);

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        Interval.ZonedDateTimeInterval i1 = Interval.dateInterval("past_24_hours", ZonedDateTime.now().minus(Duration.ofDays(1)), ZonedDateTime.now());
        Interval.ZonedDateTimeInterval i2 = Interval.dateInterval("past_week", ZonedDateTime.now().minus(Duration.ofDays(7)), ZonedDateTime.now());

        FulltextSearch search = Search.fulltext("hello")
                .filter(category.between(0, 10))
                .filter(not(created.after(ZonedDateTime.now())))
                .filter(modified.before(new Date()))
                .facet(pivot("cats", category, created))
                .facet(pivot("catVStitle", category, title))
                .facet(stats("avg Cat", category, "cats", "catVStitle").count().sum().percentiles(9.9, 1.0).mean())
                .facet(stats("countDate", created).count().sum().mean())
                .facet(query("new An dHot", category.between(0, 5), "cats"))
                .facet(query("anotherQuery", and(category.between(7, 10), created.after(ZonedDateTime.now().minus(Duration.ofDays(1))))))
                .facet(range("dates", created, ZonedDateTime.now().minus(Duration.ofDays(1)), ZonedDateTime.now(), Duration.ofHours(1)))
                .facet(range("mod a", modified, new Date(), new Date(), 1L, TimeUnit.HOURS, "cats"))
                .facet(interval("quality", category, Interval.numericInterval("low", 0L, 2L), Interval.numericInterval("high", 3L, 4L)))

                .facet(interval("time", created, i1, i2))
                .facet(interval("time2", modified,
                        Interval.dateInterval("early", new Date(0), new Date(10000)),
                        Interval.dateInterval("late", new Date(10000), new Date(20000))))
                .facet(category)
                .facet(created)
                .facet(modified)
                .page(1, 25)
                .sort(desc(created));

        PageResult result = (PageResult)server.execute(search,assets);

        assertEquals(2, result.getNumOfResults());
        assertEquals(2, result.getResults().size());
        assertEquals("2", result.getResults().get(0).getId());
        assertEquals("asset", result.getResults().get(0).getType());
        assertEquals("2", result.getResults().get(0).getId());
        assertEquals("asset", result.getResults().get(0).getType());
        assertTrue(now.equals(result.getResults().get(0).getValue(created)));
        assertTrue(now.equals(result.getResults().get(0).getValue("created")));
        assertEquals(2, result.getFacetResults().getIntervalFacet("quality").getValues().size());
        assertEquals(2, result.getFacetResults().getIntervalFacet("time").getValues().size());

        System.out.println(result);

        PageResult next = (PageResult)result.nextPage();
        SearchResult prev = next.previousPage();

        TermFacetResult<Long> facet = result.getFacetResults().getTermFacet(category);

        RangeFacetResult<ZonedDateTime> dates = result.getFacetResults().getRangeFacet("dates", ZonedDateTime.class);
        ZonedDateTime rangeDate = dates.getValues().get(0).getValue();
    }

    @Test
    public void testRuntimeLib() throws SolrServerException, IOException {
        SearchServer server = testSearchServer.getSearchServer();

        SolrClient client = (SolrClient) server.getBackend();

        SolrInputDocument document = new SolrInputDocument();
        document.setField("_id_", "1");
        document.setField("_type_", "doc");
        document.setField("dynamic_multi_facet_string_f1", "test");
        document.setField("dynamic_multi_facet_string_f2", "hello");

        client.add(document);
        client.commit();

        SolrQuery query = new SolrQuery("t");
        query.setRequestHandler("/suggester");
        query.set("suggestion.df", "facets");
        query.set("suggestion.field", "dynamic_multi_facet_string_f1");

        QueryResponse response = client.query(query);

        assertEquals(1, ((NamedList) response.getResponse().get("suggestions")).get("suggestion_count"));
    }

    @Test
    public void testSuggestions() {
        SearchServer server = testSearchServer.getSearchServer();

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime yesterday = ZonedDateTime.now().minus(1, ChronoUnit.DAYS);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .setSuggest(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .setSuggest(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(created)
                .addField(category)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello 4 World")
                .setValue(created, yesterday)
                .setValues(category, Arrays.asList(1L, 2L, 4L));

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello 4 Friends")
                .setValue(created, now)
                .addValue(category, 4L);

        SuggestionResult emptySuggestion = server.execute(Search.suggest().addField(title).filter(created.before(ZonedDateTime.now().minus(6, ChronoUnit.HOURS))).text("Hel"), assets);
        assertTrue(emptySuggestion.size() == 0);

        server.index(d1);
        server.index(d2);
        server.commit(true);

        SuggestionResult suggestion = server.execute(Search.suggest().fields(title, category).filter(created.before(ZonedDateTime.now().minus(6, ChronoUnit.HOURS))).text("4"), assets);
        assertTrue(suggestion.size() > 0);

        suggestion = server.execute(Search.suggest("helli").fields(title, category), assets);
        assertEquals("hello", suggestion.getSpellcheck());
    }

    @Test
    public void testTypeIDScoreAsFieldname() {
        SearchServer server = testSearchServer.getSearchServer();

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        FieldDescriptor<String> id = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("id");

        FieldDescriptor<String> type = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("type");

        FieldDescriptor<String> score = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildTextField("score");


        DocumentFactory factory = new DocumentFactoryBuilder("test")
                .addField(score, type, id, title)
                .build();

        server.index(factory.createDoc("1").setValue(title, "Title").setValue(id, "ID").setValue(type, "TYPE").setValue(score, "Score"));
        server.commit();

        SearchResult result = server.execute(Search.fulltext().facet("id", "type", "score"), factory);

        assertEquals(1, result.getNumOfResults());

        Document doc = result.getResults().get(0);
        assertEquals("ID", doc.getValue("id"));
        assertEquals("Title", doc.getValue("title"));
        assertEquals("Score", doc.getValue("score"));
        assertEquals("TYPE", doc.getValue("type"));
        assertEquals(1.0, doc.getScore(), 0);
        assertEquals("test", doc.getType());
        assertEquals("1", doc.getId());
    }

   @Test
    public void testPartialUpdate() {
        SearchServer server = testSearchServer.getSearchServer();

        SingleValueFieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        SingleValueFieldDescriptor.NumericFieldDescriptor<Integer> count = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildNumericField("count", Integer.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .setUpdatable(true)
                .addField(title, created, category, count)
                .build();

        ZonedDateTime now = ZonedDateTime.now();
       Document d1 = assets.createDoc("123")
                .setValue(title, "Hello World")
                .setValue(count, 0)
                .setValues(category, 1L, 2L, 3L)
                .setValue(created, now);

        server.index(d1);
        server.commit();


        server.execute(Search.update("123").set(title, "123").add(category, 6L, 10L).remove(created, now).removeRegex(category, "10").increment(count, 1), assets);

        server.commit();
        SearchResult result = server.execute(Search.fulltext("123"), assets);

        assertEquals(1, result.getResults().size());
        assertEquals("123", result.getResults().get(0).getValue("_id_"));
        assertEquals("asset", result.getResults().get(0).getType());

        server.execute(Search.update("123").remove(category).increment(count, -1), assets);
        server.commit();
        SearchResult result2 = server.execute(Search.fulltext(), assets);

        assertEquals(1, result2.getResults().size());
    }

    @Test
    public void testSubdocuments() throws IOException {
        SearchServer server = testSearchServer.getSearchServer();

        SingleValueFieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor<String> color = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("color");

        DocumentFactory asset = new DocumentFactoryBuilder("asset")
                .addField(title, color)
                .build();

        DocumentFactory marker = new DocumentFactoryBuilder("marker")
                .addField(title, color)
                .build();


        Document a1 = asset.createDoc("A1")
                .setValue(title, "A1 1")
                .setValue(color, "blue");
        Document a2 = asset.createDoc("A2")
                .setValue(title, "A2")
                .setValue(color, "red")
                .addChild(marker.createDoc("M1")
                                .setValue(title, "M1")
                                .setValue(color, "blue")
                );

        Document a3 = asset.createDoc("A3").setValue(title,"A3").setValue(color, "green").addChild(marker.createDoc("M2").setValue(title, "M2").setValue(color, "red"));

        Document a4 = asset.createDoc("A4").setValue(title, "A4").setValue(color, "blue").addChild(marker.createDoc("M3").setValue(title, "M3").setValue(color, "blue"));

        server.index(a1);
        server.index(a2);
        server.index(a3);
        server.index(a4);
        server.commit();

        server.execute(Search.fulltext("some"), asset); //search in all assets
        server.execute(Search.fulltext("some"), marker); //search in all markers

        SearchResult orChildrenFilteredSearch = server.execute(Search.fulltext().filter(Filter.eq(color, "blue")).orChildrenSearch(marker), asset);//search in all markers
        SearchResult andChildrenFilteredSearch = server.execute(Search.fulltext().filter(Filter.eq(color, "blue")).andChildrenSearch(marker), asset);//search in all markers

        Assert.assertTrue(orChildrenFilteredSearch.getNumOfResults() == 3);
        Assert.assertTrue(andChildrenFilteredSearch.getNumOfResults() == 1);

        SearchResult orChildrenCustomSearch = server.execute(Search.fulltext("1").filter(Filter.eq(color, "blue")).orChildrenSearch(Search.fulltext().filter(Filter.eq(color, "red")), marker), asset);//search in all markers

        //TODO: confirm with Thomas
        Assert.assertEquals(3, orChildrenCustomSearch.getNumOfResults());
        //server.execute(Search.fulltext("some").facet(children(title)),asset); //get title facts for children
        //server.executdeepSearchResulte(Search.fulltext("some").facet(parent(title)),marker); //get title facets for parents

        //server.execute(Search.fulltext("some").includeChildren(true),marker); //search everywhere and include matching children
    }

    public void testTermFacetAccess() {
        SearchServer server = testSearchServer.getSearchServer();

        MultiValueFieldDescriptor<String> term = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildMultivaluedTextField("term");

        DocumentFactory factory = new DocumentFactoryBuilder("doc").addField(term).build();

        server.index(factory.createDoc("1").setValue(term, "t1").addValue(term, "t2"));
        server.index(factory.createDoc("2").setValue(term, "t2"));
        server.commit();

        SearchResult result = server.execute(Search.fulltext().facet(term), factory);

        assertEquals(2, result.getFacetResults().getTermFacet(term).getValues().size());
        assertEquals(2, result.getFacetResults().getTermFacet("term", String.class).getValues().size());
        assertEquals(2, result.getFacetResults().getTermFacets().get(term).getValues().size());
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testDoubleDependency() {

        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.EmbeddedSolrServerProvider");
        SearchServer server = SearchServer.getInstance();

        assertEquals("org.apache.solr.client.solrj.embedded.EmbeddedSolrServer", server.getBackend().getClass().getSuperclass().getCanonicalName());

        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.RemoteSolrServerProvider");

        expectedEx.expectCause(isA(RuntimeException.class));
        expectedEx.expectMessage("Provider com.rbmhtechnology.vind.solr.backend.SolrSearchServer could not be instantiated");

        SearchServer.getInstance();
    }

    //MBDN-352
    @Test
    public void testIndexMultipleDocuments () {


        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yesterday = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS);
        ZonedDateTime oneHourAgo = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.HOURS);
        ZonedDateTime halfDayAgo = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.HALF_DAYS);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> modified = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("modified");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(created)
                .addField(category)
                .addField(modified)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, yesterday)
                .setValue(modified, new Date())
                .setValues(category, Arrays.asList(1L, 2L));

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello Friends")
                .setValue(created, now)
                .setValue(modified, new Date())
                .addValue(category, 4L);

        Document d3 = assets.createDoc("3")
                .setValue(title, "Hello You")
                .setValue(created, halfDayAgo)
                .setValue(modified, new Date())
                .setValues(category, Arrays.asList(4L, 5L));

        Document d4 = assets.createDoc("4")
                .setValue(title, "Hello them")
                .setValue(created, oneHourAgo)
                .setValue(modified, new Date())
                .addValue(category, 7L);

        SearchServer server = testSearchServer.getSearchServer();

        List<Document> docList = new ArrayList<>();
        docList.add(d3);
        docList.add(d4);

        server.index(d1,d2);
        server.index(docList);
        server.commit();

        Interval.ZonedDateTimeInterval i1 = Interval.dateInterval("past_24_hours", ZonedDateTime.now().minus(Duration.ofDays(1)), ZonedDateTime.now());
        Interval.ZonedDateTimeInterval i2 = Interval.dateInterval("past_week", ZonedDateTime.now().minus(Duration.ofDays(7)), ZonedDateTime.now());

        FulltextSearch search = Search.fulltext("hello")
                .page(1, 25)
                .sort(desc(created));

        SearchResult result = server.execute(search,assets);

        assertEquals(4, result.getNumOfResults());
        assertEquals(4, result.getResults().size());
        assertEquals("2", result.getResults().get(0).getId());
        assertEquals("asset", result.getResults().get(0).getType());
        assertEquals("4", result.getResults().get(1).getId());
        assertEquals("asset", result.getResults().get(1).getType());
        assertTrue(now.equals(result.getResults().get(0).getValue(created)));
        assertTrue(now.equals(result.getResults().get(0).getValue("created")));
    }

    @Test
    public void testSearchGetById() {
        SearchServer server = testSearchServer.getSearchServer();

        MultiValueFieldDescriptor<String> term = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildMultivaluedTextField("term");

        DocumentFactory factory = new DocumentFactoryBuilder("testDocFactory").addField(term).build();

        server.index(factory.createDoc("1").setValue(term,"t1").addValue(term,"t2"));
        server.index(factory.createDoc("2").setValue(term, "t2"));
        server.index(factory.createDoc("21").setValue(term,"t2"));
        server.index(factory.createDoc("42").setValue(term, "t42"));
        server.commit();

        GetResult result = server.execute(Search.getById("2"), factory);

        assertEquals(1, result.getResults().size());
        assertEquals("2", result.getResults().get(0).getId());

        GetResult multiResult = server.execute(Search.getById("1", "2", "42"), factory);

        assertEquals(3, multiResult.getResults().size());
        assertEquals("1", multiResult.getResults().get(0).getId());
    }


    /*
    @Test
    public void testZKConnection() {

        SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_PROVIDER, "RemoteSolrServerProvider");
        SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, true);
        SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_HOST, "zkServerA:2181,zkServerB:2181,zkServerC:2181");
        SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_COLLECTION, "my_collection");

        SearchServer server = SearchServer.getInstance();

        DocumentFactory assets = new DocumentFactoryBuilder("asset").build();
        SearchResult result = server.execute(Search.fulltext(), assets);

    }
    */
    //MBDN-431
    @Test
    public void testDeleteByQuery() {

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yesterday = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS);

        ZonedDateTime oneHourAgo = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.HOURS);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> modified = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("modified");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(created)
                .addField(category)
                .addField(modified)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, yesterday)
                .setValue(modified, new Date())
                .setValues(category, Arrays.asList(1L, 2L));

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello Friends")
                .setValue(created, now)
                .setValue(modified, new Date())
                .addValue(category, 4L);

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        FulltextSearch searchAll = Search.fulltext();

        final SearchResult searchResult = server.execute(searchAll, assets);
        assertEquals("No of results", 2, searchResult.getNumOfResults());

        final Document result = searchResult.getResults().get(0);
        assertThat("Doc-Title", result.getValue(title), Matchers.equalTo("Hello World"));

        Delete deleteBeforeOneHourAgo = new Delete(Filter.before("created",oneHourAgo));

        server.execute(deleteBeforeOneHourAgo, assets);
        server.commit();

        final SearchResult searchAfterDeleteResult = server.execute(searchAll, assets);
        assertEquals("No of results", 1, searchAfterDeleteResult.getNumOfResults());

        final Document nonDeletedResult = searchAfterDeleteResult.getResults().get(0);
        assertThat("Doc-Title", nonDeletedResult.getValue(title), Matchers.equalTo("Hello Friends"));

        Delete deleteByTitle = new Delete(Filter.eq(title, "Hello Friends"));

        server.execute(deleteByTitle, assets);
        server.commit();

        final SearchResult searchAfterTitleDeletionResult = server.execute(searchAll, assets);
        assertEquals("No of results", 0, searchAfterTitleDeletionResult.getNumOfResults());

    }

    @Test
    public void testSolrUtilDateMultivalueFields() {
        MultiValueFieldDescriptor.UtilDateFieldDescriptor<Date> date = new FieldDescriptorBuilder()
                .buildMultivaluedUtilDateField("date");

        DocumentFactory factory = new DocumentFactoryBuilder("doc")
                .addField(date)
                .build();

        Document document = factory.createDoc("1").setValue(date, new Date());

        SearchServer server = testSearchServer.getSearchServer();

        server.index(document);
        server.commit();

    }

    //MBDN-432
    @Test
    public void testClearIndex() {

        //Storing as text_en solr type
        SingleValueFieldDescriptor.TextFieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setStored(true)
                .buildTextField("title");

        FieldDescriptor<String> entityID = new FieldDescriptorBuilder()
                .buildTextField("entityID");

        SingleValueFieldDescriptor<String> description = new FieldDescriptorBuilder()
                .setLanguage(Language.English)
                .buildTextField("description");


        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(description)
                .addField(entityID)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(entityID, "123")
                .setValue(description, "This value is not stored");

        Document d2 = assets.createDoc("2")
                .setValue(entityID, "456")
                .setValue(title, "Hello Friends")
                .setValue(description, "This value is also not stored");

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        FulltextSearch searchAll = Search.fulltext();

        final SearchResult searchResult = server.execute(searchAll, assets);
        assertEquals("No of results", 2, searchResult.getNumOfResults());

        final Document result = searchResult.getResults().get(0);
        assertThat("Doc-Title", result.getValue(title), Matchers.equalTo("Hello World"));

        server.clearIndex();
        server.commit();

        final SearchResult searchResultClear = server.execute(searchAll, assets);
        assertEquals("No of results", 0, searchResultClear.getNumOfResults());
    }


    //MBDN-441
    @Test
    public void timeZoneSearchTest() {

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yesterday = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS);

        ZonedDateTime oneHourAgo = ZonedDateTime.now(ZoneId.systemDefault()).minus(1, ChronoUnit.HOURS);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> modified = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("modified");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(created)
                .addField(category)
                .addField(modified)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, yesterday)
                .setValue(modified, new Date())
                .setValues(category, Arrays.asList(1L, 2L));

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello Friends")
                .setValue(created, now)
                .setValue(modified, new Date())
                .addValue(category, 4L);

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        FulltextSearch searchAll = Search.fulltext().filter(before("created", new DateMathExpression().sub(1, HOUR))).timeZone("America/Havana");

        final SearchResult searchResult = server.execute(searchAll, assets);
        assertEquals("No of results", 1, searchResult.getNumOfResults());

        final Document result = searchResult.getResults().get(0);
        assertThat("Doc-Title", result.getValue(title), Matchers.equalTo("Hello World"));
    }


    //MBDN-450
    @Test
    public void testDateMathIntervals() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yesterday = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .setStored(true)
                .buildDateField("created");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> modified = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("modified");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(created)
                .addField(category)
                .addField(modified)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, yesterday)
                .setValue(modified, new Date())
                .setValues(category, Arrays.asList(1L, 2L));

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello Friends")
                .setValue(created, now)
                .setValue(modified, new Date())
                .addValue(category, 4L);

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        Interval.DateMathInterval i1 = Interval.dateInterval("past_24_hours", new DateMathExpression().sub(1, DAY), new DateMathExpression());
        Interval.DateMathInterval i2 = Interval.dateInterval("past_week", new DateMathExpression().sub(7, DAY), new DateMathExpression());

        FulltextSearch search = Search.fulltext("hello")
                .filter(created.before(new DateMathExpression()))
                .facet(query("anotherQuery", and(category.between(7, 10), created.after(new DateMathExpression().sub(1, DAY)))))
                .facet(range("dates", created, new DateMathExpression().sub(1, DAY), new DateMathExpression(), Duration.ofHours(1)))
                .facet(range("mod a", modified, new Date(), new Date(), 1L, TimeUnit.HOURS, "cats"))
                .facet(interval("quality", category, Interval.numericInterval("low", 0L, 2L), Interval.numericInterval("high", 3L, 4L)))

                .facet(interval("time", created, i1, i2))
                .facet(interval("time2", modified,
                        Interval.dateInterval("early", new Date(0), new Date(10000)),
                        Interval.dateInterval("late", new Date(10000), new Date(20000))))

                .facet(interval("time3", modified,
                        Interval.dateInterval("early", new DateMathExpression(), new DateMathExpression().add(10000, MILLISECONDS)),
                        Interval.dateInterval("late", new DateMathExpression().add(10000, MILLISECONDS), new DateMathExpression().add(20000, MILLISECONDS))))
                .page(1, 25)
                .sort(desc(created));

        PageResult result = (PageResult)server.execute(search,assets);

        assertEquals(2, result.getNumOfResults());
        assertEquals(2, result.getResults().size());
        assertEquals("2", result.getResults().get(0).getId());
        assertEquals("asset", result.getResults().get(0).getType());
        assertEquals("2", result.getResults().get(0).getId());
        assertEquals("asset", result.getResults().get(0).getType());
        assertTrue(now.equals(result.getResults().get(0).getValue(created)));
        assertTrue(now.equals(result.getResults().get(0).getValue("created")));
        assertEquals(2, result.getFacetResults().getIntervalFacet("quality").getValues().size());
        assertEquals(2, result.getFacetResults().getIntervalFacet("time").getValues().size());

        System.out.println(result);

        PageResult next = (PageResult)result.nextPage();
        SearchResult prev = next.previousPage();

        TermFacetResult<Long> facet = result.getFacetResults().getTermFacet(category);

        RangeFacetResult<ZonedDateTime> dates = result.getFacetResults().getRangeFacet("dates", ZonedDateTime.class);
        ZonedDateTime rangeDate = dates.getValues().get(0).getValue();
    }

    //MBDN-451
    @Test
    public void wildcardIntervalLimitTest() {

        SingleValueFieldDescriptor<Float> numberField = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildNumericField("number", Float.class);

        FieldDescriptor<String> entityID = new FieldDescriptorBuilder()
                .buildTextField("entityID");

        SingleValueFieldDescriptor<Date> dateField = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("date");


        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(numberField)
                .addField(dateField)
                .addField(entityID)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(numberField, 24f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());

        Document d2 = assets.createDoc("2")
                .setValue(numberField, 2f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());


        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        FulltextSearch searchAll = Search.fulltext().facet(interval("numbers", numberField,
                Interval.numericInterval("low", null, 20f),
                Interval.numericInterval("high", 21f, null)))
                .facet(interval("dates", dateField,
                        Interval.dateInterval("before", null, new Date()),
                        Interval.dateInterval("after", new Date(), null)));

        final SearchResult searchResult = server.execute(searchAll, assets);
        assertEquals("No of interval facets", 2, searchResult.getFacetResults().getIntervalFacet("numbers").getValues().size());
        assertEquals("No of interval facets", 2, searchResult.getFacetResults().getIntervalFacet("dates").getValues().size());
        assertEquals("No of interval facets", 2, searchResult.getFacetResults().getIntervalFacet("dates").getValues().get(0).getCount());
    }

    //MBDN-442
    @Test public void string2FacetTest() {

        SingleValueFieldDescriptor<Float> numberField = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildNumericField("number", Float.class);

        FieldDescriptor<String> entityID = new FieldDescriptorBuilder()
                .buildTextField("entityID");

        SingleValueFieldDescriptor<Date> dateField = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("date");


        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(numberField)
                .addField(dateField)
                .addField(entityID)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(numberField, 24f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());

        Document d2 = assets.createDoc("2")
                .setValue(numberField, 2f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        HashMap<String, String> numberIntervals = new HashMap<>();

        numberIntervals.put("low","[* TO 20]");
        numberIntervals.put("high", "[20 TO *]");

        HashMap<String, String> dateIntervals = new HashMap<>();
        dateIntervals.put("after", "[NOW+23DAYS/DAY TO *]");
        dateIntervals.put("before", "[* TO NOW+23DAYS/DAY]");

        FulltextSearch searchAll = Search.fulltext().facet(FacetMapper.stringQuery2FacetMapper(numberField, "numberFacets", numberIntervals))
                .facet(FacetMapper.stringQuery2FacetMapper(dateField, "dateFacets", dateIntervals));

        final SearchResult searchResult = server.execute(searchAll, assets);
        assertEquals("No of interval number facets", 2, searchResult.getFacetResults().getIntervalFacet("numberFacets").getValues().size());
        assertEquals("No of interval date facets", 2, searchResult.getFacetResults().getIntervalFacet("dateFacets").getValues().size());
    }

    //MBDN-454
    @Test
    public void binaryFieldTest() {
        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> modified = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("modified");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        SingleValueFieldDescriptor.BinaryFieldDescriptor<ByteBuffer> byteField = new FieldDescriptorBuilder()
                .setStored(true)
                .buildBinaryField("blob");

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(created)
                .addField(category)
                .addField(modified)
                .addField(byteField)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(modified, new Date())
                .setValue(byteField,ByteBuffer.wrap("ooalalalalala".getBytes()))
                .setValues(category, Arrays.asList(1L, 2L));

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello Friends")
                .setValue(modified, new Date())
                .addValue(category, 4L)
                .setValue(byteField, ByteBuffer.wrap("oolelelelele".getBytes()));

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();


        FulltextSearch search = Search.fulltext();

        SearchResult result = server.execute(search, assets);

        assertEquals(2, result.getNumOfResults());
        assertEquals(2, result.getResults().size());
        assertTrue(ByteBuffer.class.isAssignableFrom(result.getResults().get(0).getValue("blob").getClass()));
        assertEquals("ooalalalalala",new String(((ByteBuffer)result.getResults().get(0).getValue("blob")).array()));
    }

    //MBDN-455
    @Test
    public void complexFieldTest() {


        SingleValuedComplexField.NumericComplexField<Taxonomy,Integer,String> numericComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,Integer,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getId()))
                .setFullText(true, tx -> Arrays.asList(tx.getTerm()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .buildNumericComplexField("numberFacetTaxonomy", Taxonomy.class, Integer.class, String.class);

        SingleValuedComplexField.DateComplexField<Taxonomy,ZonedDateTime,String> dateComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,ZonedDateTime,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getDate()))
                .setFullText(true, tx -> Arrays.asList(tx.getTerm()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .buildDateComplexField("dateFacetTaxonomy", Taxonomy.class, ZonedDateTime.class, String.class);

        SingleValuedComplexField.TextComplexField<Taxonomy,String,String> textComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .setStored(true, tx -> tx.getTerm())
                .buildTextComplexField("textFacetTaxonomy", Taxonomy.class, String.class, String.class);


        SingleValuedComplexField.DateComplexField<Taxonomy,ZonedDateTime,ZonedDateTime> dateStoredComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,ZonedDateTime,ZonedDateTime>()
                .setStored(true, tx -> tx.getDate())
                .buildSortableDateComplexField("dateStoredFacetTaxonomy", Taxonomy.class, ZonedDateTime.class, ZonedDateTime.class, cdf -> cdf.getDate());

        MultiValuedComplexField.TextComplexField<Taxonomy,String,String> multiComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .setStored(true, tx -> tx.getTerm())
                .buildMultivaluedTextComplexField("multiTextTaxonomy", Taxonomy.class, String.class, String.class);



        FieldDescriptor<String> entityID = new FieldDescriptorBuilder()
                .buildTextField("entityID");

        SingleValueFieldDescriptor<Date> dateField = new FieldDescriptorBuilder()
                .buildUtilDateField("date");

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(numericComplexField)
                .addField(dateComplexField)
                .addField(textComplexField)
                .addField(dateField)
                .addField(dateStoredComplexField)
                .addField(entityID)
                .addField(multiComplexField)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(numericComplexField, new Taxonomy("uno", 1, "Uno label", ZonedDateTime.now()))
                .setValue(dateComplexField, new Taxonomy("uno", 1, "Uno label",ZonedDateTime.now()))
                .setValue(textComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()))
                .setValue(entityID, "123")
                .setValue(dateStoredComplexField, new Taxonomy("uno", 1, "Label",ZonedDateTime.now()))
                .setValues(multiComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos", 2, "Label dos", ZonedDateTime.now()))
                .setValue(dateField, new Date());

        Document d2 = assets.createDoc("2")
                .setValue(numericComplexField, new Taxonomy("dos", 2, "dos label", ZonedDateTime.now()))
                .setValue(dateComplexField,new Taxonomy("dos", 2, "dos label",ZonedDateTime.now()))
                .setValue(textComplexField,new Taxonomy("uno", 2, "Label",ZonedDateTime.now()))
                .setValue(entityID, "456")
                .setValue(dateStoredComplexField,new Taxonomy("uno", 1, "Label",ZonedDateTime.now().plusMonths(1)))
                .setValues(multiComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos", 1, "Label", ZonedDateTime.now()))
                .setValue(dateField, new Date());


        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        FulltextSearch searchAll = Search.fulltext().filter(
                and(textComplexField.isNotEmpty(Scope.Facet),
                    or(Filter.eq(numericComplexField, 1), Filter.eq(numericComplexField, 2)),
                    numericComplexField.between(0, 5),
                    dateComplexField.between(ZonedDateTime.now().minusDays(3), ZonedDateTime.now().plusDays(3)),
                    textComplexField.equals("Label")))
                .facet(interval("facetNumber", numericComplexField, Interval.numericInterval("[1-4]", 0, 5), Interval.numericInterval("[6-9]", 5, 10)))
                .facet(interval("facetDateInterval", dateComplexField,
                        Interval.dateInterval("3_days_ago_till_now-1_hour]", ZonedDateTime.now().minusDays(3), ZonedDateTime.now().minusHours(1)),
                        Interval.dateInterval("[now-1_hour_till_the_future]", ZonedDateTime.now().minusHours(1), null)))
                .facet(range("facetRange", numericComplexField, 0, 10, 5))
                .facet(range("facetDateRange", dateComplexField, ZonedDateTime.now().minusDays(3), ZonedDateTime.now().plusDays(3), Duration.ofDays(1)))
                .facet(stats("facetStats", numericComplexField))
                .facet(stats("facetDateStats", dateComplexField))
                .facet(stats("facetTextStats", textComplexField))
                .facet(pivot("facetPivot", numericComplexField, entityID, dateComplexField, textComplexField))
                .facet(numericComplexField, entityID, dateComplexField, textComplexField)
                .sort(desc(dateStoredComplexField));



        final SearchResult searchResult = server.execute(searchAll, assets);

        assertEquals("Stored text exists", "uno", searchResult.getResults().get(0).getValue(textComplexField));
        assertThat("Multivalued text exists", (List<String>) searchResult.getResults().get(0).getValue(multiComplexField), containsInAnyOrder("uno", "dos"));
        assertEquals("No of interval", 2, searchResult.getFacetResults().getIntervalFacet("facetNumber").getValues().size());
        assertEquals("No of doc in interval", 2, searchResult.getFacetResults().getIntervalFacet("facetNumber").getValues().get(0).getCount());
        assertEquals("No of StatsFacets", 3, searchResult.getFacetResults().getStatsFacets().size());
        assertEquals("Stats Min: ", (Integer) 1, searchResult.getFacetResults().getStatsFacet("facetStats", Integer.class).getMin());
        assertEquals("Stats Max: ", (Integer) 2, searchResult.getFacetResults().getStatsFacet("facetStats", Integer.class).getMax());
        assertEquals("Stats Sum: ", (Integer) 3, searchResult.getFacetResults().getStatsFacet("facetStats", Integer.class).getSum());

        final SuggestionResult suggestSearch = server.execute(Search.suggest("la").fields(numericComplexField, textComplexField),assets);
        assertEquals(3, suggestSearch.size());
        assertEquals("Label", suggestSearch.get(textComplexField).getValues().get(0).getValue());
        assertEquals("Uno label", suggestSearch.get(numericComplexField).getValues().get(0).getValue());

    }

    //MBDN-452
    @Test
    public void supportUnderscoredFieldNamesTest() {

        SingleValueFieldDescriptor<Float> numberField = new FieldDescriptorBuilder()
                .buildNumericField("number_one", Float.class);

        FieldDescriptor<String> entityID = new FieldDescriptorBuilder()
                .setLanguage(Language.English)
                .buildTextField("entity_ID");

        SingleValueFieldDescriptor<Date> dateField = new FieldDescriptorBuilder()
                .buildUtilDateField("date_field");


        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(numberField)
                .addField(dateField)
                .addField(entityID)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(numberField, 24f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());

        Document d2 = assets.createDoc("2")
                .setValue(numberField, 2f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());


        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        FulltextSearch searchAll = Search.fulltext();
        final SearchResult searchResult = server.execute(searchAll, assets);
        assertEquals("Number of results", 2, searchResult.getNumOfResults());
        assertEquals("Number_one field", 24f, searchResult.getResults().get(0).getValue("number_one"));
    }

    @Test
    public void testLocationDescriptor() {

        SingleValueFieldDescriptor.LocationFieldDescriptor<LatLng> locationSingle = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildLocationField("locationSingle");

        MultiValueFieldDescriptor.LocationFieldDescriptor<LatLng> locationMulti = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedLocationField("locationMulti");

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(locationSingle)
                .addField(locationMulti)
                .build();

        Document doc1 = assets.createDoc("1")
                .setValue(locationSingle, new LatLng(10, 10))
                .setValues(locationMulti, new LatLng(15, 15));

        Document doc2 = assets.createDoc("2")
                .setValue(locationSingle, new LatLng(20, 20))
                .setValues(locationMulti, new LatLng(11, 11));

        Document doc3 = assets.createDoc("3")
                .setValues(locationMulti, new LatLng(10, 10), new LatLng(20, 20));

        SearchServer server = testSearchServer.getSearchServer();

        server.index(doc1);
        server.index(doc2);
        server.index(doc3);
        server.commit();

        //test bbox filter
        FulltextSearch searchAll = Search.fulltext().filter(locationSingle.withinBBox(new LatLng(10, 10), new LatLng(11, 1)));
        SearchResult searchResult = server.execute(searchAll, assets).print();
        assertEquals("LatLng filter 'within' does not filter properly single value fields", 1, searchResult.getNumOfResults());

        //test bbox filter multivalue
        searchAll = Search.fulltext().filter(locationMulti.withinBBox(new LatLng(10,10), new LatLng(12,12)));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("LatLng filter 'within' does not filter properly mutivalue fields", 2, searchResult.getNumOfResults());

        //test circle filter
        searchAll = Search.fulltext().filter(locationSingle.withinCircle(new LatLng(10, 10), 1));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("LatLng filter 'within' does not filter properly singlevalue fields", 1, searchResult.getNumOfResults());

        searchAll = Search.fulltext().filter(locationMulti.withinCircle(new LatLng(10,10), 160));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("LatLng filter 'within' does not filter properly singlevalue fields", 2, searchResult.getNumOfResults());

        //test retrieving geodist
        //TODO this feature is a little hacky, but should be easy to clean uo
        searchAll = Search.fulltext().geoDistance(locationSingle,new LatLng(5,5));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Distance is not appended to results", 782.78015, searchResult.getResults().get(0).getDistance(),0.001);

        //test sorting
        //TODO does not yet work (parsing error)
        searchAll = Search.fulltext().sort(Sort.SpecialSort.distance()).geoDistance(locationSingle, new LatLng(30, 30));;
        searchResult = server.execute(searchAll, assets).print();
        assertTrue("Distance sorting is not correct", searchResult.getResults().get(0).getDistance() < searchResult.getResults().get(1).getDistance());
    }

    //MBDN-458
    @Test
    public void testContextSearch() {

        final SingleValueFieldDescriptor<Float> numberField = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildNumericField("numberone", Float.class);

        final FieldDescriptor<String> entityID = new FieldDescriptorBuilder()
                .setFacet(true)
                .setLanguage(Language.English)
                .buildTextField("entityID");

        final SingleValueFieldDescriptor<Date> dateField = new FieldDescriptorBuilder()
                .buildUtilDateField("datefield");

        final MultiValueFieldDescriptor.TextFieldDescriptor<String> multiTextField = new FieldDescriptorBuilder()
                .setFacet(true)
                .setSuggest(true)
                .buildMultivaluedTextField("textMulti");

        final DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(numberField)
                .addField(dateField)
                .addField(entityID)
                .addField(multiTextField)
                .setUpdatable(true)
                .build();

        final Document d1 = assets.createDoc("1")
                .setValue(numberField, 0f)
                .setContextualizedValue(numberField, "numberContext", 24f)
                .setContextualizedValue(numberField, "singleContext", 3f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());

        final Document d2 = assets.createDoc("2")
                .setValue(numberField, 2f)
                .setValue(entityID, "123")
                .setContextualizedValues(multiTextField, "multicontext", "text1", "text2")
                .setValue(dateField, new Date());


        final SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        final SuggestionResult suggestion = server.execute(Search.suggest("text1").context("multicontext").addField(multiTextField), assets);
        assertEquals("One contextualize suggestion expected",1,suggestion.size());

        FulltextSearch searchAll = Search.fulltext();
        SearchResult searchResult = server.execute(searchAll, assets);
        assertEquals("Number of results", 2, searchResult.getNumOfResults());
        assertEquals("Number_one field", 0f, searchResult.getResults().get(0).getValue("numberone"));

        searchAll = Search.fulltext().context("numberContext").filter(and(eq(entityID, "123"), eq(numberField,24f))).facet(entityID);
        searchResult = server.execute(searchAll, assets);
        assertEquals("Number of results", 1, searchResult.getNumOfResults());
        assertEquals("Number_one field", 24f, searchResult.getResults().get(0).getContextualizedValue("numberone", "numberContext"));
        assertEquals("Number_one field", null, searchResult.getResults().get(0).getContextualizedValue("numberone", "singleContext"));

        searchAll = Search.fulltext().context("singleContext");
        searchResult = server.execute(searchAll, assets);
        assertEquals("Number of results", 2, searchResult.getNumOfResults());
        assertEquals("Number_one field", 3f, searchResult.getResults().get(0).getContextualizedValue("numberone", "singleContext"));
        assertEquals("Number_one field", null, searchResult.getResults().get(0).getContextualizedValue("numberone", "numberContext"));

        searchAll = Search.fulltext().context("multicontext");
        searchResult = server.execute(searchAll, assets);
        assertEquals("Number of results", 2, searchResult.getNumOfResults());
        assertThat("textMulti multi text field", (List<String>) searchResult.getResults().get(1).getContextualizedValue("textMulti", "multicontext"), containsInAnyOrder("text1", "text2"));

        Delete deleteInContext = new Delete(multiTextField.isNotEmpty()).context("multicontext");
        server.execute(deleteInContext, assets);
        server.commit();
        searchResult = server.execute(searchAll, assets);
        assertEquals("Number of results", 1, searchResult.getResults().size());


        server.execute(Search.update("1").set(numberField, 1f), assets);
        server.commit();
        searchResult = server.execute(searchAll, assets);
        assertEquals("Number_one field", 1f, searchResult.getResults().get(0).getValue("numberone"));

        server.execute(Search.update("1").set("singleContext", numberField, 4f), assets);
        server.commit();
        searchResult = server.execute(searchAll.context("singleContext"), assets);
        assertEquals("Number_one field", 4f, searchResult.getResults().get(0).getContextualizedValue("numberone", "singleContext"));

        server.execute(Search.update("1").remove("singleContext", numberField, null), assets);
        server.commit();
        searchResult = server.execute(searchAll.context("singleContext"), assets);
        assertEquals("Number_one field in single context has been removed", null, searchResult.getResults().get(0).getContextualizedValue("numberone", "singleContext"));

    }

    //MBDN-459
    @Test
    public void isNotEmptyFilterTest() {

        final SingleValueFieldDescriptor.TextFieldDescriptor<String> textSingle = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildTextField("textSingle");

        final SingleValueFieldDescriptor.NumericFieldDescriptor<Integer> numberSingle = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildNumericField("intSingle", Integer.class);

        final MultiValueFieldDescriptor.TextFieldDescriptor<String> textMulti = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedTextField("textMulti");

        final DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(textSingle)
                .addField(textMulti)
                .addField(numberSingle)
                .build();

        final Document doc1 = assets.createDoc("1")
                .setValue(textSingle, "text 1");

        final Document doc2 = assets.createDoc("2")
                .setValues(textMulti, "text 2.1", "text 2.2");

        final Document doc3 = assets.createDoc("3")
                .setValue(textSingle, "")
                .addValue(textMulti, null)
                .setValue(numberSingle, 9);

        final SearchServer server = testSearchServer.getSearchServer();

        server.index(doc1);
        server.index(doc2);
        server.index(doc3);
        server.commit();

        //test empty filter in single valued field
        FulltextSearch searchAll = Search.fulltext().filter(textSingle.isNotEmpty());
        SearchResult searchResult = server.execute(searchAll, assets).print();
        assertEquals("Just documents with single text fields are returned", 1, searchResult.getNumOfResults());

       //test empty filter in multivalue field
        searchAll = Search.fulltext().filter(textMulti.isNotEmpty());
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Just documents with multi valued text fields are returned", 1, searchResult.getNumOfResults());

        //test empty filter in single valued field
        searchAll = Search.fulltext().filter(textSingle.isEmpty());
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Just documents with not or empty single text fields are returned", 2, searchResult.getNumOfResults());

        //test empty filter in single valued field
        searchAll = Search.fulltext().filter(not(textMulti.isNotEmpty()));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Just documents with not or empty multi text fields are returned", 2, searchResult.getNumOfResults());

        //test empty filter in single valued numberfield
        searchAll = Search.fulltext().filter(numberSingle.isNotEmpty());
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Just documents with not or empty multi number fields are returned", 1, searchResult.getNumOfResults());

    }

    //MBDN-430
    @Test
    public void testSortableMultiValuedFields() {


        final TextFieldDescriptor textMulti = new FieldDescriptorBuilder()
                .buildMultivaluedTextField("textMulti");

        final NumericFieldDescriptor<Integer> numMulti = new FieldDescriptorBuilder()
                .buildMultivaluedNumericField("numMulti", Integer.class);

        final Function<Collection<ZonedDateTime>, ZonedDateTime> dateSortFunction = txs -> txs.stream().max(Comparator.<ZonedDateTime>naturalOrder()).get();
        final DateFieldDescriptor dateMulti = new FieldDescriptorBuilder()
                .buildSortableMultivaluedDateField("dateMulti", dateSortFunction);

        final SingleValuedComplexField.UtilDateComplexField<Taxonomy,Date,Date> dateSingle = new ComplexFieldDescriptorBuilder<Taxonomy,Date,Date>()
                .setStored(true, tx -> tx.getUtilDate())
                .buildUtilDateComplexField("singleDate", Taxonomy.class, Date.class, Date.class);

        final MultiValuedComplexField.DateComplexField<Taxonomy,ZonedDateTime,ZonedDateTime> dateComplexMulti = new ComplexFieldDescriptorBuilder<Taxonomy,ZonedDateTime,ZonedDateTime>()
                .setStored(true, tx -> tx.getDate())
                .buildMultivaluedDateComplexField("dateComplexMulti", Taxonomy.class, ZonedDateTime.class, ZonedDateTime.class);

        final DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(textMulti)
                .addField(numMulti)
                .addField(dateMulti)
                .addField(dateSingle)
                .addField(dateComplexMulti)
                .build();

        final Document doc1 = assets.createDoc("1")
                .setValues(textMulti, "text 1.1", "text 1.2")
                .setValues(numMulti,6,7,8)
                .setValue(dateSingle, new Taxonomy("today", 2, "todays date", ZonedDateTime.now()))
                .setValues(dateComplexMulti, new Taxonomy("today", 2, "todays date", ZonedDateTime.now()), new Taxonomy("today", 2, "todays date", ZonedDateTime.now().minusDays(1)))
                .setValues(dateMulti, ZonedDateTime.now().minusMonths(3));

        final Document doc2 = assets.createDoc("2")
                .setValues(textMulti, "text 2.1", "text 2.2")
                .setValues(numMulti, 1, 2, 3)
                .setValue(dateSingle, new Taxonomy("today", 1, "todays date", ZonedDateTime.now().plusDays(1)))
                .setValues(dateComplexMulti, new Taxonomy("today", 2, "todays date", ZonedDateTime.now().plusDays(2)), new Taxonomy("today", 2, "todays date", ZonedDateTime.now().minusDays(1)))
                .setValues(dateMulti, ZonedDateTime.now().plusMonths(1));

        final Document doc3 = assets.createDoc("3")
                .addValue(textMulti, null);

        final SearchServer server = testSearchServer.getSearchServer();

        server.index(doc1);
        server.index(doc2);
        server.index(doc3);
        server.commit();

        //test empty filter in single valued field
        FulltextSearch searchAll = Search.fulltext().sort(asc(textMulti));
        SearchResult searchResult = server.execute(searchAll, assets).print();
        assertEquals("Documents are properly ordered by multi valued text field", "1", searchResult.getResults().get(0).getId());


        searchAll = Search.fulltext().sort(desc(numMulti));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Documents are properly ordered by multi valued numeric field", "1", searchResult.getResults().get(0).getId());

        searchAll = Search.fulltext().sort(desc(dateMulti));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Documents are properly ordered by multi valued date field", "2",searchResult.getResults().get(0).getId());

        searchAll = Search.fulltext().sort(desc(dateSingle));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Documents are properly ordered by multi valued date field", "2",searchResult.getResults().get(0).getId());

        searchAll = Search.fulltext().sort(desc(dateComplexMulti));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Documents are properly ordered by multi valued date field", "2",searchResult.getResults().get(0).getId());
    }

    //MBDN-483
    @Test
    public void atomicUpdateComplexFieldsTest() {
        SingleValuedComplexField.NumericComplexField<Taxonomy,Integer,String> numericComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,Integer,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getId()))
                .setFullText(true, tx -> Arrays.asList(tx.getTerm()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .buildNumericComplexField("numberFacetTaxonomy", Taxonomy.class, Integer.class, String.class);

        SingleValuedComplexField.DateComplexField<Taxonomy,ZonedDateTime,String> dateComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,ZonedDateTime,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getDate()))
                .setFullText(true, tx -> Arrays.asList(tx.getTerm()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .buildDateComplexField("dateFacetTaxonomy", Taxonomy.class, ZonedDateTime.class, String.class);

        SingleValuedComplexField.TextComplexField<Taxonomy,String,String> textComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .setStored(true, tx -> tx.getTerm())
                .buildTextComplexField("textFacetTaxonomy", Taxonomy.class, String.class, String.class);


        SingleValuedComplexField.DateComplexField<Taxonomy,ZonedDateTime,ZonedDateTime> dateStoredComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,ZonedDateTime,ZonedDateTime>()
                .setStored(true, tx -> tx.getDate())
                .buildSortableDateComplexField("dateStoredFacetTaxonomy", Taxonomy.class, ZonedDateTime.class, ZonedDateTime.class, cdf -> cdf.getDate());

        MultiValuedComplexField.TextComplexField<Taxonomy,String,String> multiComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .setStored(true, tx -> tx.getTerm())
                .buildMultivaluedTextComplexField("multiTextTaxonomy", Taxonomy.class, String.class, String.class);



        FieldDescriptor<String> entityID = new FieldDescriptorBuilder()
                .buildTextField("entityID");

        SingleValueFieldDescriptor<Date> dateField = new FieldDescriptorBuilder()
                .buildUtilDateField("date");

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(numericComplexField)
                .addField(dateComplexField)
                .addField(textComplexField)
                .addField(dateField)
                .addField(dateStoredComplexField)
                .addField(entityID)
                .addField(multiComplexField)
                .setUpdatable(true)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(numericComplexField, new Taxonomy("uno", 1, "Uno label", ZonedDateTime.now()))
                .setValue(dateComplexField, new Taxonomy("uno", 1, "Uno label",ZonedDateTime.now()))
                .setValue(textComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()))
                .setValue(entityID, "123")
                .setValue(dateStoredComplexField, new Taxonomy("uno", 1, "Label",ZonedDateTime.now()))
                .setValues(multiComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos", 2, "Label dos", ZonedDateTime.now()))
                .setValue(dateField, new Date());

        Document d2 = assets.createDoc("2")
                .setValue(numericComplexField, new Taxonomy("dos", 2, "dos label", ZonedDateTime.now()))
                .setValue(dateComplexField,new Taxonomy("dos", 2, "dos label",ZonedDateTime.now()))
                .setValue(textComplexField,new Taxonomy("uno", 2, "Label",ZonedDateTime.now()))
                .setValue(entityID, "456")
                .setValue(dateStoredComplexField,new Taxonomy("uno", 1, "Label",ZonedDateTime.now().plusMonths(1)))
                .setValues(multiComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos", 1, "Label", ZonedDateTime.now()))
                .setValue(dateField, new Date());


        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        server.execute(Search.update("1").set(textComplexField, new Taxonomy("unoUpdated", 11, "Uno label updated", ZonedDateTime.now().plusMonths(1))), assets);
        server.commit();

        GetResult result = server.execute(Search.getById("1"), assets);
        Assert.assertTrue(true);

    }

    //MDBN-486
    @Test
    public void advanceFilterTest() {

        SingleValuedComplexField.TextComplexField<Taxonomy,String,String> textComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setAdvanceFilter(true, tx -> Arrays.asList(tx.getTerm()))
                .buildTextComplexField("textFacetTaxonomy", Taxonomy.class, String.class, String.class);


        MultiValuedComplexField.TextComplexField<Taxonomy,String,String> multiComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setStored(true, tx -> tx.getTerm())
                .setAdvanceFilter(true, tx -> Arrays.asList(tx.getTerm()))
                .buildMultivaluedTextComplexField("multiTextTaxonomy", Taxonomy.class, String.class, String.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(textComplexField)
                .addField(multiComplexField)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(textComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()))
                .setValues(multiComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos", 2, "Label dos", ZonedDateTime.now()));

        Document d2 = assets.createDoc("2")
                .setValue(textComplexField, new Taxonomy("dos", 2, "Label", ZonedDateTime.now()))
                .setValues(multiComplexField, new Taxonomy("dos . uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos . dos", 1, "Label", ZonedDateTime.now()));

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        SearchResult result = server.execute(Search.fulltext().filter(textComplexField.equals("uno",Scope.Filter)), assets);
        Assert.assertEquals(1, result.getNumOfResults());
        Assert.assertEquals("1", result.getResults().get(0).getId());

        result = server.execute(Search.fulltext().filter(multiComplexField.equals("uno",Scope.Filter)), assets);
        Assert.assertEquals(1, result.getNumOfResults());
        Assert.assertEquals("1", result.getResults().get(0).getId());
    }

    //MBDN-461
    @Test
    public void prefixFilterTest(){

        final SingleValueFieldDescriptor.TextFieldDescriptor<String> textSingle = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildTextField("textSingle");


        final MultiValueFieldDescriptor.TextFieldDescriptor<String> textMulti = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedTextField("textMulti");

        final DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(textSingle)
                .addField(textMulti)
                .build();

        final Document doc1 = assets.createDoc("1")
                .setValue(textSingle, "text 1");

        final Document doc2 = assets.createDoc("2")
                .setValue(textSingle, "Other text 2")
                .setValues(textMulti, "text 2.1", "text 2.2");

        final SearchServer server = testSearchServer.getSearchServer();

        server.index(doc1);
        server.index(doc2);
        server.commit();

        FulltextSearch searchAll = Search.fulltext().filter(textSingle.prefix("tex"));
        SearchResult searchResult = server.execute(searchAll, assets).print();
        assertEquals("Just documents with single text fields starting with 'tex'", 1, searchResult.getNumOfResults());

        searchAll = Search.fulltext().filter(textMulti.prefix("tex"));
        searchResult = server.execute(searchAll, assets).print();
        assertEquals("Just documents with multi text fields starting with 'tex'", 1, searchResult.getNumOfResults());
    }

    //MBDN-487
    @Test
    public void scopedFilterTest() {

        MultiValuedComplexField.TextComplexField<Taxonomy,String,String> multiComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setStored(true, tx -> tx.getTerm())
                .setAdvanceFilter(true, tx -> Arrays.asList(tx.getTerm()))
                .buildMultivaluedTextComplexField("multiTextTaxonomy", Taxonomy.class, String.class, String.class);

        SingleValuedComplexField.TextComplexField<Taxonomy,String,String> textComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setAdvanceFilter(true, tx -> Arrays.asList(tx.getTerm()))
                .buildTextComplexField("textFacetTaxonomy", Taxonomy.class, String.class, String.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(textComplexField)
                .addField(multiComplexField)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(textComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()))
                .setValues(multiComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos", 2, "Label dos", ZonedDateTime.now()));

        Document d2 = assets.createDoc("2")
                .setValue(textComplexField, new Taxonomy("dos", 2, "Label", ZonedDateTime.now()))
                .setValues(multiComplexField, new Taxonomy("dos . uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos . dos", 1, "Label", ZonedDateTime.now()));

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        SearchResult result = server.execute(Search.fulltext().filter(textComplexField.equals("uno", Scope.Filter)), assets);
        Assert.assertEquals(1, result.getNumOfResults());
        Assert.assertEquals("1", result.getResults().get(0).getId());

        result = server.execute(Search.fulltext().filter(multiComplexField.equals("uno", Scope.Filter)), assets);
        Assert.assertEquals(1, result.getNumOfResults());
        Assert.assertEquals("1", result.getResults().get(0).getId());

    }

    //MBDN-495
    @Test
    public void sliceResultTest(){
        final TextFieldDescriptor textMulti = new FieldDescriptorBuilder()
                .buildMultivaluedTextField("textMulti");

        final NumericFieldDescriptor<Integer> numMulti = new FieldDescriptorBuilder()
                .buildMultivaluedNumericField("numMulti", Integer.class);

        final DateFieldDescriptor dateMulti = new FieldDescriptorBuilder()
                .buildSortableMultivaluedDateField("dateMulti", txs -> ((Collection<ZonedDateTime>) txs).stream().max(Comparator.<ZonedDateTime>naturalOrder()).get());

        final SingleValuedComplexField.UtilDateComplexField<Taxonomy,Date,Date> dateSingle = new ComplexFieldDescriptorBuilder<Taxonomy,Date,Date>()
                .setStored(true, tx -> tx.getUtilDate())
                .buildUtilDateComplexField("singleDate", Taxonomy.class, Date.class, Date.class);

        final MultiValuedComplexField.DateComplexField<Taxonomy,ZonedDateTime,ZonedDateTime> dateComplexMulti = new ComplexFieldDescriptorBuilder<Taxonomy,ZonedDateTime,ZonedDateTime>()
                .setStored(true, tx -> tx.getDate())
                .buildMultivaluedDateComplexField("dateComplexMulti", Taxonomy.class, ZonedDateTime.class, ZonedDateTime.class);

        final DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(textMulti)
                .addField(numMulti)
                .addField(dateMulti)
                .addField(dateSingle)
                .addField(dateComplexMulti)
                .build();

        final Document doc1 = assets.createDoc("1")
                .setValues(textMulti, "text 1.1", "text 1.2")
                .setValues(numMulti,6,7,8)
                .setValue(dateSingle, new Taxonomy("today", 2, "todays date", ZonedDateTime.now()))
                .setValues(dateComplexMulti, new Taxonomy("today", 2, "todays date", ZonedDateTime.now()), new Taxonomy("today", 2, "todays date", ZonedDateTime.now().minusDays(1)))
                .setValues(dateMulti, ZonedDateTime.now().minusMonths(3));

        final Document doc2 = assets.createDoc("2")
                .setValues(textMulti, "text 2.1", "text 2.2")
                .setValues(numMulti, 1, 2, 3)
                .setValue(dateSingle, new Taxonomy("today", 1, "todays date", ZonedDateTime.now()))
                .setValues(dateComplexMulti, new Taxonomy("today", 2, "todays date", ZonedDateTime.now().plusDays(2)), new Taxonomy("today", 2, "todays date", ZonedDateTime.now().minusDays(1)))
                .setValues(dateMulti, ZonedDateTime.now().plusMonths(1));

        final Document doc3 = assets.createDoc("3")
                .addValue(textMulti, null);

        final SearchServer server = testSearchServer.getSearchServer();

        server.index(doc1);
        server.index(doc2);
        server.index(doc3);
        server.commit();

        //test empty filter in single valued field
        FulltextSearch searchAll = Search.fulltext().slice(1).sort(asc(textMulti));
        SearchResult searchResult = server.execute(searchAll, assets).print();
        assertEquals("An Slice starting in index 1", "2", searchResult.getResults().get(0).getId());
    }

    //MBDN-498
    @Test
    public void byQueryFacetConfigurationTest() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yesterday = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> modified = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("modified");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title, created, category, modified)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, yesterday)
                .setValue(modified, new Date())
                .setValues(category,1L, 2L);

        Document d2 = assets.createDoc("2")
                .setValue(title, "Friends")
                .setValue(created, now)
                .setValue(modified, new Date())
                .addValue(category, 4L);

        Document d3 = assets.createDoc("3")
                .setValue(title, "Hello")
                .setValue(created, now)
                .setValue(modified, new Date());

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.index(d3);
        server.commit();

        FulltextSearch search = Search.fulltext("hello")
                .facet(category)
                .setFacetLimit(2)
                .setFacetMinCount(1)
                .sort(desc(created));

        PageResult result = (PageResult)server.execute(search,assets);

        assertEquals(2, result.getFacetResults().getTermFacet(category).getValues().size());
    }

    @Test
    public void queryTermWithDashCharTest(){


        SingleValueFieldDescriptor.TextFieldDescriptor internalId = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("internalId");

        SingleValueFieldDescriptor.TextFieldDescriptor textField = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("textField");


        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(internalId, textField, category)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(internalId, "A-20170322-001")
                .setValue(textField, "")
                .setValues(category, 1L, 2L);

        Document d2 = assets.createDoc("2")
                .setValue(internalId, "PN-1HBTR8P952111")
                .setValue(textField, "")
                .addValue(category, 4L);

        Document d3 = assets.createDoc("3")
                .setValue(internalId, "1234-34345-54656")
                .setValue(textField, "");

        Document d4 = assets.createDoc("4")
                .setValue(internalId, "")
                .setValue(textField, "This is a text1234 field");

        Document d5 = assets.createDoc("5")
                .setValue(internalId, "And this is another text-1234 field")
                .setValue(textField, "");

        Document d6= assets.createDoc("6")
                .setValue(internalId, "")
                .setValue(textField, "This is a text 1234 field");

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.index(d3);
        server.index(d4);
        server.index(d5);
        server.index(d6);
        server.commit();

        FulltextSearch search = Search.fulltext("A-20170322-001 1234-34345-54656 PN-1HBTR8P952111");

        PageResult result = (PageResult)server.execute(search,assets);

        assertEquals(3, result.getResults().size());

        search = Search.fulltext("1234");

        result = (PageResult)server.execute(search,assets);

        assertEquals(1, result.getResults().size());
    }

    //MBDN-563
    @Test
    public void testSubdocumentFullReindex() throws IOException {
        SearchServer server = testSearchServer.getSearchServer();
        //SearchServer server = SolrSearchServer.getInstance("com.rbmhtechnology.searchlib.solr.RemoteSolrServerProvider","http://localhost:8983/solr","core");

        server.clearIndex();
        server.commit();

        SingleValueFieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor<String> color = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .setSuggest(true)
                .buildTextField("color");

        SingleValueFieldDescriptor<String> parent = new FieldDescriptorBuilder().setFacet(true).buildTextField("parent");

        DocumentFactory asset = new DocumentFactoryBuilder("asset")
                .setUpdatable(true)
                .addField(title, color)
                .build();

        DocumentFactory marker = new DocumentFactoryBuilder("marker")
                .setUpdatable(true)
                .addField(title, color, parent)
                .build();


        Document a1 = asset.createDoc("A1")
                .setValue(title, "A1 1")
                .setValue(color, "blue");

        Document a2 = asset.createDoc("A2")
                .setValue(title, "A2")
                .setValue(color, "red")
                .addChild(marker.createDoc("M1")
                                .setValue(title, "M1")
                                .setValue(parent, "A2")
                                .setValue(color, "blue")
                )
                .addChild(marker.createDoc("M4")
                                .setValue(title, "M4")
                                .setValue(parent, "A2")
                                .setValue(color, "green")
                )
                .addChild(marker.createDoc("M5")
                                .setValue(title, "M5")
                                .setValue(parent, "A2")
                                .setValue(color, "yellow")
                );

        Document a3 = asset.createDoc("A3").setValue(title,"A3").setValue(color, "green").addChild(marker.createDoc("M2").setValue(title, "M2").setValue(parent, "A3").setValue(color, "red"));

        Document a4 = asset.createDoc("A4").setValue(title, "A4").setValue(color, "blue").addChild(marker.createDoc("M3").setValue(title, "M3").setValue(parent, "A4").setValue(color, "blue"));

        server.index(a1);
        server.index(a2);
        server.index(a3);
        server.index(a4);
        server.commit();


        //Test whether sub-documents are automatically removed from index when full indexing a parent document without them
        a2.getChildren().clear();

        a2.addChild(marker.createDoc("M6")
                        .setValue(title, "M6")
                        .setValue(parent, "A2")
                        .setValue(color, "purple")
        );

        server.index(a2);
        server.commit();

        final GetResult getM6child = server.execute(Search.getById("M6"), marker);//search in all markers
        assertEquals("Subdocument with id:'M6' is indexed", 1, getM6child.getNumOfResults());

        final GetResult getM4child = server.execute(Search.getById("M4"), marker);//search in all markers
        assertEquals("subdocument with id:'M4' is no more in the index", 0, getM4child.getNumOfResults());




//* The search UI displays the special facets "Assets" and "Markers" with according numbers matching the search criteria.
        SearchResult result = server.execute(Search.fulltext().orChildrenSearch(marker).facet(type()), asset);
        assertEquals("Asset Count",4,result.getNumOfResults());
        //TODO assertEquals("Facet Asset count",4,result.getFacetResults().getTypeFacet().getValues().get(0).getCount());
        //TODO assertEquals("Facet Asset count",3,result.getFacetResults().getTypeFacet().getValues().get(1).getCount());

//* If the user selects the "Markers" facet only the assets with markers will be displayed.
        result = server.execute(Search.fulltext().filter(hasChildrenDocuments(asset)), asset);
        assertEquals("Only Assets with markers",3,result.getNumOfResults());

//* No partial updates for markers yet (but likely to be requested, for example comments)
        //Test behaviour of sub-documents when performing partial updates in children documents.
        server.execute(Search.update("M6").set(title,"M6 seis"),marker);
        server.commit();

        result = server.execute(Search.fulltext().filter(eq(color, "purple", Scope.Suggest)).orChildrenSearch(marker), asset);
        assertEquals("Nested document M6 is still linked to A2 parent document",1,result.getNumOfResults());

        //Test behabiour of sub-documents when full indexing a parent document after performing partial updates to
        // children documents
        a2.getChildren().clear();
        a2.addChild(marker.createDoc("M6").setValue(title, "M6 seis").setValue(color, "purple"));
        server.index(a2);
        server.commit();

        result = server.execute(Search.fulltext().filter(eq(color, "purple")).orChildrenSearch(marker), asset);
        assertEquals("Nested document M6 is still linked to A2 parent document",1,result.getNumOfResults());

//* Partial update are required for the asset.

        assertEquals(1, server.execute(Search.getById("A2"),asset).getNumOfResults());
        assertEquals(4, server.execute(Search.fulltext(),asset).getNumOfResults());

        //Test behaviour of sub-documents when performing partial updates in parent documents.
        server.execute(Search.update("A2").set(title, "A2 dos"), asset);
        server.commit();

        assertEquals(1, server.execute(Search.getById("A2"),asset).getNumOfResults());
        //assertEquals(4, server.execute(Search.fulltext(),asset).getNumOfResults()); TODO!!

        result = server.execute(Search.fulltext().filter(eq(color, "purple")).orChildrenSearch(marker), asset);
        assertEquals("Nested document M6 is still linked to A2 parent document",1,result.getNumOfResults());

        //Test behabiour of sub-documents when full indexing a parent document after performing partial updates to
        // parent documents
        a2.setValue(title, "A2 dos");
        server.index(a2);
        server.commit();

        result = server.execute(Search.fulltext().filter(eq(color, "purple")).orChildrenSearch(marker), asset);
        assertEquals("Nested document M6 is still linked to A2 parent document",1,result.getNumOfResults());

//* The search uses one index configuration for both. The field names for markers and assets are the same (for
// example a field "Person" of the asset is als available as "Person" on the Marker)

        server.clearIndex();
        server.commit();
    }

    @Test
    public void complexFieldBooleanTest() {


        SingleValuedComplexField.NumericComplexField<Taxonomy,Integer,Boolean> numericComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,Integer,Boolean>()
                .setFacet(true, tx -> Arrays.asList(tx.getId()))
                .setStored(true, tx -> true)
                .setFullText(true, tx -> Arrays.asList(tx.getTerm()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .buildNumericComplexField("numberFacetBooleanStoredTaxonomy", Taxonomy.class, Integer.class, Boolean.class);

        MultiValuedComplexField.TextComplexField<Taxonomy,String,String> multiComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
                .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
                .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
                .setStored(true, tx -> tx.getTerm())
                .buildMultivaluedTextComplexField("multiTextTaxonomy", Taxonomy.class, String.class, String.class);


        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(numericComplexField)
                .addField(multiComplexField)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(numericComplexField, new Taxonomy("uno", 1, "Uno label", ZonedDateTime.now()))
                .setValues(multiComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos", 2, "Label dos", ZonedDateTime.now()));

        Document d2 = assets.createDoc("2")
                .setValue(numericComplexField, new Taxonomy("dos", 2, "dos label", ZonedDateTime.now()))
                .setValues(multiComplexField, new Taxonomy("uno", 1, "Label", ZonedDateTime.now()), new Taxonomy("dos", 1, "Label", ZonedDateTime.now()));

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        final FulltextSearch searchAll = Search.fulltext();

        final SearchResult searchResult = server.execute(searchAll, assets);

    }

    @Test
    public void testPartialUpdates() {

        SearchServer server = testSearchServer.getSearchServer();

        MultiValueFieldDescriptor<Integer> value = new FieldDescriptorBuilder<Integer>()
                .setFullText(true)
                .setFacet(true)
                .buildMultivaluedNumericField("value", Integer.class);

        DocumentFactory documentFactory = new DocumentFactoryBuilder("doc")
                .setUpdatable(true)
                .addField(value)
                .build();

        server.index(documentFactory.createDoc("1").setValues(value, 1, 2));
        server.commit();

        Assert.assertEquals(2, ((List)server.execute(Search.getById("1"), documentFactory).getResults().get(0).getValues().get("value")).size());

        server.execute(Search.update("1").add(value, 3, 4), documentFactory);
        server.commit();

        Assert.assertEquals(4, ((List)server.execute(Search.getById("1"), documentFactory).getResults().get(0).getValues().get("value")).size());

        server.execute(Search.update("1").set(value,5), documentFactory);
        server.commit();

        Assert.assertEquals(1, ((List)server.execute(Search.getById("1"), documentFactory).getResults().get(0).getValues().get("value")).size());

    }

    @Test
    public void testSuggestionFiltering() {

        SearchServer server = testSearchServer.getSearchServer();

        SingleValueFieldDescriptor<String> value = new FieldDescriptorBuilder<String>()
                .setSuggest(true)
                .buildTextField("value");

        DocumentFactory documentFactory = new DocumentFactoryBuilder("doc")
                .setUpdatable(true)
                .addField(value)
                .build();

        server.index(
                documentFactory.createDoc("1").setValue(value,"hello world")
        );

        server.commit();

        SuggestionResult suggestionResult = server.execute(Search.suggest("he").addField(value), documentFactory);

        SearchResult searchResult = server.execute(Search.fulltext().filter(eq(value, "hello world")), documentFactory);

        Assert.assertEquals(1, searchResult.getNumOfResults());

    }

    @Test
    public void testEmptyChildrenSearch() {
        SearchServer server = testSearchServer.getSearchServer();
        //SearchServer server = SolrSearchServer.getInstance("com.rbmhtechnology.searchlib.solr.RemoteSolrServerProvider","http://localhost:8983/solr","core");

        server.clearIndex();
        server.commit();

        SingleValueFieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor<String> color = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .setSuggest(true)
                .buildTextField("color");

        SingleValueFieldDescriptor<String> parent = new FieldDescriptorBuilder().setFacet(true).buildTextField("parent");

        DocumentFactory asset = new DocumentFactoryBuilder("asset")
                .setUpdatable(true)
                .addField(title, color)
                .build();

        DocumentFactory marker = new DocumentFactoryBuilder("marker")
                .setUpdatable(true)
                .addField(title, color, parent)
                .build();


        Document a1 = asset.createDoc("A1")
                .setValue(title, "A1 1")
                .setValue(color, "blue");

        Document a2 = asset.createDoc("A2")
                .setValue(title, "A2")
                .setValue(color, "red")
                .addChild(marker.createDoc("M1")
                                .setValue(title, "M1")
                                .setValue(parent, "A2")
                                .setValue(color, "blue")
                )
                .addChild(marker.createDoc("M4")
                                .setValue(title, "M4")
                                .setValue(parent, "A2")
                                .setValue(color, "green")
                )
                .addChild(marker.createDoc("M5")
                                .setValue(title, "M5")
                                .setValue(parent, "A2")
                                .setValue(color, "yellow")
                );

        Document a3 = asset.createDoc("A3").setValue(title,"A3").setValue(color, "green").addChild(marker.createDoc("M2").setValue(title, "M2").setValue(parent, "A3").setValue(color, "red"));

        Document a4 = asset.createDoc("A4").setValue(title, "A4").setValue(color, "blue").addChild(marker.createDoc("M3").setValue(title, "M3").setValue(parent, "A4").setValue(color, "blue"));

        server.index(a1);
        server.index(a2);
        server.index(a3);
        server.index(a4);
        server.commit();

        final SearchResult result = server.execute(Search.fulltext().orChildrenSearch(marker), asset);
        assertEquals(4,result.getNumOfResults());
    }

    @Test
    public void testTermQueryFilter() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime yesterday = ZonedDateTime.now(ZoneId.of("UTC")).minus(1, ChronoUnit.DAYS);

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildDateField("created");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> modified = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("modified");

        NumericFieldDescriptor<Long> category = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildMultivaluedNumericField("category", Long.class);

        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .addField(created)
                .addField(category)
                .addField(modified)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, yesterday)
                .setValue(modified, new Date())
                .setValues(category, Arrays.asList(1L, 2L));

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello Friends")
                .setValue(created, now)
                .setValue(modified, new Date())
                .addValue(category, 4L);

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        FulltextSearch search = Search.fulltext()
                .filter(Filter.terms(category,1L, 4L))
                .sort(desc(created));

        PageResult result = (PageResult)server.execute(search,assets);

        assertEquals(2, result.getNumOfResults());

        search = Search.fulltext()
                .filter(Filter.terms(created,yesterday))
                .sort(desc(created));

        result = (PageResult)server.execute(search,assets);

        assertEquals(1, result.getNumOfResults());
    }

    @Test
    public void test10001TermsTermQueryFilter() throws IOException, URISyntaxException {

        final SingleValueFieldDescriptor.TextFieldDescriptor title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        final DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(title)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(title, "Hello World");

        Document d2 = assets.createDoc("2")
                .setValue(title, "Hello Friends");

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        final Path filePath = Paths.get(this.getClass().getClassLoader().getResource("3000ids.test").toURI());
        final Charset charset = Charset.defaultCharset();
        final List<String> stringList = Files.readAllLines(filePath, charset);

        final FulltextSearch search = Search.fulltext()
                .filter(title.terms(stringList.toArray(new String[]{})));

        PageResult result = (PageResult)server.execute(search,assets);

        assertEquals(1, result.getNumOfResults());
    }

}
