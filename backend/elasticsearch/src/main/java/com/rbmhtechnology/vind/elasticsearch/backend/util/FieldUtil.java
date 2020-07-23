package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.elasticsearch.backend.ElasticSearchServer;
import com.rbmhtechnology.vind.model.ComplexFieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.value.LatLng;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rbmhtechnology.vind.elasticsearch.backend.util.FieldUtil.Fieldname.UseCase;

public class FieldUtil {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchServer.class);

    public static final String ID = "_id_";
    public static final String TYPE = "_type_";
    public static final String SCORE = "score";
    public static final String DISTANCE = "_distance_";
    public static final String FACETS = "facets";

    public static final String TEXT = "text_";
    public static final String FACET = "facet_";
    private static final String _DYNAMIC = "dynamic_";
    private static final String _COMPLEX = "complex_";
    private static final String _STORED = ".stored";
    private static final String _MULTI = "multi_";
    private static final String _SINGLE = "single_";
    private static final String _FACET = ".facet";
    private static final String _SUGGEST = ".suggestion";
    private static final String _FILTER = ".filter";

    private static final String _SORT = ".sort";

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

    public static final String INTERNAL_FACET_FIELD_PREFIX = String.format("%s(%s)?%s(%s|%s|%s|%s|%s|%s|%s)",
            _DYNAMIC,
            _STORED,
            _FACET,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(), Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(), Fieldname.Type.LOCATION.getName());

    public static final String INTERNAL_SCOPE_FACET_FIELD_PREFIX = String.format("%s(%s|%s)(%s)?(%s|%s|%s)(%s|%s|%s|%s|%s|%s|%s)",
            _DYNAMIC,
            _MULTI,_SINGLE,
            _STORED,
            _FACET,_SUGGEST,_FILTER,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(), Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(), Fieldname.Type.LOCATION.getName());

    public static final String INTERNAL_SUGGEST_FIELD_PREFIX = String.format("%s(%s|%s)(%s)?%s(%s|%s|%s|%s|%s|%s|%s|%s)",
            _DYNAMIC,
            _MULTI,_SINGLE,
            _STORED,
            _SUGGEST,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(), Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(), Fieldname.Type.LOCATION.getName(), Fieldname.Type.ANALYZED.getName());

    public static final String INTERNAL_CONTEXT_PREFIX = "(%s_)?";

    public static String getFieldName(FieldDescriptor<?> descriptor, String context) {
        return getFieldName(descriptor, null, context);
    }
    public static String getFieldName(FieldDescriptor descriptor, UseCase useCase, String context) {

        if (Objects.isNull(descriptor)) {
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

        Fieldname.Type type = Fieldname.Type.getFromClass(descriptor.getType());

        final boolean isComplexField = ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass());
        if (Objects.isNull(useCase)) {
            return fieldName + type.getName() + contextPrefix + descriptor.getName();
        }
        switch (useCase) {
            case Fulltext: {
                if (descriptor.isFullText()) {
                    fieldName = fieldName + type.getName();
                    final String lang = "." + StringUtils.defaultIfBlank(descriptor.getLanguage().getLangCode(),"text");
                    if (isComplexField) {
                        fieldName = _COMPLEX + TEXT;
                    }
                    return fieldName + contextPrefix + descriptor.getName() + lang;
                } else {
                    log.debug("Descriptor {} is not configured for full text search.", descriptor.getName());
                    return null;
                }
            }

            case Facet: {
                if(descriptor.isFacet()) {
                    if (isComplexField) {
                       type = Fieldname.Type.getFromClass(((ComplexFieldDescriptor)descriptor).getFacetType());
                        return _COMPLEX + type.getName() + FACET + contextPrefix + descriptor.getName() + _FACET;
                    }
                    return fieldName + type.getName() + contextPrefix + descriptor.getName() + _FACET;

                } else {
                    log.debug("Descriptor {} is not configured for facet search.", descriptor.getName());
                    return null;
                }
            }
            case Suggest: {
                if(descriptor.isSuggest()) {
                    if (isComplexField) {
                        return _COMPLEX + "suggestion_" + contextPrefix + descriptor.getName() + _SUGGEST;
                    } else {
                        type = Fieldname.Type.getFromClass(descriptor.getType());
                        return fieldName + type.getName() + contextPrefix + descriptor.getName() + _SUGGEST;
                    }
                } else {
                    log.debug("Descriptor {} is not configured for suggestion search.", descriptor.getName());
                    return null;
                }
            }
            case Stored: { //TODO
                if (descriptor.isStored()) {
                    fieldName = fieldName + type.getName();
                    if (isComplexField) {
                        type = Fieldname.Type.getFromClass(((ComplexFieldDescriptor) descriptor).getStoreType());
                        fieldName = _COMPLEX + type.getName() + "stored_" ;
                    }

                }
                return fieldName + contextPrefix + descriptor.getName();
            }
            case Sort: {
                fieldName = fieldName + type.getName();
                if (isComplexField) {
                    type = Fieldname.Type.getFromClass(((ComplexFieldDescriptor) descriptor).getStoreType());
                    fieldName = _COMPLEX + type.getName() + "sort_";
                }
                if (descriptor.isSort() && Objects.nonNull(type)){
                    return fieldName + contextPrefix + descriptor.getName() + _SORT;
                } else if(isComplexField && descriptor.isStored() && !descriptor.isMultiValue() && Objects.nonNull(type)){
                    return fieldName + contextPrefix + descriptor.getName() + _SORT ;
                } else {
                    log.debug("Descriptor {} is not configured for sorting.", descriptor.getName());
                    return null; //TODO: throw runtime exception?
                }
            }
            case Filter: {
                if(isComplexField && ((ComplexFieldDescriptor)descriptor).isAdvanceFilter() && Objects.nonNull(((ComplexFieldDescriptor)descriptor).getFacetType())) {
                    type = Fieldname.Type.getFromClass(((ComplexFieldDescriptor)descriptor).getFacetType());
                    return fieldName + type.getName() + "filter_" + contextPrefix + descriptor.getName() + _FILTER;

                } else {
                    log.debug("Descriptor {} is not configured for advance filter search.", descriptor.getName());
                    return null;
                }
            }
            default: {
                log.warn("Unsupported use case {}.", useCase);
                return fieldName + type.getName() + contextPrefix + descriptor.getName();
            }
        }
    }

    public static String getSourceFieldName(String elasticFieldName, String context) {
        final String contextPrefix = context != null ? context + "_" : "";
        final String pattern = "(" + FieldUtil.INTERNAL_FIELD_PREFIX + ")|(" + FieldUtil.INTERNAL_COMPLEX_FIELD_PREFIX + ")";
        final Matcher internalPrefixMatcher = Pattern.compile(pattern).matcher(elasticFieldName);
        final String contextualizedName = internalPrefixMatcher.replaceFirst("");
        return contextualizedName.replace(contextPrefix, "");
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
    }

}
