package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 12.07.17.
 */
public class ParentChildrenTest {

    @Rule
    public TestSearchServer testSearchServer = new TestSearchServer();

    private DocumentFactory parent, child;
    private SingleValueFieldDescriptor<String> parent_value;
    private SingleValueFieldDescriptor<String> child_value;
    private SingleValueFieldDescriptor<String> shared_value;
    private MultiValueFieldDescriptor<String> notation;

    private SearchServer server;

    @Before
    public void before() {

        server = testSearchServer.getSearchServer();
        //server = SolrSearchServer.getInstance("com.rbmhtechnology.searchlib.solr.RemoteSolrServerProvider", "http://localhost:8983/solr", "searchlib");

        server.clearIndex();
        server.commit();

        parent_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setFullText(true)
                .buildTextField("parent_value");

        child_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setFullText(true)
                .buildTextField("child_value");

        shared_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setFullText(true)
                .buildTextField("shared_value");

        notation = new FieldDescriptorBuilder<String>()
                .setFullText(true)
                .buildMultivaluedTextField("notation");


        parent = new DocumentFactoryBuilder("parent")
                .setUpdatable(true)
                .addField(shared_value, parent_value,notation)
                .build();

        child = new DocumentFactoryBuilder("child")
                .setUpdatable(true)
                .addField(shared_value, child_value)
                .build();


        /**
         * Testset
         *  Parent P1       (p:red,             s:yellow)
         *  Parent P2       (p:blue             s:purple)
         *      Child C1    (       c:red       s:red)
         *      Child C2    (       c:blue      s:yellow)
         *  Parent P3       (p:red              s:red)
         *      Child C3    (       c:blue      s:black)
         *  Parent P4       (p:orange              s:red)
         *      Child C4    (       c:green      s:black)
         */

        server.clearIndex();
        server.index(
                parent.createDoc("P1").setValue(parent_value, "red")
                        .setValue(shared_value, "yellow").setValue(notation,"M001"),
                parent.createDoc("P2").setValue(parent_value, "blue").setValue(shared_value, "purple")
                        .setValue(notation,"M002"),
                parent.createDoc("P3").setValue(parent_value, "red").setValue(shared_value, "red")
                        .setValue(notation,"S003")
                        .addChild(
                        child.createDoc("C3").setValue(child_value, "blue").setValue(shared_value, "black")),
                parent.createDoc("P4").setValue(parent_value, "orange").setValue(shared_value, "black")
                        .setValue(notation,"M004")
                        .addChild(
                        child.createDoc("C4").setValue(child_value, "green").setValue(shared_value, "black"))
        );

        server.commit();

        server.index(
                parent.createDoc("P2").setValue(parent_value, "blue").setValue(shared_value, "purple")
                        .setValue(notation,"M002")
                        .addChild(
                        child.createDoc("C1").setValue(child_value, "red").setValue(shared_value, "red"),
                        child.createDoc("C2").setValue(child_value, "blue").setValue(shared_value, "yellow"))
        );
        server.commit();

    }

    @Test
    public void testFilterResultsByParentValue() {

        //parent has to contain red
        FulltextSearch search1 = Search.fulltext()
                .filter(eq(parent_value, "red"));

        assertEquals(2, server.execute(search1,parent).getNumOfResults());

        FulltextSearch search2 = Search.fulltext("blue");

        assertEquals(1, server.execute(search2,parent).getNumOfResults());

        FulltextSearch search3 = Search.fulltext("blue")
                .orChildrenSearch(child);

        assertEquals(2, server.execute(search3,parent).getNumOfResults());

        /**
         * filtering children for parent values is not possible, therefor a extra search has to be used
         */
        FulltextSearch search4 = Search
                .fulltext("blue")
                .filter(eq(parent_value, "red"))
                .filter(Filter.terms(parent_value,"red"))
                .orChildrenSearch(
                        Search.fulltext("blue"),
                        child
                );

        assertEquals(1, server.execute(search4,parent).getNumOfResults());
        assertEquals("P3", server.execute(search4,parent).getResults().get(0).getId());
    }

