package com.rbmhtechnology.vind.solr.cmt;

import com.google.common.io.Resources;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.CollectionAdminRequest.Create;
import org.apache.solr.cloud.SolrCloudTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 29.03.17.
*/
@LuceneTestCase.Slow
@Ignore
public class CollectionManagementServiceTest extends SolrCloudTestCase {

    private static CollectionManagementService service;

    @BeforeClass
    public static void setupCluster() throws Exception {

        configureCluster(2)
                //add one config set
                .addConfig("com.rbmhtechnology.solr.test:test-config:1.0", Paths.get("src/test/resources/conf"))
                .configure();

        //add a collection with the set
        Create create = new CollectionAdminRequest.Create();
        create.setCollectionName("my-collection");
        create.setConfigName("com.rbmhtechnology.solr.test:test-config:1.0");
        create.setNumShards(1);
        create.setReplicationFactor(1);
        create.process(cluster.getSolrClient());

        //add blob collection
        Create create2 = new CollectionAdminRequest.Create();
        create2.setCollectionName(".system");
        create2.setNumShards(1);
        create2.setReplicationFactor(1);
        create2.process(cluster.getSolrClient());

        service = new CollectionManagementService(cluster.getZkServer().getZkAddress());

        //upload example jar
        service.uploadRuntimeLib("com.rbmhtechnology.solr.test:test-jar:1.0", Paths.get(Resources.getResource("runtimelib.jar").toURI()));

    }
    @AfterClass
    public static void resetCluster() throws Exception {
        shutdownCluster();
    }

    @Test
    public void configurationIsDeployedTest() throws IOException {
        assertTrue(service.configurationIsDeployed("com.rbmhtechnology.solr.test:test-config:1.0"));
        assertFalse(service.configurationIsDeployed("com.rbmhtechnology.solr.test:wrong-config:1.0"));
    }

    @Test
    public void checkAndInstallConfigurationTest() throws IOException {
        String configName = "com.rbmhtechnology.vind:backend-solr:1.1.2";
        service.checkAndInstallConfiguration(configName);
        assertTrue(service.configurationIsDeployed(configName));
    }

    @Test
    public void listRuntimeDependenciesTest() throws IOException, SolrServerException {
        List<String> runtimeDependencies = service.listRuntimeDependencies("my-collection");
        assertThat(runtimeDependencies, contains("org.apache.solr:solr-cell:6.1.0", "com.rbmhtechnology.solr.test:test-jar:1.0"));
    }

    @Test
    public void getVersionAndInstallIfNecessaryTest() {
        Long version = service.getVersionAndInstallIfNecessary("com.rbmhtechnology.solr.test:test-jar:1.0"); //already loaded
        assertEquals(1L, version, 0);
    }

    @Test
    public void getVersionAndInstallIfNecessaryTest2() {
        Long version = service.getVersionAndInstallIfNecessary("org.apache.solr:solr-cell:6.1.0"); //to be loaded
        assertEquals(1L, version, 0);
    }

    @Test
    public void checkAndInstallRuntimeDependenciesTest() {
        Map<String,Long> dependencies = service.checkAndInstallRuntimeDependencies("my-collection");
        assertThat(dependencies, hasEntry("org.apache.solr_solr-cell_6.1.0", 1L));
        assertThat(dependencies, hasEntry("com.rbmhtechnology.solr.test_test-jar_1.0",1L));
    }

    @Test
    public void addOrUpdateRuntimeDependenciesTest() {
        service.addOrUpdateRuntimeDependencies(Collections.singletonMap("com.rbmhtechnology.solr.test_test-jar_1.0",1L),"my-collection");
    }

}