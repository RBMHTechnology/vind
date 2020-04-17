package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.google.common.collect.Lists;
import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.api.query.facet.Interval.NumericInterval;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.query.suggestion.DescriptorSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.StringSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.SuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.UpdateOperation;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.lang3.tuple.Pair;
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
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElasticQueryBuilder {

    public static SearchSourceBuilder buildQuery(FulltextSearch search, DocumentFactory factory) {

        final String searchContext = search.getSearchContext();
        final SearchSourceBuilder searchSource = new SearchSourceBuilder();
        final BoolQueryBuilder baseQuery = QueryBuilders.boolQuery();

        //build full text disMax query
        final QueryStringQueryBuilder fullTextStringQuery = QueryBuilders.queryStringQuery(search.getSearchString())
                .minimumShouldMatch(search.getMinimumShouldMatch()); //mm
        // Set fulltext fields
        factory.getFields().values().stream()
                .filter(FieldDescriptor::isFullText)
                .forEach(field -> fullTextStringQuery.field(FieldUtil.getFieldName(field, searchContext).concat(".text"), field.getBoost()));


        final DisMaxQueryBuilder query = QueryBuilders.disMaxQuery()
                .add(fullTextStringQuery);

        baseQuery.must(query);
        searchSource.query(baseQuery);

//        if(search.getTimeZone() != null) {
//            query.set(CommonParams.TZ,search.getTimeZone());
//        }


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

        //TODO if nested document search is implemented
        // fulltext search deep search

        if(search.hasFacet()) {
            search.getFacets().entrySet().stream()
                    .map(vindFacet -> buildElasticAggregations(
                                        vindFacet.getKey(),
                                        vindFacet.getValue(),
                                        factory,
                                        searchContext,
                                        search.getFacetMinCount()))
                    .forEach(searchSource::aggregation);
        }

        // sorting
        if(search.hasSorting()) {
            search.getSorting().stream()
                    .map( sort -> buildSort(sort, search, factory, searchContext))
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

    public static QueryBuilder buildFilterQuery(Filter filter, DocumentFactory factory, String context) {
        final BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        // Add base doc type filter
        filterQuery.must(QueryBuilders.termQuery(FieldUtil.TYPE, factory.getType()));
        Optional.ofNullable(filter)
                .ifPresent(vindFilter -> {
                    filterQuery.must(filterMapper(vindFilter, factory, context));
                });
        return filterQuery;

    }

    private static QueryBuilder filterMapper(Filter filter, DocumentFactory factory, String context) {

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
                            .termQuery(FieldUtil.getFieldName(factory.getField(termFilter.getField()),context),
                                    termFilter.getTerm());
                case "PrefixFilter":
                    final Filter.PrefixFilter prefixFilter = (Filter.PrefixFilter) filter;
                    return QueryBuilders
                            .prefixQuery(FieldUtil.getFieldName(factory.getField(prefixFilter.getField()),context),
                                    prefixFilter.getTerm());
                case "DescriptorFilter":
                    //TODO: Add scope support
                    final Filter.DescriptorFilter descriptorFilter = (Filter.DescriptorFilter) filter;
                    return QueryBuilders
                            .termQuery(FieldUtil.getFieldName(descriptorFilter.getDescriptor(),context),
                                    descriptorFilter.getTerm());
                case "BetweenDatesFilter":
                    //TODO: Add scope support
                    final Filter.BetweenDatesFilter betweenDatesFilter = (Filter.BetweenDatesFilter) filter;
                    return QueryBuilders
                            .rangeQuery(FieldUtil.getFieldName(factory.getField(betweenDatesFilter.getField()),context))
                            .from(betweenDatesFilter.getStart().toString())
                            .to(betweenDatesFilter.getEnd().toString());
                case "BeforeFilter":
                    //TODO: Add scope support
                    final Filter.BeforeFilter beforeFilter = (Filter.BeforeFilter) filter;
                    return QueryBuilders
                            .rangeQuery(FieldUtil.getFieldName(factory.getField(beforeFilter.getField()),context))
                            .lte(beforeFilter.getDate().toElasticString()) ;
                case "AfterFilter":
                    //TODO: Add scope support
                    final Filter.AfterFilter afterFilter = (Filter.AfterFilter) filter;
                    return QueryBuilders
                            .rangeQuery(FieldUtil.getFieldName(factory.getField(afterFilter.getField()),context))
                            .gte(afterFilter.getDate().toElasticString()) ;
                case "BetweenNumericFilter":
                    //TODO: Add scope support
                    final Filter.BetweenNumericFilter betweenNumericFilter = (Filter.BetweenNumericFilter) filter;
                    return QueryBuilders
                            .rangeQuery(FieldUtil.getFieldName(factory.getField(betweenNumericFilter.getField()),context))
                            .from(betweenNumericFilter.getStart())
                            .to(betweenNumericFilter.getEnd());
                case "LowerThanFilter":
                    //TODO: Add scope support
                    final Filter.LowerThanFilter lowerThanFilter = (Filter.LowerThanFilter) filter;
                    return QueryBuilders
                            .rangeQuery(FieldUtil.getFieldName(factory.getField(lowerThanFilter.getField()),context))
                            .lte(lowerThanFilter.getNumber()) ;
                case "GreaterThanFilter":
                    //TODO: Add scope support
                    final Filter.GreaterThanFilter greaterThanFilter = (Filter.GreaterThanFilter) filter;
                    return QueryBuilders
                            .rangeQuery(FieldUtil.getFieldName(factory.getField(greaterThanFilter.getField()),context))
                            .gte(greaterThanFilter.getNumber()) ;
                case "NotEmptyTextFilter":
                    //TODO: Add scope support
                    final Filter.NotEmptyTextFilter notEmptyTextFilter = (Filter.NotEmptyTextFilter) filter;
                    final String fieldName = FieldUtil.getFieldName(factory.getField(notEmptyTextFilter.getField()), context);
                    return QueryBuilders.boolQuery()
                            .must(QueryBuilders.existsQuery(fieldName))
                            .mustNot(QueryBuilders.regexpQuery(fieldName , " *"))
                            ;
                case "NotEmptyFilter":
                    //TODO: Add scope support
                    final Filter.NotEmptyFilter notEmptyFilter = (Filter.NotEmptyFilter) filter;
                    return QueryBuilders
                            .existsQuery(FieldUtil.getFieldName(factory.getField(notEmptyFilter.getField()), context));
                case "NotEmptyLocationFilter":
                    //TODO: Add scope support
                    final Filter.NotEmptyLocationFilter notEmptyLocationFilter = (Filter.NotEmptyLocationFilter) filter;
                    return QueryBuilders
                            .existsQuery(FieldUtil.getFieldName(factory.getField(notEmptyLocationFilter.getField()), context));
                case "WithinBBoxFilter":
                    //TODO: Add scope support
                    final Filter.WithinBBoxFilter withinBBoxFilter = (Filter.WithinBBoxFilter) filter;
                    return QueryBuilders
                            .geoBoundingBoxQuery(FieldUtil.getFieldName(factory.getField(withinBBoxFilter.getField()), context))
                            .setCorners(
                                    withinBBoxFilter.getUpperLeft().getLat(),
                                    withinBBoxFilter.getUpperLeft().getLng(),
                                    withinBBoxFilter.getLowerRight().getLat(),
                                    withinBBoxFilter.getLowerRight().getLng()
                                    );
                case "WithinCircleFilter":
                    //TODO: Add scope support
                    final Filter.WithinCircleFilter withinCircleFilter = (Filter.WithinCircleFilter) filter;
                    return QueryBuilders
                            .geoDistanceQuery(FieldUtil.getFieldName(factory.getField(withinCircleFilter.getField()), context))
                            .point(withinCircleFilter.getCenter().getLat(),withinCircleFilter.getCenter().getLng())
                            .distance(withinCircleFilter.getDistance(), DistanceUnit.METERS);
                default:
                    throw new SearchServerException(String.format("Error parsing filter to Elasticsearch query DSL: filter type not known %s", filter.getType()));
            }
    }

    private static SortBuilder buildSort(Sort sort, FulltextSearch search, DocumentFactory factory, String searchContext) {
       switch (sort.getType()) {
            case "SimpleSort":
                final FieldDescriptor<?> simpleSortField = factory.getField(((Sort.SimpleSort) sort).getField());
                final String sortFieldName = Optional.ofNullable(simpleSortField)
                        .filter(FieldDescriptor::isSort)
                        .map(descriptor -> FieldUtil.getFieldName(descriptor, searchContext))
                        .orElse(((Sort.SimpleSort) sort).getField());
                return SortBuilders
                        .fieldSort(sortFieldName)
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            case "DescriptorSort":
                final String descriptorFieldName = Optional.ofNullable(FieldUtil.getFieldName(((Sort.DescriptorSort) sort).getDescriptor(), searchContext))
                        .orElseThrow(() ->
                                new RuntimeException("The field '" + ((Sort.DescriptorSort) sort).getDescriptor().getName() + "' is not set as sortable"));
                return SortBuilders
                        .fieldSort(descriptorFieldName)
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
           case "DistanceSort":
               Optional.ofNullable(search.getGeoDistance())
                       .orElseThrow(() -> new SearchServerException("Sorting by distance requires a geodistance set"));
               final String distanceFieldName = FieldUtil.getFieldName(search.getGeoDistance().getField(), searchContext);
               return SortBuilders
                       .geoDistanceSort(distanceFieldName,
                               search.getGeoDistance().getLocation().getLat(),
                               search.getGeoDistance().getLocation().getLng())
                       .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
           case "ScoredDate":
               throw new NotImplementedException();
           default:
               throw  new SearchServerException(String
                        .format("Unable to parse Vind sort '%s' to ElasticSearch sorting: sort type not supported.",
                                sort.getType()));
       }
    }

    private static AggregationBuilder buildElasticAggregations(String name, Facet vindFacet, DocumentFactory factory, String searchContext, int minCount) {
        final String contextualizedFacetName = Stream.of(searchContext, name)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("_"));

        switch (vindFacet.getType()) {
            case "TermFacet":
                final Facet.TermFacet termFacet = (Facet.TermFacet) vindFacet;
                final FieldDescriptor<?> field = factory.getField(termFacet.getFieldName());
                final String fieldName = Optional.ofNullable(FieldUtil.getFieldName(field, searchContext))
                        .orElse(termFacet.getFieldName());

                return AggregationBuilders
                        .terms(contextualizedFacetName)
                        .field(fieldName)
                        .minDocCount(minCount);
            case "TypeFacet":
                final Facet.TypeFacet typeFacet = (Facet.TypeFacet) vindFacet;
                return AggregationBuilders
                        .terms(name)
                        .field(FieldUtil.TYPE)
                        .minDocCount(minCount);

            case "QueryFacet":
                final Facet.QueryFacet queryFacet = (Facet.QueryFacet) vindFacet;
                return AggregationBuilders
                        .filters(contextualizedFacetName, filterMapper(queryFacet.getFilter(), factory, searchContext));

            case "NumericRangeFacet":
                final Facet.NumericRangeFacet<?> numericRangeFacet = (Facet.NumericRangeFacet) vindFacet;
                final HistogramAggregationBuilder rangeAggregation = AggregationBuilders
                        .histogram(name)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(numericRangeFacet.getFieldDescriptor(), searchContext))
                        .interval(numericRangeFacet.getGap().doubleValue())
                        .minDocCount(minCount);

                final RangeAggregationBuilder numericIntervalRangeAggregation = AggregationBuilders
                        .range(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(numericRangeFacet.getFieldDescriptor(), searchContext))
                        .subAggregation(rangeAggregation);

                numericIntervalRangeAggregation
                        .addRange(
                                numericRangeFacet.getStart().doubleValue(),
                                numericRangeFacet.getEnd().doubleValue());

                return numericIntervalRangeAggregation;

            case "ZoneDateRangeFacet":
                final Facet.DateRangeFacet.ZoneDateRangeFacet zoneDateRangeFacet = (Facet.DateRangeFacet.ZoneDateRangeFacet) vindFacet;

                final DateRangeAggregationBuilder dateRangeAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(zoneDateRangeFacet.getFieldDescriptor(), searchContext));

                final ZonedDateTime zonedDateTimeEnd = (ZonedDateTime) zoneDateRangeFacet.getEnd();
                final ZonedDateTime zonedDateTimeStart = (ZonedDateTime) zoneDateRangeFacet.getStart();

                dateRangeAggregation.addRange(zonedDateTimeStart,zonedDateTimeEnd);

                final DateHistogramAggregationBuilder histogramDateRangeAggregation = AggregationBuilders
                        .dateHistogram(name)
                        .minDocCount(minCount)
                        .fixedInterval(DateHistogramInterval
                                .minutes(new Long(zoneDateRangeFacet.getGapDuration().toMinutes()).intValue()))
                        .keyed(true)
                        .field(FieldUtil.getFieldName(zoneDateRangeFacet.getFieldDescriptor(), searchContext));

                dateRangeAggregation.subAggregation(histogramDateRangeAggregation);

                return dateRangeAggregation;
            case "UtilDateRangeFacet":
                final Facet.DateRangeFacet.UtilDateRangeFacet utilDateRangeFacet = (Facet.DateRangeFacet.UtilDateRangeFacet) vindFacet;

                final DateRangeAggregationBuilder utilDateRangeAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(utilDateRangeFacet.getFieldDescriptor(), searchContext));

                final ZonedDateTime dateTimeEnd = ZonedDateTime.ofInstant(((Date) utilDateRangeFacet.getEnd()).toInstant(), ZoneId.of("UTC"));
                final ZonedDateTime dateTimeStart = ZonedDateTime.ofInstant(((Date) utilDateRangeFacet.getStart()).toInstant(), ZoneId.of("UTC"));

                 utilDateRangeAggregation.addRange(dateTimeStart, dateTimeEnd);

                 final DateHistogramAggregationBuilder histogramUtilDateRangeAggregation = AggregationBuilders
                         .dateHistogram(name)
                         .keyed(true)
                         .fixedInterval(DateHistogramInterval
                                 .minutes(new Long(utilDateRangeFacet.getGapDuration().toMinutes()).intValue()))
                         .field(FieldUtil.getFieldName(utilDateRangeFacet.getFieldDescriptor(), searchContext));

                 utilDateRangeAggregation.subAggregation(histogramUtilDateRangeAggregation);

                return utilDateRangeAggregation;

            case "DateMathRangeFacet":
                final Facet.DateRangeFacet.DateMathRangeFacet dateMathRangeFacet = (Facet.DateRangeFacet.DateMathRangeFacet) vindFacet;

                final DateRangeAggregationBuilder dateMathDateRangeAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(dateMathRangeFacet.getFieldDescriptor(), searchContext));

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
                        .field(FieldUtil.getFieldName(dateMathRangeFacet.getFieldDescriptor(), searchContext));
                dateMathDateRangeAggregation.subAggregation(histogramDateMathDateRangeAggregation);

                return dateMathDateRangeAggregation;

            case "NumericIntervalFacet":
                final Facet.NumericIntervalFacet numericIntervalFacet = (Facet.NumericIntervalFacet) vindFacet;
                final RangeAggregationBuilder numericIntervalAggregation = AggregationBuilders
                        .range(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(numericIntervalFacet.getFieldDescriptor(), searchContext));

                numericIntervalFacet.getIntervals()
                        .forEach( interval -> intervalToRange((NumericInterval<?>) interval, numericIntervalAggregation));

                return numericIntervalAggregation;

            case "ZoneDateTimeIntervalFacet":
                final Facet.DateIntervalFacet.ZoneDateTimeIntervalFacet zoneDateTimeIntervalFacet = (Facet.DateIntervalFacet.ZoneDateTimeIntervalFacet) vindFacet;
                final DateRangeAggregationBuilder ZoneDateIntervalAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(zoneDateTimeIntervalFacet.getFieldDescriptor(), searchContext));

                zoneDateTimeIntervalFacet.getIntervals()
                        .forEach( interval -> intervalToRange((Interval.ZonedDateTimeInterval<?>) interval, ZoneDateIntervalAggregation));

                return ZoneDateIntervalAggregation;

            case "UtilDateIntervalFacet":
                final Facet.DateIntervalFacet.UtilDateIntervalFacet utilDateIntervalFacet = (Facet.DateIntervalFacet.UtilDateIntervalFacet) vindFacet;
                final DateRangeAggregationBuilder utilDateIntervalAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(utilDateIntervalFacet.getFieldDescriptor(), searchContext));

                utilDateIntervalFacet.getIntervals()
                        .forEach( interval -> intervalToRange((Interval.UtilDateInterval<?>) interval, utilDateIntervalAggregation));

                return utilDateIntervalAggregation;

            case "ZoneDateTimeDateMathIntervalFacet":
            case "UtilDateMathIntervalFacet":
                final Facet.DateIntervalFacet dateMathIntervalFacet = (Facet.DateIntervalFacet) vindFacet;
                final DateRangeAggregationBuilder dateMathIntervalAggregation = AggregationBuilders
                        .dateRange(contextualizedFacetName)
                        .keyed(true)
                        .field(FieldUtil.getFieldName(dateMathIntervalFacet.getFieldDescriptor(), searchContext));

                dateMathIntervalFacet.getIntervals()
                        .forEach( interval -> intervalToRange((Interval.DateMathInterval<?>) interval, dateMathIntervalAggregation));

                return dateMathIntervalAggregation;

            default:
                throw new RuntimeException(String.format("Error mapping Vind facet to Elasticsearch aggregation: Unknown facet type %s",vindFacet.getType()));
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
//                    ZonedDateTime.ofInstant(Instant.ofEpochSecond(start.getTimeStamp()), ZoneId.of("UTC")),
//                    ZonedDateTime.ofInstant(Instant.ofEpochSecond(end.getTimeStamp()), ZoneId.of("UTC"))
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

    public static PainlessScript.ScriptBuilder buildUpdateScript(HashMap<FieldDescriptor<?>, HashMap<String, SortedSet<UpdateOperation>>> options, DocumentFactory factory, String updateContext) {
        final PainlessScript.ScriptBuilder scriptBuilder = new PainlessScript.ScriptBuilder();
                options.entrySet().stream()
                .map(entry -> scriptBuilder.addOperations(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return scriptBuilder;
    }

    public static SearchSourceBuilder buildExperimentalSuggestionQuery(ExecutableSuggestionSearch search, DocumentFactory factory) {

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

        final SearchSourceBuilder searchSource = new SearchSourceBuilder();

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
                        .terms(FieldUtil.getSourceFieldName(field.replaceAll(".suggestion", ""), searchContext))
                        .field(field)
                        .includeExclude(new IncludeExclude(Suggester.getSuggestionRegex(search.getInput()), null))
                )
                .forEach(searchSource::aggregation);

        return searchSource;
    }

    protected static String[] getSuggestionFieldNames(ExecutableSuggestionSearch search, DocumentFactory factory, String searchContext) {

        if(search.isStringSuggestion()) {
            final StringSuggestionSearch suggestionSearch =(StringSuggestionSearch) search;
            return suggestionSearch.getSuggestionFields().stream()
                    .map(factory::getField)
                    .map(descriptor -> FieldUtil.getFieldName(descriptor, searchContext))
                    .filter(Objects::nonNull)
                    .map(name ->  name.concat(".suggestion"))
                    .toArray(String[]::new);
        } else {
            final DescriptorSuggestionSearch suggestionSearch =(DescriptorSuggestionSearch) search;
            return suggestionSearch.getSuggestionFields().stream()
                    .map(descriptor -> FieldUtil.getFieldName(descriptor, searchContext))
                    .filter(Objects::nonNull)
                    .map(name ->  name.concat(".suggestion"))
                    .toArray(String[]::new);
        }
    }
}
