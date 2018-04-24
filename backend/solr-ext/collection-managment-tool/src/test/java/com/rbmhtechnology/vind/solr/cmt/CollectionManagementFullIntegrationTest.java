package com.rbmhtechnology.vind.solr.cmt;

import org.junit.Ignore;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 03.04.17.
 * BEFORE RUN THIS TEST:
 * 0. Be sure that you have installed the latest fix (for runtime libs) of backend/solr module locally.
 * 1. Download/Unpackage Solr `rm -R solr-5.5.2 && wget http://archive.apache.org/dist/lucene/solr/5.5.2/solr-5.5.2.zip && unzip solr-5.5.2.zip && rm solr-5.5.2.zip`
 * 2. Start Solr in default cloud example `solr-5.5.2/bin/solr -e cloud -noprompt -a "-Denable.runtime.lib=true"`
 * 3. Change the path for local maven repo (in test code) and run the test
 * 4. Test if core exits and runtimelib is installed `curl 'http://localhost:8983/solr/test-for-searchlib/suggester?indent=on&q=*:*&wt=json'` (ATTENTION: returns with code 400, which is okay ;)
 */
public class CollectionManagementFullIntegrationTest {

    @Ignore
    public void testAll() throws IOException {

        String zkHost = "localhost:9983";

        String[] repositories = {
                "/~/.m2/repository"
        };

        CollectionManagementService service = new CollectionManagementService(zkHost, repositories);

        assertFalse(service.collectionExists("test-for-vind"));

        service.createCollection("test-for-vind", "com.rbmhtechnology.vind:backend-solr:1.1.2", 1, 1);

        assertTrue(service.collectionExists("test-for-vind"));

        service.updateCollection("test-for-vind", "com.rbmhtechnology.vind:backend-solr:1.1.2");

        service.removeCollection("test-for-vind");

        assertFalse(service.collectionExists("test-for-vind"));

        service.removeConfigSet("com.rbmhtechnology.vind:backend-solr:1.1.2");
    }

}
