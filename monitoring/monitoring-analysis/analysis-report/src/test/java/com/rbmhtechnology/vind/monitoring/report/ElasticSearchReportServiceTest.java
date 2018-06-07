/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Created on 01.03.18.
 */
public class ElasticSearchReportServiceTest {

    //TODO:Mock elasticsearch client and remove ignores
    private final String esHost = "localhost";
    private final String esPort = "9200";
    private final String esIndex = "logindex";
    private final String applicationName = "Application name - 0.0.0";
    private final String messageWrapper = "message_json";

    @Test
    @Ignore
    public void getTotalRequestsTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1), applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);
        final long totalRequests = esRepsortService.getTotalRequests();

        Assert.assertEquals(10, totalRequests);
        esRepsortService.close();

    }

    @Test
    @Ignore
    public void getTopDaysTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1), applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);
        final LinkedHashMap<ZonedDateTime, Integer> topDays = esRepsortService.getTopDays();

        Assert.assertEquals(2, topDays.size());

        esRepsortService.close();

    }

    @Test
    @Ignore
    public void getTopUsersTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1), applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);

        final LinkedHashMap<String, Long> topUsers = esRepsortService.getTopUsers();

        Assert.assertEquals(3, topUsers.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getTopFacetFieldsTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1), applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);

        final LinkedHashMap<String, Long> topFaceFields = esRepsortService.getTopFaceFields();

        Assert.assertEquals(2, topFaceFields.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getFacetFieldsValuesTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1), applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);

        final LinkedHashMap<String,LinkedHashMap<Object, Long>> topFaceFieldsValues = esRepsortService.getFacetFieldsValues(Arrays.asList("photoCategory","videoType","videoVersion"));

        Assert.assertEquals(1, topFaceFieldsValues.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getTopSuggestionFieldsTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1), applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);

        final LinkedHashMap<String, Long> topSuggestionFields = esRepsortService.getTopSuggestionFields();

        Assert.assertEquals(1, topSuggestionFields.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getTopQueriesTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(esHost, esPort, esIndex, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1), applicationName);
        esRepsortService.setMessageWrapper(messageWrapper);

        final LinkedHashMap<String, Long> topQueries = esRepsortService.getTopQueries();

        Assert.assertEquals(3, topQueries.size());

        esRepsortService.close();
    }
}
