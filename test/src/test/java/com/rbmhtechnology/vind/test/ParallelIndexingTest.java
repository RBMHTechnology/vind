package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.MasterSlaveSearchServer;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ParallelIndexingTest {

    private ElasticTestSearchServer elasticTestSearchServer;

    private SearchServer elastic;

    private SearchServer solr;

    private SearchServer masterSlave;

    @Before
    public void before() {
        //start elastic container
        elasticTestSearchServer = new ElasticTestSearchServer();
        elasticTestSearchServer.start();

        //get elastic search server
        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.elasticsearch.backend.ElasticServerProvider");
        elastic = SearchServer.getInstance();

        //get embedded solr search server
        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.EmbeddedSolrServerProvider");
        SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, false);
        System.setProperty("runtimeLib", "false");
        solr = SearchServer.getInstance();

        //create combined search server
        masterSlave = new MasterSlaveSearchServer(solr, elastic);
    }

    @After
    public void after() {
        masterSlave.close();
        elasticTestSearchServer.close();
    }

    @Test
    public void testParallelIndexing() {

        FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildTextField("title");

        DocumentFactory assets = new DocumentFactoryBuilder("asset").addField(title).build();

        Document d1 = assets.createDoc("1").setValue(title, "Hello World");

        masterSlave.index(d1);
        masterSlave.commit();

        Assert.assertEquals("MasterSlave is not indexed properly", 1, masterSlave.execute(Search.fulltext(), assets).getNumOfResults());
        Assert.assertEquals("Master is not indexed properly", 1, solr.execute(Search.fulltext(), assets).getNumOfResults());
        Assert.assertEquals("Salve is not indexed properly", 1, elastic.execute(Search.fulltext(), assets).getNumOfResults());

        elastic.clearIndex();

        Assert.assertEquals("Salve is not deleted properly", 0, elastic.execute(Search.fulltext(), assets).getNumOfResults());
        Assert.assertEquals("MasterSlave is not searching properly", 1, masterSlave.execute(Search.fulltext(), assets).getNumOfResults());
    }

}
