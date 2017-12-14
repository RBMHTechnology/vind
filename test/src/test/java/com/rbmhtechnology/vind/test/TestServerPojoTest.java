package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.annotations.*;
import com.rbmhtechnology.vind.annotations.language.Language;
import com.rbmhtechnology.vind.annotations.util.FunctionHelpers;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.BeanGetResult;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.api.result.facet.TermFacetResult;

import org.hamcrest.CoreMatchers;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.*;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;
import static com.rbmhtechnology.vind.api.query.filter.Filter.or;
import static com.rbmhtechnology.vind.api.query.sort.Sort.desc;
import static org.junit.Assert.*;

/**
 */
public class TestServerPojoTest {

    @Rule
    public TestSearchServer searchServer = new TestSearchServer();

    @Test
    public void testPojoRoundtrip() {
        final SearchServer server = searchServer.getSearchServer();

        final Pojo doc1 = Pojo.create("doc1", "Eins", "Erstes Dokument", "simple");
        final Pojo doc2 = Pojo.create("doc2", "Zwei", "Zweites Dokument", "simple");
        final Pojo doc3 = Pojo.create("doc3", "Drei", "Dieses ist das dritte Dokument", "complex");
        final Pojo doc4 = Pojo.create("doc3", "Drei", "Dieses ist das dritte Dokument", "complex");

        server.indexBean(doc1);
        server.indexBean(doc2);
        server.indexBean(doc3);
        server.commit();

        final BeanSearchResult<Pojo> dritte = server.execute(Search.fulltext("dritte"), Pojo.class);
        assertThat("#numOfResults", dritte.getNumOfResults(), CoreMatchers.equalTo(1l));
        assertThat("results.size()", dritte.getResults(), IsCollectionWithSize.hasSize(1));
        checkPojo(doc3, dritte.getResults().get(0));

        final BeanSearchResult<Pojo> all = server.execute(Search.fulltext()
                .facet("category")
                .filter(or(eq("title", "Eins"), or(eq("title", "Zwei"),eq("title","Drei"))))
                .sort("_id_", Sort.Direction.Desc), Pojo.class); //TODO create special sort for reserved fields (like score, type, id)
        assertThat("#numOfResults", all.getNumOfResults(), CoreMatchers.equalTo(3l));
        assertThat("results.size()", all.getResults(), IsCollectionWithSize.hasSize(3));
        checkPojo(doc3, all.getResults().get(0));
        checkPojo(doc2, all.getResults().get(1));
        checkPojo(doc1, all.getResults().get(2));

        TermFacetResult<String> facets = all.getFacetResults().getTermFacet("category", String.class);
        assertEquals(2,facets.getValues().size());
        assertEquals("simple", facets.getValues().get(0).getValue());
        assertEquals("complex",facets.getValues().get(1).getValue());
        assertEquals(2,facets.getValues().get(0).getCount());
        assertEquals(1,facets.getValues().get(1).getCount());
    }

    private void checkPojo(Pojo expected, Pojo actual) {
        assertThat("results[" + expected.id + "]", actual, SamePropertyValuesAs.samePropertyValuesAs(expected));
        assertThat("results[" + expected.id + "].id", actual.id, CoreMatchers.equalTo(expected.id));
        assertThat("results[" + expected.id + "].title", actual.title, CoreMatchers.equalTo(expected.title));
        assertThat("results[" + expected.id + "].content", actual.content, CoreMatchers.equalTo(expected.content));
        assertThat("results[" + expected.id + "].category", actual.category, IsIterableContainingInAnyOrder.containsInAnyOrder(expected.category.toArray()));
    }

    @Test
    public void testRoundtrip2() {
        final SearchServer server = searchServer.getSearchServer();
        server.indexBean(new SimplePojo("1", "Hello World", 1, "hello"));
        server.indexBean(new SimplePojo("2","Hello Thomas",2,"foo","bar"));
        server.commit();
        BeanSearchResult<SimplePojo> result = server.execute(Search.fulltext().facet("voting", "category").sort(desc("voting")), SimplePojo.class);
        assertNotNull(result);
        assertEquals(1,result.getResults().get(0).getScoring(),0);

        //test suggestion
        SuggestionResult suggestions = server.execute(Search.suggest("*").fields("category","voting").filter("voting","1"), SimplePojo.class);

        assertEquals(1,suggestions.get("category").getValues().size());
    }

    //MBDN-352
    @Test
    public void testMultipleBeanIndex() {
        final SearchServer server = searchServer.getSearchServer();

        final Pojo doc1 = Pojo.create("doc1", "Eins", "Erstes Dokument", "simple");
        final Pojo doc2 = Pojo.create("doc2", "Zwei", "Zweites Dokument", "simple");
        final Pojo doc3 = Pojo.create("doc3", "Drei", "Dieses ist das dritte Dokument", "complex");
        final Pojo doc4 = Pojo.create("doc4", "Vier", "Das vierte Dokument", "complex");

        server.indexBean(doc1,doc2);

        List<Object> beanList = new ArrayList<>();
        beanList.add(doc3);
        beanList.add(doc4);

        server.indexBean(beanList);
        server.commit();

        final BeanSearchResult<Pojo> all = server.execute(Search.fulltext(), Pojo.class);
        assertThat("#numOfResults", all.getNumOfResults(), CoreMatchers.equalTo(4l));
        assertThat("results.size()", all.getResults(), IsCollectionWithSize.hasSize(4));
        checkPojo(doc3, all.getResults().get(2));
        checkPojo(doc2, all.getResults().get(1));
        checkPojo(doc1, all.getResults().get(0));
        checkPojo(doc4, all.getResults().get(3));
    }

