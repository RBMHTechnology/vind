package com.rbmhtechnology.vind.solr.cmt;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 30.03.17.
 */
public class ConfigurationService {

    private static Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    private static ConfigurationService configurationService;

    private static final String configFile = "config.properties";

    private Properties properties;

    protected ConfigurationService() {
        properties = new Properties();
        try {
            properties.load(Resources.getResource(configFile).openStream());
        } catch (IOException e) {
            logger.error("Cannot load properties file");
            throw new RuntimeException(e);
        }
    }

    public static ConfigurationService getInstance() {
        if(configurationService == null) {
            configurationService = new ConfigurationService();
        }
        return configurationService;
    }

    public Properties getProperties() {
        return properties;
    }
}
