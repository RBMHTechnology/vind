package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 27.06.16.
 */

public class SolrFilterSerializer {

    private static Logger log = LoggerFactory.getLogger(SolrFilterSerializer.class);

    private DocumentFactory factory;
    private final boolean strict;

    public SolrFilterSerializer(DocumentFactory factory, boolean strict) {
        this.factory = factory;
        this.strict = strict;
    }

    public String serialize(Filter filter, String searchContext) {
        if(!strict) {
           filter = checkScopeValidity(filter);
        }
        if(filter instanceof Filter.AndFilter) return serialize((Filter.AndFilter)filter, searchContext);
        if(filter instanceof Filter.OrFilter) return serialize((Filter.OrFilter)filter, searchContext);
        if(filter instanceof Filter.NotFilter) return serialize((Filter.NotFilter)filter, searchContext);
        if(filter instanceof Filter.TermFilter) return serialize((Filter.TermFilter)filter, searchContext);
        if(filter instanceof Filter.TermsQueryFilter) return serialize((Filter.TermsQueryFilter)filter, searchContext);
        if(filter instanceof Filter.PrefixFilter) return serialize((Filter.PrefixFilter)filter, searchContext);
        if(filter instanceof Filter.DescriptorFilter) return serialize((Filter.DescriptorFilter)filter, searchContext);
        if(filter instanceof Filter.BeforeFilter) return serialize((Filter.BeforeFilter)filter, searchContext);
        if(filter instanceof Filter.AfterFilter) return serialize((Filter.AfterFilter)filter, searchContext);
        if(filter instanceof Filter.GreaterThanFilter) return serialize((Filter.GreaterThanFilter)filter, searchContext);
        if(filter instanceof Filter.LowerThanFilter) return serialize((Filter.LowerThanFilter)filter, searchContext);
        if(filter instanceof Filter.BetweenDatesFilter) return serialize((Filter.BetweenDatesFilter)filter, searchContext);
        if(filter instanceof Filter.BetweenNumericFilter) return serialize((Filter.BetweenNumericFilter)filter, searchContext);
        if(filter instanceof Filter.WithinBBoxFilter) return serialize((Filter.WithinBBoxFilter)filter, searchContext);
        if(filter instanceof Filter.WithinCircleFilter) return serialize((Filter.WithinCircleFilter)filter, searchContext);
        if(filter instanceof Filter.NotEmptyTextFilter) return serialize((Filter.NotEmptyTextFilter)filter, searchContext);
        if(filter instanceof Filter.NotEmptyFilter) return serialize((Filter.NotEmptyFilter)filter, searchContext);
        if(filter instanceof Filter.NotEmptyLocationFilter) return serialize((Filter.NotEmptyLocationFilter)filter, searchContext);
        if(filter instanceof Filter.ChildrenDocumentFilter) return serialize((Filter.ChildrenDocumentFilter)filter, searchContext);
        if (Objects.isNull(filter)) {
            return "";
        }
        throw new RuntimeException("Filter '" + filter.getClass() + "' not supported!");
    }

    public String serialize(Filter.AndFilter filter, String searchContext) {
        return filter.getChildren().stream().map(f -> serialize(f, searchContext)).collect(Collectors.joining(" AND ", "(", ")"));
    }

    public String serialize(Filter.OrFilter filter, String searchContext) {
        return filter.getChildren().stream().map(f -> serialize(f, searchContext)).collect(Collectors.joining(" OR ", "(", ")"));
    }

    public String serialize(Filter.NotFilter filter, String searchContext) {
        return "NOT(" + serialize(filter.getDelegate(), searchContext) + ")";
    }

