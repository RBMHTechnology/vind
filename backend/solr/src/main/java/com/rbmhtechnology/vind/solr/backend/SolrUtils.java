package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.FacetResults;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.api.result.facet.*;
import com.rbmhtechnology.vind.model.*;
import com.rbmhtechnology.vind.model.value.LatLng;
import com.rbmhtechnology.vind.solr.backend.SolrUtils.Fieldname.UseCase;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.response.*;
import org.apache.solr.client.solrj.response.IntervalFacet;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.DateUtil;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.rbmhtechnology.vind.api.query.facet.Facet.*;
import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static com.rbmhtechnology.vind.solr.backend.SolrUtils.Fieldname.UseCase.*;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 22.06.16.
 */
public class SolrUtils {
    private static final Logger log = LoggerFactory.getLogger(SolrSearchServer.class);

    private static final String INTERNAL_FIELD_PREFIX = String.format("%s(%s|%s)(%s|%s|%s|%s|%s|%s|%s|%s)",
            Fieldname._DYNAMIC,
            Fieldname._MULTI,Fieldname._SINGLE,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(),Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(),Fieldname.Type.BINARY.getName(),Fieldname.Type.LOCATION.getName());

    private static final String INTERNAL_FACET_FIELD_PREFIX = String.format("%s(%s|%s)(%s)?%s(%s|%s|%s|%s|%s|%s|%s)",
            Fieldname._DYNAMIC,
            Fieldname._MULTI,Fieldname._SINGLE,
            Fieldname._STORED,
            Fieldname._FACET,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(),Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(),Fieldname.Type.LOCATION.getName());

    private static final String INTERNAL_SCOPE_FACET_FIELD_PREFIX = String.format("%s(%s|%s)(%s)?(%s|%s|%s)(%s|%s|%s|%s|%s|%s|%s)",
            Fieldname._DYNAMIC,
            Fieldname._MULTI,Fieldname._SINGLE,
            Fieldname._STORED,
            Fieldname._FACET,Fieldname._SUGGEST,Fieldname._FILTER,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(),Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(),Fieldname.Type.LOCATION.getName());

    private static final String INTERNAL_SUGGEST_FIELD_PREFIX = String.format("%s(%s|%s)(%s)?%s(%s|%s|%s|%s|%s|%s|%s|%s)",
            Fieldname._DYNAMIC,
            Fieldname._MULTI,Fieldname._SINGLE,
            Fieldname._STORED,
            Fieldname._SUGGEST,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(),Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(),Fieldname.Type.LOCATION.getName(), Fieldname.Type.ANALYZED.getName());

    private static final String INTERNAL_CONTEXT_PREFIX = "(%s_)?";

    public static Map<String,Integer> getChildCounts(SolrResponse response) {

        //check if there are subdocs
        if (Objects.nonNull(response.getResponse())) {
            final Object subDocumentFacetResult = response.getResponse().get("facets");
            if (Objects.nonNull(subDocumentFacetResult)) {
                Map<String,Integer> childCounts = new HashMap<>();

                log.debug("Parsing subdocument facet result from JSON ");

                final int facetCount = (int) ((SimpleOrderedMap) subDocumentFacetResult).get("count");
                if (facetCount > 0 && Objects.nonNull(((SimpleOrderedMap) subDocumentFacetResult).get("parent_facet"))) {
                    final List<SimpleOrderedMap> parentDocs = (ArrayList) ((SimpleOrderedMap) ((SimpleOrderedMap) subDocumentFacetResult).get("parent_facet")).get("buckets");
                    childCounts = parentDocs.stream().collect(Collectors.toMap(p -> (String) p.get("val"), p -> ((Integer) ((SimpleOrderedMap) p.get("children_facet")).get("count"))));
                }

                return childCounts;
            }
        }

        return null;
    }

    public static Map<Integer,Integer> getSubdocumentCounts(SolrResponse response) {

        //check if there are subdocs
        if (Objects.nonNull(response.getResponse())) {
            final Object subDocumentFacetResult = response.getResponse().get("facets");
            if (Objects.nonNull(subDocumentFacetResult)) {
                Map<Integer,Integer> childCounts = new HashMap<>();

                log.debug("Parsing subdocument facet result from JSON ");

                final int facetCount = (int) ((SimpleOrderedMap) subDocumentFacetResult).get("count");
                if (facetCount > 0 && Objects.nonNull(((SimpleOrderedMap) subDocumentFacetResult).get("childrenCount"))) {
                    final SimpleOrderedMap parentDocs = ((SimpleOrderedMap) ((SimpleOrderedMap) subDocumentFacetResult).get("childrenCount"));
                    final Integer childCount = (Integer) parentDocs.get("count");
                    final Integer parentCount;
                    if(childCount > 0) {
                        parentCount =(Integer)((SimpleOrderedMap)((List)((SimpleOrderedMap)parentDocs.get("parentFilteredCount")).get("buckets")).get(0)).get("count");
                    } else {
                        parentCount = 0;
                    }
                    childCounts.put(parentCount, childCount);
                }

                return childCounts;
            }
        }

        return null;
    }

    public static final class Query {

        public static String serializeFacetFilter(Filter filter, DocumentFactory factory, String searchContext, boolean strict) {

            final SolrFilterSerializer serializer = new SolrFilterSerializer(factory, strict);
            final String serializedFilters = serializer.serialize(filter, searchContext);
            final String typeFilterString = Fieldname.TYPE + ":" + factory.getType();
            return serializedFilters.equals("")?
                    typeFilterString :
                    "(" + String.join(" AND ", typeFilterString,"("+serializedFilters+")") + ")";
        }

        public static String buildFilterString(Filter filter, DocumentFactory factory,String searchContext, boolean strict) {
          return buildFilterString(filter, factory, (DocumentFactory)null, searchContext, strict);
        }

        public static String buildFilterString(Filter filter, DocumentFactory factory,DocumentFactory childFactory,String searchContext, boolean strict) {

            final String serializedFilters = new ChildrenFilterSerializer(factory,childFactory,searchContext, strict, false).serialize(filter);
            final String typeFilterString = "+_type_:" + factory.getType();
            if(StringUtils.isNotBlank(serializedFilters)) {
                return String.join(" +", typeFilterString, serializedFilters);
            } else {
                return typeFilterString;
            }
        }

        public static void buildFilterString(Filter filter, DocumentFactory factory,SolrQuery query,String searchContext, boolean strict) {
            buildFilterString(filter, factory, null, query, searchContext, strict);
        }
        public static void buildFilterString(Filter filter, DocumentFactory factory,DocumentFactory childFactory,SolrQuery query,String searchContext, boolean strict) {
           // query.add(CommonParams.FQ,"_type_:"+factory.getType());
            final String serialize = new ChildrenFilterSerializer(factory,childFactory,searchContext, strict, false).serialize(filter);
            if(StringUtils.isNotBlank(serialize)) {
                query.add(CommonParams.FQ, serialize);
            }
        }

