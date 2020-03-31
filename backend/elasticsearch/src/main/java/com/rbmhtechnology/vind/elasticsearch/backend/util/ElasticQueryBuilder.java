package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class ElasticQueryBuilder {

    public static SearchSourceBuilder buildQuery(FulltextSearch search, DocumentFactory factory) {

        final String searchContext = search.getSearchContext();
        final SearchSourceBuilder searchSource = new SearchSourceBuilder();
        final BoolQueryBuilder baseQuery = QueryBuilders.boolQuery();

        //build full text disMax query
        final QueryStringQueryBuilder fullTextStringQuery = QueryBuilders.queryStringQuery(search.getSearchString());
        // Set fulltext fields
        factory.getFields().values().stream()
                .filter(FieldDescriptor::isFullText)
                .forEach(field -> fullTextStringQuery.field(FieldUtil.getFieldName(field, searchContext).concat(".text"), field.getBoost()));


        final DisMaxQueryBuilder query = QueryBuilders.disMaxQuery()
                .add(fullTextStringQuery);

        baseQuery.should(query);
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


        //TODO: Add when query DSL implementation
//        if(search.getGeoDistance() != null) {
//            final FieldDescriptor descriptor = factory.getField(search.getGeoDistance().getFieldName());
//            if (Objects.nonNull(descriptor)) {
//                query.setParam(CommonParams.FL, query.get(CommonParams.FL) + "," + DISTANCE + ":geodist()");
//                query.setParam("pt", search.getGeoDistance().getLocation().toString());
//                query.setParam("sfield", getFieldname(descriptor, UseCase.Facet, searchContext));
//            }
//        }

        final TermQueryBuilder typeFilterQuery = QueryBuilders.termQuery(FieldUtil.TYPE, factory.getType());
        baseQuery.filter(typeFilterQuery);


        //mm
        searchSource.minScore(Float.parseFloat(search.getMinimumShouldMatch())/10);

        //TODO: Add when query DSL implementation
        //if(search.hasFilter()) {
        //    SolrUtils.Query.buildFilterString(search.getFilter(), factory,search.getChildrenFactory(),query, searchContext, search.getStrict());
        //}

        //TODO if nested document search is implemented
        // fulltext search deep search


        //TODO on aggregation implementation
        //// faceting
        // if(search.hasFacet()) {
        // }

        //TODO on sorting implementation
        //// sorting
        //if(search.hasSorting()) {
        //}
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
}
