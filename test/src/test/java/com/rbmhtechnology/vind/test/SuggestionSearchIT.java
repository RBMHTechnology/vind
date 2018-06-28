/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import com.rbmhtechnology.vind.solr.backend.SolrSearchServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created on 20.09.17.
 */
public class SuggestionSearchIT {

    @Rule
    public TestSearchServer testSearchServer = new TestSearchServer();

    private DocumentFactory parent, child;
    private SingleValueFieldDescriptor<String> parent_value;
    private SingleValueFieldDescriptor<String> child_value;
    private SingleValueFieldDescriptor<String> shared_value;

    private SearchServer server;

    @Before
    public void before() {

        server = testSearchServer.getSearchServer();
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

        parent = new DocumentFactoryBuilder("parent")
                .setUpdatable(true)
                .addField(shared_value, parent_value)
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
}