    public String serialize(Filter.TermFilter filter, String searchContext) {
        return getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())) + ":\"" + filter.getTerm() + "\"";
    }

    public String serialize(Filter.TermsQueryFilter filter, String searchContext) {
        return SolrUtils.Query.buildSolrTermsQuery(filter.getTerm(), filter.getDescriptor(), filter.getFilterScope(), searchContext);
    }

    public String serialize(Filter.PrefixFilter filter, String searchContext) {
        return getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())) + ":" + filter.getTerm() + "*";
    }

    public String serialize(Filter.DescriptorFilter filter, String searchContext) {
        return getFieldName(filter.getDescriptor().getName(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getDescriptor()).toString())) + ":\"" + (filter.getTerm() instanceof ZonedDateTime ? DateTimeFormatter.ISO_INSTANT.format((ZonedDateTime)filter.getTerm()) : filter.getTerm())+ "\"";
    }

    public String serialize(Filter.BeforeFilter filter, String searchContext) {
        return String.format("%s:[* TO %s]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())), filter.getDate());
    }

    public String serialize(Filter.AfterFilter filter, String searchContext) {
        return String.format("%s:[%s TO *]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())), filter.getDate());
    }
    public String serialize(Filter.LowerThanFilter filter, String searchContext) {
        return String.format("%s:[* TO %s]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())), filter.getNumber());
    }
    public String serialize(Filter.GreaterThanFilter filter, String searchContext) {
        return String.format("%s:[%s TO *]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())), filter.getNumber());
    }
    public String serialize(Filter.BetweenDatesFilter filter, String searchContext) {
        return String.format("%s:[%s TO %s]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())), filter.getStart(), filter.getEnd());
    }
    public String serialize(Filter.BetweenNumericFilter filter, String searchContext) {
        return String.format("%s:[%s TO %s]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())), filter.getStart(), filter.getEnd());

    }
    public String serialize(Filter.WithinBBoxFilter filter, String searchContext) {
        //TODO: could be also bbox filter function
        return String.format("%s:[%s TO %s]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())), filter.getUpperLeft(), filter.getLowerRight());
    }

    public String serialize(Filter.WithinCircleFilter filter, String searchContext) {
        return String.format("{!geofilt sfield=%s pt=%s d=%s}", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())), filter.getCenter(), filter.getDistance());
    }
    public String serialize(Filter.NotEmptyTextFilter filter, String searchContext) {
        return String.format("%s:['' TO *]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())));
    }
    public String serialize(Filter.NotEmptyFilter filter, String searchContext) {
        return String.format("%s:*", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())));
    }
    public String serialize(Filter.NotEmptyLocationFilter filter, String searchContext) {
        return String.format("%s:[-90,-180 TO 90,180]", getFieldName(filter.getField(), searchContext, SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(),factory).toString())));
    }
    public String serialize(Filter.ChildrenDocumentFilter filter, String searchContext) {
        if(StringUtils.isNotEmpty(filter.getNestedDocType())) {
            return String.format("{!parent which=\"%s:%s\" v='%s:* AND %s:%s'}", SolrUtils.Fieldname.TYPE, filter.getParentDocType(), SolrUtils.Fieldname.ID, SolrUtils.Fieldname.TYPE, filter.getNestedDocType());
        } else {
            return String.format("{!parent which=\"%s:%s\"  v='%s:* AND -%s:%s'}", SolrUtils.Fieldname.TYPE, filter.getParentDocType(), SolrUtils.Fieldname.ID, SolrUtils.Fieldname.TYPE, filter.getParentDocType());
        }
    }


    public Filter checkScopeValidity(Filter filter) {
        if(Objects.isNull(filter)) { return null; }
        if(filter instanceof Filter.AndFilter) return coolNamedMethod((Filter.AndFilter) filter);
        if(filter instanceof Filter.OrFilter) return coolNamedMethod((Filter.OrFilter) filter);
        if(filter instanceof Filter.NotFilter) return coolNamedMethod((Filter.NotFilter) filter);
        if(filter instanceof Filter.TermFilter) return coolNamedMethod((Filter.TermFilter) filter);
        if(filter instanceof Filter.TermsQueryFilter) return coolNamedMethod((Filter.TermsQueryFilter)filter);
        if(filter instanceof Filter.PrefixFilter) return coolNamedMethod((Filter.PrefixFilter) filter);
        if(filter instanceof Filter.DescriptorFilter) return coolNamedMethod((Filter.DescriptorFilter) filter);
        if(filter instanceof Filter.BeforeFilter) return coolNamedMethod((Filter.BeforeFilter) filter);
        if(filter instanceof Filter.AfterFilter) return coolNamedMethod((Filter.AfterFilter) filter);
        if(filter instanceof Filter.GreaterThanFilter) return coolNamedMethod((Filter.GreaterThanFilter) filter);
        if(filter instanceof Filter.LowerThanFilter) return coolNamedMethod((Filter.LowerThanFilter) filter);
        if(filter instanceof Filter.BetweenDatesFilter) return coolNamedMethod((Filter.BetweenDatesFilter) filter);
        if(filter instanceof Filter.BetweenNumericFilter) return coolNamedMethod((Filter.BetweenNumericFilter) filter);
        if(filter instanceof Filter.WithinBBoxFilter) return coolNamedMethod((Filter.WithinBBoxFilter) filter);
        if(filter instanceof Filter.WithinCircleFilter) return coolNamedMethod((Filter.WithinCircleFilter) filter);
        if(filter instanceof Filter.NotEmptyTextFilter) return coolNamedMethod((Filter.NotEmptyTextFilter) filter);
        if(filter instanceof Filter.NotEmptyFilter) return coolNamedMethod((Filter.NotEmptyFilter) filter);
        if(filter instanceof Filter.NotEmptyLocationFilter) return coolNamedMethod((Filter.NotEmptyLocationFilter) filter);
        if(filter instanceof Filter.ChildrenDocumentFilter) return coolNamedMethod((Filter.ChildrenDocumentFilter)filter);
        throw new RuntimeException("Filter '" + filter.getClass() + "' not supported!");
    }

    public Filter coolNamedMethod(Filter.AndFilter filter) {
        final Set<Filter> filters = filter.getChildren().stream()
                .map(f -> checkScopeValidity(f))
                .filter(f -> Objects.nonNull(f))
                .collect(Collectors.toSet());
        if (filters.size() > 1) {
            return Filter.AndFilter.fromSet(filters);
        } else if (CollectionUtils.isNotEmpty(filters)){
            return filters.iterator().next();
        }
        return null;
    }

    public Filter coolNamedMethod(Filter.OrFilter filter) {
        final Set<Filter> filters = filter.getChildren().stream()
                .map(f -> checkScopeValidity(f))
                .filter(f -> Objects.nonNull(f))
                .collect(Collectors.toSet());
        if (filters.size() > 1) {
            return Filter.OrFilter.fromSet(filters);
        } else if (CollectionUtils.isNotEmpty(filters)){
            return filters.iterator().next();
        }
        return null;
    }

    public Filter coolNamedMethod(Filter.NotFilter filter) {
        if (Objects.nonNull(checkScopeValidity(filter.getDelegate()))) {
            return filter;
        } else {
            return null;
        }
    }
    public Filter coolNamedMethod(Filter.TermFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }

    public Filter coolNamedMethod(Filter.TermsQueryFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }

    public Filter coolNamedMethod(Filter.PrefixFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }

    public Filter coolNamedMethod(Filter.DescriptorFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getDescriptor().getName());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }

    public Filter coolNamedMethod(Filter.BeforeFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }

    public Filter coolNamedMethod(Filter.AfterFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }
    public Filter coolNamedMethod(Filter.LowerThanFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }
    public Filter coolNamedMethod(Filter.GreaterThanFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }
    public Filter coolNamedMethod(Filter.BetweenDatesFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }
    public Filter coolNamedMethod(Filter.BetweenNumericFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }

    }
    public Filter coolNamedMethod(Filter.WithinBBoxFilter filter) {
        //TODO: could be also bbox filter function
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }

    public Filter coolNamedMethod(Filter.WithinCircleFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }
    public Filter coolNamedMethod(Filter.NotEmptyTextFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }
    public Filter coolNamedMethod(Filter.NotEmptyFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }
    public Filter coolNamedMethod(Filter.NotEmptyLocationFilter filter) {
        final FieldDescriptor descriptor = factory.getField(filter.getField());
        if(Objects.isNull(descriptor)){
            return null;
        } else {
            return filter;
        }
    }
    public Filter coolNamedMethod(Filter.ChildrenDocumentFilter filter) {
        return filter;
    }

    private String getFieldName(String name, String searchContext, SolrUtils.Fieldname.UseCase usecase) {
        FieldDescriptor descriptor = factory.getField(name);

        if(Objects.isNull(descriptor)){
            log.error("Unable to serialize solr filter: there is no field descriptor with name '{}'", name);
            throw new IllegalArgumentException("Unable to serialize solr filter: there is no field descriptor with name '" + name +"'");
        }

        if(Objects.isNull(usecase)) {
            usecase = SolrUtils.Fieldname.UseCase.Facet;
        }

        final String fieldName = SolrUtils.Fieldname.getFieldname(descriptor, usecase, searchContext);
        if (Objects.isNull(fieldName)) {
            log.error("Unable to serialize solr filter: there is no valid solr field for descriptor with name '{}' and use case {}", name, usecase);
            throw new RuntimeException("Unable to serialize solr filter: there is no valid solr field for descriptor with name '"+ name +"' and use case " + usecase);
        }
        return fieldName;
    }

}
