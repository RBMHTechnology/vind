
package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextTerm;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.rbmhtechnology.vind.api.query.sort.Sort.SpecialSort.numberOfMatchingTermsSort;
import static com.rbmhtechnology.vind.api.query.sort.Sort.desc;
import static com.rbmhtechnology.vind.test.Backend.Elastic;
import static com.rbmhtechnology.vind.test.Backend.Solr;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created on 20.09.17.
 */
public class SuggestionSearchIT {

    @Rule
    public TestBackend backend = new TestBackend();

    private DocumentFactory parent, child;
    private SingleValueFieldDescriptor<String> parent_value;
    private SingleValueFieldDescriptor<String> child_value;
    private SingleValueFieldDescriptor<String> shared_value;
    private MultiValueFieldDescriptor<String> multi_value;

    private SearchServer server;

    @Before
    public void before() {

        server = backend.getSearchServer();
        //
        // server = SolrSearchServer.getInstance("com.rbmhtechnology.vind.solr.backend.RemoteSolrServerProvider", "http://localhost:8983/solr", "colorsTest");

        server.clearIndex();
        server.commit();

        parent_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setFullText(true)
                .setSuggest(true)
                .buildTextField("parent_value");

        child_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setSuggest(true)
                .setFullText(true)
                .buildTextField("child_value");

        shared_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setFullText(true)
                .setSuggest(true)
                .buildTextField("shared_value");

        multi_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setSuggest(true)
                .setFullText(true)
                .buildMultivaluedTextField("multi_value");

        parent = new DocumentFactoryBuilder("parent")
                .setUpdatable(true)
                .addField(shared_value, parent_value, multi_value)
                .build();

        child = new DocumentFactoryBuilder("child")
                .setUpdatable(true)
                .addField(shared_value, child_value)
                .build();


        /**
         * Testset
         *  Parent P1       (p:black,             s:yellow)
         *  Parent P2       (p:blue             s:purple)
         *      Child C1    (       c:red       s:red)
         *      Child C2    (       c:blue      s:yellow)
         *  Parent P3       (p:red              s:red)
         *      Child C3    (       c:blue      s:black)
         *  Parent P4       (p:orange              s:black)
         *      Child C4    (       c:green      s:black)
         */

        server.clearIndex();
        server.index(
                parent.createDoc("P1").setValue(parent_value, "black").setValue(shared_value, "yellow"),
                parent.createDoc("P2").setValue(parent_value, "blue").setValue(shared_value, "purple").addChild(
                        child.createDoc("C1").setValue(child_value, "red").setValue(shared_value, "red"),
                        child.createDoc("C2").setValue(child_value, "blue").setValue(shared_value, "yellow")),
                parent.createDoc("P3").setValue(parent_value, "red").setValue(shared_value, "red").addChild(
                        child.createDoc("C3").setValue(child_value, "blue").setValue(shared_value, "black")),
                parent.createDoc("P4").setValue(parent_value, "orange").setValue(shared_value, "black").addChild(
                        child.createDoc("C4").setValue(child_value, "green").setValue(shared_value, "black"))
        );

        server.commit();

    }

    @Test
    @RunWithBackend(Solr)
    public void childrenSuggestionTest(){
        SuggestionResult suggestionSearch = server.execute(Search.suggest("gree").fields(child_value), parent, child);
        assertEquals(1, suggestionSearch.get(child_value).getValues().size());
        assertEquals("green", suggestionSearch.get(child_value).getValues().get(0).getValue());
        assertEquals(1, suggestionSearch.get(child_value).getValues().get(0).getCount());

        suggestionSearch = server.execute(Search.suggest("blu").fields(child_value), parent, child);
        assertEquals(1, suggestionSearch.get(child_value).getValues().size());
        assertEquals("blue", suggestionSearch.get(child_value).getValues().get(0).getValue());
        assertEquals(2, suggestionSearch.get(child_value).getValues().get(0).getCount());

        suggestionSearch = server.execute(Search.suggest("blu").fields(parent_value), parent, child);
        assertEquals(1, suggestionSearch.get(parent_value).getValues().size());
        assertEquals("blue", suggestionSearch.get(parent_value).getValues().get(0).getValue());
        assertEquals(1, suggestionSearch.get(parent_value).getValues().get(0).getCount());

        suggestionSearch = server.execute(Search.suggest("bl").fields(shared_value), parent, child);
        assertEquals(1, suggestionSearch.get(shared_value).getValues().size());
        assertEquals("black", suggestionSearch.get(shared_value).getValues().get(0).getValue());
        assertEquals(3, suggestionSearch.get(shared_value).getValues().get(0).getCount());

        suggestionSearch = server.execute(Search.suggest("bl").fields(shared_value,parent_value), parent, child);
        assertEquals(1, suggestionSearch.get(shared_value).getValues().size());
        assertEquals("black", suggestionSearch.get(shared_value).getValues().get(0).getValue());
        assertEquals(3, suggestionSearch.get(shared_value).getValues().get(0).getCount());
        assertEquals(2, suggestionSearch.get(parent_value).getValues().size());
        assertEquals("black", suggestionSearch.get(parent_value).getValues().get(0).getValue());
        assertEquals(1, suggestionSearch.get(parent_value).getValues().get(0).getCount());

        suggestionSearch = server.execute(Search.suggest("bl")
                .fields(shared_value,parent_value)
                .filter(Filter.eq(parent_value,"orange")), parent, child);
        assertEquals(1, suggestionSearch.get(shared_value).getValues().size());
        assertEquals("black", suggestionSearch.get(shared_value).getValues().get(0).getValue());
        assertEquals(2, suggestionSearch.get(shared_value).getValues().get(0).getCount());

        suggestionSearch = server.execute(Search.suggest("bl")
                .fields(shared_value,parent_value,child_value)
                .filter(Filter.eq(child_value,"blue")), parent, child);
        assertEquals(1, suggestionSearch.get(parent_value).getValues().size());
        assertEquals(1, suggestionSearch.get(child_value).getValues().size());
        assertEquals("blue", suggestionSearch.get(child_value).getValues().get(0).getValue());
        assertEquals(1, suggestionSearch.get(shared_value).getValues().size());

        suggestionSearch = server.execute(Search.suggest("bl")
                .fields(shared_value,parent_value,child_value)
                .filter(Filter.eq(shared_value,"yellow")), parent, child);
        assertEquals(1, suggestionSearch.get(parent_value).getValues().size());
        assertEquals(1, suggestionSearch.get(child_value).getValues().size());
        assertEquals("blue", suggestionSearch.get(child_value).getValues().get(0).getValue());
        assertEquals(null, suggestionSearch.get(shared_value));
    }