        public static String buildSortString(FulltextSearch search, List<Sort> sortList, DocumentFactory factory) {
            return sortList.stream().map(sort -> {
                if (sort instanceof Sort.SimpleSort) {
                    Sort.SimpleSort ssort = (Sort.SimpleSort) sort;
                    FieldDescriptor descriptor = factory.getField(ssort.getField());
                    if (descriptor != null) {
                        if (!descriptor.isSort()) {
                            log.error("Cannot sort on field '{}'. The field is not defined as sortable.", ssort.getField());
                            throw new RuntimeException("Cannot sort on field " + ssort.getField());
                        }
                        return Fieldname.getFieldname(descriptor, Sort, search.getSearchContext()) + " " + ssort.getDirection();

                    } else {
                        return ssort.getField() + " " + ssort.getDirection();
                    }
                } else if (sort instanceof Sort.SpecialSort.ScoredDate) {
                    Sort.SpecialSort.ScoredDate ssort = (Sort.SpecialSort.ScoredDate) sort;
                    return "score " + ssort.getDirection();//TODO this is wrong isn't it?
                } else if (sort instanceof Sort.SpecialSort.DistanceSort) {
                    Sort.SpecialSort.DistanceSort ssort = (Sort.SpecialSort.DistanceSort) sort;
                    if (search.getGeoDistance() == null) {
                        throw new RuntimeException("Sorting by distance requires a geodistance set");
                    }
                    return "geodist() " + ssort.getDirection();
                } else {
                    final Sort.DescriptorSort s = (Sort.DescriptorSort) sort;
                    final String fieldname = Fieldname.getFieldname(s.getDescriptor(), Sort, search.getSearchContext());
                    if (fieldname == null) {
                        throw new RuntimeException("The field '"+ s.getDescriptor().getName()+"' is not set as sortable");
                    }
                    return fieldname + " " + s.getDirection();

                }
            }).collect(Collectors.joining(", "));
        }

        //TODO sorting stuff is a mess
        public static String buildBoostFunction(List<Sort> sortList, String searchContext) {
            //String bf =
            return sortList.stream().map(sort -> {
                if (sort instanceof Sort.SpecialSort.ScoredDate) {
                    Sort.SpecialSort.ScoredDate ssort = (Sort.SpecialSort.ScoredDate) sort;
                    return String.format("recip(abs(ms(NOW/HOUR,%s)),3.16e-11,1,.1)", Fieldname.getFieldname(ssort.getDescriptor(), Stored, searchContext));
                } else return null;
            }).filter(Objects::nonNull).collect(Collectors.joining(" "));
        }

        public static String buildQueryFieldString(Collection<FieldDescriptor<?>> fulltext, String searchContext) {
            return fulltext.stream()
                    .map(descriptor ->
                                    SolrUtils.Fieldname.getFieldname(descriptor, Fulltext, searchContext) +
                                            "^" +
                                            descriptor.getBoost()
                    )
                    .collect(Collectors.joining(" "));
        }

        public static String[] buildFacetFieldList(Map<String, Facet> facets, DocumentFactory factory, DocumentFactory childFactory, String searchContext) {
            final List<String> termFacetQuery = facets.values().stream()
                    .filter(facet -> facet instanceof TermFacet)
                    .map(facet -> (TermFacet) facet)
                    .map(facet -> {
                        if(Objects.nonNull(facet.getFieldDescriptor())) {
                            return facet;
                        } else {
                            FieldDescriptor<?> field = factory.getField(facet.getFieldName());
                            if(Objects.isNull(field) && Objects.nonNull(childFactory)) {
                                field = childFactory.getField(facet.getFieldName());
                            }
                            return new TermFacet(field);
                        }
                    })
                    .map(facet -> Fieldname.getFieldname(facet.getFieldDescriptor(), UseCase.valueOf(facet.getScope().name()), searchContext))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            final List<String> typeFacet = facets.values().stream()
                    .filter(facet -> facet instanceof TypeFacet)
                    .map(facet -> Fieldname.TYPE)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            termFacetQuery.addAll(typeFacet);
            return termFacetQuery.stream().toArray(String[]::new);
        }

