package com.rbmhtechnology.vind.monitoring.report.configuration;

public class ElasticSearchConnectionConfiguration {

    private String esHost;
    private String esPort;
    private String esIndex;

    public ElasticSearchConnectionConfiguration(String esHost, String esPort, String esIndex) {
        this.esHost = esHost;
        this.esPort = esPort;
        this.esIndex = esIndex;
    }

    public String getEsHost() {
        return esHost;
    }

    public String getEsPort() {
        return esPort;
    }

    public String getEsIndex() {
        return esIndex;
    }
}
