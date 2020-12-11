package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.MasterSlaveSearchServer;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.CursorResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import org.apache.commons.math3.util.IntegerSequence;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CursorSearchTest {

    private ElasticTestSearchServer elasticTestSearchServer;
    private SearchServer elastic;
    private DocumentFactory factory;

    @Before
    public void before() {
        //start elastic container
        elasticTestSearchServer = new ElasticTestSearchServer();
        elasticTestSearchServer.start();

        //get elastic search server
        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.elasticsearch.backend.ElasticServerProvider");
        elastic = SearchServer.getInstance();

        final FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildTextField("title");

        factory = new DocumentFactoryBuilder("testDocs").addField(title).build();

        for( int i: new IntegerSequence.Range(0,50,1) ) {
            elastic.index(factory.createDoc(String.valueOf(i)).setValue(title, "Hello "+i));
        }
    }

    @After
    public void after() {
        elastic.close();
        elasticTestSearchServer.close();
    }

    @Test
    public void testCursorSearch() {

        final CursorResult cursorResult = (CursorResult)elastic.execute(
                Search.fulltext().cursor(10L, 50),
                factory);
        Assert.assertNotNull(cursorResult.getCursor());
        Assert.assertEquals(51, cursorResult.getNumOfResults() );
        Assert.assertEquals(50, cursorResult.getResults().size());

        final CursorResult next = cursorResult.next();
        Assert.assertNotNull(cursorResult.getCursor());
        Assert.assertEquals(51, next.getNumOfResults() );
        Assert.assertEquals(1, next.getResults().size() );

        next.closeCursor();

    }
}
