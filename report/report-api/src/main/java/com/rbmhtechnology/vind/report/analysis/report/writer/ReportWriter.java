/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.analysis.report.writer;

import com.rbmhtechnology.vind.report.analysis.report.Report;

/**
 * Created on 01.03.18.
 */
public interface ReportWriter {

    public String write(Report report);

    public boolean write(Report report, String reportFile);
}
