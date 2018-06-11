/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

import com.rbmhtechnology.vind.monitoring.report.writer.HtmlReportWriter;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created on 08.06.18.
 */
public class createReportIT {

    private String esHost = "localhost";
    private String esPort = "9200";
    private String esIndex = "logAnalysis";
    private String applicationName = "App name";
    private final String messageWrapper = "message_json";
    private ElasticSearchReportService esRepsortService;

    private Report report;
    private HtmlReportWriter reportWriter =  new HtmlReportWriter();
    private File reportFile;


    @Before
    public void setUp() throws IOException {
        final ZonedDateTime from = ZonedDateTime.now().minusYears(1);
        final ZonedDateTime to = ZonedDateTime.now().plusYears(1);

        esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, from, to, applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);

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

        reportFile = File.createTempFile("reportTest", ".html");
    }

    @Test
    @Ignore
    public void createReportIT() {

        Assert.assertTrue(reportWriter.write(this.report,reportFile.getAbsolutePath()));

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
