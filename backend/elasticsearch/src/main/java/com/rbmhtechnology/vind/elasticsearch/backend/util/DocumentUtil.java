package com.rbmhtechnology.vind.elasticsearch.backend.util;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
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
                .forEach(descriptor -> addFieldToDoc(doc, docMap, descriptor));

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
                .forEach(descriptor -> addFieldToDoc(doc, docMap, descriptor));

        addFieldToDoc(doc, docMap, InverseSearchQueryFactory.BINARY_QUERY_FIELD);

        docMap.put(FieldUtil.ID, doc.getId());
        docMap.put(FieldUtil.TYPE, doc.getType());
        docMap.put(FieldUtil.PERCOLATOR_FLAG, false);

        return docMap;
    }

    private static void addFieldToDoc(Document doc, Map<String, Object> docMap, FieldDescriptor<?> descriptor) {
        if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            addFieldToDoc(doc, docMap, (ComplexFieldDescriptor) descriptor);
        } else {
            doc.getFieldContexts(descriptor)
                    .forEach(context ->
                            Optional.ofNullable(FieldUtil.getFieldName(descriptor, context))
                                    .ifPresent(fieldName ->
                                            Optional.ofNullable(doc.getContextualizedValue(descriptor, context))
                                                    .ifPresent(value -> docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType(value)))
                                    )
                    );
        }
    }

    private static void addFieldToDoc(Map<String, Object> docMap, FieldDescriptor<?> descriptor) {
        if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            addFieldToDoc( docMap, (ComplexFieldDescriptor) descriptor);
        } else {
            Optional.ofNullable(FieldUtil.getFieldName(descriptor, null))
                    .ifPresent(fieldName ->
                            docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType("0"))
                    );
        }
    }
    private static void addFieldToDoc(InverseSearchQuery doc, Map<String, Object> docMap, FieldDescriptor<?> descriptor) {
        if (ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            addFieldToDoc(doc, docMap, (ComplexFieldDescriptor) descriptor);
        } else {
            Optional.ofNullable(FieldUtil.getFieldName(descriptor, null))
                    .ifPresent(fieldName ->
                            Optional.ofNullable(doc.getValue(descriptor))
                                    .ifPresent(value -> docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType(value)))
                    );
        }
    }

    private static void addFieldToDoc(Document doc, Map<String, Object> docMap, ComplexFieldDescriptor<?,?,?> descriptor) {
        doc.getFieldContexts(descriptor)
                .forEach(context ->
                    Stream.of(FieldUtil.Fieldname.UseCase.values()).forEach( useCase -> {
                        final String name = FieldUtil.getFieldName(descriptor, useCase, context);
                        Optional.ofNullable(name).ifPresent( fieldName ->
                            Optional.ofNullable( doc.getContextualizedValue(descriptor, context)).ifPresent(
                                contextualizedValue ->
                                    docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType(contextualizedValue, descriptor, useCase)))
                        );
                    })
                );
    }

    private static void addFieldToDoc( Map<String, Object> docMap, ComplexFieldDescriptor<?,?,?> descriptor) {

        Stream.of(FieldUtil.Fieldname.UseCase.values()).forEach( useCase -> {
            final String name = FieldUtil.getFieldName(descriptor, useCase, null);
            Optional.ofNullable(name)
                    .ifPresent( fieldName ->
                            docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType("0", descriptor, useCase))
            );
        });
    }

    private static void addFieldToDoc(InverseSearchQuery doc, Map<String, Object> docMap, ComplexFieldDescriptor<?,?,?> descriptor) {

                        Stream.of(FieldUtil.Fieldname.UseCase.values()).forEach( useCase -> {
                            final String name = FieldUtil.getFieldName(descriptor, useCase, null);
                            Optional.ofNullable(name).ifPresent( fieldName ->
                                    Optional.ofNullable( doc.getValue(descriptor)).ifPresent(
                                            contextualizedValue ->
                                                    docMap.put(fieldName.replaceAll("\\.\\w+" , ""), toElasticType(contextualizedValue, descriptor, useCase)))
                            );
                        });
    }

    private static Object toElasticType(Object value) {
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
    private static Object toElasticType(Object value, ComplexFieldDescriptor descriptor, FieldUtil.Fieldname.UseCase useCase) {
        if(value!=null) {
            if (Object[].class.isAssignableFrom(value.getClass())) {
                return toElasticType(Arrays.asList((Object[]) value, descriptor, useCase));
            }
            if (Collection.class.isAssignableFrom(value.getClass())) {
                final Collection<Object> values = (Collection<Object>) value;
                List<Object> objs = values.stream()
                        .map(v -> toElasticType(v, descriptor, useCase))
                        .collect(Collectors.toList());

                if (objs.stream().allMatch( o -> Collection.class.isAssignableFrom(o.getClass()))) {
                    objs =  objs.stream()
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
                                return toElasticType(value, singleField, FieldUtil.Fieldname.UseCase.Stored);
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

    public static Document buildVindDoc(SearchHit hit , DocumentFactory factory, String searchContext) {

        final Document document = buildVindDoc(hit.getSourceAsMap(), factory, searchContext);


        // Setting score if present in result
        document.setScore(hit.getScore());

        // Setting distance if present in result
        Optional.ofNullable(hit.field(FieldUtil.DISTANCE))
                .ifPresent(distance -> document.setDistance(((Double)distance.getValue()).floatValue()/1000));

        return document;
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
                    final String fieldRawName = contextualizedName.replace(contextPrefix, "");
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
                                final Collection<Object> elasticValues = new ArrayList<>();
                                if (ZonedDateTime.class.isAssignableFrom(type)) {
                                    ((Collection<?>) o).stream()
                                            .filter(Objects::nonNull)
                                            .forEach(ob -> elasticValues.add(ZonedDateTime.parse(ob.toString())));
                                } else if (Date.class.isAssignableFrom(type)) {
                                    ((Collection<?>) o).stream()
                                            .filter(Objects::nonNull)
                                            .forEach(ob -> elasticValues.add(Date.from(Instant.parse(ob.toString()))));
                                } else if (LatLng.class.isAssignableFrom(type)) {
                                    ((Collection<?>) o).stream()
                                            .filter(Objects::nonNull)
                                            .forEach(ob -> {
                                        try {
                                            elasticValues.add(LatLng.parseLatLng(ob.toString()));
                                        } catch (ParseException e) {
                                            log.error("Unable to parse Elastic result field '{}' value '{}' to field descriptor type [{}]",
                                                    fieldRawName, o.toString(), type);
                                            throw new RuntimeException(e);
                                        }
                                    });
                                } else {
                                    final Collection<Object> values = ((Collection<?>) o).stream()
                                            .filter(Objects::nonNull)
                                            .map(ob -> castForDescriptor(ob, field))
                                            .collect(Collectors.toList());

                                    elasticValues.addAll(values);
                                }

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
                                Object storedValue;
                                if (ZonedDateTime.class.isAssignableFrom(type)) {
                                    storedValue = ZonedDateTime.parse(val.toString()).withZoneSameLocal(ZoneId.of("UTC"));
                                } else if (Date.class.isAssignableFrom(type)) {
                                    storedValue = Date.from(Instant.parse(val.toString())) ;
                                } else if (LatLng.class.isAssignableFrom(type)) {
                                    storedValue = LatLng.parseLatLng(val.toString());
                                } else {
                                    storedValue = castForDescriptor(val, field, FieldUtil.Fieldname.UseCase.Stored);
                                }
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

    private static Object castForDescriptor(String s, FieldDescriptor<?> descriptor, FieldUtil.Fieldname.UseCase useCase) {

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

    protected static Object castForDescriptor(Object o, FieldDescriptor<?> descriptor, FieldUtil.Fieldname.UseCase useCase) {

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
                return Date.from(Instant.parse(o.toString()));
            }
            if(ByteBuffer.class.isAssignableFrom(type)) {
                return ByteBuffer.wrap(Base64.getDecoder().decode(((String) o).getBytes(UTF_8))) ;
            }
            if (String.class.isAssignableFrom(type)) {
                return o.toString();
            }
        }
        return o;
    }
    public static Map<String, Object> createEmptyDocument(DocumentFactory factory) {

        final Map<String, Object> docMap = new HashMap<>();
        //add fields
        factory.getFields().values().stream()
                .forEach(descriptor -> addFieldToDoc(docMap, descriptor));

        docMap.put(FieldUtil.ID, "");
        docMap.put(FieldUtil.TYPE, factory.getType());
        docMap.put(FieldUtil.PERCOLATOR_FLAG, true);

        return docMap;
    }
}
