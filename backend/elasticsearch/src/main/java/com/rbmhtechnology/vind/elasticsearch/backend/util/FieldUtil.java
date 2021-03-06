package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.elasticsearch.backend.ElasticSearchServer;
import com.rbmhtechnology.vind.model.ComplexFieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValuedComplexField;
import com.rbmhtechnology.vind.model.SingleValuedComplexField;
import com.rbmhtechnology.vind.model.value.LatLng;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.rbmhtechnology.vind.elasticsearch.backend.util.DocumentUtil.castForDescriptor;
import static com.rbmhtechnology.vind.model.FieldDescriptor.UseCase;

public class FieldUtil {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchServer.class);

    public static final String ID = "_id_";
    public static final String TYPE = "_type_";
    public static final String PERCOLATOR_FLAG = "_percolator_flag_";
    public static final String SCORE = "score";
    public static final String DISTANCE = "_distance_";
    public static final String FACETS = "facets";

    public static final String TEXT = "text_";
    public static final String FACET = "facet_";
    public static final String _DYNAMIC = "dynamic_";
    public static final String _COMPLEX = "complex_";

    private static final String _FACET = ".facet";
    private static final String _SUGGEST = ".suggestion";
    private static final String _FILTER = ".filter";

    private static final String _SORT = ".sort";

    public static final String DOT = "U+0323";

    public static final String INTERNAL_FIELD_PREFIX = String.format("%s(%s|%s|%s|%s|%s|%s|%s|%s)",
            _DYNAMIC,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(), Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(), Fieldname.Type.BINARY.getName(), Fieldname.Type.LOCATION.getName());

    public static final String INTERNAL_COMPLEX_FIELD_PREFIX = String.format("%s(%s|%s|%s|%s|%s|%s|%s|%s)?(%s|%s|%s|%s|%s)",
            _COMPLEX,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(), Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(), Fieldname.Type.BINARY.getName(), Fieldname.Type.LOCATION.getName(),
            TEXT, FACET, "suggestion_", "sort_", "stored_"
            );


    public static Optional<String> getFieldName(FieldDescriptor<?> descriptor, String context, List<String> indexFootprint) {
        return getFieldName(descriptor, null, context, indexFootprint);
    }
    public static Optional<String> getFieldName(
            FieldDescriptor descriptor,
            UseCase useCase,
            String context,
            List<String> indexFootprint) {

        if (Objects.isNull(descriptor)) {
            log.warn("Trying to get name of null field descriptor.");
            return Optional.empty();
        }

        String contextPrefix = "";
        if (Objects.isNull(context) || !descriptor.isContextualized()) {
            contextPrefix = "";
        } else if (descriptor.isContextualized()){
            contextPrefix = context + "_";
        }
        String fieldName = _DYNAMIC;

        Fieldname.Type type = Fieldname.Type.getFromClass(descriptor.getType());

        final boolean isComplexField = ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass());
        final String descriptorName = descriptor.getName().replaceAll("\\.", DOT);
        if (Objects.isNull(useCase)) {
            return Optional.of(fieldName + type.getName() + contextPrefix + descriptorName);
        }
        final String resultName;
        switch (useCase) {
            case Fulltext: {
                if (descriptor.isFullText()) {
                    fieldName = fieldName + type.getName();
                    final String lang = "." + StringUtils.defaultIfBlank(descriptor.getLanguage().getLangCode(),"text");
                    if (isComplexField) {
                        fieldName = _COMPLEX + TEXT;
                    }
                    resultName = fieldName + contextPrefix + descriptorName + lang;
                    break;
                } else {
                    log.debug("Descriptor {} is not configured for full text search.", descriptorName);
                    resultName = null;
                    break;
                }
            }

            case Facet: {
                if(descriptor.isFacet()) {
                    if (isComplexField) {
                       type = Fieldname.Type.getFromClass(((ComplexFieldDescriptor)descriptor).getFacetType());
                       resultName = _COMPLEX + type.getName() + FACET + contextPrefix + descriptorName;
                    } else {
                        resultName = fieldName + type.getName() + contextPrefix + descriptorName + _FACET;
                    }
                } else {
                    log.debug("Descriptor {} is not configured for facet search.", descriptorName);
                    resultName = null;
                }
                break;
            }
            case Suggest: {
                if(descriptor.isSuggest()) {
                    if (isComplexField) {
                        resultName = _COMPLEX + "suggestion_" + contextPrefix + descriptorName;

                    } else {
                        type = Fieldname.Type.getFromClass(descriptor.getType());
                        resultName = fieldName + type.getName() + contextPrefix + descriptorName + _SUGGEST;
                    }
                } else {
                    log.debug("Descriptor {} is not configured for suggestion search.", descriptorName);
                    resultName = null;
                }
                break;
            }
            case Stored: {
                if (descriptor.isStored()) {
                    fieldName = fieldName + type.getName();
                    if (isComplexField) {
                        type = Fieldname.Type.getFromClass(((ComplexFieldDescriptor) descriptor).getStoreType());
                        fieldName = _COMPLEX + type.getName() + "stored_" ;
                    }

                }
                resultName = fieldName + contextPrefix + descriptorName;
                break;
            }
            case Sort: {
                fieldName = fieldName + type.getName();
                if (isComplexField) {
                    type = Fieldname.Type.getFromClass(((ComplexFieldDescriptor) descriptor).getStoreType());
                    resultName = _COMPLEX + type.getName() + "sort_" + descriptorName;
                }
                else
                    if (descriptor.isSort() && Objects.nonNull(type)){
                        resultName = fieldName + contextPrefix + descriptorName + _SORT;
                    } else if(isComplexField && descriptor.isStored() && !descriptor.isMultiValue() && Objects.nonNull(type)){
                        resultName = fieldName + contextPrefix + descriptorName + _SORT ;
                    } else {
                        log.debug("Descriptor {} is not configured for sorting.", descriptorName);
                        resultName = null; //TODO: throw runtime exception?
                    }
                break;
            }
            case Filter: {
                if(isComplexField && ((ComplexFieldDescriptor)descriptor).isAdvanceFilter() && Objects.nonNull(((ComplexFieldDescriptor)descriptor).getFacetType())) {
                    type = Fieldname.Type.getFromClass(((ComplexFieldDescriptor)descriptor).getFacetType());
                    fieldName = _COMPLEX;
                    resultName = fieldName + type.getName() + "filter_" + contextPrefix + descriptorName;

                } else {
                    log.debug("Descriptor {} is not configured for advance filter search.", descriptorName);
                    resultName = null;
                }
                break;
            }
            default: {
                log.warn("Unsupported use case {}.", useCase);
                resultName = fieldName + type.getName() + contextPrefix + descriptorName;
                break;
            }
        }
        if( Objects.nonNull(resultName) && (Objects.isNull(indexFootprint) || indexFootprint.contains(resultName.replaceAll("\\.\\w+" , "")))) {
            return Optional.of(resultName);
        } else {
            return Optional.empty();
        }

    }

    public static String getSourceFieldName(String elasticFieldName, String context) {
        final String contextPrefix = context != null ? context + "_" : "";
        final String pattern = "(" + FieldUtil.INTERNAL_FIELD_PREFIX + ")|(" + FieldUtil.INTERNAL_COMPLEX_FIELD_PREFIX + ")";
        final Matcher internalPrefixMatcher = Pattern.compile(pattern).matcher(elasticFieldName);
        final String contextualizedName = internalPrefixMatcher.replaceFirst("");
        return contextualizedName.replace(contextPrefix, "").replaceAll(DOT,".");
    }

    public static final class Fieldname {

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
    }

    public static Boolean compareFieldLists(Collection<FieldDescriptor<?>> fields1, Collection<FieldDescriptor<?>> fields2) {
        if(fields1.size()!= fields2.size()) {
            return false;
        }

        final List<String> fields1Names = fields1.stream()
                .map(FieldDescriptor::getName)
                .collect(Collectors.toList());

        final List<String> fields2Names = fields2.stream()
                .map(FieldDescriptor::getName)
                .collect(Collectors.toList());

        return fields1Names.containsAll(fields2Names);
    }

    public static Class<?> getFieldType(FieldDescriptor<?> descriptor, UseCase useCase) {
        if(ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass())) {
            return getComplexFieldType((ComplexFieldDescriptor<?, ?, ?>) descriptor,useCase);
        }
        return descriptor.getType();
    }
    public static Class<?> getComplexFieldType(ComplexFieldDescriptor<?,?,?> descriptor, UseCase useCase) {
        switch (useCase) {
            case Suggest:
                if(descriptor.isSuggest() && descriptor.getSuggestFunction() != null) {
                    return String.class;
                }
                break;
            case Facet:
                if(descriptor.isFacet() && descriptor.getFacetFunction() != null) {
                    return descriptor.getFacetType();
                }
                break;
            case Filter:
                if(descriptor.isAdvanceFilter() && descriptor.getAdvanceFilter() != null) {
                    return descriptor.getFacetType();
                }
                break;
            case Stored:
                if(descriptor.isStored() && descriptor.getStoreFunction() != null) {
                    return descriptor.getStoreType();
                }
                break;
            case Sort:
                if(descriptor.isSort()){
                    return descriptor.getStoreType();
                }
                break;
            case Fulltext:
                if(descriptor.isFullText() && descriptor.getFullTextFunction() != null) {
                    return String.class;
                }
                break;
        }
        return null;
    }
}
