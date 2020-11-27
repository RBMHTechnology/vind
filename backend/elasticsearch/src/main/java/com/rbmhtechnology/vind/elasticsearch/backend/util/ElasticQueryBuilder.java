package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.api.query.facet.Interval.NumericInterval;
import com.rbmhtechnology.vind.api.query.facet.TermFacetOption;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.query.suggestion.DescriptorSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.StringSuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.UpdateOperation;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rbmhtechnology.vind.model.FieldDescriptor.*;

public class ElasticQueryBuilder {
    private static final Logger log = LoggerFactory.getLogger(ElasticQueryBuilder.class);

    private final static Map<String, String> reservedChars = new HashMap<String, String>() {{
        put("\\+","\\\\+");
        put("\\-","\\\\-");
        put("=","\\\\=");
        put("&","\\\\&");
        put("\\|","\\\\|");
        put("!","\\\\!");
        put("\\(","\\\\(");
        put("\\)","\\\\)");
        put("\\{","\\\\{");
        put("\\}","\\\\}");
        put("\\[","\\\\[");
        put("\\]","\\\\]");
        put("\\^","\\\\^");
        put("\"","\\\\\"");
        put("~","\\\\~");
        put("\\*","\\\\*");
        put("\\?","\\\\?");
        put(":","\\\\:");
        put("\\\\","\\\\\\");
        put("\\/","\\\\/");
        put("<","");
        put(">","");
    }};

    public static SearchSourceBuilder buildQuery(FulltextSearch search, DocumentFactory factory) {
        return buildQuery(search,factory,false);
    }

    public static SearchSourceBuilder buildQuery(FulltextSearch search, DocumentFactory factory, boolean escape) {


        final String searchContext = search.getSearchContext();
        final SearchSourceBuilder searchSource = new SearchSourceBuilder();
        final BoolQueryBuilder baseQuery = QueryBuilders.boolQuery();

        // Set total hits count
        final boolean trackTotalHits = Boolean.parseBoolean(
                SearchConfiguration.get(SearchConfiguration.TRACK_TOTAL_HITS, "true"));
        searchSource.trackTotalHits(trackTotalHits);

        //build full text disMax query
        String searchString = "*".equals(search.getSearchString())
                || Strings.isEmpty(search.getSearchString().trim())? "*:*" : search.getSearchString();


        if (escape){
            //Escape especial characters: + - = && || > < ! ( ) { } [ ] ^ " ~ * ? : \ /
            for(Map.Entry<String,String> wordEntry: reservedChars.entrySet()) {
                searchString = searchString.replaceAll(wordEntry.getKey(), wordEntry.getValue());
            }
        }


        String minimumShouldMatch = search.getMinimumShouldMatch();
        if(StringUtils.isNumeric(minimumShouldMatch) && !minimumShouldMatch.startsWith("-")) {
            minimumShouldMatch = "0<" + minimumShouldMatch;
        }
        final QueryStringQueryBuilder fullTextStringQuery = QueryBuilders.queryStringQuery(searchString)
                .minimumShouldMatch(minimumShouldMatch); //mm
        // Set fulltext fields
        factory.getFields().values().stream()
                .filter(FieldDescriptor::isFullText)
                .forEach(field -> fullTextStringQuery
                        .field(FieldUtil.getFieldName(field, UseCase.Fulltext, searchContext), field.getBoost()));


        final DisMaxQueryBuilder query = QueryBuilders.disMaxQuery()
                .add(fullTextStringQuery);

        baseQuery.must(query);
        searchSource.query(baseQuery);

//        if(search.getTimeZone() != null) {
//            query.set(CommonParams.TZ,search.getTimeZone());
//        }

        if (search.isSpellcheck()) {
            final SuggestBuilder suggestBuilder = new SuggestBuilder();
            suggestBuilder.setGlobalText(search.getSearchString());
            Lists.newArrayList(getFullTextFieldNames(search,factory,searchContext))
                    .forEach(fieldName -> suggestBuilder
                            .addSuggestion(
                                    FieldUtil.getSourceFieldName(fieldName.replaceAll(".text", ""), searchContext),
                                    SuggestBuilders.termSuggestion(fieldName).prefixLength(0)));

            searchSource.suggest(suggestBuilder);
        }

        searchSource.trackScores(SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_SHOW_SCORE, true));
//        if(SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_SHOW_SCORE, true)) {
//            query.set(CommonParams.FL, "*,score");
//        } else {
//            query.set(CommonParams.FL, "*");
//        }


        if(search.getGeoDistance() != null) {
            final FieldDescriptor distanceField = factory.getField(search.getGeoDistance().getFieldName());
            if (Objects.nonNull(distanceField)) {
                searchSource.scriptField(
                        FieldUtil.DISTANCE,
                        new Script(
                                ScriptType.INLINE,
                                "painless",
                                String.format(
                                        Locale.ENGLISH,
                                        "if(doc['%s'].size()!=0)" +
                                                "doc['%s'].arcDistance(%f,%f);" +
                                            "else []",
                                        FieldUtil.getFieldName(distanceField, searchContext),
                                        FieldUtil.getFieldName(distanceField, searchContext),
                                        search.getGeoDistance().getLocation().getLat(),
                                        search.getGeoDistance().getLocation().getLng()
                                ),
                                Collections.emptyMap()
                        )
                );
            }
        }
        searchSource.fetchSource(true);
        baseQuery.filter(buildFilterQuery(search.getFilter(), factory, searchContext));

