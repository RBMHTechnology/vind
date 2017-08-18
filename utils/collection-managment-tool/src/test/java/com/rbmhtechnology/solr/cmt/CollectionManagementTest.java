package com.rbmhtechnology.solr.cmt;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.cloud.SolrCloudTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 03.04.17.
 */
@LuceneTestCase.Slow
public class CollectionManagementTest extends SolrCloudTestCase {

    private static CollectionManagementService service;

    @BeforeClass
    public static void setupCluster() throws Exception {

        configureCluster(1)
                //add one config set
                .configure();

        String zkHost = cluster.getZkServer().getZkHost();

        service = new CollectionManagementService(zkHost);
        service.setClient(cluster.getSolrClient());
    }

    @AfterClass
    public static void resetCluster() throws Exception {
        shutdownCluster();
    }

    //TODO add some test
}