    @Test
    public void testGetNumberOfChildren() {
        FulltextSearch search = Search.fulltext().orChildrenSearch(child);
        SearchResult result = server.execute(search, parent);
        assertEquals(4, result.getNumOfResults());
        assertEquals(Integer.valueOf(2),result.getResults().get(2).getChildCount());
    }

    @Test
    public void testFilterOnlyWithChildrenValue() {
        FulltextSearch search = Search.fulltext()
                .setStrict(false)
                .filter(eq(child_value, "red"))
                .filter(Filter.terms(child_value,"red"))
                .orChildrenSearch(child);
        SearchResult result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());
        assertEquals(Integer.valueOf(1),result.getResults().get(0).getChildCount());
    }

    @Test
    public void testFilterOnlyWithParentValue() {
        final FulltextSearch search = Search.fulltext().setStrict(false)
                .filter(or(eq(shared_value, "blue"),or(eq(child_value, "red"),eq(child_value,"green")))).orChildrenSearch(child);
        final SearchResult result = server.execute(search, parent);
        assertEquals(2, result.getNumOfResults());
        assertEquals(Integer.valueOf(1),result.getResults().get(0).getChildCount());
    }

    @Test
    public void testParentDuplicationOnAtomicUpdate() {

        //Safe check: ensure the orChildren search works
        FulltextSearch search = Search.fulltext().filter(eq(shared_value, "red")).orChildrenSearch(child);
        SearchResult result = server.execute(search, parent);
        assertEquals(2, result.getNumOfResults());
        assertEquals(Integer.valueOf(1),result.getResults().get(1).getChildCount());


        //Update parent document to pink
        final Update updateToPink = Search.update("P2").set(parent_value, "pink");
        server.execute(updateToPink,parent);
        server.commit();

        search = Search.fulltext().filter(eq(parent_value, "pink"));
        result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());

        //Safe check: ensure the orChildren search still works
        search = Search.fulltext().filter(eq(shared_value, "red")).orChildrenSearch(child);
        result = server.execute(search, parent);
        assertEquals(2, result.getNumOfResults());
        assertEquals(Integer.valueOf(1),result.getResults().get(1).getChildCount());

        //////////////////////////////////////
        server.index(
                parent.createDoc("P2").setValue(parent_value, "blue").setValue(shared_value, "purple").addChild(
                        child.createDoc("C1").setValue(child_value, "red").setValue(shared_value, "red"),
                        child.createDoc("C2").setValue(child_value, "goblin-green").setValue(shared_value, "yellow"))
        );
         server.commit();
        /////////////////////////////////////

        //Update parent document to neon-orange
        final Update updateToNeonOrange = Search.update("P2").set(parent_value, "neon-orange");
        server.execute(updateToNeonOrange,parent);
        server.commit();

        search = Search.fulltext().filter(eq(parent_value, "neon-orange"));
        result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());

        //Safe check: ensure the orChildren search still works
        search = Search.fulltext().filter(eq(shared_value, "red")).orChildrenSearch(child);
        result = server.execute(search, parent);
        assertEquals(2, result.getNumOfResults());
        assertEquals(Integer.valueOf(1),result.getResults().get(1).getChildCount());

        server.index(parent.createDoc("P2").setValue(parent_value, "neon-yellow").setValue(shared_value, "purple").addChild(
                child.createDoc("C1").setValue(child_value, "red").setValue(shared_value, "red"),
                child.createDoc("C2").setValue(child_value, "blue").setValue(shared_value, "yellow")));
        server.commit();

        search = Search.fulltext().filter(eq(parent_value, "neon-yellow"));
        result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());

        //Safe check: ensure the orChildren search still works
        search = Search.fulltext().filter(eq(shared_value, "red")).orChildrenSearch(child);
        result = server.execute(search, parent);
        assertEquals(2, result.getNumOfResults());
        assertEquals(Integer.valueOf(1),result.getResults().get(1).getChildCount());

        server.index(parent.createDoc("P2").setValue(parent_value, "neon-yellow").setValue(shared_value, "purple").addChild(
                child.createDoc("C0").setValue(child_value, "blue").setValue(shared_value, "yellow")));
        server.commit();

        assertEquals(1, 1);

    }

    @Test
    public void testFilterOnlyWithParentValues() {
        FulltextSearch search = Search.fulltext()
                .setStrict(false)
                .filter(and(not(eq(parent_value, "orange")), or(eq(parent_value, "blue"),eq(parent_value, "red"))))
                .orChildrenSearch(child);
        SearchResult result = server.execute(search, parent);
        assertEquals(3, result.getNumOfResults());
        assertEquals(Integer.valueOf(2),result.getResults().get(1).getChildCount());
    }

    @Test
    public void testFilterRandomOrderFailure() {
        FulltextSearch search = Search.fulltext()
                .setStrict(false)
                .filter(and(eq(child_value, "blue"), Filter.terms(shared_value,"red")))
                .orChildrenSearch(child);
        SearchResult result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());
        assertEquals(Integer.valueOf(0),result.getResults().get(0).getChildCount());//0 because none of the assets have the shared_value:red
    }

    @Test
    public void testSubdocumentFacetCountsFailure() {
        FulltextSearch search = Search.fulltext()
                .setStrict(false)
                .filter(eq(shared_value, "red"))
                .orChildrenSearch(child);
        SearchResult result = server.execute(search, parent);

        assertEquals(1,result.getFacetResults().getSubdocumentFacets().stream().findFirst().get().getChildrenCount());
        assertEquals(1,(long)result.getFacetResults().getSubdocumentFacets().stream().findFirst().get().getParentCount());

        search = Search.fulltext()
                .setStrict(false)
                .filter(or(eq(shared_value, "yellow"),eq(child_value,"red")))
                .orChildrenSearch(child);
        result = server.execute(search, parent);

        assertEquals(2,result.getFacetResults().getSubdocumentFacets().stream().findFirst().get().getChildrenCount());
        assertEquals(1,(long)result.getFacetResults().getSubdocumentFacets().stream().findFirst().get().getParentCount());
    }

    @Test
    public void testQuerySyntaxExceptionOnChildrenFacetSearch() {

        FulltextSearch search = Search.fulltext("S003 \"M001\" M004").setStrict(false).orChildrenSearch(child);
        SearchResult result = server.execute(search, parent);
        assertEquals(3, result.getNumOfResults());

        search = Search.fulltext("S003 AND").setStrict(false).orChildrenSearch(child);
        result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());

        search = Search.fulltext("S003 \"").setStrict(false).orChildrenSearch(child);
        result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());

        search = Search.fulltext("S003 (").setStrict(false).orChildrenSearch(child);
        result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());
    }

    //Vind #57
    @Test
    public void testMultipleChildrenSearches(){
        FulltextSearch childSearch1 = Search.fulltext()
                .filter(eq(child_value, "red"))
                .filter(eq(child_value, "blue"));

        FulltextSearch search = Search.fulltext()
                .setStrict(false)
                .andChildrenSearch(childSearch1,child);

        SearchResult result = server.execute(search, parent);
        assertEquals(0, result.getNumOfResults());

        childSearch1 = Search.fulltext()
                .filter(eq(child_value, "red"));
        FulltextSearch childSearch2 = Search.fulltext()
                .filter(eq(child_value, "blue"));
        search = Search.fulltext()
                .setStrict(false)
                .andChildrenSearch(child, childSearch1,childSearch2);

        result = server.execute(search, parent);
        assertEquals(1, result.getNumOfResults());
        assertEquals(Integer.valueOf(2),result.getResults().get(0).getChildCount());

        childSearch1 = Search.fulltext()
                .filter(eq(child_value, "red"));
        childSearch2 = Search.fulltext()
                .filter(eq(child_value, "purple"));
        search = Search.fulltext()
                .setStrict(false)
                .andChildrenSearch(child, childSearch1, childSearch2);

        result = server.execute(search, parent);
        assertEquals(0, result.getNumOfResults());

        childSearch1 = Search.fulltext()
                .filter(eq(child_value, "red"))
                .filter(eq(shared_value, "red"));
        childSearch2 = Search.fulltext()
                .filter(eq(child_value, "green"));
        search = Search.fulltext()
                .setStrict(false)
                .filter(eq(parent_value, "red"))
                .orChildrenSearch(child, childSearch1,childSearch2);

        result = server.execute(search, parent);
        assertEquals(4, result.getNumOfResults());
    }
}