        public static HashMap<String, Object> buildJsonTermFacet(Map<String, Facet> facets, int facetLimit, DocumentFactory factory, DocumentFactory childFactory, String searchContext) {
            final List<HashMap<String, String>> termFacetQuery = facets.entrySet().stream()
                    .filter(facet -> facet.getValue() instanceof TermFacet)
                    //.map(facet -> facet.setValue((Facet.TermFacet) facet.getValue()))
                    .map(facet -> {
                        final HashMap<String, String> termFacet = new HashMap<>();
                        termFacet.put("type","terms");
                        final TermFacet value = (TermFacet) facet.getValue();
                        FieldDescriptor<?> field = factory.getField(value.getFieldName());
                        if(Objects.isNull(field) && Objects.nonNull(childFactory)) {
                            field = childFactory.getField(value.getFieldName());
                            termFacet.put("domain",
                                    "{blockChildren:\"" + Fieldname.TYPE +":" +factory.getType() + "\"}");
                        }

                        final UseCase useCase = UseCase.valueOf(facet.getValue().getScope().name());
                        final String fieldName = Fieldname.getFieldname(field, useCase, searchContext);

                        if(StringUtils.isEmpty(fieldName)) {
                            log.warn("Field {} is not set for faceting", fieldName);
                            return null;
                        }
                        termFacet.put("field", fieldName);
                        termFacet.put("limit", String.valueOf(facetLimit));

                        return termFacet;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            final List<HashMap<String, String>> typeFacet = facets.values().stream()
                    .filter(facet -> facet instanceof TypeFacet)
                    .map(facet ->{
                        final HashMap<String, String> termFacet = new HashMap<>();
                        termFacet.put("type","terms");
                        termFacet.put("field", Fieldname.TYPE);
                        termFacet.put("limit", String.valueOf(facetLimit));
                        return termFacet;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            termFacetQuery.addAll(typeFacet);

            final HashMap<String, Object> jsonFieldFacet = new HashMap<>();
            termFacetQuery.stream().forEach( facet -> jsonFieldFacet.put(facet.get("field"),facet));
            return jsonFieldFacet;
        }

        public static String buildSolrQueryValue(Object o){
            if(o != null){
                if(ZonedDateTime.class.isAssignableFrom(o.getClass())) {
                    return ((ZonedDateTime)o).format(DateTimeFormatter.ISO_INSTANT);
                }
                if(Date.class.isAssignableFrom(o.getClass())) {
                    return DateUtil.getThreadLocalDateFormat().format((Date)o);
                }
                if(DateMathExpression.class.isAssignableFrom(o.getClass())) {
                    //TODO: Do not delegate on the toString DateMath, a solr specific parse would be better
                    DateMathExpression dateMath = (DateMathExpression) o;
                    return dateMath.toString();
                }
                if(ByteBuffer.class.isAssignableFrom(o.getClass())) {
                    return new String (((ByteBuffer) o).array());
                }
                return o.toString(); //TODO check if this is this correct

            }
            return "";
        }

        public static  String buildSolrTimeGap(Long duration){
            String solrGap = "+"+String.valueOf(duration)+ "MILLISECOND";
            return solrGap;
        }

        public static String buildSolrFieldAlias(String field, String alias){
            return String.join(":", alias, field);
        }

        public static String buildSolrFacetTags(String ... keys){
            return StringUtils.join("tag='",StringUtils.join(keys,','),"'");
        }

        public static String buildSolrFacetKey(String s){
            if(s == null || s.contains(" ")) throw new RuntimeException("key string may not be empty or contain blanks");
            return String.format("{!key=%s}",s);
        }

        public static String buildSolrFacetCustomName(String field, Facet facet){
            return StringUtils.join("{!",buildSolrFacetTags(facet.getTagedPivots())," ex=dt key='", facet.getFacetName(), "'}", field);
        }

        public static String buildSolarPivotCustomName(String name,String... fields){
            return StringUtils.join("{!ex=dt key=", name, "}",  StringUtils.join(fields,','));
        }

        public static <T extends Facet> String buildSolrPivotSubFacetName(String name, String... fields){

            return StringUtils.join("{!query='", name,"' stats='", name,"' range='", name,"' ","ex=dt key='", name, "'}", StringUtils.join(fields,','));
        }

        public static <T> String buildSolrTermsQuery(List<T> values, FieldDescriptor<T> field, Scope scope, String context) {
            final String prefixQuery =
                    "{!terms f=" + Fieldname.getFieldname(field,UseCase.valueOf(scope.name()), context) + "}";
            final String  query = values.stream()
                    .map( v -> FieldValue.getStringFieldValue(v, field))
                    .collect(Collectors.joining(","));
            return  prefixQuery + query;
        }

        public static String buildSolrStatsQuery(String solrfieldName, StatsFacet stats){
            String query = buildSolrFacetCustomName(solrfieldName,stats);

            String statsQuery = "{!";
            if(stats.getMin()) {
                statsQuery += "min=true ";
            }
            if(stats.getMax()) {
                statsQuery += "max=true ";
            }
            if(stats.getSum()) {
                statsQuery += "sum=true ";
            }
            if(stats.getCount()) {
                statsQuery += "count=true ";
            }
            if(stats.getMissing()) {
                statsQuery += "missing=true ";
            }
            if(stats.getSumOfSquares()) {
                statsQuery += "sumOfSquares=true ";
            }
            if(stats.getMean()) {
                statsQuery += "mean=true ";
            }
            if(stats.getStddev()) {
                statsQuery += "stddev=true ";
            }
            if(stats.getPercentiles().length > 0) {

                statsQuery += "percentiles='"+ StringUtils.join(stats.getPercentiles(),',')+"' ";
            }
            if(stats.getDistinctValues()) {
                statsQuery += "distinctValues=true ";
            }
            if(stats.getCountDistinct()) {
                statsQuery += "countDistinct=true ";
            }
            if(stats.getCardinality()) {
                statsQuery += "cardinality=true ";
            }

            query = query.replace("{!", statsQuery);
            return query;
        }
        public static Object buildUpdateQuery(FieldDescriptor field, Object value){
            return SolrUtils.Result.castForDescriptor(value, field);
        }

        public static String buildSubdocumentFacet(FulltextSearch search, DocumentFactory factory,String searchContext) {

            final Optional<String> facetOptional = search.getFacets().values().stream()
                    .filter(facet -> SubdocumentFacet.class.isAssignableFrom(facet.getClass()))
                    .map(genericFacet -> (SubdocumentFacet) genericFacet)
                    .map(facet -> {
                        final String type = facet.getFacetName();
                        String filter;
                        //final String childrenFilterSerialized;
                        filter = search.getChildrenSearches().stream()
                                .filter(FulltextSearch::hasFilter)
                                .map( childrenSearch -> {
                                    final String childrenFilterSerialized = serializeFacetFilter(childrenSearch.getFilter(), search.getChildrenFactory(), searchContext, search.getStrict()).replaceAll("\"", "\\\\\"");;
                                    return "(" +childrenFilterSerialized + " AND " + StringEscapeUtils.escapeJson(search.getSearchString()) +")";
                                })
                                .collect(Collectors.joining(" OR "));

                        if(StringUtils.isBlank(filter)) {
                            filter = StringEscapeUtils.escapeJson(search.getSearchString());
                        }

                        filter = "{!edismax}" + filter;
                        return String.format(
                                "{" +//TODO this should be done by an inner component (paging!!)
                                    "parent_facet:{" +
                                        "type:terms," +
                                        "field:%s," +
                                        "limit:999999999," +
                                        "mincount:1," +
                                        "sort:{index:asc}," +
                                        "domain:{blockParent:\"%s:%s\"}," +
                                        "facet:{" +
                                            "children_facet:{" +
                                                "type:query," +
                                                "q:\"%s\"," +
                                                "domain:{blockChildren:\"%s:%s\"}" +
                                            "}" +
                                        "}" +
                                    "}," +

                                    "childrenCount:{" +
                                        "type:query," +
                                        "mincount:1," +
                                        "q:\"%s\"," +
                                        "domain:{blockChildren:\"%s:%s\"}," +
                                        "facet:{" +
                                            "parentFilteredCount:{" +
                                                "type:terms," +
                                                "field: _type_," +
                                                "domain:{blockParent :\"%s:%s\"}" +
                                            "}" +
                                        "}" +
                                    "}" +

                                "}", Fieldname.ID, Fieldname.TYPE, type, filter, Fieldname.TYPE, type, filter, Fieldname.TYPE, type, Fieldname.TYPE, type);
                    }).findAny();
            return facetOptional.orElse(null);
        }
    }

    public static final class FieldValue {
        public static Object getFieldCaseValue(Object value, FieldDescriptor descriptor, UseCase useCase) {
            if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
                ComplexFieldDescriptor complexDescriptor = (ComplexFieldDescriptor) descriptor;
                if(value!=null) {
                    if(Object[].class.isAssignableFrom(value.getClass())){
                        return getFieldCaseValue(Arrays.asList((Object[]) value), descriptor, useCase);
                    }
                    if(Collection.class.isAssignableFrom(value.getClass()) && !useCase.equals(Sort)){
                         List<Object> values = (List<Object>) ((Collection) value).stream()
                                .map(o -> getFieldCaseValue(o, descriptor, useCase))
                                .collect(Collectors.toList());

                        if (values.stream().allMatch( o -> Collection.class.isAssignableFrom(o.getClass()))) {
                            values =  values.stream()
                                    .map(o -> (List<Collection<Object>>)o)
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toList());
                        }

                        return values;
                    }
                    switch (useCase) {
                        case Fulltext: {
                                if(complexDescriptor.getFullTextFunction() != null) {
                                    return complexDescriptor.getFullTextFunction().apply(value);
                                } else {
                                    return null;
                                }
                            }
                        case Facet: {
                            if(complexDescriptor.getFacetFunction() != null) {
                                return complexDescriptor.getFacetFunction().apply(value);
                            } else {
                                return null;
                            }
                        }
                        case Suggest:{
                            if(complexDescriptor.getSuggestFunction() != null) {
                                return complexDescriptor.getSuggestFunction().apply(value);
                            } else {
                                return null;
                            }
                        }
                        case Stored:{
                            if(complexDescriptor.getStoreFunction() != null) {
                                return complexDescriptor.getStoreFunction().apply(value);
                            } else {
                                return null;
                            }
                        }
                        case Sort:{
                            if (complexDescriptor.isMultiValue()) {
                                final MultiValuedComplexField multiField = (MultiValuedComplexField) complexDescriptor;
                                if(multiField.getSortFunction() != null) {
                                    return multiField.getSortFunction().apply(value);
                                } else {
                                    return null;
                                }
                            } else {
                                final SingleValuedComplexField singleField = (SingleValuedComplexField) complexDescriptor;
                                if(singleField.getSortFunction() != null) {
                                    return singleField.getSortFunction().apply(value);
                                } else {
                                    if (singleField.isStored()) {
                                        return getFieldCaseValue(value, singleField, Stored);
                                    }
                                    return null;
                                }
                            }

                        }
                        case Filter:{
                            if(complexDescriptor.isAdvanceFilter() && Objects.nonNull(complexDescriptor.getFacetType())) {
                                return complexDescriptor.getAdvanceFilter().apply(value);
                            } else {
                                return null;
                            }

                        }
                        default: {
                            try {
                                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                                ObjectOutputStream oos = new ObjectOutputStream(bytesOut);
                                oos.writeObject(value);
                                oos.flush();
                                byte[] bytes = bytesOut.toByteArray();
                                bytesOut.close();
                                oos.close();
                                return bytes;
                            } catch (IOException e) {
                                //TODO:
                                throw new RuntimeException("Unable to serialize complex Object",e);
                            }
                        }
                    }
                } else {
                    return value; //TODO: Throw exception?
                }
            } else {
                if (value != null && useCase.equals(Sort) && descriptor.isSort() && descriptor.isMultiValue()
                    && (Collection.class.isAssignableFrom(value.getClass()) || value instanceof Object[] )) {
                    return  ((MultiValueFieldDescriptor)descriptor).getsortFunction().apply(value);
                } else {
                    return value;
                }
            }
        }
        public static String getStringFieldValue(Object value, FieldDescriptor<?> field) {
            if (value instanceof ZonedDateTime) {
             return DateTimeFormatter.ISO_INSTANT.format((ZonedDateTime) value);
            }
            if (value instanceof Date) {
                final DateFormat df = new SimpleDateFormat("YYYY-MM-DDThh:mm:ssZ");
                return df.format((Date) value);
            }
            return value.toString();

        }
    }


    public static final class Fieldname {


        public enum UseCase {
            Facet,
            Fulltext,
            Stored,
            Suggest,
            Sort,
            Filter
        }

        private enum Type {
            DATE("date_"),
            STRING("string_"),
            INTEGER("int_"),
            LONG("long_"),
            NUMBER("float_"),
            LOCATION("location_"),
            BOOLEAN("boolean_"),
            BINARY("binary_"),
            ANALYZED("analyzed_");

            private String name;

            Type(String name) {
                this.name = name;
            }

            public String getName() {
                return this.name;
            }

            public static Type getFromClass(Class clazz) {
                if (Objects.nonNull(clazz)) {
                    if (Integer.class.isAssignableFrom(clazz)) {
                        return INTEGER;
                    } else if (Long.class.isAssignableFrom(clazz)) {
                        return LONG;
                    } else if (Number.class.isAssignableFrom(clazz)) {
                        return NUMBER;
                    } else if (Boolean.class.isAssignableFrom(clazz)) {
                        return BOOLEAN;
                    } else if (ZonedDateTime.class.isAssignableFrom(clazz)) {
                        return DATE;
                    } else if (Date.class.isAssignableFrom(clazz)) {
                        return DATE;
                    } else if (LatLng.class.isAssignableFrom(clazz)) {
                        return LOCATION;
                    } else if (ByteBuffer.class.isAssignableFrom(clazz)) {
                        return BINARY;
                    } else if (CharSequence.class.isAssignableFrom(clazz)) {
                        return STRING;
                    } else {
                        return BINARY;
                    }
                } else return null;
            }
        }

        public static final String ID = "_id_";
        public static final String TYPE = "_type_";
        public static final String SCORE = "score";
        public static final String DISTANCE = "_distance_";
        public static final String TEXT = "text";
        public static final String FACETS = "facets";

        private static final String _DYNAMIC = "dynamic_";
        private static final String _STORED = "stored_";
        private static final String _MULTI = "multi_";
        private static final String _SINGLE = "single_";
        private static final String _FACET = "facet_";
        private static final String _SUGGEST = "suggest_";
        private static final String _FILTER = "filter_";

        private static final String _SORT = "sort_";

        public static Set<String> getFieldnames(FieldDescriptor descriptor, String context) {
            Set<String> fieldsnames = new HashSet<>();
            for(UseCase useCase : UseCase.values()) {
                CollectionUtils.addIgnoreNull(fieldsnames, getFieldname(descriptor, useCase, context));
            }
            return fieldsnames;
        }

        public static String getFieldname(FieldDescriptor descriptor, UseCase useCase, String context) {

            if (Objects.isNull(descriptor)){
                log.warn("Trying to get name of null field descriptor.");
                return null;
            }

            final String contextPrefix;
            if (Objects.isNull(context) || !descriptor.isContextualized()) {
                contextPrefix = "";
            } else {
                contextPrefix = context + "_";
            }

            String fieldName = _DYNAMIC;

            if(descriptor.isMultiValue()) {
                fieldName = fieldName.concat(_MULTI);
            } else {
                fieldName = fieldName.concat(_SINGLE);
            }

            if(descriptor.isUpdate()) {
                fieldName = fieldName.concat(_STORED);
            }

            final boolean isComplexField = ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass());
            switch (useCase) {
                case Fulltext: {
                    if (descriptor.isFullText()) {
                        if (isComplexField) {
                            String lang = StringUtils.defaultIfBlank(descriptor.getLanguage().getLangCode(),"none") + "_";
                            return fieldName.replace(_SINGLE,_MULTI) +  lang + contextPrefix + descriptor.getName();
                        } else {
                            final String lang = StringUtils.defaultIfBlank(descriptor.getLanguage().getLangCode(),"none") + "_";
                            return fieldName +  lang + contextPrefix + descriptor.getName();
                        }

                    } else {
                        log.debug("Descriptor {} is not configured for full text search.", descriptor.getName());
                        return null;
                    }
                }

                case Facet: {
                    if(descriptor.isFacet()) {
                        if (isComplexField) {
                            final  Type type = Type.getFromClass(((ComplexFieldDescriptor)descriptor).getFacetType());
                            return fieldName.replace(_SINGLE,_MULTI) + _FACET + type.getName() + contextPrefix + descriptor.getName();
                        } else {
                            final  Type type = Type.getFromClass(descriptor.getType());
                            return fieldName + _FACET + type.getName() + contextPrefix + descriptor.getName();
                        }
                    } else {
                        log.debug("Descriptor {} is not configured for facet search.", descriptor.getName());
                        return null;
                    }
                }
                case Suggest: {
                    if(descriptor.isSuggest()) {
                        Type type;
                        if (isComplexField) {
                            return fieldName.replace(_SINGLE,_MULTI) + _SUGGEST + Type.ANALYZED.getName() + contextPrefix + descriptor.getName();
                        } else {
                            type = Type.getFromClass(descriptor.getType());
                            type = type.getName().equals(Type.STRING.getName()) ? Type.ANALYZED : type;
                            return fieldName + _SUGGEST + type.getName() + contextPrefix + descriptor.getName();
                        }
                    } else {
                        log.debug("Descriptor {} is not configured for suggestion search.", descriptor.getName());
                        return null;
                    }
                }
                case Stored: {
                    if (descriptor.isStored()) {
                        Type type;
                        if (isComplexField) {
                            type = Type.getFromClass(((ComplexFieldDescriptor) descriptor).getStoreType());
                        } else {
                            type = Type.getFromClass(descriptor.getType());
                        }
                        return fieldName.replaceFirst(_STORED, "") + type.getName() + contextPrefix + descriptor.getName();
                    }
                }
                case Sort: {
                    Type type;
                    if (isComplexField) {
                        type = Type.getFromClass(((ComplexFieldDescriptor) descriptor).getStoreType());
                    } else {
                        type = Type.getFromClass(descriptor.getType());
                    }
                    if (descriptor.isSort() && Objects.nonNull(type)){
                        return fieldName.replaceFirst(_MULTI,_SINGLE) + _SORT + type.getName() + contextPrefix + descriptor.getName();
                    } else if(isComplexField && descriptor.isStored() && !descriptor.isMultiValue() && Objects.nonNull(type)){
                        return fieldName.replaceFirst(_MULTI,_SINGLE) + _SORT + type.getName() + contextPrefix + descriptor.getName();
                    } else {
                        log.debug("Descriptor {} is not configured for sorting.", descriptor.getName());
                        return null; //TODO: throw runtime exception?
                    }
                }
                case Filter: {
                    if(isComplexField && ((ComplexFieldDescriptor)descriptor).isAdvanceFilter() && Objects.nonNull(((ComplexFieldDescriptor)descriptor).getFacetType())) {
                        Type type = Type.getFromClass(((ComplexFieldDescriptor)descriptor).getFacetType());
                        return fieldName.replace(_SINGLE,_MULTI) + _FILTER + type.getName() + contextPrefix + descriptor.getName();

                    } else {
                        log.debug("Descriptor {} is not configured for advance filter search.", descriptor.getName());
                        return null;
                    }
                }
                default: {
                    log.warn("Unsupported use case {}.", useCase);
                    return null;//TODO: throw runtime exception
                }
            }
        }
    }

