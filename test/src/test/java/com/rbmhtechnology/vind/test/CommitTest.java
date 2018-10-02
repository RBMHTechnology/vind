package com.rbmhtechnology.vind.test;

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
    }

}