        if(search.hasFacet() && search.getFacetLimit() !=0) {
            final int facetLimit = search.getFacetLimit() < 0 ? Integer.MAX_VALUE : search.getFacetLimit();
            if( search.getFacetLimit() < 0) {
                log.info("Facet limit has been set to {}: " +
                                "Elastic search does not support unlimited facet results, setting it to Integer.MAX_VALUE ({})",
                        search.getFacetLimit(),
                        facetLimit);
            }
            search.getFacets().entrySet().stream()
                    .map(vindFacet -> {
                        return buildElasticAggregations(
                                            vindFacet.getKey(),
                                            vindFacet.getValue(),
                                            factory,
                                            UseCase.Facet,//TODO : complex fields can have aggregation on non facet?
                                            searchContext,
                                            search.getFacetMinCount() ,
                                facetLimit);
                    })
                    .flatMap(Collection::stream)
                    .filter(Objects::nonNull)
                    .forEach(searchSource::aggregation);
        }

        search.getFacets().values().stream()
                .filter( facet -> facet.getType().equals("PivotFacet"))
                .map( pivotFacet -> searchSource.aggregations().getAggregatorFactories().stream()
                        .filter( agg -> agg.getName().equals(Stream.of(searchContext, pivotFacet.getFacetName())
                                .filter(Objects::nonNull)
                                .collect(Collectors.joining("_"))))
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .forEach( pivotAgg -> {
                    final List<String> facets = search.getFacets().values().stream()
                            .filter(facet ->
                                    Arrays.asList(facet.getTagedPivots())
                                            .contains(pivotAgg.getName().replaceAll(searchContext + "_", "")))
                            .map(facet -> Stream.of(searchContext, facet.getFacetName())
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.joining("_")))
                            .collect(Collectors.toList());
                    final List<AggregationBuilder> aggs = searchSource.aggregations().getAggregatorFactories().stream()
                            .filter(agg -> facets.contains(agg.getName()))
                            .collect(Collectors.toList());

                    addToPivotAggs(pivotAgg, aggs);
                });

        // sorting
        if(search.hasSorting()) {
            search.getSorting().stream()
                    .map( sort -> SortUtils.buildSort(sort, search, factory, searchContext))
                    .forEach(searchSource::sort);
        }
        ////boost functions
        //  if(search.hasSorting()) {
        //}

