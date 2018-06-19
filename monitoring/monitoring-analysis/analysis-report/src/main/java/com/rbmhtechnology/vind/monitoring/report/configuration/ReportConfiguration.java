package com.rbmhtechnology.vind.monitoring.report.configuration;

public class ReportConfiguration {

    private String applicationId;
    private ReportWriterConfiguration reportWriterConfiguration;

    //Getter
    public ReportWriterConfiguration getReportWriterConfiguration() {
        return reportWriterConfiguration;
    }

    public String getApplicationId() {
        return applicationId;
    }

    //Setter

    public ReportConfiguration setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public ReportConfiguration setReportWriterConfiguration(ReportWriterConfiguration reportWriterConfiguration) {
        this.reportWriterConfiguration = reportWriterConfiguration;
        return this;
    }
}
