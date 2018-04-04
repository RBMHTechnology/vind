/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

import com.google.common.base.Joiner;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClient;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClientBuilder;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created on 28.02.18.
 */
public class ElasticSearchReportService extends ReportService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchClientBuilder.class);
    private ElasticSearchClient elasticClient = new ElasticSearchClient();


    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, ZonedDateTime from, ZonedDateTime to, String applicationId) {
        super(from, to, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex,Date from, Date to, String timeZoneID, String applicationId) {
        super(from, to, timeZoneID, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex,long from, long to, String timeZoneId, String applicationId) {
        super(from, to, timeZoneId, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex);
    }



    @Override
    public long getTotalRequests() {

        final String query = this.loadQueryFromFile("totalRequests",
                this.getApplicationId(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult result = elasticClient.getQuery(query);
        return result.getTotal();
    }

    @Override
    public LinkedHashMap<ZonedDateTime, Integer> getTopDays() {
        final String query = this.loadQueryFromFile("topDays",
                this.getApplicationId(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final LinkedHashMap<ZonedDateTime, Integer> result = new LinkedHashMap<>();
        searchResult.getAggregations().getDateHistogramAggregation("days" )
                .getBuckets().stream().sorted(Comparator.comparingLong(DateHistogramAggregation.DateHistogram::getCount).reversed())
                .forEach( dateHistogram ->
                        result.put(ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateHistogram.getTime()), this.getZoneId()), dateHistogram.getCount().intValue()));
        return result;
    }

    @Override
    public LinkedHashMap<String, Long> getTopUsers() {
        final String query = this.loadQueryFromFile("topUsers",
                this.getApplicationId(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("user").getBuckets();
        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount).reversed())
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public LinkedHashMap<String, Long> getTopFaceFields() {
        final String query = this.loadQueryFromFile("topFacetFields",
                this.getApplicationId(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("fields").getBuckets();
        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount).reversed())
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public LinkedHashMap<String, LinkedHashMap<Object,Long>> getFaceFieldsValues(List<String> fields) {

        final String query = this.loadQueryFromFile("topFacetFieldsValues",
                this.getApplicationId(),
                "\"".concat(Joiner.on("\", \"").skipNulls().join(fields)).concat("\""),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("fields").getBuckets();
        final LinkedHashMap<String, LinkedHashMap<Object,Long>> result = new LinkedHashMap<>();
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount).reversed())
                .forEach(entry -> {
                    LinkedHashMap<Object,Long> valuesMap = new LinkedHashMap<>();
                    entry.getTermsAggregation("values").getBuckets().stream()
                        .sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount).reversed())
                        .forEach(valueEntry -> valuesMap.put(valueEntry.getKey(),valueEntry.getCount()));

                    result.put(entry.getKey(),valuesMap);
                    }
                );
        return result;
    }

    @Override
    public LinkedHashMap<String, Long> getTopSuggestionFields() {
        final String query = this.loadQueryFromFile("topSuggestionFields",
                this.getApplicationId(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("suggestionFields").getBuckets();
        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount).reversed())
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public LinkedHashMap<String, List<LinkedHashMap<String,Long>>> getSuggestionFieldsValues(List<String> fields) {
        return null;
    }

    @Override
    public LinkedHashMap<String, Long> getTopQueries() {
        final String query = this.loadQueryFromFile("topQueries",
                this.getApplicationId(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("queries").getBuckets();
        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount).reversed())
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public LinkedHashMap<String, Long> getTopFilteredQueries(String regexFilter) {
        final String query = this.loadQueryFromFile("topFilteredQueries",
                this.getApplicationId(),
                regexFilter,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("queries").getBuckets();
        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount).reversed())
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public void close() throws Exception {
        this.elasticClient.destroy();
    }

    private String loadQueryFromFile(String fileName, Object ... args) {
        final Path path = Paths.get(ElasticSearchClient.class.getClassLoader().getResource("queries/" + fileName).getPath());
        try {
            final byte[] encoded = Files.readAllBytes(path);
            final String query = new String(encoded, "UTF-8");
            return String.format(query, args);

        } catch (IOException e) {
            log.error("Error preparing query from file '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Error preparing query from file '" + path + "': " + e.getMessage(), e);
        }

    }
}
