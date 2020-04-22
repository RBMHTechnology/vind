package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.result.FacetResults;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.facet.FacetValue;
import com.rbmhtechnology.vind.api.result.facet.IntervalFacetResult;
import com.rbmhtechnology.vind.api.result.facet.PivotFacetResult;
import com.rbmhtechnology.vind.api.result.facet.QueryFacetResult;
import com.rbmhtechnology.vind.api.result.facet.RangeFacetResult;
import com.rbmhtechnology.vind.api.result.facet.StatsFacetResult;
import com.rbmhtechnology.vind.api.result.facet.SubdocumentFacetResult;
import com.rbmhtechnology.vind.api.result.facet.TermFacetResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilters;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedHistogram;
import org.elasticsearch.search.aggregations.bucket.range.ParsedDateRange;
import org.elasticsearch.search.aggregations.bucket.range.ParsedRange;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultUtils {

    public static GetResult buildRealTimeGetResult(MultiGetResponse response, RealTimeGet query, DocumentFactory factory, long elapsedTime) {

        final List<Document> docResults = new ArrayList<>();

        final List<MultiGetItemResponse> results = Arrays.asList(response.getResponses());
        if(CollectionUtils.isNotEmpty(results)){
            docResults.addAll(results.stream()
                    .map(MultiGetItemResponse::getResponse)
                    .map(GetResponse::getSourceAsMap)
                    .map(jsonMap -> DocumentUtil.buildVindDoc(jsonMap ,factory,null))
                    .collect(Collectors.toList()));
        }
        final long nResults = docResults.size();
        return new GetResult(nResults,docResults,query,factory,elapsedTime).setElapsedTime(elapsedTime);
    }

    public static FacetResults buildFacetResults(Aggregations aggregations, DocumentFactory factory, Map<String,Facet> vindFacets, String searchContext) {

        final Map<String, Aggregation> aggregationsMap = Optional.ofNullable(aggregations)
                .orElse(new Aggregations(Collections.emptyList())).asMap();

        // Creating term facet results from terms aggregations
        final HashMap<FieldDescriptor, TermFacetResult<?>> termFacetResults = new HashMap<>();
        vindFacets.values().stream()
            .filter(facet -> facet.getType().equals("TermFacet"))
            .map(termFacet -> getTermFacetResults(
                    aggregationsMap.get(Stream.of(searchContext,termFacet.getFacetName()).filter(Objects::nonNull).collect(Collectors.joining("_"))),
                    (Facet.TermFacet)termFacet,
                    factory))
            .forEach(pair -> termFacetResults.put(pair.getKey(), pair.getValue()));

        // Creating Type facet result
        final TermFacetResult<String> typeFacetResults = getTypeFacetResults(aggregationsMap.get("DocType"));

        final HashMap<String, QueryFacetResult<?>> queryFacetResults = new HashMap<>();

        vindFacets.values().stream()
            .filter(facet -> facet.getType().equals("QueryFacet"))
            .map(queryFacet -> getQueryFacetResults(aggregationsMap.get(Stream.of(searchContext,queryFacet.getFacetName()).filter(Objects::nonNull).collect(Collectors.joining("_"))), (Facet.QueryFacet)queryFacet))
            .forEach(pair -> queryFacetResults.put(pair.getKey(), pair.getValue()));


        final HashMap<String, RangeFacetResult<?>> rangeFacetResults = new HashMap<>();
        vindFacets.values().stream()
            .filter(facet -> Arrays.asList("NumericRangeFacet","ZoneDateRangeFacet","UtilDateRangeFacet","DateMathRangeFacet").
                    contains(facet.getType()))
            .map(rangeFacet -> getRangeFacetResults(aggregationsMap.get(Stream.of(searchContext, rangeFacet.getFacetName()).filter(Objects::nonNull).collect(Collectors.joining("_"))), rangeFacet))
            .forEach(pair -> rangeFacetResults.put(pair.getKey(), pair.getValue()));

        HashMap<String, IntervalFacetResult> intervalFacetResults = new HashMap<>();
        vindFacets.values().stream()
            .filter(facet -> Arrays.asList("NumericIntervalFacet","ZoneDateTimeIntervalFacet","UtilDateIntervalFacet","ZoneDateTimeDateMathIntervalFacet","UtilDateMathIntervalFacet").
                    contains(facet.getType()))
            .map(intervalFacet -> getIntervalFacetResults(aggregationsMap.get(Stream.of(searchContext, intervalFacet.getFacetName()).filter(Objects::nonNull).collect(Collectors.joining("_"))), (Facet.IntervalFacet)intervalFacet))
            .forEach(pair -> intervalFacetResults.put(pair.getKey(), pair.getValue()));

//TODO:
        HashMap<String, StatsFacetResult<?>> statsFacetResults = new HashMap<>();
//        if(response.getFieldStatsInfo()!=null) {
//            statsFacetResults = getStatsFacetsResults(response.getFieldStatsInfo().entrySet(), facetsQuery);
//        }
//
        HashMap<String, List<PivotFacetResult<?>>> pivotFacetResults = new HashMap<>();
//        if(response.getFacetPivot()!=null) {
//
//            final Stream<Map.Entry<String, List<PivotField>>> pivotFacets = StreamSupport.stream(
//                    Spliterators.spliteratorUnknownSize(response.getFacetPivot().iterator(), Spliterator.ORDERED),
//                    false);
//            pivotFacets.forEach(pivotFacet -> {
//                final List<PivotFacetResult<?>> facet = pivotFacet.getValue().stream()
//                        .map(pivotField -> getPivotFacetResult(pivotField, response, factory,facetsQuery,searchContext))
//                        .collect(Collectors.toList());
//
//                pivotFacetResults.put(pivotFacet.getKey(), facet);
//            });
//        }
//
//        final Map<Integer, Integer> childCounts =  getSubdocumentCounts(response);
        final Collection<SubdocumentFacetResult> subDocumentFacet = Collections.emptyList();
//        if (Objects.nonNull(childCounts)) {
//            subDocumentFacet = childCounts.entrySet().stream()
//                    .map(e -> new SubdocumentFacetResult(e.getKey(), e.getValue()))
//                    .collect(Collectors.toList());
//        } else {
//            subDocumentFacet = Collections.emptyList();
//        }

        return new FacetResults(
                factory,
                termFacetResults,
                typeFacetResults,
                queryFacetResults,
                rangeFacetResults,
                intervalFacetResults,
                statsFacetResults,
                pivotFacetResults,
                subDocumentFacet);
    }

    private static Pair<String, QueryFacetResult<?>> getQueryFacetResults(Aggregation aggregation, Facet.QueryFacet queryFacet) {

        if(Objects.nonNull(aggregation)) {
            return Pair.of( queryFacet.getFacetName(),
                    new QueryFacetResult(queryFacet.getFilter(), new Long(((ParsedFilters) aggregation).getBucketByKey("0").getDocCount()).intValue()));
       }

        return Pair.of( queryFacet.getFacetName(),
                new QueryFacetResult<>(queryFacet.getFilter(),0));
    }

    private static TermFacetResult<String> getTypeFacetResults(Aggregation aggregation) {
        final TermFacetResult<String> result = new TermFacetResult<>();
        Optional.ofNullable(aggregation)
                .ifPresent(agg ->
                        ((ParsedStringTerms) aggregation).getBuckets().stream()
                                .map( termBucket -> new FacetValue<>( termBucket.getKey().toString(), termBucket.getDocCount()))
                                .forEach(result::addFacetValue));
        return result;
    }

    private static Pair<FieldDescriptor<?> ,TermFacetResult<?>> getTermFacetResults(Aggregation aggregation, Facet.TermFacet termFacet, DocumentFactory factory) {
        final TermFacetResult<?> result = new TermFacetResult<>();
        final FieldDescriptor<?> field = factory.getField(termFacet.getFieldName());
        Optional.ofNullable(aggregation)
                .ifPresent(agg ->
                        ((ParsedTerms) aggregation).getBuckets().stream()
                            .map( bucket -> {
                                final Object key = Number.class.isAssignableFrom(field.getType()) ?  bucket.getKeyAsNumber() : bucket.getKeyAsString();
                                return Pair.of(key, bucket.getDocCount());
                            })
                            .map( p -> new FacetValue(
                                    DocumentUtil.castForDescriptor(p.getKey(), field, FieldUtil.Fieldname.UseCase.Facet), p.getValue()))
                            .forEach(result::addFacetValue));

        return Pair.of(termFacet.getFieldDescriptor(), result);
    }

    private static Pair<String ,RangeFacetResult<?>> getRangeFacetResults(Aggregation aggregation, Facet rangeFacet) {

            switch (rangeFacet.getType()) {
                case "NumericRangeFacet":
                    final Facet.NumericRangeFacet numericRangeFacet = (Facet.NumericRangeFacet) rangeFacet;

                    final List<FacetValue> numericValues =  new ArrayList<>();

                    Optional.ofNullable(aggregation)
                            .ifPresent(agg -> ((ParsedHistogram)((ParsedRange) agg).getBuckets().get(0).getAggregations().getAsMap().get(rangeFacet.getFacetName())).getBuckets().stream()
                                .map(rangeBucket -> new FacetValue(
                                        DocumentUtil.castForDescriptor(rangeBucket.getKey(), numericRangeFacet.getFieldDescriptor(), FieldUtil.Fieldname.UseCase.Facet),
                                        rangeBucket.getDocCount()))
                                .forEach(numericValues::add));
                    return Pair.of(
                            rangeFacet.getFacetName(),
                            new RangeFacetResult(numericValues,numericRangeFacet.getStart(), numericRangeFacet.getEnd(), numericRangeFacet.getGap().longValue()));
                default:
                    final Facet.DateRangeFacet dateRangeFacet = (Facet.DateRangeFacet) rangeFacet;

                    final List<FacetValue> dateValues =  new ArrayList<>();

                    Optional.ofNullable(aggregation)
                            .ifPresent(agg -> ((ParsedDateHistogram)((ParsedDateRange) agg).getBuckets().get(0).getAggregations().getAsMap().get(dateRangeFacet.getFacetName())).getBuckets().stream()
                                    .map(rangeBucket -> new FacetValue(
                                            DocumentUtil.castForDescriptor(rangeBucket.getKey(), dateRangeFacet.getFieldDescriptor(), FieldUtil.Fieldname.UseCase.Facet),
                                            rangeBucket.getDocCount()))
                                    .forEach(dateValues::add));
                    return Pair.of(
                            rangeFacet.getFacetName(),
                            new RangeFacetResult(dateValues,dateRangeFacet.getStart(), dateRangeFacet.getEnd(), dateRangeFacet.getGap().longValue()));
            }

    }

    private static Pair<String, IntervalFacetResult> getIntervalFacetResults(Aggregation aggregation, Facet.IntervalFacet intervalFacet) {
        final List<FacetValue<String>> values = new ArrayList<>();
        Optional.ofNullable(aggregation)
                .ifPresent(agg -> ((ParsedRange) agg).getBuckets().stream()
                        .map(bucket -> new FacetValue( bucket.getKey(), bucket.getDocCount()))
                        .forEach(values::add));

        final IntervalFacetResult intervalFacetResult = new IntervalFacetResult(values);

        return Pair.of(intervalFacet.getFacetName(), intervalFacetResult);
    }

    public static HashMap<FieldDescriptor, TermFacetResult<?>> buildSuggestionResults(SearchResponse response, DocumentFactory factory, String context) {

        if(Objects.nonNull(response)
                && Objects.nonNull(response.getHits())
                && Objects.nonNull(response.getHits().getHits())){

            //TODO: if nested doc search is implemented

            final HashMap<FieldDescriptor, TermFacetResult<?>> suggestionValues = new HashMap<>();

            response.getAggregations().asList().stream()
                    .map(aggregation -> getTermFacetResults(aggregation, new Facet.TermFacet(factory.getField(aggregation.getName())), factory))
                    .forEach(pair -> suggestionValues.put(pair.getKey(), pair.getValue()));


            return suggestionValues;
        } else {
            throw new ElasticsearchException("Empty result from ElasticClient");
        }
    }

    public static HashMap<FieldDescriptor, TermFacetResult<?>> buildExperimentalSuggestionResults(SearchResponse response, DocumentFactory factory, String context) {

        if(Objects.nonNull(response)
                && Objects.nonNull(response.getHits())
                && Objects.nonNull(response.getHits().getHits())){
            //TODO: if nested doc search is implemented
            //final Map<String,Integer> childCounts = SolrUtils.getChildCounts(response);

            final HashMap<FieldDescriptor, TermFacetResult<?>> suggestionValues = new HashMap<>();


            Arrays.stream(response.getHits().getHits())
                    .map(SearchHit::getHighlightFields)
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .forEach(e -> addSuggestionValue(suggestionValues,e.getKey(),e.getValue().fragments()[0].string(),factory, context));

            return suggestionValues;
        } else {
            throw new ElasticsearchException("Empty result from ElasticClient");
        }
    }

    private static void addSuggestionValue(HashMap<FieldDescriptor, TermFacetResult<?>> suggestionValues,
                                    String suggestionFieldName,
                                    String suggestedValue,
                                    DocumentFactory factory,
                                    String context) {

        final String fieldName = FieldUtil.getSourceFieldName(suggestionFieldName.replaceAll(".suggestion", ""),context);
        final FieldDescriptor<?> suggestionField = factory.getField(fieldName);
        final String cleanValue = suggestedValue.replaceAll("</*em>", "");
        final TermFacetResult<?> termSuggestions = Optional.ofNullable(suggestionValues.get(suggestionField))
                .orElse(new TermFacetResult(Collections.singletonList(new FacetValue(cleanValue, 0))));


        final List<FacetValue> updatedFacetValues = new ArrayList<>();
        final Optional<FacetValue> updatedValue = termSuggestions.getValues().stream()
                .map((FacetValue<?> oldFacetValue) -> {
                    if (oldFacetValue.getValue().toString().equals(cleanValue)) {
                        final long updatedCount = oldFacetValue.getCount() + 1;
                        final FacetValue updatedFacetValue = new FacetValue(cleanValue, updatedCount);
                        updatedFacetValues.add(updatedFacetValue);
                        return updatedFacetValue;
                    } else {
                        updatedFacetValues.add(oldFacetValue);
                        return oldFacetValue;
                    }
                })
                .filter((FacetValue oldFacetValue) -> oldFacetValue.getValue().toString().equals(cleanValue))
                .findAny();

        if(!updatedValue.isPresent()){
            updatedFacetValues.add(new FacetValue(cleanValue, 1));
        }

        suggestionValues.put(suggestionField,new TermFacetResult(updatedFacetValues));
    }
}
