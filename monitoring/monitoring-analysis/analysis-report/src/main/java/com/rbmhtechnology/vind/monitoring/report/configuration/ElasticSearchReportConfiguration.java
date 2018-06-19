package com.rbmhtechnology.vind.monitoring.report.configuration;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ElasticSearchReportConfiguration extends ReportConfiguration {

    private String messageWrapper;
    private Map<String, String> esFilters = new HashMap<>();
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

    public Map<String, String> getEsFilters() {
        return esFilters;
    }

    public ElasticSearchReportConfiguration setEsFilters(Map<String, String> esFilters) {
        this.esFilters = esFilters;
        return this;
    }

    public ElasticSearchReportConfiguration addEsFilter(String field, String value) {
        if(Objects.nonNull(field) && Objects.nonNull(value)) {
            this.esFilters.put(field, value);
        }
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

    @Override
    public ElasticSearchReportConfiguration setSystemFilterFields(String ... systemFilterFields) {
        super.setSystemFilterFields(systemFilterFields);
        return this;
    }

    @Override
    public ElasticSearchReportConfiguration setForcePreprocessing(boolean forcePreprocessing) {
        super.setForcePreprocessing(forcePreprocessing);
        return this;
    }
}
