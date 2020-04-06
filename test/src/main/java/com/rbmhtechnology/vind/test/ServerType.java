package com.rbmhtechnology.vind.test;

import com.google.common.base.CaseFormat;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.configure.SearchConfiguration;

import java.util.Optional;

public enum ServerType {

    SolrEmbedded {
        @Override
        SearchServer getSearchServer() {
            SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.EmbeddedSolrServerProvider");
            SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, false);
            System.setProperty("runtimeLib", "false");
            return SearchServer.getInstance();
        }
    },

    Elastic {
        @Override
        SearchServer getSearchServer() {
            SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.elasticsearch.backend.ElasticServerProvider");
            if(!SearchConfiguration.isSet(SearchConfiguration.SERVER_HOST)) {
                SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://localhost:9200");
            }
            if(!SearchConfiguration.isSet(SearchConfiguration.SERVER_COLLECTION)) {
                SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, "vind");
            }

            return SearchServer.getInstance();
        }
    },

    // * docker run --name vind-solr-2.1.0 -p 8983:8983 redlinkgmbh/vind-solr-server:2.1.0
    SolrRemote {
        @Override
        SearchServer getSearchServer() {
            SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.RemoteSolrServerProvider");
            SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://localhost:8983/solr");
            SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, "vind");
            SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, false);
            System.setProperty("runtimeLib", "false");
            return SearchServer.getInstance();
        }
    },

    // * ./bin/solr start c
    // * download exec jar for collection mgtm tool, e.g. http://central.maven.org/maven2/com/rbmhtechnology/vind/collection-managment-tool/2.1.0/
    // * java -jar collection-managment-tool-2.1.0-exec.jar -cc vind -from com.rbmhtechnology.vind:backend-solr:2.1.0 --in localhost:9983
    SolrRemoteCloud {
        @Override
        SearchServer getSearchServer() {
            SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.RemoteSolrServerProvider");
            SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://localhost:8983/solr");
            SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, "vind");
            SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, true);
            System.setProperty("runtimeLib", "true");
            return SearchServer.getInstance();
        }
    };

    abstract SearchServer getSearchServer();

    private static ServerType forName(String name) {
        //from solr-remote-cloud to SolrRemoteCloud
        return ServerType.valueOf(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, name));
    }

    public static ServerType current() {
        return Optional.ofNullable(System.getenv("VIND.TEST.BACKEND"))
                .map(ServerType::forName).orElse(ServerType.SolrEmbedded);
    }
}
