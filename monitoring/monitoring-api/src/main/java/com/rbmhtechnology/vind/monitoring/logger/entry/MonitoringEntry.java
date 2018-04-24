/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.logger.entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.monitoring.model.application.Application;
import com.rbmhtechnology.vind.monitoring.model.request.DateMathExpressionMixIn;
import com.rbmhtechnology.vind.monitoring.model.request.RootTimeMixIn;
import com.rbmhtechnology.vind.monitoring.model.request.facet.*;
import com.rbmhtechnology.vind.monitoring.model.request.filter.*;
import com.rbmhtechnology.vind.monitoring.model.session.Session;
import com.rbmhtechnology.vind.monitoring.model.request.sort.SortMixIn;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Objects;

/**
 * Created on 03.10.17.
 */

public abstract class MonitoringEntry {



    public HashMap<String,Object> metadata = new HashMap<>();

    public abstract Application getApplication();
    public abstract Session getSession();
    public abstract EntryType getType();
    public abstract ZonedDateTime getTimeStamp();

    public void addMetadata(String key, Object value) {
        if (Objects.nonNull(key)) {
            metadata.put(key, value);
        }
    }

    public HashMap<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(HashMap<String, Object> metadata) {
        if (Objects.nonNull(metadata)) {
            this.metadata = metadata;
        } else {
            this.metadata = new HashMap<>();
        }
    }

    public String toJson(){
        try {
            return MonitoringEntry.getMapper().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static ObjectMapper getMapper(){
        final ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(Filter.class, FilterMixIn.class)
                .addMixIn(Filter.AndFilter.class, AndFilterMixIn.class)
                .addMixIn(Filter.OrFilter.class, OrFilterMixIn.class)
                .addMixIn(Filter.NotFilter.class, NotFilterMixIn.class)
                .addMixIn(Facet.class, FacetMixin.class)
                .addMixIn(Sort.class, SortMixIn.class)
                .addMixIn(DateMathExpression.class, DateMathExpressionMixIn.class)
                .addMixIn(DateMathExpression.RootTime.class, RootTimeMixIn.class)
                .addMixIn(Interval.ZonedDateTimeInterval.class, ZoneDateIntervalMixin.class)
                .addMixIn(Interval.UtilDateInterval.class, UtilDateIntervalMixin.class)
                .addMixIn(Interval.DateMathInterval.class, DateMathIntervalMixin.class)
                .addMixIn(Facet.DateRangeFacet.class, DateRangeMixin.class)
                .addMixIn(Filter.BetweenDatesFilter.class, BetweenDatesMixIn.class);
        objectMapper.getSerializerProvider().setNullKeySerializer(new MyDtoNullKeySerializer());
        return objectMapper;
    }

    public enum EntryType {
        fulltext, suggestion, index, get, delete, update, interaction
    }

    static class MyDtoNullKeySerializer extends StdSerializer<Object> {
        public MyDtoNullKeySerializer() {
            this(null);
        }

        public MyDtoNullKeySerializer(Class<Object> t) {
            super(t);
        }

        @Override
        public void serialize(Object nullKey, JsonGenerator jsonGenerator, SerializerProvider unused)
                throws IOException, JsonProcessingException {
            jsonGenerator.writeFieldName("");
        }
    }
}