    public static final class Result {

        private static Logger log = LoggerFactory.getLogger(Result.class);

        public static List<Document> buildResultList(SolrDocumentList results, Map<String,Integer> childCounts, DocumentFactory factory, String searchContext) {

            return results.stream().map(result -> {

                Document document = factory.createDoc((String) result.getFieldValue(Fieldname.ID));

                if (childCounts != null) {
                    document.setChildCount(ObjectUtils.defaultIfNull(childCounts.get(document.getId()), 0));
                }

                if (Objects.nonNull(result.get(Fieldname.SCORE))) {
                    document.setScore((Float) result.get(Fieldname.SCORE));
                }

                if (Objects.nonNull(result.get(Fieldname.DISTANCE))) {
                    document.setDistance((Float) result.get(Fieldname.DISTANCE));
                }

                result.getFieldNames().stream()
                        .filter(name -> !name.equals(Fieldname.ID))
                        .filter(name -> !name.equals(Fieldname.TYPE))
                        .filter(name -> !name.equals(Fieldname.SCORE))
                        .filter(name -> !name.equals(Fieldname.DISTANCE))
                        .forEach(name -> {
                            final Object o = result.get(name);
                            final String contextPrefix = searchContext != null ? searchContext + "_" : "";
                            final Matcher internalPrefixMatcher = Pattern.compile(INTERNAL_FIELD_PREFIX).matcher(name);
                            final String contextualizedName = internalPrefixMatcher.replaceFirst("");
                            final boolean contextualized = Objects.nonNull(searchContext) && contextualizedName.contains(contextPrefix);
                            final String fname = contextualizedName.replace(contextPrefix, "");
                            if (factory.hasField(fname)) {
                                final FieldDescriptor<?> field = factory.getField(fname);
                                Class<?> type;
                                if (ComplexFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                                    type = ((ComplexFieldDescriptor) field).getStoreType();
                                } else {
                                    type = field.getType();
                                }
                                try {
                                    if (o instanceof Collection) {
                                        final Collection<Object> solrValues = new ArrayList<>();
                                        if (ZonedDateTime.class.isAssignableFrom(type)) {
                                            ((Collection<?>) o).forEach(ob -> solrValues.add(ZonedDateTime.ofInstant(((Date) ob).toInstant(), ZoneId.of("UTC"))));
                                        } else if (Date.class.isAssignableFrom(type)) {
                                            ((Collection<?>) o).forEach(ob -> {
                                                try {
                                                    solrValues.add(DateUtil.parseDate(ob.toString()));
                                                } catch (ParseException e) {
                                                    log.error("Unable to parse solr result field '{}' value '{}' to field descriptor type [{}]",
                                                            fname, o.toString(), type);
                                                    throw new RuntimeException(e);
                                                }
                                            });
                                        } else if (LatLng.class.isAssignableFrom(type)) {
                                            ((Collection<?>) o).forEach(ob -> {
                                                try {
                                                    solrValues.add(LatLng.parseLatLng(ob.toString()));
                                                } catch (ParseException e) {
                                                    log.error("Unable to parse solr result field '{}' value '{}' to field descriptor type [{}]",
                                                            fname, o.toString(), type);
                                                    throw new RuntimeException(e);
                                                }
                                            });
                                        } else {
                                            solrValues.addAll((Collection<Object>) o);
                                        }

                                        if (ComplexFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                                            if (contextualized) {
                                                document.setContextualizedValues((MultiValuedComplexField<Object, ?, ?>) field, searchContext, solrValues);
                                            } else {
                                                document.setValues((MultiValuedComplexField<Object, ?, ?>) field, solrValues);
                                            }

                                        } else {
                                            if (contextualized) {
                                                document.setContextualizedValues((MultiValueFieldDescriptor<Object>) field, searchContext, solrValues);
                                            } else {
                                                document.setValues((MultiValueFieldDescriptor<Object>) field, solrValues);
                                            }
                                        }

                                    } else {
                                        Object solrValue;
                                        if (ZonedDateTime.class.isAssignableFrom(type)) {
                                            solrValue = ZonedDateTime.ofInstant(((Date) o).toInstant(), ZoneId.of("UTC"));
                                        } else if (Date.class.isAssignableFrom(type)) {
                                            try {
                                                solrValue = (DateUtil.parseDate(o.toString()));
                                            } catch (ParseException e) {
                                                log.error("Unable to parse solr result field '{}' value '{}' to field descriptor type [{}]",
                                                        fname, o.toString(), type);
                                                throw new RuntimeException(e);
                                            }
                                        } else if (LatLng.class.isAssignableFrom(type)) {
                                            solrValue = LatLng.parseLatLng(o.toString());
                                        } else {
                                            solrValue = castForDescriptor(o, field, Stored);
                                        }
                                        if (contextualized) {
                                            document.setContextualizedValue((FieldDescriptor<Object>) field, searchContext, solrValue);
                                        } else {
                                            document.setValue((FieldDescriptor<Object>) field, solrValue);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("Unable to parse solr result field '{}' value '{}' to field descriptor type [{}]",
                                            fname, o.toString(), type);
                                    throw new RuntimeException(e);
                                }
                            }
                        });

                return document;
            }).collect(Collectors.toList());
        }

        private static HashMap<FieldDescriptor, TermFacetResult<?>> getTermFacetResults(QueryResponse response, DocumentFactory factory, DocumentFactory childFactory, Map<String,Facet>  facetsQuery, String searchContext) {
            final HashMap<FieldDescriptor, TermFacetResult<?>> facets = new HashMap<>();
            //term facets
            if (Objects.nonNull(response.getResponse())) {
                final SimpleOrderedMap jsonFacetResult = (SimpleOrderedMap) response.getResponse().get("facets");
                if (Objects.nonNull(jsonFacetResult)) {
                    for (int i = 0; i < jsonFacetResult.size(); i++) {
                        final String facetName = jsonFacetResult.getName(i);
                        if (jsonFacetResult.getName(i).startsWith("dynamic_")) {
                            final String fieldName = getFieldDescriptorName(searchContext, facetName);
                            FieldDescriptor<?> fieldDesc = factory.getField(fieldName);
                            if (Objects.isNull(fieldDesc) && Objects.nonNull(childFactory)) {
                                fieldDesc = childFactory.getField(fieldName);
                            }
                            final FieldDescriptor<?> descriptor = fieldDesc;

                            final ArrayList<SimpleOrderedMap> termFacet =
                                    ((ArrayList<SimpleOrderedMap>) ((SimpleOrderedMap) jsonFacetResult.get(facetName)).get("buckets"));

                            if (Objects.nonNull(descriptor)) {
                                final UseCase useCase = UseCase.valueOf(facetsQuery.get(fieldName).getScope().name());
                                final TermFacetResult<?> facet = new TermFacetResult(termFacet.stream()
                                        .map(f ->
                                            new FacetValue<>(castForDescriptor(f.get("val"), descriptor, useCase), ((Integer) f.get("count")).longValue())
                                        )
                                        .collect(Collectors.toList()));

                                facets.put(descriptor, facet);
                            } else {
                                log.error("Unable to create a facet result: the field '{}' is not configured as facet.", fieldName);
                                throw new RuntimeException("Unable to create a faceted result: the field '" + fieldName + "' is not configured as facet.");
                            }
                        }
                    }
                }
            }
            return facets;
        }

        private static TermFacetResult<String> getTypeFacetResults(QueryResponse response) {
            final TermFacetResult typeFacetResults = new TermFacetResult();
            //term facets
            if (Objects.nonNull(response.getResponse())) {
                final SimpleOrderedMap jsonFacetResult = (SimpleOrderedMap) response.getResponse().get("facets");
                if (Objects.nonNull(jsonFacetResult)) {
                    for (int i = 0; i < jsonFacetResult.size(); i++) {
                        if (jsonFacetResult.getName(i).equals(Fieldname.TYPE)) {
                            final ArrayList<SimpleOrderedMap> termFacet =
                                    ((ArrayList<SimpleOrderedMap>) ((SimpleOrderedMap) jsonFacetResult.get(jsonFacetResult.getName(i))).get("buckets"));

                            termFacet.stream().forEach(f -> typeFacetResults
                                    .addFacetValue(new FacetValue<>((String) f.get("val"), ((Integer)f.get("count")).longValue())));

                        }
                    }
                }
            }
            return typeFacetResults;
        }

        public static FacetResults buildFacetResult(QueryResponse response, DocumentFactory factory, DocumentFactory childFactory, Map<String,Facet>  facetsQuery, String searchContext) {

            final HashMap<FieldDescriptor, TermFacetResult<?>> facets =
                    getTermFacetResults(response, factory, childFactory, facetsQuery, searchContext);

            final TermFacetResult<String> typeFacetResults = getTypeFacetResults(response);

            HashMap<String, QueryFacetResult<?>> queryFacetResults = new HashMap<>();
            if(response.getFacetQuery()!=null) {
                queryFacetResults = getFacetQueryResults(response.getFacetQuery().entrySet(), facetsQuery);
            }

            HashMap<String, RangeFacetResult<?>> rangeFacetResults = new HashMap<>();
            if(response.getFacetRanges()!=null) {
                rangeFacetResults = getRangeFacetResult(response.getFacetRanges(),response,factory,facetsQuery, searchContext);
            }

            HashMap<String, IntervalFacetResult> intervalFacetResults = new HashMap<>();
            if(response.getIntervalFacets() != null) {
                intervalFacetResults = getIntervalFacetResult(response.getIntervalFacets(),response,factory);
            }

            HashMap<String, StatsFacetResult<?>> statsResults = new HashMap<>();
            if(response.getFieldStatsInfo()!=null) {
                statsResults = getStatsFacetsResults(response.getFieldStatsInfo().entrySet(), facetsQuery);
            }

            HashMap<String, List<PivotFacetResult<?>>> pivotFacetResults = new HashMap<>();
            if(response.getFacetPivot()!=null) {

                final Stream<Map.Entry<String, List<PivotField>>> pivotFacets = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(response.getFacetPivot().iterator(), Spliterator.ORDERED),
                        false);
                pivotFacets.forEach(pivotFacet -> {
                    final List<PivotFacetResult<?>> facet = pivotFacet.getValue().stream()
                            .map(pivotField -> getPivotFacetResult(pivotField, response, factory,facetsQuery,searchContext))
                            .collect(Collectors.toList());

                    pivotFacetResults.put(pivotFacet.getKey(), facet);
                });
            }

            final Map<Integer, Integer> childCounts = getSubdocumentCounts(response);
            final Collection<SubdocumentFacetResult> subDocumentFacet;
            if (Objects.nonNull(childCounts)) {
                subDocumentFacet = childCounts.entrySet().stream()
                        .map(e -> new SubdocumentFacetResult(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());
            } else {
                subDocumentFacet = Collections.emptyList();
            }

            return new FacetResults(factory, facets, typeFacetResults, queryFacetResults, rangeFacetResults, intervalFacetResults, statsResults, pivotFacetResults,subDocumentFacet);
        }

        private static HashMap<String, StatsFacetResult<?>> getStatsFacetsResults(Set<Map.Entry<String, FieldStatsInfo>> entries, Map<String, Facet> facetsQuery) {

            HashMap<String, StatsFacetResult<?>> statsResults = new HashMap<>();
            entries.stream()
                    .forEach(statsInfoEntry -> {
                        final StatsFacet statsFacet = (StatsFacet) facetsQuery.get(statsInfoEntry.getKey());
                        final FieldDescriptor field = statsFacet.getField();
                        final FieldStatsInfo statsInfo = statsInfoEntry.getValue();
                        final UseCase useCase = UseCase.valueOf(statsFacet.getScope().name());
                        final Object typedMean;
                        //Mean can be either Double or date. In the latest scenario it can be casted to the specific type
                        if(Number.class.isAssignableFrom(field.getType())){
                            typedMean = statsInfo.getMean();
                        }else {
                            typedMean = castForDescriptor(statsInfo.getMean(),field, useCase);
                        }
                        final StatsFacetResult<?> statsResult =
                                new StatsFacetResult(field,
                                                    castForDescriptor(statsInfo.getMin(),field, useCase),
                                                    castForDescriptor(statsInfo.getMax(),field, useCase),
                                                    castForDescriptor(statsInfo.getSum(),field, useCase),
                                                    statsInfo.getCount(),
                                                    statsInfo.getMissing(),
                                                    statsInfo.getSumOfSquares(),
                                                    typedMean,
                                                    statsInfo.getStddev(),
                                                    statsInfo.getPercentiles(),
                                                    (List)castForDescriptor(statsInfo.getDistinctValues(),field, useCase),
                                                    statsInfo.getCountDistinct(),
                                                    statsInfo.getCardinality());

                        statsResults.put(statsInfoEntry.getKey(), statsResult);
                    });

            return statsResults;
        }

        private static HashMap<String, QueryFacetResult<?>> getFacetQueryResults(Set<Map.Entry<String, Integer>> solrFacetQueries, Map<String, Facet> facetsQuery) {
            HashMap<String, QueryFacetResult<?>> queryFacetResults = new HashMap<>();
            solrFacetQueries.stream()
                    .forEach(queryFacet -> {
                        QueryFacetResult facet = new QueryFacetResult<>(((QueryFacet)facetsQuery.get(queryFacet.getKey())).getFilter(),queryFacet.getValue());
                        queryFacetResults.put(queryFacet.getKey(), facet);
                    });
            return queryFacetResults;
        }

        private static PivotFacetResult<?> getPivotFacetResult(PivotField pivotField,QueryResponse response, DocumentFactory factory, Map<String, Facet> facetsQuery, String searchContext) {

            final String fieldName = getFieldDescriptorName(searchContext, pivotField.getField());
            final FieldDescriptor<?> descriptor = factory.getField(fieldName);

            if (descriptor == null) {
                log.error("Unable to create a pivot faced result: the field '{}' is not configured as facet.",  fieldName);
                throw new RuntimeException("Unable to create a pivot faced result: the field '"+ fieldName +"' is not configured as facet.");
            }

            final List<PivotFacetResult<?>> pivot = new ArrayList<>();
            if(pivotField.getPivot()!=null) {
                pivotField.getPivot().stream().forEach(pivotF -> pivot.add(getPivotFacetResult(pivotF,response,factory,facetsQuery, searchContext)));
            }

            HashMap<String, QueryFacetResult<?>> pivotQueryResult = new HashMap<>();
            if (pivotField.getFacetQuery() != null) {
                pivotQueryResult= getFacetQueryResults(pivotField.getFacetQuery().entrySet(),facetsQuery);
            }

            HashMap<String, RangeFacetResult<?>> pivotRangeResult = new HashMap<>();
            if (pivotField.getFacetRanges() != null) {
                pivotRangeResult = getRangeFacetResult(pivotField.getFacetRanges(), response, factory, facetsQuery, searchContext);
            }

            HashMap<String, StatsFacetResult<?>> pivotStatsResults = new HashMap<>();
            if(pivotField.getFieldStatsInfo()!=null) {
                pivotStatsResults = getStatsFacetsResults(pivotField.getFieldStatsInfo().entrySet(), facetsQuery);
            }
            /*TODO: check value type: castForDescriptor(pivotField.getValue(), descriptor)*/
            return new PivotFacetResult(pivot, pivotField.getValue(), descriptor, pivotField.getCount(),pivotQueryResult, pivotStatsResults, pivotRangeResult);
        }

        private static  HashMap<String, IntervalFacetResult> getIntervalFacetResult(List<IntervalFacet> facetIntervals, QueryResponse response, DocumentFactory factory) {
            HashMap<String, IntervalFacetResult> intervalFacetResults = new HashMap<>();
            facetIntervals.stream()
                    .forEach(intervalFacet -> {
                        List<FacetValue<String>> values = new ArrayList<>();
                        intervalFacet.getIntervals().forEach(count ->
                                values.add(new FacetValue<>(count.getKey(), count.getCount())));
                        intervalFacetResults.put(intervalFacet.getField(), new IntervalFacetResult(values));
                    });

            return intervalFacetResults;
        }

        private static  HashMap<String, RangeFacetResult<?>> getRangeFacetResult(List<RangeFacet> facetRanges, QueryResponse response,DocumentFactory factory, Map<String,Facet>  facetsQuery, String searchContext) {

            final HashMap<String, RangeFacetResult<?>> rangeFacetResults = new HashMap<>();
            facetRanges.stream()
                    .forEach(rangeFacet -> {
                        //Getting FacetRange original query to know the solr field name
                        final Object facetRangesQuery = ((SimpleOrderedMap) response.getHeader().get("params")).get("facet.range");

                        final List<String> rangeQueries = new ArrayList<>();
                        if (ArrayList.class.isAssignableFrom(facetRangesQuery.getClass())) {
                            rangeQueries.addAll((ArrayList<String>) facetRangesQuery);
                        } else if (String.class.isAssignableFrom(facetRangesQuery.getClass())){
                            rangeQueries.add((String) facetRangesQuery);
                        }

                        final Optional<String> facetFieldQuery = rangeQueries
                                .stream()
                                .filter(facetRangeField -> facetRangeField.contains(rangeFacet.getName()))
                                .findFirst();

                        final String facetFieldName = Pattern.compile("\\{.*\\}").matcher(facetFieldQuery.get()).replaceFirst("");
                        final String fieldName = getFieldDescriptorName(searchContext, facetFieldName);
                        final FieldDescriptor<?> descriptor = factory.getField(fieldName);

                        if (descriptor == null) {
                            log.error("Unable to create a range facet result: the field '{}' is not configured as facet.", fieldName);
                            throw new RuntimeException("Unable to create a range facet result: the field '"+ fieldName +"' is not configured as facet.");
                        }

                        final UseCase useCase = UseCase.valueOf(facetsQuery.get(rangeFacet.getName()).getScope().name());
                        final List<FacetValue> facetRangesResults = (List<FacetValue>) rangeFacet.getCounts().stream()
                                .map(count -> {
                                    RangeFacet.Count solrCount = (RangeFacet.Count) count;
                                    return new FacetValue<>(castForDescriptor(solrCount.getValue(),descriptor, useCase), solrCount.getCount());
                                }).collect(Collectors.toList());

                        final Object start = castForDescriptor(rangeFacet.getStart(), descriptor, useCase);
                        final Object end = castForDescriptor(rangeFacet.getEnd(), descriptor, useCase);
                        final long gap = Long.parseLong(rangeFacet.getGap().toString().replaceAll("[^\\d]", ""));
                        final RangeFacetResult facet =new RangeFacetResult(facetRangesResults, start, end, gap);

                        rangeFacetResults.put(rangeFacet.getName(), facet);
                    });


            return rangeFacetResults;
        }

        private static Object castForDescriptor(String s, FieldDescriptor<?> descriptor, UseCase useCase) {

            Class<?> type;
            if(Objects.nonNull(descriptor)) {
                if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
                    switch (useCase) {
                        case Facet:
                            type = ((ComplexFieldDescriptor) descriptor).getFacetType();
                            break;
                        case Stored:
                            type = ((ComplexFieldDescriptor) descriptor).getStoreType();
                            break;
                        case Suggest: type = String.class;
                            break;
                        case Filter: type = ((ComplexFieldDescriptor)descriptor).getFacetType();
                            break;
                        default:
                            type = descriptor.getType();
                    }
                } else {
                    type = descriptor.getType();
                }

                return castForDescriptor(s, type);
            } else return s;

        }

        private static Object castForDescriptor(String s, FieldDescriptor<?> descriptor) {

            return castForDescriptor(s,descriptor.getType());
        }

        private static Object castForDescriptor(String s, Class<?> type) {

            if(Long.class.isAssignableFrom(type)) {
                return Long.valueOf(s);
            }
            if(Integer.class.isAssignableFrom(type)) {
                return Integer.valueOf(s);
            }
            if(Double.class.isAssignableFrom(type)) {
                return Double.valueOf(s);
            }
            if(Number.class.isAssignableFrom(type)) {
                return Float.valueOf(s);
            }
            if(Boolean.class.isAssignableFrom(type)) {
                return Boolean.valueOf(s);
            }
            if(ZonedDateTime.class.isAssignableFrom(type)) {
                return ZonedDateTime.parse(s);
            }
            if(Date.class.isAssignableFrom(type)) {
                try {
                    return DateUtil.parseDate(s);
                } catch (ParseException e) {
                    log.error("Unable to parse value '{}' to valid Date", s);
                    throw new RuntimeException(e);
                }
            }
            if(ByteBuffer.class.isAssignableFrom(type)) {
                return ByteBuffer.wrap(s.getBytes(UTF_8));
            }
            return s;
        }

        private static Object castForDescriptor(Object o, FieldDescriptor<?> descriptor, UseCase useCase) {

            Class<?> type;

            if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())){
                switch (useCase) {
                    case Facet: type = ((ComplexFieldDescriptor)descriptor).getFacetType();
                        break;
                    case Stored: type = ((ComplexFieldDescriptor)descriptor).getStoreType();
                        break;
                    default: type = descriptor.getType();
                }
            } else {
                type = descriptor.getType();
            }

