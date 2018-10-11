package com.rbmhtechnology.vind.test;

import com.google.common.collect.ImmutableList;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class CommitTest {

    @Rule
    public TestSearchServer testSearchServer = new TestSearchServer();

    private DocumentFactory document = new DocumentFactoryBuilder("document").build();

    @Test
    public void testDocumentCommitWithin() throws InterruptedException {
        testSearchServer.getSearchServer().indexWithin(document.createDoc("1"), 1000);

        Assert.assertEquals(1, testSearchServer.getSearchServer().execute(Search.getById("1"), document).getNumOfResults());
        Assert.assertEquals(0, testSearchServer.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        Thread.sleep(1100);

        Assert.assertEquals(1, testSearchServer.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        testSearchServer.getSearchServer().clearIndex();
    }

    @Test
    public void testMultipleDocumentsCommitWithin() throws InterruptedException {
        testSearchServer.getSearchServer().indexWithin(ImmutableList.of(
               document.createDoc("2"),
               document.createDoc("3")
        ),1000);

        Assert.assertEquals(1, testSearchServer.getSearchServer().execute(Search.getById("2"), document).getNumOfResults());
        Assert.assertEquals(1, testSearchServer.getSearchServer().execute(Search.getById("3"), document).getNumOfResults());
        Assert.assertEquals(0, testSearchServer.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        Thread.sleep(1100);

        Assert.assertEquals(2, testSearchServer.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        testSearchServer.getSearchServer().clearIndex();
    }

    @Test
    public void testRemoveDocumentWithin() throws InterruptedException {
        Document doc = document.createDoc("4");

        testSearchServer.getSearchServer().index(doc);
        testSearchServer.getSearchServer().commit();

        testSearchServer.getSearchServer().deleteWithin(doc, 1000);

        Assert.assertEquals(0, testSearchServer.getSearchServer().execute(Search.getById("4"), document).getNumOfResults());
        Assert.assertEquals(1, testSearchServer.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        Thread.sleep(1100);

        Assert.assertEquals(0, testSearchServer.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

    }

}
