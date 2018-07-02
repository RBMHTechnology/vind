/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

import com.google.gson.JsonObject;
import com.rbmhtechnology.vind.monitoring.report.configuration.ElasticSearchConnectionConfiguration;
import com.rbmhtechnology.vind.monitoring.report.configuration.ElasticSearchReportConfiguration;
import com.rbmhtechnology.vind.monitoring.report.service.ElasticSearchReportService;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created on 01.03.18.
 */
public class ElasticSearchReportServiceTest {

    private final ElasticSearchReportConfiguration config = new ElasticSearchReportConfiguration()
            .setApplicationId("Application name - 0.0.0")
            .setEsEntryType("logEntry")
            .setMessageWrapper("")
            .setConnectionConfiguration(new ElasticSearchConnectionConfiguration(
                "localhost",
                "9200",
                "logindex"
            ));

    @Test
    @Ignore
    public void getTotalRequestsTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(config, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1));
        final long totalRequests = esRepsortService.getTotalRequests();

        Assert.assertEquals(10, totalRequests);
        esRepsortService.close();

    }

    @Test
    @Ignore
    public void getTopDaysTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(config, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1));
        final LinkedHashMap<ZonedDateTime, Long> topDays = esRepsortService.getTopDays();

        Assert.assertEquals(2, topDays.size());

        esRepsortService.close();

    }

    @Test
    @Ignore
    public void getTopUsersTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(config, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1));

        final LinkedHashMap<String, Long> topUsers = esRepsortService.getTopUsers();

        Assert.assertEquals(3, topUsers.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getTopFacetFieldsTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(config, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1));

        final List<String> topFaceFieldNames = esRepsortService.getTopFacetFields();

        final LinkedHashMap<String, JsonObject> topFaceFields = esRepsortService.prepareScopeFilterResults(topFaceFieldNames, "facet");

        Assert.assertEquals(2, topFaceFields.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getFacetFieldsValuesTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(config, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1));

        final LinkedHashMap<String,LinkedHashMap<Object, Long>> topFaceFieldsValues = esRepsortService.getFacetFieldsValues(Arrays.asList("photoCategory","videoType","videoVersion"));

        Assert.assertEquals(3, topFaceFieldsValues.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getTopSuggestionFieldsTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(config, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1));
        final LinkedHashMap<String, JsonObject> topSuggestionFields = esRepsortService.getTopSuggestionFields();

        Assert.assertEquals(1, topSuggestionFields.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getSuggestionFieldsValuesTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(config, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1));

        final LinkedHashMap<String,LinkedHashMap<Object, Long>> topSuggestionFieldsValues = esRepsortService.getSuggestionFieldsValues(
                Arrays.asList("activity",
                "anatomy",
                "leisuretime",
                "concept",
                "bodycaremedicine",
                "source",
                "title",
                "realInvestOrderNumber",
                "agriculture",
                "transport_vehicle",
                "society",
                "weather",
                "season",
                "event",
                "architecture",
                "sports",
                "nature",
                "work",
                "technology",
                "procedure",
                "economy",
                "animalsimple",
                "people",
                "productPlacement",
                "feast",
                "nutrition",
                "urban",
                "light",
                "various",
                "person",
                "infrastructure",
                "culture",
                "internalInvestOrderNumber",
                "fruitsvegetables",
                "content_model_structure",
                "topic",
                "animal",
                "style",
                "clothing",
                "scienceandresearch",
                "travel",
                "facility",
                "geolocation",
                "object",
                "fashion"));

        Assert.assertEquals(45, topSuggestionFieldsValues.size());

        esRepsortService.close();
    }

    @Test
    @Ignore
    public void getTopQueriesTest() throws Exception {
        final ElasticSearchReportService esRepsortService = new ElasticSearchReportService(config, ZonedDateTime.now().minusYears(1), ZonedDateTime.now().plusYears(1));

        final LinkedHashMap<String, Long> topQueries = esRepsortService.getTopQueries();

        Assert.assertEquals(3, topQueries.size());

        esRepsortService.close();
    }
}
