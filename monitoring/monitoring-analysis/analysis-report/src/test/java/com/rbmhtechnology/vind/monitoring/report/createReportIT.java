/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

import com.rbmhtechnology.vind.monitoring.report.preprocess.ReportPreprocessor;
import com.rbmhtechnology.vind.monitoring.report.writer.HtmlReportWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created on 08.06.18.
 */
public class createReportIT {

    private final String esHost = "localhost";
    private final String esPort = "9200";
    private final String esIndex ="logindex";
    private final String applicationName = "Application name - 0.0.0";
    private final String messageWrapper = "message_json";
    private ElasticSearchReportService esRepsortService;

    private Report report;
    private HtmlReportWriter reportWriter =  new HtmlReportWriter();
    private File reportFile;
    private ReportPreprocessor reportPreprocessor;
    private ZonedDateTime from;
    private ZonedDateTime to;


    @Before
    public void setUp() throws IOException {
        from = ZonedDateTime.now().minusYears(1);
        to = ZonedDateTime.now().plusYears(1);

        reportPreprocessor = new ReportPreprocessor(esHost, esPort, esIndex, from, to, applicationName, messageWrapper);

        esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, from, to, applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);


        reportFile = File.createTempFile("reportTest", ".html");

    }

    @Test
    @Ignore
    public void preprocessIT() {
        reportPreprocessor
                .addSystemFilterField("static_partitionID")
                .preprocess();

    }

    @Test
    @Ignore
    public void createReportIT() {
        final LinkedHashMap<String, Long> topFaceFields = esRepsortService.getTopFaceFields();
        final ArrayList<String> facetFields = new ArrayList<>(topFaceFields.keySet());

        final LinkedHashMap<String, Long> topSuggestionFields = esRepsortService.getTopSuggestionFields();
        final ArrayList<String> suggestFields = new ArrayList<>(topSuggestionFields.keySet());
        this.report = new Report()
                .setApplicationName(applicationName)
                .setFrom(from)
                .setTo(to)
                .setTopDays(esRepsortService.getTopDays())
                .setRequests(esRepsortService.getTotalRequests())
                .setTopSuggestionFields(topSuggestionFields)
                .setSuggestionFieldsValues(esRepsortService.getSuggestionFieldsValues(suggestFields))
                .setFacetFieldsValues(esRepsortService.getFacetFieldsValues(facetFields))
                .setTopFacetFields(topFaceFields)
                .setTopQueries(esRepsortService.getTopQueries())
                .setTopUsers(esRepsortService.getTopUsers());
        Assert.assertTrue(reportWriter.write(this.report,"/home/fonso/reportTest.html"/*reportFile.getAbsolutePath()*/));

        System.out.println("Report has been written to " + reportFile.getAbsolutePath());

    }

    //@After
    public void cleanUp() throws Exception {
        esRepsortService.close();

        if (reportFile.exists()) {
            reportFile.delete();
        }
    }


}
