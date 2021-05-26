package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.SuggestionSearch;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.api.result.facet.TermFacetResult;
import com.rbmhtechnology.vind.elasticsearch.backend.ElasticSearchServer;
import com.rbmhtechnology.vind.elasticsearch.backend.client.ElasticVindClient;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Suggester {
    private static final String IGNORE_CASE_REGEX = "[%s|%s]";
    public static final String PREFIX_REGEX = "((.*[^A-Za-z0-9_])?%s.*)";

    private static final Collection<String> LUCENE_REGEX_ESCAPE_CHARS =
            Arrays.asList("-", ".", "*", "+", "&&", "||", "!", "(",  ")",
                    "{", "}", "[", "]", "^" ,"\"", "~", "*", "?", ":", "\\", "/");


    private static final Logger log = LoggerFactory.getLogger(ElasticSearchServer.class);
    private static final Logger elasticClientLogger = LoggerFactory.getLogger(log.getName() + "#client");

    private final ElasticVindClient client;
    private final DocumentFactory factory;
    private String context;
    private Filter filter;
    private ExecutableSuggestionSearch search;

    private Suggester(ElasticVindClient client, DocumentFactory factory) {
        this.client = client;
        this.factory = factory;
    }
    private void setSearch(ExecutableSuggestionSearch suggestionSearch) {
        this.search = suggestionSearch;
        this.filter = suggestionSearch.getFilter();
        this.context = suggestionSearch.getSearchContext();
    }

    public SuggestionResult getExperimentalSuggestions( List<String> indexFootPrint) {
        final StopWatch elapsedtime = StopWatch.createStarted();
        final SearchSourceBuilder query =
                ElasticQueryBuilder.buildExperimentalSuggestionQuery(search, factory, indexFootPrint);
        //query
        try {
            elasticClientLogger.debug(">>> query({})", query.toString());
            final SearchResponse response = client.query(query);
            final HashMap<FieldDescriptor, TermFacetResult<?>> suggestionValues =
                    ResultUtils.buildSuggestionResults(response, factory, context);

            final SearchSourceBuilder suggestionFacetQuery = new SearchSourceBuilder();
            final BoolQueryBuilder filterSuggestions = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchAllQuery())
                    .filter(ElasticQueryBuilder.buildFilterQuery(filter, factory, context, indexFootPrint));
            suggestionFacetQuery.query(filterSuggestions);

            suggestionValues.entrySet().stream()
                    .filter(e -> FieldUtil.getFieldName(e.getKey(), context, indexFootPrint).isPresent())
                    .map(e -> Pair.of(
                            e.getKey().getName(),
                            e.getValue().getValues().stream()
                                    .map(value -> {
                                        final Optional<String> fieldName =
                                                FieldUtil.getFieldName(e.getKey(), context, indexFootPrint);
                                        return new FiltersAggregator.KeyedFilter(
                                                value.getValue().toString(),
                                                QueryBuilders.termQuery(fieldName.get(), value.getValue()));
                                    })
                                    .toArray(FiltersAggregator.KeyedFilter[]::new)
                            )
                    )
                    .forEach(fieldAggs ->
                            suggestionFacetQuery.aggregation(
                                    AggregationBuilders.filters(fieldAggs.getKey(), fieldAggs.getValue())
                            )
                    );

            elapsedtime.stop();

            final SuggestionResult result = new SuggestionResult(suggestionValues, null, response.getTook().getMillis(), factory);
            result.setElapsedTime(elapsedtime.getTime(TimeUnit.MILLISECONDS));
            return result;

        } catch (ElasticsearchException | IOException e) {
            throw new SearchServerException(String.format("Cannot issue query: %s", e.getMessage()), e);
        }
    }

    protected static String getSuggestionRegex(String input, SuggestionSearch.SuggestionOperator op) {

        String escapedInput = Suggester.unescapeQuery(input);
        if (escapedInput.contains("<")) {
            escapedInput = escapedInput.replaceAll("<", "\\\\<");
        }

        //remove *
        if(escapedInput.endsWith("*")) {
            escapedInput = escapedInput.substring(0, escapedInput.length() - 1);
        }

        //Split the query into terms separated by spaces
        List<String> terms = Arrays.asList(escapedInput.trim().split(" |\\+"));


        //Get the REGEX expression for each term to make them match as prefix in any word of a field.
        final List<String> queryPreffixes = terms.stream()
                .map(term -> term.chars()
                        .mapToObj(i -> (char)i)
                        .map(letter -> {
                            if(Character.isAlphabetic(letter)) {
                                return  String.format(IGNORE_CASE_REGEX,
                                        StringUtils.lowerCase(letter.toString()),
                                        StringUtils.upperCase(letter.toString()));
                            } else {
                                return escapeCharacter(letter.toString());
                            }
                        })
                        .collect(Collectors.joining()))
                .map(prefix -> String.format(Suggester.PREFIX_REGEX, prefix))
                .collect(Collectors.toList());

        switch (op) {
            case OR:
                return queryPreffixes.stream().collect(Collectors.joining("|"));
            default:
            case AND:
                return queryPreffixes.stream().collect(Collectors.joining("&"));
        }
    }

    public static String unescapeQuery(String query) {
        final StringBuilder sb = new StringBuilder();

        int i = 0;
        while (i < query.length()) {
            char c = query.charAt(i);
            if (c == '\\' && i < query.length() - 1) {
                if (LUCENE_REGEX_ESCAPE_CHARS.contains(String.valueOf(query.charAt(i + 1)))) {
                    sb.append(query.charAt(i + 1));
                    ++i;
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
            ++i;
        }

        return sb.toString();
    }

    private static String escapeCharacter( String letter) {
       if (LUCENE_REGEX_ESCAPE_CHARS.contains(letter) ){
           return "\\" + letter;
       }
        return letter;
    }

    public SuggestionResult getSuggestions( List<String> indexFootPrint) {
        final StopWatch elapsedtime = StopWatch.createStarted();
        final SearchSourceBuilder query =
                ElasticQueryBuilder.buildExperimentalSuggestionQuery(search, factory, indexFootPrint);
        //query
        try {
            elasticClientLogger.debug(">>> query({})", query.toString());
            final SearchResponse response = client.query(query);
            final HashMap<FieldDescriptor, TermFacetResult<?>> suggestionValues =
                    ResultUtils.buildSuggestionResults(response, factory, context);

            final SearchSourceBuilder suggestionFacetQuery = new SearchSourceBuilder();
            final BoolQueryBuilder filterSuggestions = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchAllQuery())
                    .filter(ElasticQueryBuilder.buildFilterQuery(filter, factory, context,indexFootPrint));
            suggestionFacetQuery.query(filterSuggestions);

            suggestionValues.entrySet().stream()
                    .filter(e -> FieldUtil.getFieldName(e.getKey(), context,indexFootPrint).isPresent())
                    .map(e -> Pair.of(
                            e.getKey().getName(),
                            e.getValue().getValues().stream()
                                    .map(value -> {
                                        final Optional<String> fieldName =
                                                FieldUtil.getFieldName(e.getKey(), context, indexFootPrint);
                                        return new FiltersAggregator.KeyedFilter(
                                                value.getValue().toString(),
                                                QueryBuilders.termQuery(fieldName.get(), value.getValue()));
                                    })
                                    .toArray(FiltersAggregator.KeyedFilter[]::new)
                            )
                    )
                    .forEach( fieldAggs ->
                            suggestionFacetQuery.aggregation(
                                    AggregationBuilders.filters(fieldAggs.getKey(),fieldAggs.getValue())
                            )
                    );

            elapsedtime.stop();

            final SuggestionResult result = new SuggestionResult(suggestionValues, null, response.getTook().getMillis(),factory);
            result.setElapsedTime(elapsedtime.getTime(TimeUnit.MILLISECONDS));
            return result;

        } catch (ElasticsearchException | IOException e) {
            throw new SearchServerException(String.format("Cannot issue query: %s",e.getMessage()), e);
        }
    }


    public static class SuggesterBuilder {
        private final Suggester suggester;

        public SuggesterBuilder(ElasticVindClient client, DocumentFactory factory) {
            this.suggester = new Suggester(client, factory);
        }


        public Suggester build(ExecutableSuggestionSearch suggestionSearch) {
            suggester.setSearch(suggestionSearch);
            return suggester;
        }
    }
}
