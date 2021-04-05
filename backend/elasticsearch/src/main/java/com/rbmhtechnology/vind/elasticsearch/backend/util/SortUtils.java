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
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SortUtils {
    public static final String NUMBER_OF_MATCHING_TERMS_SORT = "NumberOfMatchingTermsSort";

    protected static SortBuilder buildSort(Sort sort, FulltextSearch search, DocumentFactory factory,
                                           String searchContext, List<String> indexFootPrint) {
        switch (sort.getType()) {
            case "SimpleSort":
                final String simpleSortFieldName = ((Sort.SimpleSort) sort).getField();
                final FieldDescriptor<?> simpleSortField = factory.getField(simpleSortFieldName);
                final String sortFieldName = Optional.ofNullable(simpleSortField)
                        .filter(FieldDescriptor::isSort)
                        .map(descriptor ->
                                FieldUtil.getFieldName(descriptor, FieldDescriptor.UseCase.Sort, searchContext, indexFootPrint)
                                .orElseThrow(() ->
                                        new SearchServerException("The field '" + simpleSortFieldName + "' is not set for context ["+searchContext+"]")))
                        .orElse(simpleSortFieldName);
                return SortBuilders
                        .fieldSort(sortFieldName)
                        .unmappedType(getUnmappedType(sortFieldName)) //TODO should be set correctly for cross index search usecase
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            case "DescriptorSort":
                final String descriptorFieldName =
                        FieldUtil.getFieldName(((Sort.DescriptorSort) sort).getDescriptor(), FieldDescriptor.UseCase.Sort, searchContext, indexFootPrint)
                        .orElseThrow(() ->
                                new RuntimeException("The field '" + ((Sort.DescriptorSort) sort).getDescriptor().getName() + "' is not set as sortable"));
                return SortBuilders
                        .fieldSort(descriptorFieldName)
                        .unmappedType(getUnmappedType(descriptorFieldName)) //TODO should be set correctly for cross index search usecase
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            case "DistanceSort":
                Optional.ofNullable(search.getGeoDistance())
                        .orElseThrow(() -> new SearchServerException("Sorting by distance requires a geodistance set"));
                final String distanceFieldName =
                        FieldUtil.getFieldName(search.getGeoDistance().getField(), null, searchContext, indexFootPrint)
                                .orElseThrow(() -> new SearchServerException("Sorting by distance requires a geodistance set"));
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
                        .map( field -> FieldUtil.getFieldName(descriptor, FieldDescriptor.UseCase.Sort, searchContext, indexFootPrint)
                                .orElseThrow(() ->
                                new SearchServerException("The field '" + descriptor.getName() + "' is not set for context ["+searchContext+"]")))
                        .orElse(((Sort.SpecialSort.ScoredDate) sort).getField());
                final Map<String, Object> parameters = new HashMap<>();
                parameters.put("field",sortDateField);
                parameters.put("a",4);
                parameters.put("m",1.9e-10);
                parameters.put("b",0.1);
                parameters.put("n",0.5);
                final Script painlessDateSort = new Script(
                        ScriptType.INLINE,
                        "painless",
                        "doc[params.field].empty ? _score*params.n : _score*(params.a/(params.m*(new Date().getTime()-doc[params.field].value.millis)+params.b))",
                        parameters);
                return SortBuilders
                        .scriptSort(painlessDateSort, ScriptSortBuilder.ScriptSortType.NUMBER)
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            case "Score":
                return SortBuilders
                        .scoreSort()
                        .order(SortOrder.valueOf(sort.getDirection().name().toUpperCase()));
            default:
                throw new SearchServerException(String
                        .format("Unable to parse Vind sort '%s' to ElasticSearch sorting: sort type not supported.",
                                sort.getType()));
        }
    }

    private static String getUnmappedType(String fieldName) {
        if(FieldUtil.ID.equals(fieldName)) {
            return "keyword";
        }
        if (fieldName.contains("_")) {
            final String type = fieldName.split("_")[1];
            switch (type) {
                case "int": return "integer";
                case "long": return "long";
                case "float": return "float";
                case "string":
                case "path":
                    return "keyword";
                case "boolean": return "boolean";
                case "date": return "date";
                case "location": return "geo_point";
                case "binary": return "binary";
                default:throw new RuntimeException("Cannot get type for fieldName '" + fieldName + "'");
            }
        } else
            throw new RuntimeException("Cannot get type for fieldName '" + fieldName + "'");
    }

    protected static AggregationBuilder buildFacetSort(String name, Sort sort, String searchContext, List<String> indexFootPrint) {
        switch (sort.getType()) {
            case "ScoredDate":
                final FieldDescriptor descriptor = ((Sort.SpecialSort.ScoredDate) sort).getDescriptor();
                final String sortDateField = Optional.ofNullable(descriptor)
                        .filter(FieldDescriptor::isSort)
                        .filter( field -> Date.class.isAssignableFrom(field.getType()) || ZonedDateTime.class.isAssignableFrom(field.getType()))
                        .map( field -> FieldUtil.getFieldName(descriptor, FieldDescriptor.UseCase.Sort, searchContext, indexFootPrint)
                                .orElseThrow(() ->
                                new SearchServerException("The field '" + descriptor + "' is not set for context ["+searchContext+"]")))
                        .orElse(((Sort.SpecialSort.ScoredDate) sort).getField());
                final Map<String, Object> parameters = new HashMap<>();
                parameters.put("field",sortDateField);
                parameters.put("a",4);
                parameters.put("m",1.9e-10);
                parameters.put("b",0.1);
                parameters.put("n",0.5);
                final Script painlessDateSort = new Script(
                        ScriptType.INLINE,
                        "painless",
                        "doc[params.field].empty ? _score*params.n : _score*(params.a/(params.m*(new Date().getTime()-doc[params.field].value.millis)+params.b))",
                        parameters);
                return AggregationBuilders.avg(name)
                        .script(painlessDateSort);
            default:
                throw  new SearchServerException(String
                        .format("Unable to parse Vind sort '%s' to ElasticSearch sorting: sort type not supported.",
                                sort.getType()));
        }
    }

    protected static AggregationBuilder buildSuggestionSort(String name,
                                                            Sort sort,
                                                            String searchContext,
                                                            List<String> indexFootPrint,
                                                            String input) {
        switch (sort.getType()) {
            case NUMBER_OF_MATCHING_TERMS_SORT:
                return setNumberOfMatchingTermsSort(
                        name,
                        (Sort.SpecialSort.NumberOfMatchingTermsSort) sort,
                        searchContext,
                        indexFootPrint,
                        input);
            default:
                throw new SearchServerException(String
                        .format("Unable to parse Vind sort '%s' to ElasticSearch sorting: sort type not supported.",
                                sort.getType()));
        }
    }

    private static MaxAggregationBuilder setNumberOfMatchingTermsSort(String name,
                                                                      Sort.SpecialSort.NumberOfMatchingTermsSort sort,
                                                                      String searchContext,
                                                                      List<String> indexFootPrint,
                                                                      String input) {
        final FieldDescriptor descriptor = sort.getDescriptor();
        final String matchingField = Optional.ofNullable(descriptor)
                .filter(FieldDescriptor::isSort)
                .map(field -> FieldUtil.getFieldName(descriptor, FieldDescriptor.UseCase.Sort, searchContext, indexFootPrint)
                        .orElseThrow(() ->
                                new SearchServerException("The field '" + descriptor.getName() + "' is not set for context ["+ searchContext +"]")))
                .orElse(sort.getField());
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("field",matchingField);
        parameters.put("input", Arrays.asList(input.split(" ")));
        final Script painlessMatchingSort = new Script(
                ScriptType.INLINE,
                "painless",
                "long sum = 0;for(String value: doc[params.field]){long subSum = 0;for(String searchTerm: params.input){subSum+=value.toLowerCase().contains(searchTerm.toLowerCase()) ? 1 : 0;}if(subSum>sum){sum=subSum;}}return sum;",
                parameters
        );
        return AggregationBuilders.max(name).script(painlessMatchingSort);
    }
}
