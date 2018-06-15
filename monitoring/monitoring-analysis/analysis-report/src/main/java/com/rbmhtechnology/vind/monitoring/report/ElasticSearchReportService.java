/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

import com.google.common.base.Joiner;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rbmhtechnology.vind.monitoring.report.preprocess.ReportPreprocessor;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClient;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClientBuilder;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final ReportPreprocessor preprocessor;
    private ElasticSearchClient elasticClient = new ElasticSearchClient();
    private final String messageWrapper;


    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType, ZonedDateTime from, ZonedDateTime to, String applicationId) {
        super(from, to, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
        messageWrapper = "";
        this.preprocessor = new ReportPreprocessor(elasticHost, elasticPort, elasticIndex, this.getFrom(),this.getTo(),applicationId,this.messageWrapper, logType);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType, ZonedDateTime from, ZonedDateTime to, String applicationId, String messageWrapper) {
        super(from, to, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
        if(StringUtils.isNotBlank(messageWrapper) && !messageWrapper.endsWith(".")) {
            this.messageWrapper = messageWrapper + ".";
        } else if(StringUtils.isNotBlank(messageWrapper)) {
            this.messageWrapper = messageWrapper;
        } else {
            this.messageWrapper = "";
        }
        this.preprocessor = new ReportPreprocessor(elasticHost, elasticPort, elasticIndex, this.getFrom(),this.getTo(),applicationId,this.messageWrapper, logType);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType,Date from, Date to, String timeZoneID, String applicationId) {
        super(from, to, timeZoneID, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
        messageWrapper = "";
        this.preprocessor = new ReportPreprocessor(elasticHost, elasticPort, elasticIndex, this.getFrom(),this.getTo(),applicationId,this.messageWrapper, logType);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType,Date from, Date to, String timeZoneID, String applicationId, String messageWrapper) {
        super(from, to, timeZoneID, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
        if(StringUtils.isNotBlank(messageWrapper) && !messageWrapper.endsWith(".")) {
            this.messageWrapper = messageWrapper + ".";
        } else if(StringUtils.isNotBlank(messageWrapper)) {
            this.messageWrapper = messageWrapper;
        } else {
            this.messageWrapper = "";
        }
        this.preprocessor = new ReportPreprocessor(elasticHost, elasticPort, elasticIndex, this.getFrom(),this.getTo(),applicationId,this.messageWrapper, logType);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType, long from, long to, String timeZoneId, String applicationId) {
        super(from, to, timeZoneId, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
        messageWrapper = "";
        this.preprocessor = new ReportPreprocessor(elasticHost, elasticPort, elasticIndex, this.getFrom(),this.getTo(),applicationId,this.messageWrapper, logType);
    }

    public ElasticSearchReportService(String elasticHost, String elasticPort, String elasticIndex, String logType, long from, long to, String timeZoneId, String applicationId, String messageWrapper) {
        super(from, to, timeZoneId, applicationId);
        elasticClient.init(elasticHost, elasticPort, elasticIndex, logType);
        if(StringUtils.isNotBlank(messageWrapper) && !messageWrapper.endsWith(".")) {
            this.messageWrapper = messageWrapper + ".";
        } else if(StringUtils.isNotBlank(messageWrapper)) {
            this.messageWrapper = messageWrapper;
        } else {
            this.messageWrapper = "";
        }

        this.preprocessor = new ReportPreprocessor(elasticHost, elasticPort, elasticIndex, this.getFrom(),this.getTo(),applicationId,this.messageWrapper, logType);
    }


    public void preprocessData(String ... systemFilterFields) {
        if (Objects.nonNull(systemFilterFields)) {
            preprocessor.addSystemFilterField(systemFilterFields);
        }
        preprocessor.preprocess();
    }

    @Override
    public long getTotalRequests() {

        final String query = elasticClient.loadQueryFromFile("totalRequests",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.messageWrapper,
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult result = elasticClient.getQuery(query);
        return result.getTotal();
    }

    @Override
    public LinkedHashMap<ZonedDateTime, Long> getTopDays() {
        final String query = elasticClient.loadQueryFromFile("topDays",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.messageWrapper,
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
        final String query = elasticClient.loadQueryFromFile("topUsers",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.messageWrapper,
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
        final String query = elasticClient.loadQueryFromFile("topFacetFields",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.messageWrapper,
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.messageWrapper,
                this.messageWrapper);

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("facets").getBuckets();
        final List<String> facetFields = termEntries.stream().map(e -> e.getKey()).collect(Collectors.toList());

        final List<JsonObject> descriptorFacetFilters = getDescriptorFilters(facetFields ,"Facet");

        final List<JsonObject> facetFilters = descriptorFacetFilters.stream()
                .map(e -> e.getAsJsonArray("Facet"))
                .flatMap(Streams::stream)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());

        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();

        facetFields.stream()
                .forEach( field -> {
                    final long fieldCount = facetFilters.stream()
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

        final List<JsonObject> facetFilters = descriptorFacetFilters.stream()
                .map(e -> e.getAsJsonArray("Facet"))
                .flatMap(Streams::stream)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());

        fields.stream()
                .forEach( field -> {
                    final Map<String, Long> fieldFilters = facetFilters.stream()
                            .filter( filter -> filter.get("field").getAsString().equals(field))
                            //TODO: fix this hack or find a better way to do it
                            .map( filter -> {
                                if (filter.has("term")){
                                    return filter.get("term").getAsString();
                                }
                                if (filter.has("start") && filter.has("end")){
                                    return filter.get("start").getAsString() + " - " + filter.get("end").getAsString();
                                } else return "";
                            })
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
        final String query = elasticClient.loadQueryFromFile("topSuggestionFields",
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

        final List<JsonObject> suggestFilters = descriptorFacetFilters.stream()
                .map(e -> e.getAsJsonArray("Suggest"))
                .flatMap(Streams::stream)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());

        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();

        suggestionFields.stream()
                .forEach( field -> {
                    final long fieldCount = suggestFilters.stream()
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

        final List<JsonObject> suggestFilters = descriptorFacetFilters.stream()
                .map(e -> e.getAsJsonArray("Suggest"))
                .flatMap(Streams::stream)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());

        final LinkedHashMap<String, LinkedHashMap<Object,Long>> result = new LinkedHashMap<>();

        fields.stream()
                .forEach( field -> {
                    final Map<String, Long> fieldFilters = suggestFilters.stream()
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
        final String query = elasticClient.loadQueryFromFile("topQueries",
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                this.messageWrapper,
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
        final String query = elasticClient.loadQueryFromFile("topFilteredQueries",
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

    private List<JsonObject> getDescriptorFilters(List<String> fields, String scope) {
        int from = 0;
        int resultSize = 0;

        final String query = elasticClient.loadQueryFromFile("topFacetFieldsValues",
                resultSize, //page size
                from,
                this.messageWrapper,
                this.messageWrapper,
                this.getApplicationId(),
                this.messageWrapper,
                scope,
                this.messageWrapper,
                "\"".concat(Joiner.on("\", \"").skipNulls().join(fields)).concat("\""),
                this.messageWrapper,
                this.messageWrapper,
                this.messageWrapper,
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli()
        );

        final SearchResult matchingResults = elasticClient.getQuery(query);
        final Long totalResults = matchingResults.getTotal();
        final List<JsonObject> results = new ArrayList<>();

        resultSize = 100;
        while (from < totalResults) {
            final String q = elasticClient.loadQueryFromFile("topFacetFieldsValues",
                    resultSize, //page size
                    from,
                    this.messageWrapper,
                    this.messageWrapper,
                    this.getApplicationId(),
                    this.messageWrapper,
                    scope,
                    this.messageWrapper,
                    "\"".concat(Joiner.on("\", \"").skipNulls().join(fields)).concat("\""),
                    this.messageWrapper,
                    this.messageWrapper,
                    this.messageWrapper,
                    this.getFrom().toInstant().toEpochMilli(),
                    this.getTo().toInstant().toEpochMilli()
            );

            final SearchResult searchResult = elasticClient.getQuery(q);

            results.addAll(searchResult.getHits(JsonElement.class).stream()
                    .map(es -> {
                        final JsonObject result = es.source.getAsJsonObject()
                                 .getAsJsonObject("message_json")
                                 .getAsJsonObject("process");
                        result.addProperty("id", es.id);
                        return result;
                    })
                    .collect(Collectors.toList()));


            from += resultSize;
        }


        final Map<String, List<JsonObject>> resultsPerId =
                results.stream()
                        .collect(Collectors.groupingBy(r -> r.get("id").getAsString()));

        final List<JsonObject> finalQueries = resultsPerId.entrySet().stream()
                .map(e -> {
                    if (e.getValue().size() > 1) {
                        return e.getValue().stream().max(Comparator.comparingLong( l -> l.get("step").getAsLong())).get();

                    } else {
                        return e.getValue().get(0);
                    }
                })
                .map( r -> {
                    JsonArray filters = Streams.stream(r.getAsJsonArray("filters").iterator())
                            .filter(f -> f.getAsJsonObject().get("scope").getAsString().equals(scope))
                            .collect(JsonArray::new,
                                        JsonArray::add,
                                        JsonArray::addAll);
                    r.add(scope, filters);
                    return r;
                })
                .collect(Collectors.toList());

        return finalQueries;
    }
}