            if(o != null){
                if(Collection.class.isAssignableFrom(o.getClass())) {
                    return ((Collection)o).stream()
                            .map( element -> castForDescriptor(element,descriptor))
                            .collect(Collectors.toList());
                }
                return castForDescriptor(o,type);
            }
            return o;
        }

        private static Object castForDescriptor(Object o, FieldDescriptor<?> descriptor) {

            Class<?> type = descriptor.getType();

            if(o != null){
                if(Collection.class.isAssignableFrom(o.getClass())) {
                    return ((Collection)o).stream()
                            .map( element -> castForDescriptor(element,descriptor))
                            .collect(Collectors.toList());
                }
                return castForDescriptor(o,type);
            }
            return o;
        }

        private static Object castForDescriptor(Object o, Class<?> type) {

            if(o != null){

                if(Long.class.isAssignableFrom(type)) {
                    return ((Number)o).longValue();
                }
                if(Integer.class.isAssignableFrom(type)) {
                    return ((Number)o).intValue();
                }
                if(Double.class.isAssignableFrom(type)) {
                    return ((Number)o).doubleValue();
                }
                if(Number.class.isAssignableFrom(type)) {
                    return ((Number)o).floatValue();
                }
                if(Boolean.class.isAssignableFrom(type)) {
                    return (Boolean) o;
                }
                if(ZonedDateTime.class.isAssignableFrom(type)) {
                    if(o instanceof Date){
                        return ZonedDateTime.ofInstant(((Date) o).toInstant(), ZoneId.of("UTC"));
                    }
                    return (ZonedDateTime) o;
                }
                if(Date.class.isAssignableFrom(type)) {
                    return (Date) o;
                }
                if(ByteBuffer.class.isAssignableFrom(type)) {
                    return ByteBuffer.wrap(new String((byte[]) o).getBytes()) ;
                }
            }
            return o;
        }

