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
    public TestBackend backend = new TestBackend();

    private DocumentFactory document = new DocumentFactoryBuilder("document").build();

    @Test
    public void testDocumentCommitWithin() throws InterruptedException {
        backend.getSearchServer().indexWithin(document.createDoc("1"), 1000);

        Assert.assertEquals(1, backend.getSearchServer().execute(Search.getById("1"), document).getNumOfResults());
        Assert.assertEquals(0, backend.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        Thread.sleep(1500);

        Assert.assertEquals(1, backend.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        backend.getSearchServer().clearIndex();
    }

    @Test
    public void testMultipleDocumentsCommitWithin() throws InterruptedException {
        backend.getSearchServer().indexWithin(ImmutableList.of(
               document.createDoc("2"),
               document.createDoc("3")
        ),1000);

        Assert.assertEquals(1, backend.getSearchServer().execute(Search.getById("2"), document).getNumOfResults());
        Assert.assertEquals(1, backend.getSearchServer().execute(Search.getById("3"), document).getNumOfResults());
        Assert.assertEquals(0, backend.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        Thread.sleep(1500);

        Assert.assertEquals(2, backend.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        backend.getSearchServer().clearIndex();
    }

    @Test
    public void testRemoveDocumentWithin() throws InterruptedException {
        Document doc = document.createDoc("4");

        backend.getSearchServer().index(doc);
        backend.getSearchServer().commit();

        backend.getSearchServer().deleteWithin(doc, 1000);

        Assert.assertEquals(0, backend.getSearchServer().execute(Search.getById("4"), document).getNumOfResults());
        Assert.assertEquals(1, backend.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

        Thread.sleep(1500);

        Assert.assertEquals(0, backend.getSearchServer().execute(Search.fulltext(), document).getNumOfResults());

    }

}