        // paging
        switch(search.getResultSet().getType()) {
            case page:{
                final Page resultSet = (Page) search.getResultSet();
                searchSource.from(resultSet.getOffset());
                searchSource.size(resultSet.getPagesize());
                break;
            }
            case slice: {
                final Slice resultSet = (Slice) search.getResultSet();
                searchSource.from(resultSet.getOffset());
                searchSource.size(resultSet.getSliceSize());
                break;
            }
        }
        return searchSource;
    }

    public static SearchSourceBuilder buildPercolatorQueryReadiness(DocumentFactory factory) {

        final SearchSourceBuilder searchSource = new SearchSourceBuilder();
        final QueryBuilder baseQuery =  QueryBuilders.termQuery(FieldUtil.PERCOLATOR_FLAG, true);
        searchSource.query(baseQuery);
        searchSource.fetchSource(true);
        return searchSource;
    }

    private static void addToPivotAggs(AggregationBuilder pivotAgg, List<AggregationBuilder> aggs) {
        pivotAgg.getSubAggregations()
                .forEach(subAgg -> addToPivotAggs(subAgg,aggs));
        aggs.forEach(pivotAgg::subAggregation);
    }
    public static QueryBuilder buildFilterQuery(Filter filter, DocumentFactory factory, String context) {
        return buildFilterQuery(filter, factory, context,false);
    }
    public static QueryBuilder buildFilterQuery(Filter filter, DocumentFactory factory, String context, Boolean percolatorFlag) {
        final BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        // Add base doc type filter
        filterQuery.must(QueryBuilders.termQuery(FieldUtil.TYPE, factory.getType()));
        filterQuery.must(QueryBuilders.termQuery(FieldUtil.PERCOLATOR_FLAG, percolatorFlag));
        Optional.ofNullable(filter)
                .ifPresent(vindFilter -> {
                    filterQuery.must(filterMapper(vindFilter, factory, context));
                });
        return filterQuery;

    }

    private static QueryBuilder filterMapper(Filter filter, DocumentFactory factory, String context) {
        final UseCase useCase = UseCase.valueOf(filter.getFilterScope().name());

        switch (filter.getType()) {
            case "AndFilter":
                final Filter.AndFilter andFilter = (Filter.AndFilter) filter;
                final BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();
                andFilter.getChildren()
                        .forEach(nestedFilter -> {
                            boolMustQuery.must(filterMapper(nestedFilter, factory, context));
                        });
                return boolMustQuery;
            case "OrFilter":
                final Filter.OrFilter orFilter = (Filter.OrFilter) filter;
                final BoolQueryBuilder boolShouldQuery = QueryBuilders.boolQuery();
                orFilter.getChildren()
                        .forEach(nestedFilter -> {
                            boolShouldQuery.should(filterMapper(nestedFilter, factory, context));
                        });
                return boolShouldQuery;
            case "NotFilter":
                final Filter.NotFilter notFilter = (Filter.NotFilter) filter;
                final BoolQueryBuilder boolMustNotQuery = QueryBuilders.boolQuery();
                return boolMustNotQuery.mustNot(filterMapper(notFilter.getDelegate(), factory, context));

            case "TermFilter":
                final Filter.TermFilter termFilter = (Filter.TermFilter) filter;
                return QueryBuilders
                        .termQuery(FieldUtil.getFieldName(factory.getField(termFilter.getField()), useCase, context),
                                termFilter.getTerm());
            case "TermsQueryFilter":
                final Filter.TermsQueryFilter termsQueryFilter = (Filter.TermsQueryFilter) filter;
                return QueryBuilders
                        .termsQuery(FieldUtil.getFieldName(factory.getField(termsQueryFilter.getField()), useCase,context),
                                termsQueryFilter.getTerm());
            case "PrefixFilter":
                final Filter.PrefixFilter prefixFilter = (Filter.PrefixFilter) filter;
                return QueryBuilders
                        .prefixQuery(FieldUtil.getFieldName(factory.getField(prefixFilter.getField()), useCase,context),
                                prefixFilter.getTerm());
            case "DescriptorFilter":
                final Filter.DescriptorFilter descriptorFilter = (Filter.DescriptorFilter) filter;
                return QueryBuilders
                        .termQuery(FieldUtil.getFieldName(descriptorFilter.getDescriptor(), useCase, context),
                                descriptorFilter.getTerm());
            case "BetweenDatesFilter":
                final Filter.BetweenDatesFilter betweenDatesFilter = (Filter.BetweenDatesFilter) filter;
                return QueryBuilders
                        .rangeQuery(FieldUtil.getFieldName(factory.getField(betweenDatesFilter.getField()), useCase,context))
                        .from(betweenDatesFilter.getStart().toString())
                        .to(betweenDatesFilter.getEnd().toString());
            case "BeforeFilter":
                final Filter.BeforeFilter beforeFilter = (Filter.BeforeFilter) filter;
                return QueryBuilders
                        .rangeQuery(FieldUtil.getFieldName(factory.getField(beforeFilter.getField()), useCase,context))
                        .lte(beforeFilter.getDate().toElasticString()) ;
            case "AfterFilter":
                final Filter.AfterFilter afterFilter = (Filter.AfterFilter) filter;
                return QueryBuilders
                        .rangeQuery(FieldUtil.getFieldName(factory.getField(afterFilter.getField()), useCase,context))
                        .gte(afterFilter.getDate().toElasticString()) ;
            case "BetweenNumericFilter":
                final Filter.BetweenNumericFilter betweenNumericFilter = (Filter.BetweenNumericFilter) filter;
                return QueryBuilders
                        .rangeQuery(FieldUtil.getFieldName(factory.getField(betweenNumericFilter.getField()), useCase,context))
                        .from(betweenNumericFilter.getStart())
                        .to(betweenNumericFilter.getEnd());
            case "LowerThanFilter":
                final Filter.LowerThanFilter lowerThanFilter = (Filter.LowerThanFilter) filter;
                return QueryBuilders
                        .rangeQuery(FieldUtil.getFieldName(factory.getField(lowerThanFilter.getField()), useCase,context))
                        .lte(lowerThanFilter.getNumber()) ;
            case "GreaterThanFilter":
                final Filter.GreaterThanFilter greaterThanFilter = (Filter.GreaterThanFilter) filter;
                return QueryBuilders
                        .rangeQuery(FieldUtil.getFieldName(factory.getField(greaterThanFilter.getField()), useCase,context))
                        .gte(greaterThanFilter.getNumber()) ;
            case "NotEmptyTextFilter":
                final Filter.NotEmptyTextFilter notEmptyTextFilter = (Filter.NotEmptyTextFilter) filter;
                final String fieldName = FieldUtil.getFieldName(factory.getField(notEmptyTextFilter.getField()), useCase, context);
                return QueryBuilders.boolQuery()
                        .must(QueryBuilders.existsQuery(fieldName))
                        .mustNot(QueryBuilders.regexpQuery(fieldName , " *"))
                        ;
            case "NotEmptyFilter":
                final Filter.NotEmptyFilter notEmptyFilter = (Filter.NotEmptyFilter) filter;
                return QueryBuilders
                        .existsQuery(FieldUtil.getFieldName(factory.getField(notEmptyFilter.getField()), useCase, context));
            case "NotEmptyLocationFilter":
                final Filter.NotEmptyLocationFilter notEmptyLocationFilter = (Filter.NotEmptyLocationFilter) filter;
                return QueryBuilders
                        .existsQuery(FieldUtil.getFieldName(factory.getField(notEmptyLocationFilter.getField()), useCase, context));
            case "WithinBBoxFilter":
                final Filter.WithinBBoxFilter withinBBoxFilter = (Filter.WithinBBoxFilter) filter;
                return QueryBuilders
                        .geoBoundingBoxQuery(FieldUtil.getFieldName(factory.getField(withinBBoxFilter.getField()), null, context))
                        .setCorners(
                                new GeoPoint(
                                    withinBBoxFilter.getUpperLeft().getLat(),
                                    withinBBoxFilter.getUpperLeft().getLng()),
                                new GeoPoint(
                                    withinBBoxFilter.getLowerRight().getLat(),
                                    withinBBoxFilter.getLowerRight().getLng())
                                );
            case "WithinCircleFilter":
                final Filter.WithinCircleFilter withinCircleFilter = (Filter.WithinCircleFilter) filter;
                return QueryBuilders
                        .geoDistanceQuery(FieldUtil.getFieldName(factory.getField(withinCircleFilter.getField()), null, context))
                        .point(withinCircleFilter.getCenter().getLat(),withinCircleFilter.getCenter().getLng())
                        .distance(withinCircleFilter.getDistance(), DistanceUnit.KILOMETERS);
            default:
                throw new SearchServerException(String.format("Error parsing filter to Elasticsearch query DSL: filter type not known %s", filter.getType()));
        }
    }

    private static List<AggregationBuilder> buildElasticAggregations(String name, Facet vindFacet, DocumentFactory factory, UseCase useCase, String searchContext, int minCount, int facetLimit) {
        final String contextualizedFacetName = Stream.of(searchContext, name)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("_"));

        switch (vindFacet.getType()) {
            case "TermFacet":
                final Facet.TermFacet termFacet = (Facet.TermFacet) vindFacet;
                final FieldDescriptor<?> field = factory.getField(termFacet.getFieldName());
                final String fieldName = Optional.ofNullable(FieldUtil.getFieldName(field, useCase, searchContext))
                        .orElse(termFacet.getFieldName());

                final TermsAggregationBuilder termsAgg = AggregationBuilders
                        .terms(contextualizedFacetName)
                        .field(fieldName)
                        .minDocCount(minCount)
                        .size(facetLimit);

                Optional.ofNullable(termFacet.getOption()).ifPresent(option -> setTermOptions(termsAgg, option));

                addSortToAggregation(vindFacet, searchContext, termsAgg);

                return Collections.singletonList(termsAgg);
            case "TypeFacet":
                final Facet.TypeFacet typeFacet = (Facet.TypeFacet) vindFacet;
                final TermsAggregationBuilder typeAgg = AggregationBuilders
                        .terms(name)
                        .field(FieldUtil.TYPE)
                        .minDocCount(minCount)
                        .size(facetLimit);
                addSortToAggregation(vindFacet, searchContext, typeAgg);
                return Collections.singletonList(typeAgg);

            case "QueryFacet":
                final Facet.QueryFacet queryFacet = (Facet.QueryFacet) vindFacet;
                final Filter filter = queryFacet.getFilter();
                final FiltersAggregationBuilder queryAgg = AggregationBuilders
                        .filters(
                                contextualizedFacetName,
                                filterMapper(filter, factory, searchContext));

                return Collections.singletonList(queryAgg);

            case "NumericRangeFacet":
                final Facet.NumericRangeFacet<?> numericRangeFacet = (Facet.NumericRangeFacet) vindFacet;
                final HistogramAggregationBuilder rangeAggregation = AggregationBuilders
                        .histogram(name)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(numericRangeFacet.getFieldDescriptor(), useCase, searchContext))
                        .interval(numericRangeFacet.getGap().doubleValue())
                        .minDocCount(minCount);

                addSortToAggregation(vindFacet, searchContext, rangeAggregation);

                final RangeAggregationBuilder numericIntervalRangeAggregation = AggregationBuilders
                        .range(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(numericRangeFacet.getFieldDescriptor(), useCase, searchContext))
                        .subAggregation(rangeAggregation);

                numericIntervalRangeAggregation
                        .addRange(
                                numericRangeFacet.getStart().doubleValue(),
                                numericRangeFacet.getEnd().doubleValue());

                return Collections.singletonList(numericIntervalRangeAggregation);

            case "ZoneDateRangeFacet":
                final Facet.DateRangeFacet.ZoneDateRangeFacet zoneDateRangeFacet = (Facet.DateRangeFacet.ZoneDateRangeFacet) vindFacet;

                final DateRangeAggregationBuilder dateRangeAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(zoneDateRangeFacet.getFieldDescriptor(), useCase, searchContext));

                final ZonedDateTime zonedDateTimeEnd = (ZonedDateTime) zoneDateRangeFacet.getEnd();
                final ZonedDateTime zonedDateTimeStart = (ZonedDateTime) zoneDateRangeFacet.getStart();

                dateRangeAggregation.addRange(zonedDateTimeStart,zonedDateTimeEnd);

                final DateHistogramAggregationBuilder histogramDateRangeAggregation = AggregationBuilders
                        .dateHistogram(name)
                        .minDocCount(minCount)
                        .fixedInterval(DateHistogramInterval
                                .minutes(new Long(zoneDateRangeFacet.getGapDuration().toMinutes()).intValue()))
                        .keyed(true)
                        .field(FieldUtil.getFieldName(zoneDateRangeFacet.getFieldDescriptor(), useCase, searchContext));

                addSortToAggregation(vindFacet, searchContext, histogramDateRangeAggregation);

                dateRangeAggregation.subAggregation(histogramDateRangeAggregation);

                return Collections.singletonList(dateRangeAggregation);
            case "UtilDateRangeFacet":
                final Facet.DateRangeFacet.UtilDateRangeFacet utilDateRangeFacet = (Facet.DateRangeFacet.UtilDateRangeFacet) vindFacet;

                final DateRangeAggregationBuilder utilDateRangeAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(utilDateRangeFacet.getFieldDescriptor(), useCase, searchContext));

                final ZonedDateTime dateTimeEnd = ZonedDateTime.ofInstant(((Date) utilDateRangeFacet.getEnd()).toInstant(), ZoneId.of("UTC"));
                final ZonedDateTime dateTimeStart = ZonedDateTime.ofInstant(((Date) utilDateRangeFacet.getStart()).toInstant(), ZoneId.of("UTC"));

                 utilDateRangeAggregation.addRange(dateTimeStart, dateTimeEnd);

                 final DateHistogramAggregationBuilder histogramUtilDateRangeAggregation = AggregationBuilders
                         .dateHistogram(name)
                         .keyed(true)
                         .fixedInterval(DateHistogramInterval
                                 .minutes(new Long(utilDateRangeFacet.getGapDuration().toMinutes()).intValue()))
                         .field(FieldUtil.getFieldName(utilDateRangeFacet.getFieldDescriptor(), useCase, searchContext));

                 addSortToAggregation(vindFacet, searchContext, histogramUtilDateRangeAggregation);

                utilDateRangeAggregation.subAggregation(histogramUtilDateRangeAggregation);

                return Collections.singletonList(utilDateRangeAggregation);

            case "DateMathRangeFacet":
                final Facet.DateRangeFacet.DateMathRangeFacet dateMathRangeFacet = (Facet.DateRangeFacet.DateMathRangeFacet) vindFacet;

                final DateRangeAggregationBuilder dateMathDateRangeAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(dateMathRangeFacet.getFieldDescriptor(), useCase, searchContext));

                final ZonedDateTime dateMathEnd =
                        ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(((DateMathExpression) dateMathRangeFacet.getEnd()).getTimeStamp()),
                                ZoneId.of("UTC"));
                final ZonedDateTime dateMathStart =
                        ZonedDateTime.ofInstant(
                                Instant.ofEpochSecond(((DateMathExpression) dateMathRangeFacet.getStart()).getTimeStamp()),
                                ZoneId.of("UTC"));

                dateMathDateRangeAggregation
                        .addRange(name,
                                ((DateMathExpression)dateMathRangeFacet.getStart()).toElasticString(),
                                ((DateMathExpression)dateMathRangeFacet.getEnd()).toElasticString());

                final Long minutesGap = dateMathRangeFacet.getGapDuration().toMinutes();

                final DateHistogramAggregationBuilder histogramDateMathDateRangeAggregation = AggregationBuilders
                        .dateHistogram(name)
                        .minDocCount(minCount)
                        .fixedInterval(DateHistogramInterval.minutes(minutesGap.intValue()))
                        .keyed(true)
                        .field(FieldUtil.getFieldName(dateMathRangeFacet.getFieldDescriptor(), useCase, searchContext));
                dateMathDateRangeAggregation.subAggregation(histogramDateMathDateRangeAggregation);

                addSortToAggregation(vindFacet, searchContext, histogramDateMathDateRangeAggregation);

                return Collections.singletonList(dateMathDateRangeAggregation);

            case "NumericIntervalFacet":
                final Facet.NumericIntervalFacet numericIntervalFacet = (Facet.NumericIntervalFacet) vindFacet;
                final RangeAggregationBuilder numericIntervalAggregation = AggregationBuilders
                        .range(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(numericIntervalFacet.getFieldDescriptor(), useCase, searchContext));

                numericIntervalFacet.getIntervals()
                        .forEach( interval -> intervalToRange((NumericInterval<?>) interval, numericIntervalAggregation));

                return Collections.singletonList(numericIntervalAggregation);

            case "ZoneDateTimeIntervalFacet":
                final Facet.DateIntervalFacet.ZoneDateTimeIntervalFacet zoneDateTimeIntervalFacet = (Facet.DateIntervalFacet.ZoneDateTimeIntervalFacet) vindFacet;
                final DateRangeAggregationBuilder ZoneDateIntervalAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(zoneDateTimeIntervalFacet.getFieldDescriptor(), useCase, searchContext));

                zoneDateTimeIntervalFacet.getIntervals()
                        .forEach( interval -> intervalToRange((Interval.ZonedDateTimeInterval<?>) interval, ZoneDateIntervalAggregation));

                return Collections.singletonList(ZoneDateIntervalAggregation);

            case "UtilDateIntervalFacet":
                final Facet.DateIntervalFacet.UtilDateIntervalFacet utilDateIntervalFacet = (Facet.DateIntervalFacet.UtilDateIntervalFacet) vindFacet;
                final DateRangeAggregationBuilder utilDateIntervalAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(utilDateIntervalFacet.getFieldDescriptor(), useCase, searchContext));

                utilDateIntervalFacet.getIntervals()
                        .forEach( interval -> intervalToRange((Interval.UtilDateInterval<?>) interval, utilDateIntervalAggregation));

                return Collections.singletonList(utilDateIntervalAggregation);

            case "ZoneDateTimeDateMathIntervalFacet":
            case "UtilDateMathIntervalFacet":
                final Facet.DateIntervalFacet dateMathIntervalFacet = (Facet.DateIntervalFacet) vindFacet;
                final DateRangeAggregationBuilder dateMathIntervalAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(dateMathIntervalFacet.getFieldDescriptor(), useCase, searchContext));

                dateMathIntervalFacet.getIntervals()
                        .forEach( interval -> intervalToRange((Interval.DateMathInterval<?>) interval, dateMathIntervalAggregation));

                return Collections.singletonList(dateMathIntervalAggregation);

            case "StatsFacet":
            case "StatsDateFacet":
            case "StatsUtilDateFacet":
            case "StatsNumericFacet":
                final Facet.StatsFacet statsFacet = (Facet.StatsFacet) vindFacet;
                if(!CharSequence.class.isAssignableFrom(((Facet.StatsFacet) vindFacet).getField().getType())) {
                    return getStatsAggregationBuilders(searchContext, contextualizedFacetName, UseCase.Facet, statsFacet);
                }
            case "PivotFacet":
                final Facet.PivotFacet pivotFacet = (Facet.PivotFacet) vindFacet;
                final List<TermsAggregationBuilder> termFacets = pivotFacet.getFieldDescriptors().stream()
                        .map(f -> FieldUtil.getFieldName(f, useCase, searchContext))
                        .filter(Objects::nonNull)
                        .map(n -> AggregationBuilders
                                .terms(contextualizedFacetName)
                                .field(n)
                                .minDocCount(minCount)
                                .size(facetLimit))
                        //.forEach(agg -> addSortToAggregation(vindFacet, searchContext, agg))
                        .collect(Collectors.toList());
                termFacets.forEach(agg -> addSortToAggregation(vindFacet, searchContext, agg));

                final Optional<TermsAggregationBuilder> pivotAgg = Lists.reverse(termFacets).stream().reduce((agg1, agg2) -> agg2.subAggregation(agg1));

                return Collections.singletonList(pivotAgg.orElse(null));

            default:
                throw new SearchServerException(
                        String.format(
                                "Error mapping Vind facet to Elasticsearch aggregation: Unknown facet type %s",
                                vindFacet.getType()));
        }
    }

    private static void addSortToAggregation(Facet vindFacet, String searchContext, TermsAggregationBuilder termsAgg) {
        Optional.ofNullable(vindFacet.getSortings())
                .ifPresent(sortings -> {
                    for (Map.Entry<String, Sort> sort: sortings.entrySet()) {
                        final AggregationBuilder sortAggregation = SortUtils.buildFacetSort(sort.getKey(), sort.getValue(), searchContext);
                        termsAgg.order(BucketOrder.aggregation(sortAggregation.getName(), Sort.Direction.Asc.equals(sort.getValue().getDirection())));
                        termsAgg.subAggregation(sortAggregation);
                    }
                });
    }
    private static void addSortToAggregation(Facet vindFacet, String searchContext, HistogramAggregationBuilder agg) {
        Optional.ofNullable(vindFacet.getSortings())
                .ifPresent(sortings -> {
                    for (Map.Entry<String, Sort> sort: sortings.entrySet()) {
                        final AggregationBuilder sortAggregation = SortUtils.buildFacetSort(sort.getKey(), sort.getValue(), searchContext);
                        agg.order(BucketOrder.aggregation(sortAggregation.getName(), Sort.Direction.Asc.equals(sort.getValue().getDirection())));
                        agg.subAggregation(sortAggregation);
                    }
                });
    }

    private static void addSortToAggregation(Facet vindFacet, String searchContext, DateHistogramAggregationBuilder agg) {
        Optional.ofNullable(vindFacet.getSortings())
                .ifPresent(sortings -> {
                    for (Map.Entry<String, Sort> sort: sortings.entrySet()) {
                        final AggregationBuilder sortAggregation = SortUtils.buildFacetSort(sort.getKey(), sort.getValue(), searchContext);
                        agg.order(BucketOrder.aggregation(sortAggregation.getName(), Sort.Direction.Asc.equals(sort.getValue().getDirection())));
                        agg.subAggregation(sortAggregation);
                    }
                });
    }

    private static List<AggregationBuilder> getStatsAggregationBuilders(String searchContext, String contextualizedFacetName, UseCase useCase,  Facet.StatsFacet statsFacet) {
        final List<AggregationBuilder> statsAggs = new ArrayList<>();
        final ExtendedStatsAggregationBuilder statsAgg = AggregationBuilders
                .extendedStats(contextualizedFacetName)
                .field(FieldUtil.getFieldName(statsFacet.getField(), useCase, searchContext));

        if (ArrayUtils.isNotEmpty(statsFacet.getPercentiles())) {
            statsAggs.add(AggregationBuilders
                    .percentileRanks(contextualizedFacetName + "_percentiles", ArrayUtils.toPrimitive(statsFacet.getPercentiles()))
                    .field(FieldUtil.getFieldName(statsFacet.getField(), useCase, searchContext))
            );
        }

        if (statsFacet.getCardinality()) {
            statsAggs.add(AggregationBuilders
                    .cardinality(contextualizedFacetName + "_cardinality")
                    .field(FieldUtil.getFieldName(statsFacet.getField(), useCase, searchContext))
            );
        }

        if (statsFacet.getCountDistinct() || statsFacet.getDistinctValues()) {
            statsAggs.add(AggregationBuilders
                    .terms(contextualizedFacetName + "_values")
                    .field(FieldUtil.getFieldName(statsFacet.getField(), useCase, searchContext))
            );
        }

        if (statsFacet.getMissing()) {
            statsAggs.add(AggregationBuilders
                    .missing(contextualizedFacetName + "_missing")
                    .field(FieldUtil.getFieldName(statsFacet.getField(), useCase, searchContext))
            );
        }

        statsAggs.add(statsAgg);
        return statsAggs;
    }

    private static void setTermOptions(TermsAggregationBuilder agg, TermFacetOption option) {
        if(Objects.nonNull(option.getPrefix())) {
            agg.includeExclude(new IncludeExclude(option.getPrefix() + ".*", null));
        }
        if(Objects.nonNull(option.getLimit())) {
            agg.size(option.getLimit());
        }

        if(Objects.nonNull(option.getMethod())) {
            log.warn("Elasticearch backend implementation does not support set method for term facets");
        }

        if(Objects.nonNull(option.getMincount())) {
            agg.minDocCount(option.getMincount());
        }

        if(Objects.nonNull(option.getOffset())) {
            log.warn("Elasticearch backend implementation does not support set offset for term facets");
        }

        if(Objects.nonNull(option.getOverrefine())) {
            log.warn("Elasticearch backend implementation does not support set overrefine for term facets");
        }

        if(Objects.nonNull(option.getOverrequest())) {
            log.warn("Elasticearch backend implementation does not support set overrequest for term facets");
        }

        if(Objects.nonNull(option.getSort())) {
            log.warn("Elasticearch backend implementation does not support set sorting for term facets");
        }

        if(Objects.nonNull(option.isAllBuckets())) {
            log.warn("Elasticearch backend implementation does not support set all Buckets for term facets");
        }

        if(Objects.nonNull(option.isMissing())) {
            agg.missing("");
        }

        if(Objects.nonNull(option.isNumBuckets())) {
            log.warn("Elasticearch backend implementation does not support set numBuckets for term facets");
        }

        if(Objects.nonNull(option.isRefine())) {
            log.warn("Elasticearch backend implementation does not support set refine for term facets");
        }
    }

    private static void intervalToRange(NumericInterval<?> interval, RangeAggregationBuilder rangeAggregation) {
        final Number start = interval.getStart();
        final Number end = interval.getEnd();
        if (Objects.nonNull(start) && Objects.nonNull(end)) {
            rangeAggregation.addRange(interval.getName(), start.doubleValue(), end.doubleValue());
        } else {
            Optional.ofNullable(start).ifPresent(n -> rangeAggregation.addUnboundedFrom(interval.getName(), n.doubleValue()));
            Optional.ofNullable(end).ifPresent(n -> rangeAggregation.addUnboundedTo(interval.getName(), n.doubleValue()));
        }

    }

    private static void intervalToRange(Interval.ZonedDateTimeInterval<?> interval, DateRangeAggregationBuilder rangeAggregation) {

        final ZonedDateTime start = interval.getStart();
        final ZonedDateTime end =  interval.getEnd();

        if (Objects.nonNull(start) && Objects.nonNull(end)) {
            rangeAggregation.addRange(interval.getName(), start, end);
        } else {
            Optional.ofNullable(start).ifPresent(n -> rangeAggregation.addUnboundedFrom(interval.getName(), n));
            Optional.ofNullable(end).ifPresent(n -> rangeAggregation.addUnboundedTo(interval.getName(), n));
        }
    }

    private static void intervalToRange(Interval.UtilDateInterval<?> interval, DateRangeAggregationBuilder rangeAggregation) {

        final Date start = interval.getStart();
        final Date end =  interval.getEnd();

        if (Objects.nonNull(start) && Objects.nonNull(end)) {
            rangeAggregation.addRange(
                    interval.getName(),
                    ZonedDateTime.ofInstant(start.toInstant(), ZoneId.of("UTC")),
                    ZonedDateTime.ofInstant(end.toInstant(), ZoneId.of("UTC")));
        } else {
            Optional.ofNullable(start).ifPresent(n -> rangeAggregation.addUnboundedFrom(
                    interval.getName(),
                    ZonedDateTime.ofInstant(n.toInstant(), ZoneId.of("UTC"))));
            Optional.ofNullable(end).ifPresent(n -> rangeAggregation.addUnboundedTo(
                    interval.getName(),
                    ZonedDateTime.ofInstant(n.toInstant(), ZoneId.of("UTC"))));
        }
    }

    private static void intervalToRange(Interval.DateMathInterval<?> interval, DateRangeAggregationBuilder rangeAggregation) {

        final DateMathExpression start = interval.getStart();
        final DateMathExpression end =  interval.getEnd();

        if (Objects.nonNull(start) && Objects.nonNull(end)) {
            rangeAggregation.addRange(
                    interval.getName(),
                    start.toElasticString(),
                    end.toElasticString()
                    );
        } else {
            Optional.ofNullable(start).ifPresent(n ->
                    rangeAggregation.addUnboundedFrom(
                            interval.getName(),
                            n.toElasticString()
                            //ZonedDateTime.ofInstant(Instant.ofEpochSecond(n.getTimeStamp()), ZoneId.of("UTC"))
                    )
            );
            Optional.ofNullable(end).ifPresent(n ->
                    rangeAggregation.addUnboundedTo(
                            interval.getName(),
                            n.toElasticString()
                            //ZonedDateTime.ofInstant(Instant.ofEpochSecond(n.getTimeStamp()), ZoneId.of("UTC"))
                    )
            );
        }

    }

    public static PainlessScript.ScriptBuilder buildUpdateScript(
            HashMap<FieldDescriptor<?>, HashMap<String, SortedSet<UpdateOperation>>> options,
            DocumentFactory factory,
            String updateContext) {
        final PainlessScript.ScriptBuilder scriptBuilder = new PainlessScript.ScriptBuilder();
        options.entrySet().stream()
                .map(entry -> scriptBuilder.addOperations(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return scriptBuilder;
    }

    public static SearchSourceBuilder buildExperimentalSuggestionQuery(
            ExecutableSuggestionSearch search,
            DocumentFactory factory) {

        final String searchContext = search.getSearchContext();
        final SearchSourceBuilder searchSource = new SearchSourceBuilder();

        final BoolQueryBuilder baseQuery = QueryBuilders.boolQuery();

        final String[] suggestionFieldNames = Stream.of(getSuggestionFieldNames(search, factory, searchContext))
                .map(name -> name.concat("_experimental"))
                .toArray(String[]::new);

        final MultiMatchQueryBuilder suggestionQuery = QueryBuilders
                .multiMatchQuery(search.getInput(),suggestionFieldNames)
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                .operator(Operator.OR);

        baseQuery.must(suggestionQuery);

//        if(search.getTimeZone() != null) {
//            query.set(CommonParams.TZ,search.getTimeZone());
//        }

        baseQuery.filter(buildFilterQuery(search.getFilter(), factory, searchContext));

        searchSource.query(baseQuery);

        final HighlightBuilder highlighter = new HighlightBuilder().numOfFragments(0);
        Stream.of(suggestionFieldNames)
                .forEach(highlighter::field);

        searchSource.highlighter(highlighter);
        searchSource.trackScores(SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_SHOW_SCORE, true));
        searchSource.fetchSource(true);

        //TODO if nested document search is implemented

        return searchSource;
    }

    public static SearchSourceBuilder buildSuggestionQuery(ExecutableSuggestionSearch search, DocumentFactory factory) {

        final String searchContext = search.getSearchContext();

        final SearchSourceBuilder searchSource = new SearchSourceBuilder()
                .size(0);

        final BoolQueryBuilder filterSuggestions = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchAllQuery())
                .filter(buildFilterQuery(search.getFilter(), factory, searchContext));

        searchSource.query(filterSuggestions);

//        if(search.getTimeZone() != null) {
//            query.set(CommonParams.TZ,search.getTimeZone());
//        }

        final List<String> suggestionFieldNames =
                Lists.newArrayList(getSuggestionFieldNames(search, factory, searchContext));

        suggestionFieldNames.stream()
                .map(field -> AggregationBuilders
                        .terms(FieldUtil.getSourceFieldName(field.replaceAll("\\.suggestion", ""), searchContext))
                        .field(field)
                        .includeExclude(
                                new IncludeExclude(Suggester.getSuggestionRegex(search.getInput()), null))
                )
                .forEach(searchSource::aggregation);

        final SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.setGlobalText(search.getInput());
        suggestionFieldNames
                .forEach(fieldName -> suggestBuilder
                        .addSuggestion(
                                FieldUtil.getSourceFieldName(fieldName.replaceAll("\\.suggestion", ""), searchContext),
                                SuggestBuilders.termSuggestion(fieldName.concat("_experimental")).prefixLength(0)));

        searchSource.suggest(suggestBuilder);
        return searchSource;
    }

    public static List<String> getSpellCheckedQuery(SearchResponse response) {
        final Suggest suggestions = response.getSuggest();
        if(suggestions!= null) {
            final Map<String, Pair<String,Double>> spellcheck = Streams.stream(suggestions.iterator())
                    .map(e ->Pair.of(e.getName(),  e.getEntries().stream()
                            .map(word ->
                                    word.getOptions().stream()
                                            .sorted(Comparator.comparingDouble(Option::getScore).reversed())
                                            .map(o -> Pair.of(o.getText().string(),o.getScore()))
                                            .findFirst()
                                            .orElse(Pair.of(word.getText().string(),0f))
                            ).collect(Collectors.toList()))
                    )
                    .collect(Collectors.toMap(
                            Pair::getKey,
                            // Pair( "value",  score)
                            p -> Pair.of(
                                    p.getRight().stream().map(Pair::getKey).collect(Collectors.joining(" ")),
                                    p.getValue().stream().mapToDouble(Pair::getValue).sum())));
            // sorted by spellcheck score
            return spellcheck.values().stream()
                    .filter( v -> v.getValue() >= 0.0)
                    .sorted((p1,p2) -> Double.compare(p2.getValue(),p1.getValue()))
                    .map(Pair::getKey)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();

    }

    protected static String[] getSuggestionFieldNames(ExecutableSuggestionSearch search, DocumentFactory factory, String searchContext) {

        if(search.isStringSuggestion()) {
            final StringSuggestionSearch suggestionSearch =(StringSuggestionSearch) search;
            return suggestionSearch.getSuggestionFields().stream()
                    .map(factory::getField)
                    .map(descriptor -> FieldUtil.getFieldName(descriptor, UseCase.Suggest, searchContext))
                    .filter(Objects::nonNull)
                    .map(name -> name.contains(".suggestion")? name : name+".suggestion" )
                    .toArray(String[]::new);
        } else {
            final DescriptorSuggestionSearch suggestionSearch =(DescriptorSuggestionSearch) search;
            return suggestionSearch.getSuggestionFields().stream()
                    .map(descriptor -> FieldUtil.getFieldName(descriptor, UseCase.Suggest, searchContext))
                    .filter(Objects::nonNull)
                    .map(name -> name.contains(".suggestion")? name : name+".suggestion" )
                    .toArray(String[]::new);
        }
    }
    protected static String[] getFullTextFieldNames(FulltextSearch search, DocumentFactory factory, String searchContext) {

        return factory.getFields().entrySet().stream()
                .filter( e -> e.getValue().isFullText())
                .map(e -> FieldUtil.getFieldName(e.getValue(), UseCase.Fulltext, searchContext))
                .filter(Objects::nonNull)
                .toArray(String[]::new);

    }
}
