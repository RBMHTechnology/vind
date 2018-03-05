/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.analysis.report.writer;

import com.rbmhtechnology.vind.report.analysis.report.Report;

/**
 * Created on 01.03.18.
 */
public class HtmlReportWriter implements ReportWriter{
    @Override
    public String write(Report report) {
        return null;
    }

    @Override
    public boolean write(Report report, String reportFile) {
        return false;
    }


    public boolean write(Report report, String reportFile, String htmlTemplate) {
        return false;
    }
}
