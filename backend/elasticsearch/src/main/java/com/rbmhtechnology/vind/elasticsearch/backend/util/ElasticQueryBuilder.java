package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.FulltextTerm;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.division.Cursor;
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
import com.rbmhtechnology.vind.elasticsearch.backend.client.ElasticVindClient;
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
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.HistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

import static com.rbmhtechnology.vind.elasticsearch.backend.util.CursorUtils.fromSearchAfterCursor;
import static com.rbmhtechnology.vind.elasticsearch.backend.util.SortUtils.NUMBER_OF_MATCHING_TERMS_SORT;
import static com.rbmhtechnology.vind.model.FieldDescriptor.UseCase;

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
    private static final String RELEVANCE = "relevance";

    public static SearchSourceBuilder buildQuery(FulltextSearch search, DocumentFactory factory,
                                                 List<String> indexFootPrint, ElasticVindClient client) {
        return buildQuery(search,factory,false, indexFootPrint, client);
    }

    public static SearchSourceBuilder buildQuery(FulltextSearch search, DocumentFactory factory, boolean escape,
                                                 List<String> indexFootPrint, ElasticVindClient client) {


        final String searchContext = search.getSearchContext();
        final SearchSourceBuilder searchSource = new SearchSourceBuilder();
        final BoolQueryBuilder baseQuery = QueryBuilders.boolQuery();

        // Set total hits count
        final boolean trackTotalHits = Boolean.parseBoolean(
                SearchConfiguration.get(SearchConfiguration.TRACK_TOTAL_HITS, "true"));
        searchSource.trackTotalHits(trackTotalHits);

        //build full text disMax query
        String searchString = "*".equals(search.getSearchString()) || Strings.isEmpty(search.getSearchString().trim())?
                "*:*" : search.getSearchString().trim();

        if (escape){
            //Escape especial characters: + - = && || > < ! ( ) { } [ ] ^ " ~ * ? : \ /
            for(Map.Entry<String,String> wordEntry: reservedChars.entrySet()) {
                searchString = searchString.replaceAll(wordEntry.getKey(), wordEntry.getValue());
            }
        }
        if(!escape && searchString.contains(":")) {
            String skipColonSearchString = searchString.replaceAll(":", reservedChars.get(":"));
            final String[] skipedFields = Arrays.stream(skipColonSearchString.split(" "))
                    .filter(term -> term.contains("\\:"))
                    .map(term -> term.substring(0, term.indexOf("\\:")))
                    .filter(posibleField -> indexFootPrint.contains(posibleField) || "*".equals(posibleField))
                    .toArray(String[]::new);
            for (String field : skipedFields) {
                skipColonSearchString = skipColonSearchString.replace(field+"\\:", field +":");
            }

            searchString = skipColonSearchString;
        }

        final DisMaxQueryBuilder query = createDisMaxQueryBuilder(new FulltextTerm(searchString, search.getMinimumShouldMatch()), factory, indexFootPrint, searchContext);

        baseQuery.must(query);
        searchSource.query(baseQuery);

//        if(search.getTimeZone() != null) {
//            query.set(CommonParams.TZ,search.getTimeZone());
//        }

        if (search.isSpellcheck()) {
            final SuggestBuilder suggestBuilder = new SuggestBuilder();
            suggestBuilder.setGlobalText(search.getSearchString());
            Lists.newArrayList(getFullTextFieldNames(search,factory,searchContext, indexFootPrint))
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
            final Optional<String> distanceFieldName = FieldUtil.getFieldName(distanceField, searchContext, indexFootPrint);
            if (Objects.nonNull(distanceField) && distanceFieldName.isPresent() ) {
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
                                        distanceFieldName.get(),
                                        distanceFieldName.get(),
                                        search.getGeoDistance().getLocation().getLat(),
                                        search.getGeoDistance().getLocation().getLng()
                                ),
                                Collections.emptyMap()
                        )
                );
            }
        }
        searchSource.fetchSource(true);
        baseQuery.filter(buildFilterQuery(search.getFilter(), factory, searchContext, indexFootPrint));

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
                                            searchContext,
                                            search.getFacetMinCount() ,
                                            facetLimit,
                                            indexFootPrint,
                                            client,
                                            searchSource
                                            );
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

                    addToPivotAggs(pivotAgg, aggs, indexFootPrint);
                });

        // sorting
        if(search.hasSorting()) {
            search.getSorting().stream()
                    .map( sort -> SortUtils.buildSort(sort, search, factory, searchContext,indexFootPrint))
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
            case cursor: {
                final Cursor resultSet = (Cursor) search.getResultSet();
                searchSource.size(resultSet.getSize());
                searchSource.sort("_id_");
                if( resultSet.getSearchAfter()!= null ) {
                    searchSource.searchAfter(fromSearchAfterCursor(resultSet.getSearchAfter()));
                }
                break;
            }
        }
        return searchSource;
    }

    private static DisMaxQueryBuilder createDisMaxQueryBuilder(FulltextTerm fulltextTerm, DocumentFactory factory, List<String> indexFootPrint, String searchContext) {
        String minimumShouldMatch = fulltextTerm.getMinimumMatch();
        if(StringUtils.isNumeric(minimumShouldMatch) && !minimumShouldMatch.startsWith("-")) {
            minimumShouldMatch = "0<" + minimumShouldMatch;
        }
        final QueryStringQueryBuilder fullTextStringQuery = QueryBuilders.queryStringQuery(fulltextTerm.getFulltextSearchTerm())
                .minimumShouldMatch(minimumShouldMatch); //mm
        // Set fulltext fields
        factory.getFields().values().stream()
                .filter( FieldDescriptor::isFullText)
                .map( field -> Pair.of(FieldUtil.getFieldName(field, UseCase.Fulltext, searchContext, indexFootPrint), field.getBoost()))
                .filter( pair -> pair.getKey().isPresent())
                .forEach( field -> fullTextStringQuery
                        .field(field.getKey().get(),field.getValue()));

        if(fullTextStringQuery.fields().isEmpty()) {
            fullTextStringQuery.defaultField("full_text");
        }

        return QueryBuilders.disMaxQuery()
          .add(fullTextStringQuery);
    }

    public static SearchSourceBuilder buildPercolatorQueryReadiness(DocumentFactory factory) {

        final SearchSourceBuilder searchSource = new SearchSourceBuilder();
        final QueryBuilder baseQuery =  QueryBuilders.termQuery(FieldUtil.PERCOLATOR_FLAG, true);
        searchSource.query(baseQuery);
        searchSource.fetchSource(true);
        return searchSource;
    }

    private static void addToPivotAggs(AggregationBuilder pivotAgg, List<AggregationBuilder> aggs, List<String> indexFootPrint) {
        pivotAgg.getSubAggregations()
                .forEach(subAgg -> addToPivotAggs(subAgg,aggs, indexFootPrint));
        aggs.forEach(pivotAgg::subAggregation);
    }
    public static QueryBuilder buildFilterQuery(Filter filter, DocumentFactory factory, String context, List<String> indexFootPrint) {
        return buildFilterQuery(filter, factory, context,false, indexFootPrint);
    }
    public static QueryBuilder buildFilterQuery(Filter filter, DocumentFactory factory, String context,
                                                Boolean percolatorFlag, List<String> indexFootprint) {
        final BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        // Add base doc type filter
        filterQuery.must(QueryBuilders.termQuery(FieldUtil.TYPE, factory.getType()));
        filterQuery.must(QueryBuilders.termQuery(FieldUtil.PERCOLATOR_FLAG, percolatorFlag));
        Optional.ofNullable(filter)
                .ifPresent(vindFilter ->
                        Optional.ofNullable(filterMapper(vindFilter, factory, context, indexFootprint))
                            .ifPresent(filterQuery::must));
        return filterQuery;

    }

    private static QueryBuilder filterMapper(Filter filter, DocumentFactory factory, String context, List<String> indexFootPrint) {
        final UseCase useCase = UseCase.valueOf(filter.getFilterScope().name());

        switch (filter.getType()) {
            case "AndFilter":
                final Filter.AndFilter andFilter = (Filter.AndFilter) filter;
                final BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();
                andFilter.getChildren().stream()
                        .map( f -> filterMapper(f, factory, context, indexFootPrint))
                        .filter(Objects::nonNull)
                        .forEach(boolMustQuery::must);
                return boolMustQuery;
            case "OrFilter":
                final Filter.OrFilter orFilter = (Filter.OrFilter) filter;
                final BoolQueryBuilder boolShouldQuery = QueryBuilders.boolQuery();
                orFilter.getChildren().stream()
                        .map( f -> filterMapper(f, factory, context, indexFootPrint))
                        .filter(Objects::nonNull)
                        .forEach(boolShouldQuery::should);
                return boolShouldQuery;
            case "NotFilter":
                final Filter.NotFilter notFilter = (Filter.NotFilter) filter;
                final BoolQueryBuilder boolMustNotQuery = QueryBuilders.boolQuery();
                final Optional<QueryBuilder> notFilterQueryBuilder =
                        Optional.ofNullable(filterMapper(notFilter.getDelegate(), factory, context, indexFootPrint));
                return notFilterQueryBuilder.map(boolMustNotQuery::mustNot).orElse(null);

            case "TermFilter":
                final Filter.TermFilter termFilter = (Filter.TermFilter) filter;
                final Optional<String> termFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(termFilter.getField()), useCase, context, indexFootPrint);
                return termFilterFieldName.map(s -> QueryBuilders.termQuery(s, termFilter.getTerm())).orElse(null);
            case "TermsQueryFilter":
                final Filter.TermsQueryFilter termsQueryFilter = (Filter.TermsQueryFilter) filter;

                final Optional<String> termsQueryFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(termsQueryFilter.getField()), useCase,context, indexFootPrint);
                return termsQueryFilterFieldName.map(s -> QueryBuilders
                        .termsQuery(s, termsQueryFilter.getTerm())).orElse(null);

            case "PrefixFilter":
                final Filter.PrefixFilter prefixFilter = (Filter.PrefixFilter) filter;
                final Optional<String> prefixQueryFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(prefixFilter.getField()), useCase,context, indexFootPrint)
                ;
                return prefixQueryFilterFieldName.map(s -> QueryBuilders
                        .prefixQuery(s, prefixFilter.getTerm())).orElse(null);

            case "DescriptorFilter":
                final Filter.DescriptorFilter descriptorFilter = (Filter.DescriptorFilter) filter;
                final Optional<String> descriptorQueryFilterFieldName =
                        FieldUtil.getFieldName(descriptorFilter.getDescriptor(), useCase, context, indexFootPrint)
                ;
                return descriptorQueryFilterFieldName.map(s -> QueryBuilders
                        .termQuery(descriptorQueryFilterFieldName.get(), descriptorFilter.getTerm())).orElse(null);

            case "BetweenDatesFilter":
                final Filter.BetweenDatesFilter betweenDatesFilter = (Filter.BetweenDatesFilter) filter;
                final Optional<String> betweenDatesQueryFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(betweenDatesFilter.getField()), useCase,context, indexFootPrint)
                ;
                return betweenDatesQueryFilterFieldName.map(s -> QueryBuilders
                            .rangeQuery(betweenDatesQueryFilterFieldName.get())
                            .from(betweenDatesFilter.getStart().toElasticString())
                            .to(betweenDatesFilter.getEnd().toElasticString()))
                        .orElse(null);

            case "BeforeFilter":
                final Filter.BeforeFilter beforeFilter = (Filter.BeforeFilter) filter;
                final Optional<String> beforeQueryFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(beforeFilter.getField()), useCase,context, indexFootPrint)
                ;
                return beforeQueryFilterFieldName.map(s -> QueryBuilders
                        .rangeQuery(beforeQueryFilterFieldName.get())
                        .lte(beforeFilter.getDate().toElasticString()))
                        .orElse(null);

            case "AfterFilter":
                final Filter.AfterFilter afterFilter = (Filter.AfterFilter) filter;
                final Optional<String> afterQueryFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(afterFilter.getField()), useCase,context, indexFootPrint)
                ;
                return afterQueryFilterFieldName.map(s -> QueryBuilders
                        .rangeQuery(afterQueryFilterFieldName.get())
                        .gte(afterFilter.getDate().toElasticString()))
                        .orElse(null);

            case "BetweenNumericFilter":
                final Filter.BetweenNumericFilter betweenNumericFilter = (Filter.BetweenNumericFilter) filter;
                final Optional<String> betweenNumericQueryFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(betweenNumericFilter.getField()), useCase, context, indexFootPrint);
                return betweenNumericQueryFilterFieldName.map(s -> QueryBuilders
                        .rangeQuery(betweenNumericQueryFilterFieldName.get())
                        .from(betweenNumericFilter.getStart())
                        .to(betweenNumericFilter.getEnd()))
                        .orElse(null);
            case "LowerThanFilter":
                final Filter.LowerThanFilter lowerThanFilter = (Filter.LowerThanFilter) filter;
                final Optional<String> lowerThanQueryFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(lowerThanFilter.getField()), useCase, context, indexFootPrint);
                return lowerThanQueryFilterFieldName.map(s -> QueryBuilders
                        .rangeQuery(lowerThanQueryFilterFieldName.get())
                        .lte(lowerThanFilter.getNumber()))
                        .orElse(null) ;
            case "GreaterThanFilter":
                final Filter.GreaterThanFilter greaterThanFilter = (Filter.GreaterThanFilter) filter;
                final Optional<String> greaterThanQueryFilterFieldName =
                        FieldUtil.getFieldName(factory.getField(greaterThanFilter.getField()), useCase, context, indexFootPrint);
                return greaterThanQueryFilterFieldName.map(s -> QueryBuilders
                        .rangeQuery(greaterThanQueryFilterFieldName.get())
                        .gte(greaterThanFilter.getNumber()))
                        .orElse(null) ;
            case "NotEmptyTextFilter":
                final Filter.NotEmptyTextFilter notEmptyTextFilter = (Filter.NotEmptyTextFilter) filter;
                final Optional<String> fieldName =
                        FieldUtil.getFieldName(factory.getField(notEmptyTextFilter.getField()), useCase, context, indexFootPrint);
                return fieldName.map(s -> QueryBuilders.boolQuery()
                        .must(QueryBuilders.existsQuery(fieldName.get()))
                        .mustNot(QueryBuilders.regexpQuery(fieldName.get() , " *")))
                        .orElse(null)
                        ;
            case "NotEmptyFilter":
                final Filter.NotEmptyFilter notEmptyFilter = (Filter.NotEmptyFilter) filter;
                final Optional<String> notEmptyFilterQueryFieldName =
                        FieldUtil.getFieldName(factory.getField(notEmptyFilter.getField()), useCase, context, indexFootPrint);
                return notEmptyFilterQueryFieldName.map( s -> QueryBuilders
                        .existsQuery(notEmptyFilterQueryFieldName.get()))
                        .orElse(null);
            case "NotEmptyLocationFilter":
                final Filter.NotEmptyLocationFilter notEmptyLocationFilter = (Filter.NotEmptyLocationFilter) filter;
                final Optional<String> notEmptyLocationFilterQueryFieldName =
                        FieldUtil.getFieldName(factory.getField(notEmptyLocationFilter.getField()), useCase, context, indexFootPrint);
                return notEmptyLocationFilterQueryFieldName.map(s -> QueryBuilders
                        .existsQuery(notEmptyLocationFilterQueryFieldName.get()))
                        .orElse(null);
            case "WithinBBoxFilter":
                final Filter.WithinBBoxFilter withinBBoxFilter = (Filter.WithinBBoxFilter) filter;
                final Optional<String> withinBoxFilterQueryFieldName =
                        FieldUtil.getFieldName(factory.getField(withinBBoxFilter.getField()), null, context, indexFootPrint);
                return withinBoxFilterQueryFieldName.map(s -> QueryBuilders
                        .geoBoundingBoxQuery(withinBoxFilterQueryFieldName.get())
                        .setCorners(
                                new GeoPoint(
                                    withinBBoxFilter.getUpperLeft().getLat(),
                                    withinBBoxFilter.getUpperLeft().getLng()),
                                new GeoPoint(
                                    withinBBoxFilter.getLowerRight().getLat(),
                                    withinBBoxFilter.getLowerRight().getLng())
                                ))
                        .orElse(null);
            case "WithinCircleFilter":
                final Filter.WithinCircleFilter withinCircleFilter = (Filter.WithinCircleFilter) filter;
                final Optional<String> withCircleFilterQueryFieldName =
                        FieldUtil.getFieldName(factory.getField(withinCircleFilter.getField()), null, context, indexFootPrint);
                return withCircleFilterQueryFieldName.map(s -> QueryBuilders
                        .geoDistanceQuery(withCircleFilterQueryFieldName.get())
                        .point(withinCircleFilter.getCenter().getLat(),withinCircleFilter.getCenter().getLng())
                        .distance(withinCircleFilter.getDistance(), DistanceUnit.KILOMETERS))
                        .orElse(null);
            default:
                throw new SearchServerException(String.format("Error parsing filter to Elasticsearch query DSL: filter type not known %s", filter.getType()));
        }
    }

    private static List<AggregationBuilder> buildElasticAggregations(String name, Facet vindFacet,
                                                                     DocumentFactory factory,
                                                                     String searchContext, int minCount, int facetLimit,
                                                                     List<String> indexFootPrint,
                                                                     ElasticVindClient client,
                                                                     SearchSourceBuilder searchSourceStatic) {
        final String contextualizedFacetName = Stream.of(searchContext, name)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("_"));

        final UseCase facetScope = UseCase.valueOf(vindFacet.getScope().name());

        switch (vindFacet.getType()) {
            case "TermFacet":
                final Facet.TermFacet termFacet = (Facet.TermFacet) vindFacet;
                final FieldDescriptor<?> field = factory.getField(termFacet.getFieldName());
                if (field.hasUseCase(facetScope)) {
                    final String fieldName = FieldUtil.getFieldName(field, facetScope, searchContext,indexFootPrint)
                            .orElse(termFacet.getFieldName());

                    final TermsAggregationBuilder termsAgg = AggregationBuilders
                            .terms(contextualizedFacetName)
                            .field(fieldName)
                            .minDocCount(minCount)
                            .size(facetLimit);

                    Optional.ofNullable(termFacet.getOption()).ifPresent(option -> setTermOptions(termsAgg, option));

                    addSortToAggregation(vindFacet, searchContext, termsAgg, indexFootPrint);

                    return Collections.singletonList(termsAgg);
                } else return Collections.emptyList();
            case "TypeFacet":
                final Facet.TypeFacet typeFacet = (Facet.TypeFacet) vindFacet;
                final TermsAggregationBuilder typeAgg = AggregationBuilders
                        .terms(name)
                        .field(FieldUtil.TYPE)
                        .minDocCount(minCount)
                        .size(facetLimit);
                addSortToAggregation(vindFacet, searchContext, typeAgg, indexFootPrint);
                return Collections.singletonList(typeAgg);

            case "QueryFacet":
                final List<AggregationBuilder> aggs = new ArrayList<>();
                final Facet.QueryFacet queryFacet = (Facet.QueryFacet) vindFacet;
                final Filter vindFilter = queryFacet.getFilter();
                Optional.ofNullable(filterMapper(vindFilter, factory, searchContext, indexFootPrint))
                        .ifPresent( elasticFilter ->
                                aggs.add(AggregationBuilders
                                        .filters(contextualizedFacetName, elasticFilter)));
                return aggs;

            case "NumericRangeFacet":
                final Facet.NumericRangeFacet<?> numericRangeFacet = (Facet.NumericRangeFacet) vindFacet;
                final Optional<String> numericRangeAggField =
                        FieldUtil.getFieldName(numericRangeFacet.getFieldDescriptor(), facetScope, searchContext, indexFootPrint);
                if (numericRangeAggField.isPresent()) {
                    final HistogramAggregationBuilder rangeAggregation = AggregationBuilders
                            .histogram(name)
                            .keyed(true)
                            .field(numericRangeAggField.get())
                            .interval(numericRangeFacet.getGap().doubleValue())
                            .minDocCount(minCount);

                    addSortToAggregation(vindFacet, searchContext, rangeAggregation, indexFootPrint);

                    final RangeAggregationBuilder numericIntervalRangeAggregation = AggregationBuilders
                            .range(contextualizedFacetName)
                            .keyed(true)
                            .field(numericRangeAggField.get())
                            .subAggregation(rangeAggregation);

                    numericIntervalRangeAggregation
                            .addRange(
                                    numericRangeFacet.getStart().doubleValue(),
                                    numericRangeFacet.getEnd().doubleValue());
                    return Collections.singletonList(numericIntervalRangeAggregation);
                }
                return Collections.emptyList();

            case "ZoneDateRangeFacet":
                final Facet.DateRangeFacet.ZoneDateRangeFacet zoneDateRangeFacet = (Facet.DateRangeFacet.ZoneDateRangeFacet) vindFacet;

                final Optional<String> zoneDateRangeAggFieldName =
                        FieldUtil.getFieldName(zoneDateRangeFacet.getFieldDescriptor(), facetScope, searchContext, indexFootPrint);
                if(zoneDateRangeAggFieldName.isPresent()) {

                    final DateRangeAggregationBuilder dateRangeAggregation = AggregationBuilders
                            .dateRange(contextualizedFacetName)
                            .keyed(true)
                            .field(zoneDateRangeAggFieldName.get());

                    final ZonedDateTime zonedDateTimeEnd = (ZonedDateTime) zoneDateRangeFacet.getEnd();
                    final ZonedDateTime zonedDateTimeStart = (ZonedDateTime) zoneDateRangeFacet.getStart();

                    dateRangeAggregation.addRange(zonedDateTimeStart,zonedDateTimeEnd);

                    final DateHistogramAggregationBuilder histogramDateRangeAggregation = AggregationBuilders
                            .dateHistogram(name)
                            .minDocCount(minCount)
                            .fixedInterval(DateHistogramInterval
                                    .minutes(new Long(zoneDateRangeFacet.getGapDuration().toMinutes()).intValue()))
                            .keyed(true)
                            .field(zoneDateRangeAggFieldName.get());

                    addSortToAggregation(vindFacet, searchContext, histogramDateRangeAggregation, indexFootPrint);

                    dateRangeAggregation.subAggregation(histogramDateRangeAggregation);

                    return Collections.singletonList(dateRangeAggregation);
                }
                return Collections.emptyList();

            case "UtilDateRangeFacet":
                final Facet.DateRangeFacet.UtilDateRangeFacet utilDateRangeFacet = (Facet.DateRangeFacet.UtilDateRangeFacet) vindFacet;

                final Optional<String> utilDateRangeAggFieldName =
                        FieldUtil.getFieldName(utilDateRangeFacet.getFieldDescriptor(), facetScope, searchContext, indexFootPrint);

                if (utilDateRangeAggFieldName.isPresent()) {
                    final DateRangeAggregationBuilder utilDateRangeAggregation = AggregationBuilders
                            .dateRange(contextualizedFacetName)
                            .keyed(true)
                            .field(utilDateRangeAggFieldName.get());

                    final ZonedDateTime dateTimeEnd = ZonedDateTime.ofInstant(((Date) utilDateRangeFacet.getEnd()).toInstant(), ZoneId.of("UTC"));
                    final ZonedDateTime dateTimeStart = ZonedDateTime.ofInstant(((Date) utilDateRangeFacet.getStart()).toInstant(), ZoneId.of("UTC"));

                    utilDateRangeAggregation.addRange(dateTimeStart, dateTimeEnd);

                    final DateHistogramAggregationBuilder histogramUtilDateRangeAggregation = AggregationBuilders
                            .dateHistogram(name)
                            .keyed(true)
                            .fixedInterval(DateHistogramInterval
                                    .minutes(new Long(utilDateRangeFacet.getGapDuration().toMinutes()).intValue()))
                            .field(utilDateRangeAggFieldName.get());

                    addSortToAggregation(vindFacet, searchContext, histogramUtilDateRangeAggregation, indexFootPrint);

                    utilDateRangeAggregation.subAggregation(histogramUtilDateRangeAggregation);

                    return Collections.singletonList(utilDateRangeAggregation);
                }
                return Collections.emptyList();

            case "DateMathRangeFacet":
                final Facet.DateRangeFacet.DateMathRangeFacet dateMathRangeFacet = (Facet.DateRangeFacet.DateMathRangeFacet) vindFacet;

                final Optional<String> dateMathRangeAggFieldName =
                        FieldUtil.getFieldName(dateMathRangeFacet.getFieldDescriptor(), facetScope, searchContext, indexFootPrint);
                if(dateMathRangeAggFieldName.isPresent()) {
                    final DateRangeAggregationBuilder dateMathDateRangeAggregation = AggregationBuilders
                            .dateRange(contextualizedFacetName)
                            .keyed(true)
                            .field(dateMathRangeAggFieldName.get());

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
                                    ((DateMathExpression) dateMathRangeFacet.getStart()).toElasticString(),
                                    ((DateMathExpression) dateMathRangeFacet.getEnd()).toElasticString());

                    final Long minutesGap = dateMathRangeFacet.getGapDuration().toMinutes();

                    final DateHistogramAggregationBuilder histogramDateMathDateRangeAggregation = AggregationBuilders
                            .dateHistogram(name)
                            .minDocCount(minCount)
                            .fixedInterval(DateHistogramInterval.minutes(minutesGap.intValue()))
                            .keyed(true)
                            .field(dateMathRangeAggFieldName.get());
                    dateMathDateRangeAggregation.subAggregation(histogramDateMathDateRangeAggregation);

                    addSortToAggregation(vindFacet, searchContext, histogramDateMathDateRangeAggregation, indexFootPrint);

                    return Collections.singletonList(dateMathDateRangeAggregation);
                }
                return Collections.emptyList();

            case "NumericIntervalFacet":
                final Facet.NumericIntervalFacet numericIntervalFacet = (Facet.NumericIntervalFacet) vindFacet;
                final Optional<String> numericIntervalAggFieldName =
                        FieldUtil.getFieldName(numericIntervalFacet.getFieldDescriptor(), facetScope, searchContext, indexFootPrint);

                if (numericIntervalAggFieldName.isPresent()) {
                    final RangeAggregationBuilder numericIntervalAggregation = AggregationBuilders
                            .range(contextualizedFacetName)
                            .keyed(true)
                            .field(numericIntervalAggFieldName.get());

                    numericIntervalFacet.getIntervals()
                            .forEach( interval -> intervalToRange((NumericInterval<?>) interval, numericIntervalAggregation));

                    return Collections.singletonList(numericIntervalAggregation);
                }
                return Collections.emptyList();


            case "ZoneDateTimeIntervalFacet":
                final Facet.DateIntervalFacet.ZoneDateTimeIntervalFacet zoneDateTimeIntervalFacet = (Facet.DateIntervalFacet.ZoneDateTimeIntervalFacet) vindFacet;
                final Optional<String> zoneDateTimeIntervalAggFieldName =
                        FieldUtil.getFieldName(zoneDateTimeIntervalFacet.getFieldDescriptor(), facetScope, searchContext, indexFootPrint);
                if (zoneDateTimeIntervalAggFieldName.isPresent()) {
                    final DateRangeAggregationBuilder ZoneDateIntervalAggregation = AggregationBuilders
                            .dateRange(contextualizedFacetName)
                            .keyed(true)
                            .field(zoneDateTimeIntervalAggFieldName.get());

                    zoneDateTimeIntervalFacet.getIntervals()
                            .forEach(interval -> intervalToRange((Interval.ZonedDateTimeInterval<?>) interval, ZoneDateIntervalAggregation));

                    return Collections.singletonList(ZoneDateIntervalAggregation);
                }
                return Collections.emptyList();

            case "UtilDateIntervalFacet":
                final Facet.DateIntervalFacet.UtilDateIntervalFacet utilDateIntervalFacet = (Facet.DateIntervalFacet.UtilDateIntervalFacet) vindFacet;
                final Optional<String> utilDateIntervalAggFieldName =
                        FieldUtil.getFieldName(utilDateIntervalFacet.getFieldDescriptor(), facetScope, searchContext, indexFootPrint);
                if (utilDateIntervalAggFieldName.isPresent()) {
                    final DateRangeAggregationBuilder utilDateIntervalAggregation = AggregationBuilders
                            .dateRange(contextualizedFacetName)
                            .keyed(true)
                            .field(utilDateIntervalAggFieldName.get());

                    utilDateIntervalFacet.getIntervals()
                            .forEach(interval -> intervalToRange((Interval.UtilDateInterval<?>) interval, utilDateIntervalAggregation));

                    return Collections.singletonList(utilDateIntervalAggregation);
                }
                return Collections.emptyList();

            case "ZoneDateTimeDateMathIntervalFacet":
            case "UtilDateMathIntervalFacet":
                final Facet.DateIntervalFacet dateMathIntervalFacet = (Facet.DateIntervalFacet) vindFacet;
                final Optional<String> dateMathIntervalAggFieldName =
                        FieldUtil.getFieldName(dateMathIntervalFacet.getFieldDescriptor(), facetScope, searchContext, indexFootPrint);
                if (dateMathIntervalAggFieldName.isPresent()) {
                    final DateRangeAggregationBuilder dateMathIntervalAggregation = AggregationBuilders
                            .dateRange(contextualizedFacetName)
                            .keyed(true)
                            .field(dateMathIntervalAggFieldName.get());

                    dateMathIntervalFacet.getIntervals()
                            .forEach(interval -> intervalToRange((Interval.DateMathInterval<?>) interval, dateMathIntervalAggregation));

                    return Collections.singletonList(dateMathIntervalAggregation);
                }
                return Collections.emptyList();
            case "StatsDateFacet":
            case "StatsUtilDateFacet":
            case "StatsNumericFacet":
            case "StatsFacet":
                final Facet.StatsFacet statsFacet = (Facet.StatsFacet) vindFacet;
                if(String.class.isAssignableFrom(FieldUtil.getFieldType(statsFacet.getField(), facetScope))) {
                    return getStringStatsAggregationBuilders(
                            searchContext,
                            contextualizedFacetName,
                            facetScope,
                            statsFacet,
                            indexFootPrint);
                }
                return getStatsAggregationBuilders(
                        searchContext,
                        contextualizedFacetName,
                        facetScope,
                        statsFacet,
                        indexFootPrint);
            case "PivotFacet":
                final Facet.PivotFacet pivotFacet = (Facet.PivotFacet) vindFacet;
                final int facetSize = pivotFacet.getSize().orElse(facetLimit);
                final List<TermsAggregationBuilder> termFacets = pivotFacet.getBuckets().stream()
                        .map(bucket ->
                                Pair.of(
                                        FieldUtil.getFieldName(bucket.getLeft(), facetScope, searchContext, indexFootPrint),
                                        bucket.getRight())
                        )
                        .filter(bucket -> bucket.getLeft().isPresent())
                        .map(bucket ->
                                AggregationBuilders
                                        .terms(contextualizedFacetName)
                                        .field(bucket.getLeft().get())
                                        .minDocCount(minCount)
                                        .size(Optional.ofNullable(bucket.getRight()).orElse(facetSize))
                        )
                        .collect(Collectors.toList());

                termFacets.forEach(agg -> addSortToAggregation(vindFacet, searchContext, agg, indexFootPrint));

                final TermsAggregationBuilder mainBucket = termFacets.get(0);
                if (pivotFacet.getPage().isPresent()) {
                    try {
                        final int fieldCardinality = getFieldCardinality(client, mainBucket.field(),searchSourceStatic);
                        final int partitions = (int)Math.ceil(fieldCardinality / Double.valueOf(mainBucket.size()));
                        mainBucket.includeExclude(new IncludeExclude(Math.toIntExact(pivotFacet.getPage().get()), partitions));
                    } catch (IOException e) {
                        throw new SearchServerException(
                                "Error calculating cardinality of field" + mainBucket.field()+ " for paged pivot facet: " + e.getMessage(), e);
                    }
                }

                final Optional<TermsAggregationBuilder> pivotAgg =
                        Lists.reverse(termFacets).stream().reduce((agg1, agg2) -> agg2.subAggregation(agg1));

                return Collections.singletonList(pivotAgg.orElse(null));

            default:
                throw new SearchServerException(
                        String.format(
                                "Error mapping Vind facet to Elasticsearch aggregation: Unknown facet type %s",
                                vindFacet.getType()));
        }
    }

    private static TermsAggregationBuilder createTermPivotAggregation(String descriptorName, Long page, Integer size,
                                                                      String contextualizedFacetName, int minCount,
                                                                      ElasticVindClient client,
                                                                      SearchSourceBuilder searchSourceStatic) {
        final TermsAggregationBuilder aggBuilder = AggregationBuilders
                .terms(contextualizedFacetName)
                .field(descriptorName)
                .minDocCount(minCount)
                .size(size);

        if (Optional.ofNullable(page).isPresent()) {
            try {
                final int fieldCardinality = getFieldCardinality(client, descriptorName, searchSourceStatic);
                final int partitions = (int)Math.ceil(fieldCardinality / Double.valueOf(size));
                aggBuilder.includeExclude(new IncludeExclude(Math.toIntExact(page), partitions));
            } catch (IOException e) {
                throw new SearchServerException(
                        "Error calculating cardinality of field" + descriptorName+ " for paged pivot facet: " + e.getMessage(), e);
            }
        }
        return aggBuilder;
    }
    private static void addSortToAggregation(Facet vindFacet, String searchContext, TermsAggregationBuilder termsAgg,
                                             List<String> indexFootPrint) {
        Optional.ofNullable(vindFacet.getSortings())
                .ifPresent(sortings -> {
                    for (Map.Entry<String, Sort> sort: sortings.entrySet()) {
                        final AggregationBuilder sortAggregation =
                                SortUtils.buildFacetSort(sort.getKey(), sort.getValue(), searchContext, indexFootPrint);
                        termsAgg.order(BucketOrder.aggregation(sortAggregation.getName(), Sort.Direction.Asc.equals(sort.getValue().getDirection())));
                        termsAgg.subAggregation(sortAggregation);
                    }
                });
    }
    private static void addSortToAggregation(Facet vindFacet, String searchContext, HistogramAggregationBuilder agg,
                                             List<String> indexFootPrint) {
        Optional.ofNullable(vindFacet.getSortings())
                .ifPresent(sortings -> {
                    for (Map.Entry<String, Sort> sort: sortings.entrySet()) {
                        final AggregationBuilder sortAggregation =
                                SortUtils.buildFacetSort(sort.getKey(), sort.getValue(), searchContext, indexFootPrint);
                        agg.order(BucketOrder.aggregation(sortAggregation.getName(), Sort.Direction.Asc.equals(sort.getValue().getDirection())));
                        agg.subAggregation(sortAggregation);
                    }
                });
    }

    private static void addSortToAggregation(Facet vindFacet, String searchContext, DateHistogramAggregationBuilder agg,
                                             List<String> indexFootPrint) {
        Optional.ofNullable(vindFacet.getSortings())
                .ifPresent(sortings -> {
                    for (Map.Entry<String, Sort> sort: sortings.entrySet()) {
                        final AggregationBuilder sortAggregation =
                                SortUtils.buildFacetSort(sort.getKey(), sort.getValue(), searchContext, indexFootPrint);
                        agg.order(BucketOrder.aggregation(sortAggregation.getName(), Sort.Direction.Asc.equals(sort.getValue().getDirection())));
                        agg.subAggregation(sortAggregation);
                    }
                });
    }

    private static List<AggregationBuilder> getStatsAggregationBuilders(String searchContext,
                                                                        String contextualizedFacetName, UseCase useCase,
                                                                        Facet.StatsFacet statsFacet,
                                                                        List<String> indexFootPrint) {
        final List<AggregationBuilder> statsAggs = new ArrayList<>();

        FieldUtil.getFieldName(statsFacet.getField(), useCase, searchContext, indexFootPrint)
                .ifPresent(fieldName -> {
                    if(!CharSequence.class.isAssignableFrom(statsFacet.getField().getType())) {
                        final ExtendedStatsAggregationBuilder statsAgg = AggregationBuilders
                                .extendedStats(contextualizedFacetName)
                                .field(fieldName);
                        statsAggs.add(statsAgg);
                    }

                    if (ArrayUtils.isNotEmpty(statsFacet.getPercentiles())) {
                        statsAggs.add(AggregationBuilders
                                .percentileRanks(contextualizedFacetName + "_percentiles", ArrayUtils.toPrimitive(statsFacet.getPercentiles()))
                                .field(fieldName)
                        );
                    }

                    if (statsFacet.getCardinality()) {
                        statsAggs.add(AggregationBuilders
                                .cardinality(contextualizedFacetName + "_cardinality")
                                .field(fieldName)
                        );
                    }

                    if (statsFacet.getCountDistinct() || statsFacet.getDistinctValues()) {
                        statsAggs.add(AggregationBuilders
                                .terms(contextualizedFacetName + "_values")
                                .field(fieldName)
                        );
                    }

                    if (statsFacet.getMissing()) {
                        statsAggs.add(AggregationBuilders
                                .missing(contextualizedFacetName + "_missing")
                                .field(fieldName)
                        );
                    }
                });

        return statsAggs;
    }
    private static List<AggregationBuilder> getStringStatsAggregationBuilders(String searchContext,
                                                                        String contextualizedFacetName, UseCase useCase,
                                                                        Facet.StatsFacet statsFacet,
                                                                        List<String> indexFootPrint) {
        final List<AggregationBuilder> statsAggs = new ArrayList<>();

        FieldUtil.getFieldName(statsFacet.getField(), useCase, searchContext, indexFootPrint)
                .ifPresent(fieldName -> {

                    if (ArrayUtils.isNotEmpty(statsFacet.getPercentiles())) {
                       throw new SearchServerException("Percentile stat is not supported on text fields by elastic backend");
                    }

                    if (statsFacet.getCardinality()) {
                        statsAggs.add(AggregationBuilders
                                .cardinality(contextualizedFacetName + "_cardinality")
                                .field(fieldName)
                        );
                    }

                    if (statsFacet.getCountDistinct() || statsFacet.getDistinctValues()) {
                        statsAggs.add(AggregationBuilders
                                .terms(contextualizedFacetName + "_values")
                                .field(fieldName)
                        );
                    }

                    if (statsFacet.getCount() ) {
                        statsAggs.add(AggregationBuilders
                                .count(contextualizedFacetName + "_value_count")
                                .field(fieldName)
                        );
                    }

                    if (statsFacet.getMissing()) {
                        statsAggs.add(AggregationBuilders
                                .missing(contextualizedFacetName + "_missing")
                                .field(fieldName)
                        );
                    }
                });

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

    public static PainlessScript.ScriptBuilder buildUpdateScript(HashMap<FieldDescriptor<?>, HashMap<String,
            SortedSet<UpdateOperation>>> options, DocumentFactory factory, String updateContext,
                                                                 List<String> indexFootPrint) {
        final PainlessScript.ScriptBuilder scriptBuilder = new PainlessScript.ScriptBuilder();
        options.entrySet().stream()
                .map(entry -> scriptBuilder.addOperations(entry.getKey(), entry.getValue(), indexFootPrint))
                .collect(Collectors.toList());
        return scriptBuilder;
    }

    public static SearchSourceBuilder buildExperimentalSuggestionQuery(
            ExecutableSuggestionSearch search,
            DocumentFactory factory,
            List<String> indexFootPrint) {

        final String searchContext = search.getSearchContext();
        final SearchSourceBuilder searchSource = new SearchSourceBuilder();

        final BoolQueryBuilder baseQuery = QueryBuilders.boolQuery();

        final String[] suggestionFieldNames =
                Stream.of(getSuggestionFieldNames(search, factory, searchContext, indexFootPrint))
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

        baseQuery.filter(buildFilterQuery(search.getFilter(), factory, searchContext, indexFootPrint));

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

    public static SearchSourceBuilder buildSuggestionQuery(ExecutableSuggestionSearch search,
                                                           DocumentFactory factory,
                                                           List<String> indexFootPrint) {

        final String searchContext = search.getSearchContext();

        final SearchSourceBuilder searchSource = new SearchSourceBuilder()
                .size(0);

        final BoolQueryBuilder filterSuggestions = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchAllQuery())
                .filter(buildFilterQuery(search.getFilter(), factory, searchContext, indexFootPrint));

        searchSource.query(filterSuggestions);

//        if(search.getTimeZone() != null) {
//            query.set(CommonParams.TZ,search.getTimeZone());
//        }

        final List<String> suggestionFieldNames =
                Lists.newArrayList(getSuggestionFieldNames(search, factory, searchContext, indexFootPrint));

        suggestionFieldNames.stream()
                .map(field -> AggregationBuilders
                        .terms(FieldUtil.getSourceFieldName(field.replaceAll("\\.suggestion", ""), searchContext))
                        .field(field)
                        .includeExclude(
                                new IncludeExclude(Suggester.getSuggestionRegex(search.getInput(), search.getOperator()), null))
                )
                .map(aggregation ->
                    search.getSort() != null && NUMBER_OF_MATCHING_TERMS_SORT.equals(search.getSort().getType())
                            ? addSubAggregation(aggregation, search, aggregation.field())
                            : aggregation
                )
                .forEach(searchSource::aggregation);

        final SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.setGlobalText(search.getInput());
        suggestionFieldNames
                .forEach(fieldName -> suggestBuilder
                        .addSuggestion(
                                FieldUtil.getSourceFieldName(fieldName.replaceAll("\\.suggestion", ""), searchContext),
                                SuggestBuilders.termSuggestion(fieldName.concat("_experimental")).prefixLength(0)));

        search.getFulltextTerm().ifPresent(fulltextTerm -> {
            QueryBuilder query = createDisMaxQueryBuilder(fulltextTerm, factory, indexFootPrint, searchContext);
            searchSource.query(query);
        });

        searchSource.suggest(suggestBuilder);
        return searchSource;
    }

    private static AggregationBuilder addSubAggregation(TermsAggregationBuilder aggregation,
                                                        ExecutableSuggestionSearch search,
                                                        String fieldName) {
        return aggregation
                .subAggregation(SortUtils.buildSuggestionSort(RELEVANCE, search.getSort(), search.getInput(), fieldName))
                .order(BucketOrder.aggregation(RELEVANCE, false));
    }

    public static List<String> getSpellCheckedQuery(String q, SearchResponse response) {
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
                    .filter( v -> !q.equals(v))
                    .distinct()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();

    }

    protected static String[] getSuggestionFieldNames(ExecutableSuggestionSearch search, DocumentFactory factory,
                                                      String searchContext, List<String> indexFootPrint) {

        if(search.isStringSuggestion()) {
            final StringSuggestionSearch suggestionSearch =(StringSuggestionSearch) search;
            return suggestionSearch.getSuggestionFields().stream()
                    .map(factory::getField)
                    .map(descriptor -> FieldUtil.getFieldName(descriptor, UseCase.Suggest, searchContext, indexFootPrint))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(name -> name.contains(".suggestion")? name : name+".suggestion" )
                    .toArray(String[]::new);
        } else {
            final DescriptorSuggestionSearch suggestionSearch =(DescriptorSuggestionSearch) search;
            return suggestionSearch.getSuggestionFields().stream()
                    .map(descriptor -> FieldUtil.getFieldName(descriptor, UseCase.Suggest, searchContext, indexFootPrint))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(name -> name.contains(".suggestion")? name : name+".suggestion" )
                    .toArray(String[]::new);
        }
    }
    protected static String[] getFullTextFieldNames(FulltextSearch search, DocumentFactory factory, String searchContext,
                                                    List<String> indexFootPrint) {

        return factory.getFields().entrySet().stream()
                .filter( e -> e.getValue().isFullText())
                .map(e -> FieldUtil.getFieldName(e.getValue(), UseCase.Fulltext, searchContext, indexFootPrint))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toArray(String[]::new);

    }
    private static int getFieldCardinality(ElasticVindClient client, String fieldName, SearchSourceBuilder searchSource) throws IOException {
        searchSource.size(0);
        searchSource.aggregation(AggregationBuilders
                .cardinality("_cardinality")
                .field(fieldName));
        final SearchResponse response = client.query(searchSource);
        return Math.toIntExact(((ParsedCardinality)response.getAggregations().get("_cardinality")).getValue());
    }
}
