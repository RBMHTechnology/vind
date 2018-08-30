package com.rbmhtechnology.vind.solr.cmt;

import com.google.common.io.Resources;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 29.03.17.
 */
public class CollectionManagementServiceUtilsTest {

    private CollectionManagementService service = new CollectionManagementService();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void downloadToTempDir() throws IOException {
        Path file = CollectionManagementService.Utils.downloadToTempDir(folder.getRoot().toPath(), Collections.singletonList("src/test/resources/lib/"), "at.redlink:test:1.0.0");
        assertTrue(Files.exists(file));
    }

    @Test
    public void utilsNameToPathTest() throws IOException {
        String path = CollectionManagementService.Utils.nameToPath("at.redlink:test:1.0.0");
        assertEquals(path, "at/redlink/test/1.0.0/test-1.0.0.jar");
    }

    @Test(expected = IOException.class)
    public void utilsNameToPathTest2() throws IOException {
        CollectionManagementService.Utils.nameToPath("at.redlink:test-1.0.0");
    }

    @Test
    public void unzipJarTest() throws URISyntaxException {
        Path dir = CollectionManagementService.Utils.unzipJar(folder.getRoot().toPath(), Paths.get(Resources.getResource("conf.jar").toURI()));
        assertEquals("unzipped", dir.getFileName().toString());
        assertTrue(Files.exists(Paths.get(dir.toString(),"conf")));

    }

    @Test
    public void testDownloadConfiguration() throws IOException {
        Path configDir = service.downloadConfiguration("com.rbmhtechnology.vind:backend-solr:1.2.2");
        assertTrue(configDir.toString().endsWith("/unzipped/solrhome/core/conf"));
    }
}