        public static SuggestionResult buildSuggestionResult(QueryResponse response, DocumentFactory factory,String searchContext) {

            return buildSuggestionResult(response,factory,null,searchContext);
        }

        public static SuggestionResult buildSuggestionResult(QueryResponse response, DocumentFactory factory, DocumentFactory childFactory, String searchContext) {

            final HashMap<FieldDescriptor, TermFacetResult<?>> suggestions = new HashMap<>();

            final NamedList<Object> responseObject = response.getResponse();

            if (responseObject != null && responseObject.get("suggestions") != null) {
                Class suggestionResponseClass = responseObject.get("suggestions").getClass();
                final LinkedHashMap<String , Object> suggestionsResponse;
                if(LinkedHashMap.class.isAssignableFrom(suggestionResponseClass)) {
                    //Backwards compatibility
                    suggestionsResponse = (LinkedHashMap<String , Object>)responseObject.get("suggestions");
                }else if(NamedList.class.isAssignableFrom(suggestionResponseClass)) {
                    suggestionsResponse = new LinkedHashMap<>();
                    ((NamedList<?>)responseObject.get("suggestions")).forEach( e ->suggestionsResponse.put(e.getKey(),e.getValue()));
                } else {
                    log.error("Error parsing Solr suggestion response: unknown response type");
                    throw new RuntimeException("Error parsing Solr suggestion response: unknown response type");
                }
                final Integer suggestionCount = (Integer)suggestionsResponse.get("suggestion_count");
                if(suggestionCount > 0) {

                    final LinkedHashMap<String, NamedList<Integer>> suggestion_facets;

                    if(LinkedHashMap.class.isAssignableFrom(suggestionsResponse.get("suggestion_facets").getClass())) {
                        //Backwards compatibility
                        suggestion_facets = (LinkedHashMap<String, NamedList<Integer>>) suggestionsResponse.get("suggestion_facets");
                    }else if(NamedList.class.isAssignableFrom(suggestionsResponse.get("suggestion_facets").getClass())) {
                        suggestion_facets = new LinkedHashMap<>();
                        ((NamedList<NamedList<Integer>>)suggestionsResponse.get("suggestion_facets"))
                                .forEach( e ->suggestion_facets.put(e.getKey(),e.getValue()));
                    } else {
                        log.error("Error parsing Solr suggestion response: unknown response type");
                        throw new RuntimeException("Error parsing Solr suggestion response: unknown response type");
                    }


                    suggestion_facets.keySet().forEach(field -> {

                        final Matcher internalFacetFieldMatcher = Pattern.compile(INTERNAL_SUGGEST_FIELD_PREFIX).matcher(field);
                        final String contextPrefix = searchContext != null ? searchContext + "_" : "";
                        final String contextualizedName = internalFacetFieldMatcher.replaceFirst("");
                        final String fieldName = contextualizedName.replace(contextPrefix, "");
                        final FieldDescriptor<?> descriptor =
                                Objects.nonNull(factory.getField(fieldName))?
                                        factory.getField(fieldName):childFactory.getField(fieldName);

                        if (descriptor == null) {
                            log.error("Unable to create suggestion result: the field '{}' is not configured as suggest.",  fieldName);
                            throw new RuntimeException("Unable to create suggestion result: the field '"+ fieldName+"' is not configured as facet.");
                        }

                        final NamedList<Integer> fieldSuggestions = suggestion_facets.get(field);
                        final List<FacetValue> facetValues = new ArrayList<>();
                        fieldSuggestions.forEach(suggestion ->
                                facetValues.add(new FacetValue<>(castForDescriptor(suggestion.getKey(), descriptor), suggestion.getValue())));

                        final TermFacetResult suggestionFacet = new TermFacetResult(facetValues);

                        suggestions.put(descriptor, suggestionFacet);

                    });
                }

                final String collation = response.getSpellCheckResponse() != null ?
                        response.getSpellCheckResponse().getCollatedResult().replaceFirst("\\*$","") :
                        null;

                return new SuggestionResult(suggestions, collation, response.getQTime(), factory).setElapsedTime(response.getElapsedTime());
            }
            return new SuggestionResult();
        }

