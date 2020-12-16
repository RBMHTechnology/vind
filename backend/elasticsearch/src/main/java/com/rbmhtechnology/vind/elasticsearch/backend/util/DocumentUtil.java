package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearchQueryFactory;
import com.rbmhtechnology.vind.model.ComplexFieldDescriptor;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.InverseSearchQuery;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValuedComplexField;
import com.rbmhtechnology.vind.model.SingleValuedComplexField;
import com.rbmhtechnology.vind.model.value.LatLng;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rbmhtechnology.vind.elasticsearch.backend.util.CursorUtils.toSearchAfterCursor;
import static com.rbmhtechnology.vind.elasticsearch.backend.util.FieldUtil._COMPLEX;
import static com.rbmhtechnology.vind.elasticsearch.backend.util.FieldUtil._DYNAMIC;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DocumentUtil {

    private static final Logger log = LoggerFactory.getLogger(DocumentUtil.class);

    public static Map<String, Object> createInputDocument(Document doc) {
        return createInputDocument(doc, false);
    }

    public static Map<String, Object> createInputDocument(Document doc, Boolean percolator) {

        final Map<String, Object> docMap = new HashMap<>();
        //add fields
        doc.listFieldDescriptors()
                .values()
                .stream()
                .filter(doc::hasValue)
                .forEach(descriptor -> addFieldToDoc(doc, docMap, descriptor, null));

        docMap.put(FieldUtil.ID, doc.getId());
        docMap.put(FieldUtil.TYPE, doc.getType());
        docMap.put(FieldUtil.PERCOLATOR_FLAG, percolator);

        return docMap;
    }

    public static Map<String, Object> createInputDocument(InverseSearchQuery doc) {

        final Map<String, Object> docMap = new HashMap<>();
        //add fields
        doc.listFieldDescriptors()
                .values()
                .stream()
                .filter(doc::hasValue)
                .forEach(descriptor -> addFieldToDoc(doc, docMap, descriptor, null));

        addFieldToDoc(doc, docMap, InverseSearchQueryFactory.BINARY_QUERY_FIELD, null);

        docMap.put(FieldUtil.ID, doc.getId());
        docMap.put(FieldUtil.TYPE, doc.getType());
        docMap.put(FieldUtil.PERCOLATOR_FLAG, false);

        return docMap;
    }

    private static void addFieldToDoc(Document doc, Map<String, Object> docMap, FieldDescriptor<?> descriptor,
                                      List<String> indexFootPrint) {
        if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            addFieldToDoc(doc, docMap, (ComplexFieldDescriptor) descriptor, indexFootPrint);
        } else {
            doc.getFieldContexts(descriptor)
                    .forEach(context ->
                            FieldUtil.getFieldName(descriptor, context, indexFootPrint)
                                    .ifPresent(fieldName ->
                                            Optional.ofNullable(doc.getContextualizedValue(descriptor, context))
                                                    .ifPresent(value -> docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType(value)))
                                    )
                    );
        }
    }

    private static void addEmptyFieldToDoc(Map<String, Object> docMap, FieldDescriptor<?> descriptor, List<String> indexFootPrint) {
        if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            addEmptyFieldToDoc( docMap, (ComplexFieldDescriptor) descriptor, indexFootPrint);
        } else {
            FieldUtil.getFieldName(descriptor, null, indexFootPrint)
                .ifPresent(fieldName -> {
                    if (ZonedDateTime.class.isAssignableFrom(descriptor.getType())) {
                        docMap.put(
                                fieldName.replaceAll("\\.\\w+", ""),
                                toElasticType(ZonedDateTime.ofInstant(Instant.EPOCH,ZoneId.of("UTC"))));
                    } else if (Date.class.isAssignableFrom(descriptor.getType())) {
                        docMap.put(
                                fieldName.replaceAll("\\.\\w+", ""),
                                toElasticType(Date.from(Instant.EPOCH)));
                    } else if (LatLng.class.isAssignableFrom(descriptor.getType())) {
                        docMap.put(
                                fieldName.replaceAll("\\.\\w+", ""),
                                toElasticType(new LatLng(0,0)));
                    } else if (ByteBuffer.class.isAssignableFrom(descriptor.getType())) {
                        docMap.put(
                                fieldName.replaceAll("\\.\\w+", ""),
                                toElasticType(ByteBuffer.wrap("".getBytes())));
                    } else if (Number.class.isAssignableFrom(descriptor.getType())) {
                        docMap.put(
                                fieldName.replaceAll("\\.\\w+", ""),
                                toElasticType(0));
                    } else {
                        docMap.put(
                                fieldName.replaceAll("\\.\\w+", ""),
                                toElasticType(""));
                    }
                });
        }
    }
    private static void addEmptyFieldToDoc(Map<String, Object> docMap, ComplexFieldDescriptor<?,?,?> descriptor, List<String> indexFootPrint) {

        Stream.of(FieldDescriptor.UseCase.values()).forEach( useCase -> {
            final Optional<String> name = FieldUtil.getFieldName(descriptor, useCase, null, indexFootPrint);
            name.ifPresent( fieldName -> {
                final Class<?> type = FieldUtil.getComplexFieldType(descriptor, useCase);
                if (type != null) {
                    if (ZonedDateTime.class.isAssignableFrom(type)) {
                        docMap.put(fieldName.replaceAll("\\.\\w+", ""), toElasticType(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))));
                    } else if (Date.class.isAssignableFrom(type)) {
                        docMap.put(fieldName.replaceAll("\\.\\w+", ""), toElasticType(Date.from(Instant.EPOCH)));
                    } else if (LatLng.class.isAssignableFrom(type)) {
                        docMap.put(fieldName.replaceAll("\\.\\w+", ""), toElasticType(new LatLng(0, 0)));
                    } else if (ByteBuffer.class.isAssignableFrom(type)) {
                        docMap.put(fieldName.replaceAll("\\.\\w+", ""), toElasticType(ByteBuffer.wrap(" ".getBytes())).toString());
                    } else if (Number.class.isAssignableFrom(type)) {
                        docMap.put(fieldName.replaceAll("\\.\\w+", ""), toElasticType(0));
                    } else {
                        docMap.put(fieldName.replaceAll("\\.\\w+", ""), toElasticType(""));
                    }
                }
            });

        });
    }

    private static void addFieldToDoc(InverseSearchQuery doc, Map<String, Object> docMap, FieldDescriptor<?> descriptor,
                                      List<String> indexFootPrint) {
        if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            addFieldToDoc(doc, docMap, (ComplexFieldDescriptor) descriptor, indexFootPrint);
        } else {
            FieldUtil.getFieldName(descriptor, null, indexFootPrint)
                    .ifPresent(fieldName ->
                            Optional.ofNullable(doc.getValue(descriptor))
                                    .ifPresent(value -> docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType(value)))
                    );
        }
    }

    private static void addFieldToDoc(Document doc, Map<String, Object> docMap, ComplexFieldDescriptor<?,?,?> descriptor,
                                      List<String> indexFootPrint) {
        doc.getFieldContexts(descriptor)
            .forEach(context ->
                Stream.of(FieldDescriptor.UseCase.values()).forEach( useCase -> {
                    final Optional<String> name = FieldUtil.getFieldName(descriptor, useCase, context, indexFootPrint);
                    name.ifPresent( fieldName ->
                        Optional.ofNullable( doc.getContextualizedValue(descriptor, context)).ifPresent(
                            contextualizedValue ->
                                docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType(contextualizedValue, descriptor, useCase)))
                    );
                })
            );
    }

    private static void addFieldToDoc(InverseSearchQuery doc, Map<String, Object> docMap,
                                      ComplexFieldDescriptor<?,?,?> descriptor, List<String> indexFootPrint) {
        Stream.of(FieldDescriptor.UseCase.values()).forEach( useCase -> {
            final Optional<String> name = FieldUtil.getFieldName(descriptor, useCase, null, indexFootPrint);
            name.ifPresent( fieldName ->
                Optional.ofNullable( doc.getValue(descriptor))
                    .ifPresent(contextualizedValue ->
                            docMap.put(
                                    fieldName.replaceAll("\\.\\w+" , ""),
                                    toElasticType(contextualizedValue, descriptor, useCase)
                            )
                )
            );
        });
    }

    protected static Object toElasticType(Object value) {
        if(value!=null) {
            if(Object[].class.isAssignableFrom(value.getClass())){
                return toElasticType(Arrays.asList((Object[])value));
            }
            if(Collection.class.isAssignableFrom(value.getClass())){
                final Collection<Object> values = (Collection<Object>) value;
                return values.stream()
                        .map(DocumentUtil::toElasticType)
                        .collect(Collectors.toList());
            }
            if(value instanceof ZonedDateTime) {
                return Date.from(((ZonedDateTime) value).toInstant());
            }
            if(value instanceof LatLng) {
                return value.toString();
            }
            if(value instanceof Date) {
                //noinspection RedundantCast
                return ((Date) value);
            }
            if(value instanceof ByteBuffer) {
                //noinspection RedundantCast
                return ((ByteBuffer) value).array();
            }
        }
        return value;
    }

    /*
     * Returns the value of a complex field for a given use case applying the defined function to the original field type
     */
    protected static Object toElasticType(Object value, ComplexFieldDescriptor descriptor, FieldDescriptor.UseCase useCase) {
        if(value!=null) {
            if (Object[].class.isAssignableFrom(value.getClass())) {
                return toElasticType(Arrays.asList((Object[]) value, descriptor, useCase));
            }
            if (Collection.class.isAssignableFrom(value.getClass())) {
                final Collection<Object> values = (Collection<Object>) value;
                List<Object> objs = values.stream()
                        .map(v -> toElasticType(v, descriptor, useCase))
                        .collect(Collectors.toList());

                if (objs.stream().filter(Objects::nonNull).allMatch(o -> Collection.class.isAssignableFrom(o.getClass()))) {
                    objs =  objs.stream().filter(Objects::nonNull)
                            .map(o -> (List<Collection<Object>>)o)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());
                }
                return objs;
            }
            switch (useCase) {
                case Fulltext: {
                    if (descriptor.getFullTextFunction() != null) {
                        return descriptor.getFullTextFunction().apply(value);
                    } else {
                        return null;                    }
                }
                case Facet: {
                    if (descriptor.getFacetFunction() != null) {
                        return descriptor.getFacetFunction().apply(value);
                    } else {
                        return null;                    }
                }
                case Suggest: {
                    if (descriptor.getSuggestFunction() != null) {
                        return descriptor.getSuggestFunction().apply(value);
                    } else {
                        return null;                    }
                }
                case Stored: {
                    if (descriptor.getStoreFunction() != null) {
                        return descriptor.getStoreFunction().apply(value);
                    } else {
                        return null;                    }
                }
                case Sort: {
                    if (descriptor.isMultiValue()) {
                        final MultiValuedComplexField multiField = (MultiValuedComplexField) descriptor;
                        if (multiField.getSortFunction() != null) {
                            return multiField.getSortFunction().apply(value);
                        } else {
                            return null;                        }
                    } else {
                        final SingleValuedComplexField singleField = (SingleValuedComplexField) descriptor;
                        if (singleField.getSortFunction() != null) {
                            return singleField.getSortFunction().apply(value);
                        } else {
                            if (singleField.isStored()) {
                                return toElasticType(value, singleField, FieldDescriptor.UseCase.Stored);
                            }
                            return null;                        }
                    }

                }
                case Filter: {
                    if (descriptor.isAdvanceFilter() && Objects.nonNull(descriptor.getFacetType())) {
                        return descriptor.getAdvanceFilter().apply(value);
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
                        throw new RuntimeException("Unable to serialize complex Object", e);
                    }
                }
            }
        }
        return value;
    }

    public static Document buildVindDoc(SearchHit hit, DocumentFactory factory, String searchContext, boolean isCursorSearch) {
        final Document document = buildVindDoc(hit.getSourceAsMap(), factory, searchContext);

        // Setting score if present in result
        document.setScore(hit.getScore());

        // Setting distance if present in result
        Optional.ofNullable(hit.field(FieldUtil.DISTANCE))
                .ifPresent(distance -> document.setDistance(((Double)distance.getValue()).floatValue()/1000));

        if (isCursorSearch) {
            document.setSearchAfterCursor(toSearchAfterCursor(hit.getSortValues()));
        }
        return document;
    }

    public static Document buildVindDoc(SearchHit hit , DocumentFactory factory, String searchContext) {
        return buildVindDoc(hit,factory,searchContext,false);
    }

    public static Document buildVindDoc( Map<String, Object> docMap , DocumentFactory factory, String searchContext) {
        final Document document = factory.createDoc(String.valueOf(docMap.get(FieldUtil.ID)));

        docMap.keySet().stream()
                .filter(name -> ! Arrays.asList(FieldUtil.ID, FieldUtil.TYPE, FieldUtil.SCORE, FieldUtil.DISTANCE)
                        .contains(name))
                .filter(name ->  name.startsWith(_COMPLEX) && name.contains("_stored_") || name.startsWith(_DYNAMIC))
                .forEach(name -> {
                    final Object o = docMap.get(name);
                    final String contextPrefix = searchContext != null ? searchContext + "_" : "";
                    final String pattern = "(" + FieldUtil.INTERNAL_FIELD_PREFIX + ")|(" + FieldUtil.INTERNAL_COMPLEX_FIELD_PREFIX + ")";
                    final Matcher internalPrefixMatcher = Pattern.compile(pattern).matcher(name);
                    final String contextualizedName = internalPrefixMatcher.replaceFirst("");
                    final boolean contextualized = Objects.nonNull(searchContext) && contextualizedName.contains(contextPrefix);
                    final String fieldRawName = contextualizedName.replace(contextPrefix, "").replace(FieldUtil.DOT,".");
                    if (factory.hasField(fieldRawName)) {
                        final FieldDescriptor<?> field = factory.getField(fieldRawName);
                        Class<?> type;
                        if (ComplexFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                            type = ((ComplexFieldDescriptor) field).getStoreType();
                        } else {
                            type = field.getType();
                        }
                        try {
                            if (o instanceof Collection && field.isMultiValue()) {
                                final Collection<Object> elasticValues = (Collection<Object>) castForDescriptor(o, field, FieldDescriptor.UseCase.Stored);
                                if (ComplexFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                                    if (contextualized) {
                                        document.setContextualizedValues((MultiValuedComplexField<Object, ?, ?>) field, searchContext, elasticValues);
                                    } else {
                                        document.setValues((MultiValuedComplexField<Object, ?, ?>) field, elasticValues);
                                    }

                                } else {
                                    if (contextualized) {
                                        document.setContextualizedValues((MultiValueFieldDescriptor<Object>) field, searchContext, elasticValues);
                                    } else {
                                        document.setValues((MultiValueFieldDescriptor<Object>) field, elasticValues);
                                    }
                                }

                            } else {
                                Object val = o;
                                if (val instanceof Collection) {
                                    val = ((Collection) o).iterator().next();
                                }
                                final Object storedValue = castForDescriptor(val, field, FieldDescriptor.UseCase.Stored);

                                if (contextualized) {
                                    document.setContextualizedValue((FieldDescriptor<Object>) field, searchContext, storedValue);
                                } else {
                                    document.setValue((FieldDescriptor<Object>) field, storedValue);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Unable to parse Elastic result field '{}' value '{}' to field descriptor type [{}]",
                                    fieldRawName, o.toString(), type);
                            throw new RuntimeException(e);
                        }
                    }
                });
        return document;
    }

    private static Object castForDescriptor(String s, FieldDescriptor<?> descriptor, FieldDescriptor.UseCase useCase) {

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
            return ZonedDateTime.parse(s).withZoneSameLocal(ZoneId.of("UTC"));
        }
        if(Date.class.isAssignableFrom(type)) {
            return DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(Long.valueOf(s)));
        }
        if(ByteBuffer.class.isAssignableFrom(type)) {
            return ByteBuffer.wrap(Base64.getDecoder().decode(s.getBytes(UTF_8)));
        }
        return s;
    }

    protected static Object castForDescriptor(Object o, FieldDescriptor<?> descriptor, FieldDescriptor.UseCase useCase) {

        Class<?> type = descriptor.getType();

        if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())){
            switch (useCase) {
                case Filter:
                case Facet: type = ((ComplexFieldDescriptor)descriptor).getFacetType();
                    break;
                case Stored: type = ((ComplexFieldDescriptor)descriptor).getStoreType();
                    break;
                case Suggest:
                case Fulltext: type = String.class;
                    break;
                default: type = descriptor.getType();
            }
        }

        return castForDescriptor(o,type);
    }

    private static Object castForDescriptor(Object o, Class<?> type) {

        if(o != null){
            if(Collection.class.isAssignableFrom(o.getClass())) {
                return ((Collection)o).stream()
                        .map( element -> castForDescriptor(element,type))
                        .collect(Collectors.toList());
            }
            if(Long.class.isAssignableFrom(type)) {
                if(String.class.isAssignableFrom(o.getClass()) && NumberUtils.isCreatable((String) o)) {
                    return NumberUtils.createNumber((String)o).longValue();
                }
                return ((Number)o).longValue();
            }
            if(Integer.class.isAssignableFrom(type)) {
                if(String.class.isAssignableFrom(o.getClass()) && NumberUtils.isCreatable((String) o)) {
                    return NumberUtils.createNumber((String)o).intValue();
                }
                return ((Number)o).intValue();
            }
            if(Double.class.isAssignableFrom(type)) {
                if(String.class.isAssignableFrom(o.getClass()) && NumberUtils.isCreatable((String) o)) {
                    return NumberUtils.createNumber((String)o).doubleValue();
                }
                return ((Number)o).doubleValue();
            }
            if(Number.class.isAssignableFrom(type)) {
                if(String.class.isAssignableFrom(o.getClass()) && NumberUtils.isCreatable((String) o)) {
                    return NumberUtils.createNumber((String)o).floatValue();
                }
                return ((Number)o).floatValue();
            }
            if(Boolean.class.isAssignableFrom(type)) {
                return (Boolean) o;
            }
            if(ZonedDateTime.class.isAssignableFrom(type)) {
                if(o instanceof Date){
                    return ZonedDateTime.ofInstant(((Date) o).toInstant(), ZoneId.of("UTC"));
                }
                if( Number.class.isAssignableFrom(o.getClass())) {
                    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(((Number) o).longValue()), ZoneId.of("UTC"));
                }
                return ZonedDateTime.parse(o.toString()).withZoneSameLocal(ZoneId.of("UTC"));
            }
            if(Date.class.isAssignableFrom(type)) {
                if( Number.class.isAssignableFrom(o.getClass())) {
                    return  Date.from(Instant.ofEpochMilli(((Number) o).longValue()));
                }
                if(o instanceof Date){
                    return o;
                }
                return Date.from(Instant.parse(o.toString()));
            }
            if(ByteBuffer.class.isAssignableFrom(type)) {
                final byte[] byteArray =
                        String.class.isAssignableFrom(o.getClass()) ? ((String) o).getBytes(UTF_8) : (byte[]) o;
                return ByteBuffer.wrap(Base64.getDecoder().decode(byteArray)) ;
            }
            if(LatLng.class.isAssignableFrom(type)) {
                try {
                    return LatLng.parseLatLng(o.toString()) ;
                } catch (ParseException e) {
                    log.error("Unable to parse {} to LatLong.class", o);
                    throw new SearchServerException("Unable to parse "+o.toString()+" to LatLong.class", e);
                }
            }
            if (CharSequence.class.isAssignableFrom(type)) {
                return o.toString();
            }
        }
        return null;
    }
    public static Map<String, Object> createEmptyDocument(DocumentFactory factory) {

        final Map<String, Object> docMap = new HashMap<>();
        //add fields
        factory.getFields().values().stream()
                .forEach(descriptor -> addEmptyFieldToDoc(docMap, descriptor, null));

        docMap.put(FieldUtil.ID, "");
        docMap.put(FieldUtil.TYPE, factory.getType());
        docMap.put(FieldUtil.PERCOLATOR_FLAG, true);

        return docMap;
    }

    public static Boolean equalDocs(Document doc1, Document doc2, DocumentFactory factory) {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode doc1JsonValues = mapper.valueToTree(doc1.getValues());
        final JsonNode doc2JsonValues = mapper.valueToTree(doc2.getValues());
        return FieldUtil.compareFieldLists(doc1.listFieldDescriptors().values(),factory.getFields().values())
                && FieldUtil.compareFieldLists(doc2.listFieldDescriptors().values(),factory.getFields().values())
                && FieldUtil.compareFieldLists(doc1.listFieldDescriptors().values(), doc2.listFieldDescriptors().values())
                && doc1.getType().equals(doc2.getType())
                && doc1JsonValues.equals(doc2JsonValues);
    }

    public static Boolean equalDocs(Document doc1, Map<String,Object> doc2, DocumentFactory factory) {
        final Document vindDoc = buildVindDoc(doc2, factory, null);
        return equalDocs(doc1,vindDoc, factory);
    }
}
