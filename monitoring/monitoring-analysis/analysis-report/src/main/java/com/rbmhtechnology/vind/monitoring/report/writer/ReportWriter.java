/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.writer;


import com.rbmhtechnology.vind.monitoring.report.Report;

/**
 * Created on 01.03.18.
 */
public interface ReportWriter {

    String write(Report report);
    boolean write(Report report, String reportFile);
}
