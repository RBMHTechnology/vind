/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.analysis.report;

import com.google.common.collect.Multiset;
import com.rbmhtechnology.vind.report.utils.ElasticSearchClient;
import com.rbmhtechnology.vind.report.utils.ElasticSearchClientBuilder;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

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
    public LinkedHashMap<Integer, ZonedDateTime> getTopDays() {
        final String query = this.loadQueryFromFile("topDays",
                this.getApplicationId(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final LinkedHashMap<Integer, ZonedDateTime> result = new LinkedHashMap<>();
        searchResult.getAggregations().getDateHistogramAggregation("days" )
                .getBuckets().stream().sorted()
                .forEach( dateHistogram -> result.put(dateHistogram.getCount().intValue(), ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateHistogram.getTime()), this.getZoneId())));
        return result;
    }

    @Override
    public LinkedHashMap<String, Long> getTopUsers() {
        final String query = this.loadQueryFromFile("topUsers",
                this.getApplicationId(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("person").getBuckets();
        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        termEntries.stream().sorted()
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
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount))
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public LinkedHashMap<String, List<Object>> getFaceFieldsValues(List<String> fields) {
        return null;
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
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount))
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public LinkedHashMap<String, List<Object>> getSuggestionFieldsValues(List<String> fields) {
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
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount))
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public LinkedHashMap<String, Long> getTopFilteredQueries() {
        return null;
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
