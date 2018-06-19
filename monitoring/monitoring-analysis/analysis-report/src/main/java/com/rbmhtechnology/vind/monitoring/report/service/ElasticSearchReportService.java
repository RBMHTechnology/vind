/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.service;

import com.google.common.base.Joiner;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rbmhtechnology.vind.monitoring.report.Report;
import com.rbmhtechnology.vind.monitoring.report.configuration.ElasticSearchReportConfiguration;
import com.rbmhtechnology.vind.monitoring.report.preprocess.ElasticSearchReportPreprocessor;
import com.rbmhtechnology.vind.monitoring.report.preprocess.ReportPreprocessor;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClient;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClientBuilder;
import io.searchbox.client.JestResult;
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
    private final ElasticSearchReportPreprocessor preprocessor;
    private ElasticSearchClient elasticClient = new ElasticSearchClient();
    private final ElasticSearchReportConfiguration configuration;

    public ElasticSearchReportService(ElasticSearchReportConfiguration configuration, ZonedDateTime from, ZonedDateTime to) {
        super(configuration, from, to);
        elasticClient.init(
                configuration.getConnectionConfiguration().getEsHost(),
                configuration.getConnectionConfiguration().getEsPort(),
                configuration.getConnectionConfiguration().getEsIndex(),
                configuration.getEsEntryType()
        );
        this.configuration = configuration;
        this.preprocessor = new ElasticSearchReportPreprocessor(configuration, from, to);
    }

    @Override
    ReportPreprocessor getPreprocessor() {
        return this.preprocessor;
    }

    @Override
    public long getTotalRequests() {

        final String query = elasticClient.loadQueryFromFile("totalRequests",
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli());

        final SearchResult result = elasticClient.getQuery(query);
        return result.getTotal();
    }

    @Override
    public LinkedHashMap<ZonedDateTime, Long> getTopDays() {
        final String query = elasticClient.loadQueryFromFile("topDays",
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.configuration.getMessageWrapper());

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
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.configuration.getMessageWrapper());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("user").getBuckets();
        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        termEntries.stream().sorted(Comparator.comparingLong(TermsAggregation.Entry::getCount).reversed())
                .forEach(entry -> result.put(entry.getKey(), entry.getCount()));

        return result;
    }

    @Override
    public LinkedHashMap<String, JsonObject> getTopFacetFields() {
        final String query = elasticClient.loadQueryFromFile("topFacetFields",
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("facets").getBuckets();
        final List<String> facetFields = termEntries.stream().map(e -> e.getKey()).collect(Collectors.toList());
        
        return prepareScopeFilterResults(facetFields, "Facet");
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
                            .map( filter -> filterParser(filter))
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
    public LinkedHashMap<String, JsonObject> getTopSuggestionFields() {
        final String query = elasticClient.loadQueryFromFile("topSuggestionFields",
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.configuration.getMessageWrapper());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("suggestionFields").getBuckets();
        final List<String> suggestionFields = termEntries.stream().map(e -> e.getKey()).collect(Collectors.toList());

        return prepareScopeFilterResults(suggestionFields, "Suggest");
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
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.configuration.getMessageWrapper());

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
                this.configuration.getApplicationId(),
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
    public LinkedHashMap<String, JsonObject> getTopFilterFields() {
        final String query = elasticClient.loadQueryFromFile("topFilterFields",
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli(),
                this.configuration.getMessageWrapper());

        final SearchResult searchResult = elasticClient.getQuery(query);
        final List<TermsAggregation.Entry> termEntries = searchResult.getAggregations().getTermsAggregation("filterFields").getBuckets();
        final List<String> filterFields = termEntries.stream().map(e -> e.getKey()).collect(Collectors.toList());

        return prepareScopeFilterResults(filterFields, "Filter");
    }

    @Override
    public LinkedHashMap<String, LinkedHashMap<Object, Long>> getFilterFieldsValues(List<String> fields) {

        final List<JsonObject> descriptorFacetFilters = getDescriptorFilters(fields ,"Filter");

        final List<JsonObject> filterFilters = descriptorFacetFilters.stream()
                .map(e -> e.getAsJsonArray("Filter"))
                .flatMap(Streams::stream)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());

        final LinkedHashMap<String, LinkedHashMap<Object,Long>> result = new LinkedHashMap<>();

        fields.stream()
                .forEach( field -> {
                    final Map<String, Long> fieldFilters = filterFilters.stream()
                            .filter( filter -> filter.get("field").getAsString().equals(field))
                            .map(this::filterParser)
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
    public void close() throws Exception {
        this.elasticClient.destroy();
    }

    private List<JsonObject> getDescriptorFilters(List<String> fields, String scope) {

        final Long scrollSpan = 1000l;
        final String query = elasticClient.loadQueryFromFile("scopedProcessResultsForFields",
                scrollSpan, //page size
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                scope,
                this.configuration.getMessageWrapper(),
                "\"".concat(Joiner.on("\", \"").skipNulls().join(fields)).concat("\""),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.getFrom().toInstant().toEpochMilli(),
                this.getTo().toInstant().toEpochMilli()
        );


        final List<JsonObject> results = new ArrayList<>();

        String scrollId = null;
        Long start = 0l;
        Long totalResults = 1l;
        while (start < totalResults) {

            final JestResult scrollResult;
            if (Objects.nonNull(scrollId)) {
                scrollResult =  elasticClient.scrollResults(scrollId);
            } else {
                scrollResult = elasticClient.getScrollQuery(query);
                totalResults =scrollResult.getJsonObject().getAsJsonObject("hits").get("total").getAsLong();
            }

            scrollId =
                    scrollResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();

            final JsonArray scrollHits = scrollResult.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            results.addAll(Streams.stream(scrollHits)
                    .map(hit -> {
                        final JsonObject result = hit.getAsJsonObject()
                                .getAsJsonObject("_source")
                                .getAsJsonObject("message_json")
                                .getAsJsonObject("process");
                        result.addProperty("id", hit.getAsJsonObject().get("_id").getAsString());
                        return result;
                    })
                    .collect(Collectors.toList()));

            start += scrollSpan;
        }

        final List<JsonObject> finalQueries = results.stream()
                .map( r -> {
                    final JsonObject steps = r.getAsJsonObject("steps");
                    final JsonArray filters = steps.entrySet().stream()
                            .flatMap( e -> Streams.stream(e.getValue().getAsJsonArray().iterator()))
                            .map(JsonElement::getAsJsonObject)
                            .filter( f -> f.get("scope").getAsString().equals(scope))
                            .collect(JsonArray::new,
                                        JsonArray::add,
                                        JsonArray::addAll);
                    r.add(scope, filters);
                    return r;
                })
                .collect(Collectors.toList());

        return finalQueries;
    }

    private String filterParser(JsonObject filter) {
        final String type = filter.get("type").getAsString();
        //TODO:find a better way to do this
        switch(type) {
            case "AfterFilter":
                return ">" + filter.getAsJsonObject("date").get("timeStamp").getAsString();
            case "BeforeFilter":
                return "<" +filter.getAsJsonObject("date").get("timeStamp").getAsString();
            case "DescriptorFilter":
                return filter.get("term").getAsString();
            case "TermFilter":
                return filter.get("term").getAsString();
            case "NotEmptyTextFilter":
                return "\"NotEmptyText\"";
            case "NotEmptyFilter":
                return "\"NotEmpty\"";
            case "BetweenDatesFilter":
                return  filter.get("start").getAsString() + " TO " + filter.get("end").getAsString();
            case "BetweenNumericFilter":
                return  filter.get("start").getAsString() + " TO " + filter.get("end").getAsString();
            default:
                return "NA";
        }
    }

    private LinkedHashMap<String, JsonObject> prepareScopeFilterResults(List<String> fields, String scope) {

        final List<JsonObject> descriptorFilters = getDescriptorFilters(fields ,scope);

        final List<JsonObject> filters = descriptorFilters.stream()
                .map(e -> e.getAsJsonArray(scope))
                .flatMap(Streams::stream)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());


        final LinkedHashMap<String, Long> result = new LinkedHashMap<>();

        fields.stream()
                .forEach( field -> {
                    final long fieldCount = filters.stream()
                            .filter( filter -> filter.get("field").getAsString().equals(field))
                            .count();

                    result.put(field, fieldCount);
                });


        final LinkedHashMap<String, Long> resultsAsFirst = getFieldCountAs(1, descriptorFilters, fields);
        final LinkedHashMap<String, Long> resultsAsSecond = getFieldCountAs(2, descriptorFilters, fields);
        final LinkedHashMap<String, Long> resultsAsThird = getFieldCountAs(3, descriptorFilters, fields);
        final LinkedHashMap<String, Long> resultsAsFourth = getFieldCountAs(4, descriptorFilters, fields);
        
        //Sort the results
        final LinkedHashMap sortedResult = result.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(e ->{
                    final JsonObject facetResult = new JsonObject();
                    facetResult.addProperty("total",e.getValue());
                    facetResult.addProperty("first", resultsAsFirst.get(e.getKey()));
                    facetResult.addProperty("second", resultsAsSecond.get(e.getKey()));
                    facetResult.addProperty("third", resultsAsThird.get(e.getKey()));
                    facetResult.addProperty("fourth", resultsAsFourth.get(e.getKey()));
                    return new AbstractMap.SimpleEntry<>(e.getKey(), facetResult);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        return sortedResult;
    }

    private LinkedHashMap<String, Long> getFieldCountAs(int step, List<JsonObject> descriptorFilters, List<String> filterFields) {

        final String position = String.valueOf(step);
        final List<JsonObject> filterFiltersAs = descriptorFilters.stream()
                .filter(e -> e.getAsJsonObject("steps").has(position))
                .map(e -> e.getAsJsonObject("steps").getAsJsonArray(position))
                .flatMap(Streams::stream)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());

        final LinkedHashMap<String, Long> resultsAs = new LinkedHashMap<>();

        filterFields.stream()
                .forEach( field -> {
                    final long fieldCount = filterFiltersAs.stream()
                            .filter( filter -> filter.get("field").getAsString().equals(field))
                            .count();

                    resultsAs.put(field, fieldCount);
                });

        return resultsAs;
    }
}
