package com.rbmhtechnology.vind.monitoring.report.configuration;

public class ReportConfiguration {

    private boolean forcePreprocessing = false;
    private String applicationId;
    private ReportWriterConfiguration reportWriterConfiguration = new ReportWriterConfiguration();
    private String[] systemFilterFields;

    public String getApplicationId() {
        return applicationId;
    }

    public ReportWriterConfiguration getReportWriterConfiguration() {
        return reportWriterConfiguration;
    }

    public String[] getSystemFilterFields() {
        return systemFilterFields;
    }

    public boolean isForcePreprocessing() {
        return forcePreprocessing;
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

    public ReportConfiguration setSystemFilterFields(String[] systemFilterFields) {
        this.systemFilterFields = systemFilterFields;
        return this;
    }

    public ReportConfiguration setForcePreprocessing(boolean forcePreprocessing) {
        this.forcePreprocessing = forcePreprocessing;
        return this;
    }
}
