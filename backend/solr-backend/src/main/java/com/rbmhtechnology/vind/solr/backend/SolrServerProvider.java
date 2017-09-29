package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.ServiceProvider;
import org.apache.solr.client.solrj.SolrClient;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 21.06.16.
 */
public interface SolrServerProvider extends ServiceProvider {
    SolrClient getInstance();
}