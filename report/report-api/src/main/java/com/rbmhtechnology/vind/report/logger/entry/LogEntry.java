/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.logger.entry;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.report.model.application.Application;
import com.rbmhtechnology.vind.report.model.request.facet.FacetMixin;
import com.rbmhtechnology.vind.report.model.request.filter.AndFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.FilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.NotFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.OrFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.sort.SortMixIn;
import com.rbmhtechnology.vind.report.model.session.Session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created on 03.10.17.
 */
/*@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="@class")
@JsonSubTypes({ @JsonSubTypes.Type(value = FullTextEntry.class, name = "FullTextEntry") ,
                @JsonSubTypes.Type(value = SuggestionEntry.class, name = "SuggestionEntry"),
                @JsonSubTypes.Type(value = InteractionEntry.class, name = "SuggestionEntry")})*/
public abstract class LogEntry {


    public abstract Application getApplication();
    public abstract Session getSession();
    public abstract EntryType getType();
    public abstract ZonedDateTime getTimeStamp();

    public String toJson(){
        try {
            return LogEntry.getMapper().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static ObjectMapper getMapper(){
        return new ObjectMapper()
                .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .addMixIn(Filter.class, FilterMixIn.class)
                .addMixIn(Filter.AndFilter.class, AndFilterMixIn.class)
                .addMixIn(Filter.OrFilter.class, OrFilterMixIn.class)
                .addMixIn(Filter.NotFilter.class, NotFilterMixIn.class)
                .addMixIn(Facet.class, FacetMixin.class)
                .addMixIn(Sort.class, SortMixIn.class)
                ;
    }

    public enum EntryType {
        fulltext, suggestion, interaction
    }
}
