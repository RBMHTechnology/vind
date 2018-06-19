package com.rbmhtechnology.vind.monitoring.report.configuration;

import org.apache.commons.lang3.StringUtils;

public class ElasticSearchReportConfiguration extends ReportConfiguration {

    private String messageWrapper;
    private String esEntryType;
    private ElasticSearchConnectionConfiguration connectionConfiguration;

    //Getter
    public String getMessageWrapper() {
        return messageWrapper;
    }

    public String getEsEntryType() {
        return esEntryType;
    }

    public ElasticSearchConnectionConfiguration getConnectionConfiguration() {
        return connectionConfiguration;
    }

    //Setter
    public ElasticSearchReportConfiguration setMessageWrapper(String messageWrapper) {
        if(StringUtils.isNotBlank(messageWrapper) && !messageWrapper.endsWith(".")) {
            this.messageWrapper = messageWrapper + ".";
        } else if(StringUtils.isNotBlank(messageWrapper)) {
            this.messageWrapper = messageWrapper;
        } else {
            this.messageWrapper = "";
        }
        return this;
    }

    public ElasticSearchReportConfiguration setEsEntryType(String esEntryType) {
        this.esEntryType = esEntryType;
        return this;
    }

    public ElasticSearchReportConfiguration setConnectionConfiguration(ElasticSearchConnectionConfiguration connectionConfiguration) {
        this.connectionConfiguration = connectionConfiguration;
        return this;
    }

    @Override
    public ElasticSearchReportConfiguration setApplicationId(String applicationId) {
        super.setApplicationId(applicationId);
        return this;
    }

    @Override
    public ElasticSearchReportConfiguration setReportWriterConfiguration(ReportWriterConfiguration reportWriterConfiguration) {
        super.setReportWriterConfiguration(reportWriterConfiguration);
        return this;
    }
}
