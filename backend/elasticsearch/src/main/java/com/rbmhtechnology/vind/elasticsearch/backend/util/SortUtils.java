package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SortUtils {
    protected static SortBuilder buildSort(Sort sort, FulltextSearch search, DocumentFactory factory, String searchContext) {
        switch (sort.getType()) {
            case "SimpleSort":
                final FieldDescriptor<?> simpleSortField = factory.getField(((Sort.SimpleSort) sort).getField());
                final String sortFieldName = Optional.ofNullable(simpleSortField)
                        .filter(FieldDescriptor::isSort)
                        .map(descriptor -> FieldUtil.getFieldName(descriptor, FieldUtil.Fieldname.UseCase.Sort, searchContext))
                        .orElse(((Sort.SimpleSort) sort).getField());
                return SortBuilders
                        .fieldSort(sortFieldName)
                        .unmappedType("boolean") //TODO should be set correctly for cross index search usecase
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            case "DescriptorSort":
                final String descriptorFieldName = Optional.ofNullable(FieldUtil.getFieldName(((Sort.DescriptorSort) sort).getDescriptor(), FieldUtil.Fieldname.UseCase.Sort, searchContext))
                        .orElseThrow(() ->
                                new RuntimeException("The field '" + ((Sort.DescriptorSort) sort).getDescriptor().getName() + "' is not set as sortable"));
                return SortBuilders
                        .fieldSort(descriptorFieldName)
                        .unmappedType("boolean") //TODO should be set correctly for cross index search usecase
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            case "DistanceSort":
                Optional.ofNullable(search.getGeoDistance())
                        .orElseThrow(() -> new SearchServerException("Sorting by distance requires a geodistance set"));
                final String distanceFieldName = FieldUtil.getFieldName(search.getGeoDistance().getField(), null, searchContext);
                return SortBuilders
                        .geoDistanceSort(distanceFieldName,
                                search.getGeoDistance().getLocation().getLat(),
                                search.getGeoDistance().getLocation().getLng())
                        .ignoreUnmapped(true)
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            case "ScoredDate":
                final FieldDescriptor descriptor = ((Sort.SpecialSort.ScoredDate) sort).getDescriptor();
                final String sortDateField = Optional.ofNullable(descriptor)
                        .filter(FieldDescriptor::isSort)
                        .filter( field -> Date.class.isAssignableFrom(field.getType()) || ZonedDateTime.class.isAssignableFrom(field.getType()))
                        .map( field -> FieldUtil.getFieldName(descriptor, FieldUtil.Fieldname.UseCase.Sort, searchContext))
                        .orElse(((Sort.SpecialSort.ScoredDate) sort).getField());
                final Map<String, Object> parameters = new HashMap<>();
                parameters.put("field",sortDateField);
                parameters.put("a",4);
                parameters.put("m",1.9e-10);
                parameters.put("b",0.1);
                final Script painlessDateSort = new Script(
                        ScriptType.INLINE,
                        "painless",
                        "_score*(params.a/(params.m*(new Date().getTime()-doc[params.field].value.millis)+params.b))",
                        parameters);
                return SortBuilders
                        .scriptSort(painlessDateSort, ScriptSortBuilder.ScriptSortType.NUMBER)
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            default:
                throw  new SearchServerException(String
                        .format("Unable to parse Vind sort '%s' to ElasticSearch sorting: sort type not supported.",
                                sort.getType()));
        }
    }

    protected static AggregationBuilder buildFacetSort(String name, Sort sort, String searchContext) {
        switch (sort.getType()) {
            case "ScoredDate":
                final FieldDescriptor descriptor = ((Sort.SpecialSort.ScoredDate) sort).getDescriptor();
                final String sortDateField = Optional.ofNullable(descriptor)
                        .filter(FieldDescriptor::isSort)
                        .filter( field -> Date.class.isAssignableFrom(field.getType()) || ZonedDateTime.class.isAssignableFrom(field.getType()))
                        .map( field -> FieldUtil.getFieldName(descriptor, FieldUtil.Fieldname.UseCase.Sort, searchContext))
                        .orElse(((Sort.SpecialSort.ScoredDate) sort).getField());
                final Map<String, Object> parameters = new HashMap<>();
                parameters.put("field",sortDateField);
                parameters.put("a",4);
                parameters.put("m",1.9e-10);
                parameters.put("b",0.1);
                final Script painlessDateSort = new Script(
                        ScriptType.INLINE,
                        "painless",
                        "_score*(params.a/(params.m*(new Date().getTime()-doc[params.field].value.millis)+params.b))",
                        parameters);
                return AggregationBuilders.avg(name)
                        .script(painlessDateSort);
            default:
                throw  new SearchServerException(String
                        .format("Unable to parse Vind sort '%s' to ElasticSearch sorting: sort type not supported.",
                                sort.getType()));
        }
    }
}
