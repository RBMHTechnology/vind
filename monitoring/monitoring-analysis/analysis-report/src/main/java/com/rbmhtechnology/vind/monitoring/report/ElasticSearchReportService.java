/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

import com.google.common.base.Joiner;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created on 28.02.18.
 */
public class ElasticSearchReportService extends ReportService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchClientBuilder.class);
    private ElasticSearchClient elasticClient = new ElasticSearchClient();
    private String messageWrapper = "";

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, ZonedDateTime from, ZonedDateTime to, String applicationId) {
        super(from, to, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType, ZonedDateTime from, ZonedDateTime to, String applicationId) {
        super(from, to, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex,Date from, Date to, String timeZoneID, String applicationId) {
        super(from, to, timeZoneID, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType,Date from, Date to, String timeZoneID, String applicationId) {
        super(from, to, timeZoneID, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex,long from, long to, String timeZoneId, String applicationId) {
        super(from, to, timeZoneId, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType, long from, long to, String timeZoneId, String applicationId) {
        super(from, to, timeZoneId, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
    }


    public ElasticSearchReportService setMessageWrapper(String wrapper) {
        this.messageWrapper = wrapper + ".";
        return this;
    }

    @Override
    public long getTotalRequests() {

        final String query = this.loadQueryFromFile("totalRequests",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult result = elasticClient.getQuery(query);
        return result.getTotal();
    }

    @Override
    public LinkedHashMap<ZonedDateTime, Long> getTopDays() {
        final String query = this.loadQueryFromFile("topDays",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.messageWrapper);

        final SearchResult searchResult = elasticClient.getQuery(query);
        final LinkedHashMap<ZonedDateTime, Long> result = new LinkedHashMap<>();
        searchResult.getAggregations().getDateHistogramAggregation("days" )
                .getBuckets().stream().sorted(Comparator.comparingLong(DateHistogramAggregation.DateHistogram::getCount).reversed())
                .forEach( dateHistogram ->
                        result.put(ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateHistogram.getTime()), this.getZoneId()), dateHistogram.getCount().longValue()));
        return result;
    }

    @Override
    public LinkedHashMap<String, Long> getTopUsers() {
        final String query = this.loadQueryFromFile("topUsers",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.messageWrapper);

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
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.messageWrapper,
                this.messageWrapper);

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("facets").getBuckets();
        final List<String> facetFields = termEntries.stream().map(e -> e.getKey()).collect(Collectors.toList());

        final List<JsonObject> descriptorFacetFilters = getDescriptorFilters(facetFields ,"Facet");

        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();

        facetFields.stream()
                .forEach( field -> {
                    final long fieldCount = descriptorFacetFilters.stream()
                            .filter( filter -> filter.get("field").getAsString().equals(field))
                            .count();

                    result.put(field, fieldCount);
                });


        //Sort the results
        final LinkedHashMap sortedResult = result.entrySet().stream()
                .filter( e -> e.getValue() > 0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        return sortedResult;
    }

    @Override
    public LinkedHashMap<String, LinkedHashMap<Object,Long>> getFacetFieldsValues(List<String> fields) {

        final List<JsonObject> descriptorFacetFilters = getDescriptorFilters(fields,"Facet");
        final LinkedHashMap<String, LinkedHashMap<Object,Long>> result = new LinkedHashMap<>();

        fields.stream()
                .forEach( field -> {
                    final Map<String, Long> fieldFilters = descriptorFacetFilters.stream()
                            .filter( filter -> filter.get("field").getAsString().equals(field))
                            .map( filter -> filter.get("term").getAsString())
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                    //Sort the results
                    final LinkedHashMap values = fieldFilters.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                    (oldValue, newValue) -> oldValue, LinkedHashMap::new));

                    result.put(field, values);
                });
        return result;
    }


    @Override
    public LinkedHashMap<String, Long> getTopSuggestionFields() {
        final String query = this.loadQueryFromFile("topSuggestionFields",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.messageWrapper);

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("suggestionFields").getBuckets();
        final List<String> suggestionFields = termEntries.stream().map(e -> e.getKey()).collect(Collectors.toList());

        final List<JsonObject> descriptorFacetFilters = getDescriptorFilters(suggestionFields ,"Suggest");

        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();

        suggestionFields.stream()
                .forEach( field -> {
                    final long fieldCount = descriptorFacetFilters.stream()
                            .filter( filter -> filter.get("field").getAsString().equals(field))
                            .count();

                    result.put(field, fieldCount);
                });


        //Sort the results
        final LinkedHashMap sortedResult = result.entrySet().stream()
                .filter( e -> e.getValue() > 0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        return sortedResult;
    }

    @Override
    public LinkedHashMap<String, LinkedHashMap<Object, Long>> getSuggestionFieldsValues(List<String> fields) {

        final List<JsonObject> descriptorFacetFilters = getDescriptorFilters(fields ,"Suggest");

        final LinkedHashMap<String, LinkedHashMap<Object,Long>> result = new LinkedHashMap<>();

        fields.stream()
                .forEach( field -> {
                    final Map<String, Long> fieldFilters = descriptorFacetFilters.stream()
                            .filter( filter -> filter.get("field").getAsString().equals(field))
                            .map( filter -> filter.get("term").getAsString())
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

                    //Sort the results
                    final LinkedHashMap values = fieldFilters.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                    (oldValue, newValue) -> oldValue, LinkedHashMap::new));

                    result.put(field, values);
                });
        return result;
    }

    @Override
    public LinkedHashMap<String, Long> getTopQueries() {
        final String query = this.loadQueryFromFile("topQueries",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.messageWrapper);

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

    private List<JsonObject> getDescriptorFilters(List<String> fields, String scope) {
        int from = 0;
        int resultSize = 0;

        final String query = this.loadQueryFromFile("topFacetFieldsValues",
                resultSize, //page size
                from,
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                scope,
                this.messageWrapper,
                this.messageWrapper,
                "\"".concat(Joiner.on("\", \"").skipNulls().join(fields)).concat("\""),
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli()
        );

        final SearchResult matchingResults = elasticClient.getQuery(query);
        final Long totalResults = matchingResults.getTotal();
        final List<JsonObject> results = new ArrayList<>();

        resultSize = 100;
        while (from < totalResults) {
            final String q = this.loadQueryFromFile("topFacetFieldsValues",
                    resultSize, //page size
                    from,
                    this.messageWrapper,
                    this.messageWrapper,
                    this.getApplicationId(),
                    this.messageWrapper,
                    scope,
                    this.messageWrapper,
                    this.messageWrapper,
                    "\"".concat(Joiner.on("\", \"").skipNulls().join(fields)).concat("\""),
                    this.messageWrapper,
                    this.getFrom().toInstant().toEpochMilli(),
                    this.getTo().toInstant().toEpochMilli()
            );

            final SearchResult searchResult = elasticClient.getQuery(q);

            results.addAll(searchResult.getHits(JsonElement.class).stream()
                    .map(es -> es.source.getAsJsonObject()
                            .getAsJsonObject("message_json")
                            .getAsJsonObject("request")
                            .getAsJsonObject("filter"))
                    .collect(Collectors.toList()));


            from += resultSize;
        }


        final List<JsonArray> filters = results.stream()
                .map(e -> e.getAsJsonArray("delegates"))
                .collect(Collectors.toList());

        return filters.stream()
                .map( f -> Streams.stream(f.iterator()).collect(Collectors.toList()))
                .flatMap( Collection::stream)
                .map( je -> je.getAsJsonObject())
                .filter( f -> f.get("scope").getAsString().equals(scope))
                .filter( f -> f.get("type").getAsString().equals("DescriptorFilter"))
                .collect(Collectors.toList());
    }
}
