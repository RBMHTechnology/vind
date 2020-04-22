package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.elasticsearch.backend.ElasticSearchServer;
import com.rbmhtechnology.vind.model.ComplexFieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.value.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldUtil {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchServer.class);

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

    public static final String INTERNAL_FIELD_PREFIX = String.format("%s(%s|%s|%s|%s|%s|%s|%s|%s)",
            _DYNAMIC,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(),Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(),Fieldname.Type.BINARY.getName(),Fieldname.Type.LOCATION.getName());

    public static final String INTERNAL_FACET_FIELD_PREFIX = String.format("%s(%s)?%s(%s|%s|%s|%s|%s|%s|%s)",
            _DYNAMIC,
            _STORED,
            _FACET,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(),Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(),Fieldname.Type.LOCATION.getName());

    public static final String INTERNAL_SCOPE_FACET_FIELD_PREFIX = String.format("%s(%s|%s)(%s)?(%s|%s|%s)(%s|%s|%s|%s|%s|%s|%s)",
            _DYNAMIC,
            _MULTI,_SINGLE,
            _STORED,
            _FACET,_SUGGEST,_FILTER,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(),Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(),Fieldname.Type.LOCATION.getName());

    public static final String INTERNAL_SUGGEST_FIELD_PREFIX = String.format("%s(%s|%s)(%s)?%s(%s|%s|%s|%s|%s|%s|%s|%s)",
            _DYNAMIC,
            _MULTI,_SINGLE,
            _STORED,
            _SUGGEST,
            Fieldname.Type.BOOLEAN.getName(), Fieldname.Type.DATE.getName(),
            Fieldname.Type.INTEGER.getName(), Fieldname.Type.LONG.getName(),Fieldname.Type.NUMBER.getName(),
            Fieldname.Type.STRING.getName(),Fieldname.Type.LOCATION.getName(), Fieldname.Type.ANALYZED.getName());

    public static final String INTERNAL_CONTEXT_PREFIX = "(%s_)?";

    public static String getFieldName(FieldDescriptor<?> descriptor, String context) {

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

        //TODO check complexFields implementation
        final boolean isComplexField = ComplexFieldDescriptor.class.isAssignableFrom(descriptor.getClass());

        final Fieldname.Type type = Fieldname.Type.getFromClass(descriptor.getType());
        return fieldName + type.getName() + contextPrefix + descriptor.getName();
    }

    public static String getSourceFieldName(String elasticFieldName, String context) {
        final String contextPrefix = context != null ? context + "_" : "";
        final Matcher internalPrefixMatcher = Pattern.compile(FieldUtil.INTERNAL_FIELD_PREFIX).matcher(elasticFieldName);
        final String contextualizedName = internalPrefixMatcher.replaceFirst("");
        final boolean contextualized = Objects.nonNull(context) && contextualizedName.contains(contextPrefix);
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
