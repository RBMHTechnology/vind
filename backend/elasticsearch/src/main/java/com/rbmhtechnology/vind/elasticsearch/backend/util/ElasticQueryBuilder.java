package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

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


        //TODO on aggregation implementation
        //// faceting
        // if(search.hasFacet()) {
        // }

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
                    try {
                        filterQuery.must(filterMapper(vindFilter, factory, context));
                    } catch (IOException e) {
                        throw new ElasticsearchException(
                                String.format("Error mapping Vind filter to Elasticsearch Query DSL: %s", e.getMessage()),e);
                    }
                });
        return filterQuery;

    }

    private static QueryBuilder filterMapper(Filter filter, DocumentFactory factory, String context) throws IOException {

            switch (filter.getType()) {
                case "AndFilter":
                    final Filter.AndFilter andFilter = (Filter.AndFilter) filter;
                    final BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();
                    andFilter.getChildren()
                            .forEach(nestedFilter -> {
                                try {
                                    boolMustQuery.must(filterMapper(nestedFilter, factory, context));
                                } catch (IOException e) {
                                    throw new ElasticsearchException(
                                            String.format("Error mapping Vind filter to Elasticsearch Query DSL: %s", e.getMessage()),e);
                                }
                            });
                    return boolMustQuery;
                case "OrFilter":
                    final Filter.OrFilter orFilter = (Filter.OrFilter) filter;
                    final BoolQueryBuilder boolShouldQuery = QueryBuilders.boolQuery();
                    orFilter.getChildren()
                            .forEach(nestedFilter -> {
                                try {
                                    boolShouldQuery.should(filterMapper(nestedFilter, factory, context));
                                } catch (IOException e) {
                                    throw new ElasticsearchException(
                                            String.format("Error mapping Vind filter to Elasticsearch Query DSL: %s", e.getMessage()),e);
                                }
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
                            .lte(beforeFilter.getDate().toString()) ;
                case "AfterFilter":
                    //TODO: Add scope support
                    final Filter.AfterFilter afterFilter = (Filter.AfterFilter) filter;
                    return QueryBuilders
                            .rangeQuery(FieldUtil.getFieldName(factory.getField(afterFilter.getField()),context))
                            .gte(afterFilter.getDate().toString()) ;

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
                    throw new RuntimeException(String.format("Error parsing filter to Elasticsearch query DSL: filter type not known %s", filter.getType()));
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
                       .orElseThrow(() -> new RuntimeException("Sorting by distance requires a geodistance set"));
               final String distanceFieldName = FieldUtil.getFieldName(search.getGeoDistance().getField(), searchContext);
               return SortBuilders
                       .geoDistanceSort(distanceFieldName,
                               search.getGeoDistance().getLocation().getLat(),
                               search.getGeoDistance().getLocation().getLng())
                       .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
           case "ScoredDate":
               final Sort.SpecialSort.ScoredDate scoreSort = (Sort.SpecialSort.ScoredDate) sort;
               return SortBuilders.scoreSort()
                       .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
           default:
               throw  new SearchServerException(String
                        .format("Unable to parse Vind sort '%s' to ElasticSearch sorting: sort type not supported.",
                                sort.getType()));
       }
    }

}