    @Test
    public void testSearchGetById(){
        final SearchServer server = searchServer.getSearchServer();
        server.indexBean(new SimplePojo("1", "Hello World", 1, "hello"));
        server.indexBean(new SimplePojo("2","Hello Thomas",2,"foo","bar"));
        server.commit();

        BeanGetResult<SimplePojo> result = server.execute(Search.getById("2"), SimplePojo.class);

        assertEquals(1, result.getResults().size());
        assertEquals("2", result.getResults().get(0).getId());

        BeanGetResult<SimplePojo> multiResult = server.execute(Search.getById("1","2","42"), SimplePojo.class);

        assertEquals(2, multiResult.getResults().size());
        assertEquals("1", multiResult.getResults().get(0).getId());
    }

    @Test
    public void testTaxonomyPojo() {
        final SearchServer server = searchServer.getSearchServer();

        final Pojo1 doc1 = new Pojo1();
        doc1.id = "pojo-Id-1";
        doc1.title = "title 1";
        doc1.tax = new Taxonomy("term 1",1,"", ZonedDateTime.now(),Arrays.asList("term 1","term one"));

        final Pojo1 doc2 = new Pojo1();
        doc2.id = "pojo-Id-2";
        doc2.title = "title 2";
        doc2.tax = new Taxonomy("term 2",2,"", ZonedDateTime.now(),Arrays.asList("term 2","term two"));

        server.indexBean(doc1);
        server.indexBean(doc2);
        server.commit();

        final BeanSearchResult<Pojo1> second = server.execute(Search.fulltext("two"), Pojo1.class);
        assertThat("#numOfResults", second.getNumOfResults(), CoreMatchers.equalTo(1l));
        assertThat("results.size()", second.getResults(), IsCollectionWithSize.hasSize(1));
       // checkPojo(doc3, dritte.getResults().get(0));

       /* final BeanSearchResult<Pojo> all = server.execute(Search.fulltext()
                .facet("category")
                .filter(or(eq("title", "Eins"), or(eq("title", "Zwei"),eq("title","Drei"))))
                .sort("_id_", Sort.Direction.Desc), Pojo.class); //TODO create special sort for reserved fields (like score, type, id)
        assertThat("#numOfResults", all.getNumOfResults(), CoreMatchers.equalTo(3l));
        assertThat("results.size()", all.getResults(), IsCollectionWithSize.hasSize(3));
        checkPojo(doc3, all.getResults().get(0));
        checkPojo(doc2, all.getResults().get(1));
        checkPojo(doc1, all.getResults().get(2));

        TermFacetResult<String> facets = all.getFacetResults().getTermFacet("category", String.class);
        assertEquals(2,facets.getValues().size());
        assertEquals("simple", facets.getValues().get(0).getValue());
        assertEquals("complex",facets.getValues().get(1).getValue());
        assertEquals(2,facets.getValues().get(0).getCount());
        assertEquals(1,facets.getValues().get(1).getCount());*/
    }

    public static class SimplePojo {

        @Id
        private String id;

        @FullText
        private String title;

        @Facet
        private double voting;

        @Facet
        private Date created;

        //TODO support Array ?
        @Facet
        @FullText(boost = 2)
        private ArrayList<String> category;

        @Score
        private float scoring;

        public SimplePojo(){}

        public SimplePojo(String id, String title, double voting, String ... category) {
            this.id = id;
            this.title = title;
            this.voting = voting;
            this.category = new ArrayList<>(Arrays.asList(category));
            this.created = new Date();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public double getVoting() {
            return voting;
        }

        public void setVoting(double voting) {
            this.voting = voting;
        }

        public ArrayList<String> getCategory() {
            return category;
        }

        public void setCategory(ArrayList<String> category) {
            this.category = category;
        }

        public float getScoring() {
            return scoring;
        }
    }

    public static class Pojo {

        @Id
        private String id;

        @Facet
        private String title;

        @FullText(language = Language.German)
        private String content;

        @Facet
        private HashSet<String> category;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public HashSet<String> getCategory() {
            return category;
        }

        public void setCategory(HashSet<String> category) {
            this.category = category;
        }

        public static Pojo create(String id, String title, String content, String... category) {
            final Pojo pojo = new Pojo();

            pojo.id = id;
            pojo.title = title;
            pojo.content = content;
            pojo.category = new HashSet<>(Arrays.asList(category));

            return pojo;
        }
    }

    @Type(name = "TaxPojo")
    @SuppressWarnings("unused")
    public static class Pojo1 {


        @Id
        protected String id;

        @Facet(suggestion = true)
        protected String title;

        @Ignore
        protected String someInternalData;

        @Field(name = "data")
        protected String content;

        @Field(name = "cats")
        protected HashSet<String> categories;

        @ComplexField(
                store = @Operator( function = FunctionHelpers.GetterFunction.class, fieldName = "term"),
                facet = @Operator( function = FunctionHelpers.GetterFunction.class, fieldName = "term"),
                suggestion = @Operator( function = FunctionHelpers.GetterFunction.class, fieldName = "synonyms"),
                advanceFilter = @Operator( function = FunctionHelpers.GetterFunction.class, fieldName = "id"))
        protected Taxonomy tax;

    }

}
