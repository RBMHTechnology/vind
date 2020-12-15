package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.CursorResult;
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

import java.time.ZonedDateTime;
import java.util.Date;

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

        final FieldDescriptor<Number> number = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildNumericField("number");

        final FieldDescriptor<ZonedDateTime> zonedDate = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildDateField("zonedDate");

        final FieldDescriptor<Date> utilDate = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildUtilDateField("utilDate");

        factory = new DocumentFactoryBuilder("testDocs").addField(title,number,zonedDate, utilDate).build();

        for( int i: new IntegerSequence.Range(0,50,1) ) {
            elastic.index(
                    factory.createDoc(String.valueOf(i))
                            .setValue(title, "Hello "+i)
                            .setValue(number,i)
                            .setValue(zonedDate,ZonedDateTime.now())
                            .setValue(utilDate,new Date())
            );
        }
    }

    @After
    public void after() {
        elastic.close();
        elasticTestSearchServer.close();
    }

    @Test
    public void testCursorNextSearch() {

        final CursorResult cursorResult = (CursorResult)elastic.execute(
                Search.fulltext().cursor(10L, 50).sort(Sort.desc("zonedDate")),
                factory);
        Assert.assertNotNull(cursorResult.getSearchAfter());
        Assert.assertEquals(51, cursorResult.getNumOfResults() );
        Assert.assertEquals(50, cursorResult.getResults().size());

        final CursorResult next = cursorResult.next();
        Assert.assertNotNull(cursorResult.getSearchAfter());
        Assert.assertEquals(51, next.getNumOfResults() );
        Assert.assertEquals(1, next.getResults().size() );

        next.closeCursor();

    }

    @Test
    public void testCursorSearchAfterSearch() {

        final CursorResult cursorResult = (CursorResult)elastic.execute(
                Search.fulltext().cursor(10L, 50).sort(Sort.desc("utilDate")),
                factory);
        Assert.assertNotNull(cursorResult.getSearchAfter());
        Assert.assertEquals(51, cursorResult.getNumOfResults() );
        Assert.assertEquals(50, cursorResult.getResults().size());

        final CursorResult next = (CursorResult)elastic.execute(
                Search.fulltext().cursor(cursorResult.getSearchAfter() ,10L, 50).sort(Sort.desc("utilDate")),
                factory);
        Assert.assertNotNull(cursorResult.getSearchAfter());
        Assert.assertEquals(51, next.getNumOfResults() );
        Assert.assertEquals(1, next.getResults().size() );

        next.closeCursor();

    }
}