    @Test
    @RunWithBackend({Solr, Elastic})
    public void testSpecialCharacters() {
        server.index(
               parent.createDoc("P_SPEC_CHAR").setValue(parent_value, "León"));
        server.commit();

        SuggestionResult result = server.execute(Search.suggest("2015León, Mexico").fields(parent_value),parent);
        assertNotNull(result);
        assertEquals(1, result.size());

        server.delete(parent.createDoc("P_SPEC_CHAR"));
        server.commit();

        server.index(
                parent.createDoc("P_SPEC_CHAR").setValue(parent_value, "\"Film"));
        server.commit();

        result = server.execute(Search.suggest("\"Film").fields(parent_value),parent);
        assertNotNull(result);
        assertEquals(1, result.size());

        server.delete(parent.createDoc("P_SPEC_CHAR"));
        server.commit();
    }

    @Test
    @RunWithBackend({Solr, Elastic})
    public void testMultiWordSuggestion() {


        server.index(
                parent.createDoc("multi1").setValue(parent_value, "León city"));
        server.index(
                parent.createDoc("multi2").setValue(parent_value, "Lerida"));
        server.index(
                parent.createDoc("multi3").setValue(parent_value, "Oviedo city"));
        server.commit();

        SuggestionResult suggestionResult = server.execute(Search.suggest("le ci").addField(parent_value), parent);

        Assert.assertEquals(3, suggestionResult.size());

    }

    @Test
    @RunWithBackend({Solr, Elastic})
    public void testMultiWordSuggestionWithBaseSearchTerm() {

        server.index(
                parent.createDoc("multi1").setValue(parent_value, "León city"));
        server.index(
                parent.createDoc("multi2").setValue(parent_value, "Lerida"));
        server.index(
                parent.createDoc("multi3").setValue(parent_value, "Oviedo city"));
        server.index(
                parent.createDoc("multi4").setValue(parent_value, "Oviñana"));
        server.commit();

        SuggestionResult suggestionResult = server.execute(Search.suggest("Ov").fulltextTerm(new FulltextTerm("city", "100%")).addField(parent_value), parent);

        Assert.assertEquals(1, suggestionResult.size());
        assertEquals("Oviedo city", suggestionResult.get(parent_value).getValues().get(0).getValue());
    }

    @Test
    @RunWithBackend({Solr, Elastic})
    public void testColonSearch() {
        server.index(
                parent.createDoc("P_SPEC_CHAR").setValue(parent_value, "Servus Nachrichten 19:20 -> Season 4 -> Episode 20 - January 20"));
        server.commit();

        SuggestionResult result = server.execute(Search.suggest("Servus Nachrichten 19:20 Season 4 Episode 20").fields(parent_value),parent);
        assertNotNull(result);
        assertEquals(1, result.size());

        server.delete(parent.createDoc("P_SPEC_CHAR"));
        server.commit();
    }

    @Test
    @RunWithBackend({Elastic})
    public void testSuggestionSort() {
        server.index(
                parent.createDoc("multi1").setValue(parent_value, "New Lockdown"));
        server.index(
                parent.createDoc("multi2").setValue(parent_value, "Lindsey Vonn"));
        server.index(
                parent.createDoc("multi3").setValue(parent_value, "Lindsay Lohan"));
        server.index(
                parent.createDoc("multi4").setValue(parent_value, "New Lockdown"));
        server.commit();

        SuggestionResult suggestionResult = server.execute(Search.suggest("Lin Lo")
                        .addField(parent_value)
                        .setSort(desc(numberOfMatchingTermsSort(parent_value))),
                parent);

        Assert.assertEquals(3, suggestionResult.size());
        Assert.assertEquals("Lindsay Lohan", suggestionResult.get(parent_value).getValues().get(0).getValue());

        suggestionResult = server.execute(Search.suggest("Lin Lo").addField(parent_value), parent);
        Assert.assertEquals(3, suggestionResult.size());
        Assert.assertEquals("New Lockdown", suggestionResult.get(parent_value).getValues().get(0).getValue());

        server.index(
                parent.createDoc("multi1").setValues(multi_value, "Vienna City", "Salzburg City", "Austria"));
        server.index(
                parent.createDoc("multi2").setValues(multi_value, "Madrid City", "Spain"));
        server.index(
                parent.createDoc("multi3").setValues(multi_value, "People", "Animals", "Flowers"));
        server.commit();

        suggestionResult = server.execute(Search.suggest("Sal City")
                .addField(multi_value)
                .setSort(desc(numberOfMatchingTermsSort(multi_value))), parent);
        Assert.assertEquals(3, suggestionResult.size());
        Assert.assertEquals("Salzburg City", suggestionResult.get(multi_value).getValues().get(0).getValue());
    }
}
