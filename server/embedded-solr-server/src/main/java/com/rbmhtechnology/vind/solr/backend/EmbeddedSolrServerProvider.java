package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.utils.FileSystemUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrXmlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Jakob Frank (jakob.frank@redlink.co)
 */
public class EmbeddedSolrServerProvider implements SolrServerProvider {

    public static final String CORE_NAME = "core";
    public static final String HOME_PATH = "/solrhome";
    public static final String CORE_PROPERTIES_FILE = "core.properties";

    @Override
    public SolrClient getInstance() {
        try {
            final Path tmpSolrHome = Files.createTempDirectory("solr-home");
            final Path solrHome = FileSystemUtils.toPath(this.getClass().getResource(HOME_PATH));

            Files.walkFileTree(solrHome, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new TreeCopier(solrHome, tmpSolrHome));

            final Path tmpSolrConfig = tmpSolrHome.resolve(SolrXmlConfig.SOLR_XML_FILE);

            // Needed for the tests. For some reason Solr is not replacing the solrConfig.xml ${runtimeLib} property with
            // the system property value but with the defined in the core.properties file.
            //TODO could be nicer ...
            final Properties properties = new Properties();
            final String corePropertiesFile = String.join("/",tmpSolrHome.toAbsolutePath().toString(),CORE_NAME,CORE_PROPERTIES_FILE);
            final FileInputStream iS = new FileInputStream(corePropertiesFile);
            properties.load(iS);
            iS.close();
            properties.setProperty("runtimeLib", "false");

            final FileOutputStream oS = new FileOutputStream(tmpSolrHome.toAbsolutePath().toString()+ "/core/" + CORE_PROPERTIES_FILE);
            properties.store(oS, null);
            oS.close();

            final CoreContainer container = CoreContainer.createAndLoad(tmpSolrHome, tmpSolrConfig);
            return new SolrClientWrapper(container, CORE_NAME, tmpSolrHome);

        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private class SolrClientWrapper extends EmbeddedSolrServer {

        private final Path temporaryFolder;

        public SolrClientWrapper(CoreContainer coreContainer, String coreName, Path temporaryFolder) {
            super(coreContainer, coreName);
            this.temporaryFolder = temporaryFolder;
        }

        @Override
        public void close() throws IOException {
            super.close();

            Files.walkFileTree(temporaryFolder, new RecursiveDeleter());
            Files.deleteIfExists(temporaryFolder);
        }
    }

    static class RecursiveDeleter extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    static class TreeCopier extends SimpleFileVisitor<Path> {

        private final Logger log = LoggerFactory.getLogger(getClass());
        private final Path source;
        private final Path target;

        public TreeCopier(Path source, Path target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path newdir = target.resolve(source.toAbsolutePath().relativize(dir.toAbsolutePath()).toString());
            try {
                Files.createDirectories(newdir);
            } catch (FileAlreadyExistsException x) {
                // ignore
            } catch (IOException x) {
                log.error("Unable to create: {}: {}", newdir, x);
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, target.resolve(source.toAbsolutePath().relativize(file.toAbsolutePath()).toString()));
            return FileVisitResult.CONTINUE;
        }

    }

}