        public static GetResult buildRealTimeGetResult(QueryResponse response, RealTimeGet query, DocumentFactory factory) {
            final String DOC = "doc";

            long nResults = 0;
            List<Document> docResults = new ArrayList<>();

            final SolrDocumentList results = response.getResults();
            if(results != null && results.size() >0){
                docResults = buildResultList(results, null, factory, null);
                nResults = docResults.size();
            } else {
                final SolrDocument solrDoc = (SolrDocument)response.getResponse().get(DOC);
                if(solrDoc != null) {
                    final SolrDocumentList solrDocuments = new SolrDocumentList();
                    solrDocuments.add(solrDoc);
                    docResults = buildResultList(solrDocuments, null, factory, null);
                    nResults = 1;
                }
            }

            return new GetResult(nResults,docResults,query,factory,response.getQTime()).setElapsedTime(response.getElapsedTime());
        }

        private static String getFieldDescriptorName(String searchContext, String facetName) {
            final Matcher internalFacetFieldMatcher = Pattern.compile(INTERNAL_SCOPE_FACET_FIELD_PREFIX).matcher(facetName);
            final String contextPrefix = searchContext != null ? searchContext + "_" : "";
            final String contextualizedName = internalFacetFieldMatcher.replaceFirst("");
            return contextualizedName.replace(contextPrefix, "");
        }

    }
}
