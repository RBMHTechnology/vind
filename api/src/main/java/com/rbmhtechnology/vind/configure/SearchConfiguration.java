package com.rbmhtechnology.vind.configure;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 24.06.16.
 */
public class SearchConfiguration {

    public static final String SERVER_COLLECTION = "server.collection";
    public static final String SERVER_HOST = "server.host";
    public static final String SERVER_PROVIDER = "server.provider";
    public static final String SERVER_CONNECTION_TIMEOUT = "server.connection.timeout";
    public static final String SERVER_SO_TIMEOUT = "server.so.timeout";

    public static final String SERVER_SOLR_CLOUD = "server.solr.cloud";
    @Deprecated
    public static final String SERVER_SOLR_COLLECTION = "server.solr.collection";
    @Deprecated
    public static final String SERVER_SOLR_HOST = "server.solr.host";
    @Deprecated
    public static final String SERVER_SOLR_PROVIDER = "server.solr.provider";

    public static final String APPLICATION_EXECUTOR_THREADS = "application.executor.threads";
    public static final String SEARCH_RESULT_PAGESIZE = "search.result.pagesize";
    public static final String SEARCH_RESULT_SHOW_SCORE = "search.result.showScore";
    public static final String SEARCH_RESULT_FACET_INCLUDE_EMPTY = "search.result.facet.includeEmpty";
    public static final String SEARCH_RESULT_FACET_LENGTH = "search.result.facet.length";

    private static Logger log = LoggerFactory.getLogger(SearchConfiguration.class);

    public static final String VIND_FILE_SYSTEM_PROPERTY = "vind.properties.file";
    public static final String VIND_PROPERTIES_FILE = "vind.properties";
    public static final String DEFAULT_PROPERTIES_FILE = "default.properties";

    public static final String VIND_ENV_PREFIX = "VIND_";

    private static Properties PROPERTIES;

    static void init() {
        try {
            Properties defaultProps = new Properties();
            defaultProps.load(Resources.getResource(DEFAULT_PROPERTIES_FILE).openStream());

            PROPERTIES = new Properties(defaultProps);

            log.info("Trying to load configurations file from System properties var '{}':...", VIND_FILE_SYSTEM_PROPERTY);
            final String propertyFilePath = System.getProperty(VIND_FILE_SYSTEM_PROPERTY);
            if (propertyFilePath != null) {
                log.info("Found system var with properties file path '{}'", propertyFilePath);
                try (final FileInputStream fileInputStream = new FileInputStream(propertyFilePath)){
                    log.info("Found system configuration in '{}'", propertyFilePath);
                    PROPERTIES.load(fileInputStream);
                } catch (FileNotFoundException e) {
                    log.error("Cannot load configuration from path '{}'", propertyFilePath, e);
                    throw new RuntimeException(e);
                }

            } else {
                log.info("configurations file from System properties");
            }

            ClassLoader contextClassLoader = getContextClassLoader();
            log.info("Trying to load custom configurations from file '{}':...", VIND_PROPERTIES_FILE);
            URL url = contextClassLoader.getResource(VIND_PROPERTIES_FILE);
            if(url!=null){
                log.info("Found custom configuration in '{}'", url.getPath());
                PROPERTIES.load(url.openStream());
            } else {
                log.info("Not found configurations file path in System properties, using default values");
            }

            //Try to load from EnvironmentVariables
            loadEnvVarProperties().forEach((key, value) -> PROPERTIES.setProperty(key, value));

        } catch (IOException e) {
            log.error("Cannot load configuration from path {}", System.getProperty(VIND_FILE_SYSTEM_PROPERTY), e);
            throw new RuntimeException(e);
        }
    }

    protected static Map<String, String> loadEnvVarProperties() {
        Map<String,String> envVarProps = new HashMap<>();

        System.getenv()
                .entrySet().stream()
                .filter(e -> e.getKey().startsWith(VIND_ENV_PREFIX))
                .forEach(e -> {
                    envVarProps.put(getPropertyNameFromEnvVar(e.getKey()), e.getValue());
                });

        return envVarProps;
    }

    protected static String getPropertyNameFromEnvVar(String key) {
        return key.substring(VIND_ENV_PREFIX.length()).toLowerCase().replaceAll("_", ".");
    }

    static {
        init();
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static int get(String key, int defaultValue) {
        try {
            return Integer.parseInt(PROPERTIES.getProperty(key, String.valueOf(defaultValue)));
        } catch (NullPointerException | NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean get(String key, boolean defaultValue) {
        log.trace("Get property {}", key);
        try {
            return Boolean.parseBoolean(PROPERTIES.getProperty(key, String.valueOf(defaultValue)));
        } catch (NullPointerException | ClassCastException e) {
            return defaultValue;
        }
    }

    public static void set(String key, String value) {
        PROPERTIES.setProperty(key, value);
    }

    public static void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    public static void set(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    private static List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();

        try(
                InputStream in = getResourceAsStream( path );
                BufferedReader br = new BufferedReader( new InputStreamReader( in ) ) ) {
            String resource;

            while( (resource = br.readLine()) != null ) {
                filenames.add( resource );
            }
        }

        return filenames;
    }

    private static InputStream getResourceAsStream( String resource ) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream( resource );

        return in == null ? SearchConfiguration.class.getResourceAsStream(resource) : in;
    }

    private static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }


}
