package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.utils.SerializerVisitor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by fonso on 20.07.17.
 */
public  class SolrChildrenSerializerVisitor implements SerializerVisitor {

    private static Logger log = LoggerFactory.getLogger(SolrFilterSerializer.class);

    private final DocumentFactory parentFactory;
    private final boolean strict;


    private final String TYPE_FILTER = "%s:%s";
    private final String CHILD_QUERY_TEMPLATE = "{!parent which='%s:%s' v='%s AND %s'}";

    private String searchContext;
    private final DocumentFactory childFactory;

    public SolrChildrenSerializerVisitor(DocumentFactory parentFactory, DocumentFactory childFactory, String searchContext,boolean strict){
        this.parentFactory = parentFactory;
        this.childFactory = childFactory;
        this.searchContext = searchContext;
        this.strict = strict;
    }

    public String visit(Filter filter) {
        if(filter instanceof Filter.AndFilter) return visit((Filter.AndFilter) filter);
        if(filter instanceof Filter.OrFilter) return visit((Filter.OrFilter) filter);
        if(filter instanceof Filter.NotFilter) return visit((Filter.NotFilter) filter);
        if(filter instanceof Filter.TermFilter) return visit((Filter.TermFilter) filter);
        if(filter instanceof Filter.PrefixFilter) return visit((Filter.PrefixFilter) filter);
        if(filter instanceof Filter.DescriptorFilter) return visit((Filter.DescriptorFilter) filter);
        if(filter instanceof Filter.BeforeFilter) return visit((Filter.BeforeFilter) filter);
        if(filter instanceof Filter.AfterFilter) return visit((Filter.AfterFilter) filter);
        if(filter instanceof Filter.GreaterThanFilter) return visit((Filter.GreaterThanFilter) filter);
        if(filter instanceof Filter.LowerThanFilter) return visit((Filter.LowerThanFilter) filter);
        if(filter instanceof Filter.BetweenDatesFilter) return visit((Filter.BetweenDatesFilter) filter);
        if(filter instanceof Filter.BetweenNumericFilter) return visit((Filter.BetweenNumericFilter) filter);
        if(filter instanceof Filter.WithinBBoxFilter) return visit((Filter.WithinBBoxFilter) filter);
        if(filter instanceof Filter.WithinCircleFilter) return visit((Filter.WithinCircleFilter) filter);
        if(filter instanceof Filter.NotEmptyTextFilter) return visit((Filter.NotEmptyTextFilter) filter);
        if(filter instanceof Filter.NotEmptyFilter) return visit((Filter.NotEmptyFilter) filter);
        if(filter instanceof Filter.NotEmptyLocationFilter) return visit((Filter.NotEmptyLocationFilter) filter);
        if(filter instanceof Filter.ChildrenDocumentFilter) return visit((Filter.ChildrenDocumentFilter) filter);
        throw new RuntimeException("Filter '" + filter.getClass() + "' not supported!");
    }

    public String visit(Filter.AndFilter filter) {
        return filter.getChildren().stream()
                .map(f -> this.visit(f))
                .collect(Collectors.joining(" AND ", "(", ")"));

    }

    public String visit(Filter.OrFilter filter) {
        return filter.getChildren().stream()
                .map(f -> this.visit(f))
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    public String visit(Filter.NotFilter filter) {
        final String NOT_FILTER = "NOT(%s)";
        final String serializedFilter = this.visit(filter.getDelegate());
        return  String.format(NOT_FILTER, serializedFilter);
    }

    public String visit(Filter.TermFilter filter) {
        final String TERM_FILTER = "%s:\"%s\"";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(TERM_FILTER, solrFieldName, filter.getTerm());
            return "("+ String.join(" AND ",parentTypeFilter,filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(TERM_FILTER, solrFieldName, filter.getTerm());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }

    public String visit(Filter.PrefixFilter filter){
        final String PREFIX_FILTER = "%s:%s*";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(PREFIX_FILTER, solrFieldName, filter.getTerm());
            return "("+ String.join(" AND ",parentTypeFilter,filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(PREFIX_FILTER, solrFieldName, filter.getTerm());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }

    public String visit(Filter.DescriptorFilter filter){
        final String DESCRIPTOR_FILTER = "%s:\"%s\"";

        final String term = filter.getTerm() instanceof ZonedDateTime ? DateTimeFormatter.ISO_INSTANT.format((ZonedDateTime) filter.getTerm()) : filter.getTerm().toString();
        if (this.isHierarchical(filter.getDescriptor().getName()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getDescriptor().getName(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getDescriptor().getName(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(DESCRIPTOR_FILTER, solrFieldName, term);
            return "("+ String.join(" AND ",parentTypeFilter,filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getDescriptor().getName(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getDescriptor().getName(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(DESCRIPTOR_FILTER, solrFieldName, term);
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }

    public String visit(Filter.BeforeFilter filter) {
        final String BEFORE_FILTER = "%s:[* TO %s]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(BEFORE_FILTER, solrFieldName, filter.getDate());
            return "("+ String.join(" AND ",parentTypeFilter,filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(BEFORE_FILTER, solrFieldName, filter.getDate());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }
    public String visit(Filter.AfterFilter filter) {
        final String AFTER_FILTER = "%s:[%s TO *]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(AFTER_FILTER, solrFieldName, filter.getDate());
            return "("+ String.join(" AND ",parentTypeFilter,filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(AFTER_FILTER, solrFieldName, filter.getDate());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }

    public String visit(Filter.GreaterThanFilter filter) {
        final String GREATER_THAN_FILTER = "%s:[%s TO *]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization =  String.format(GREATER_THAN_FILTER, solrFieldName, filter.getNumber());
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";

        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(GREATER_THAN_FILTER, solrFieldName, filter.getNumber());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }
    public String visit(Filter.LowerThanFilter filter) {
        final String LOWER_THAN_FILTER = "%s:[* TO %s]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(LOWER_THAN_FILTER, solrFieldName, filter.getNumber());
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(LOWER_THAN_FILTER, solrFieldName, filter.getNumber());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }
    public String visit(Filter.BetweenDatesFilter filter) {
        final String BETWEEN_DATES_FILTER = "%s:[%s TO %s]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(BETWEEN_DATES_FILTER, solrFieldName, filter.getStart(), filter.getEnd());
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(BETWEEN_DATES_FILTER, solrFieldName, filter.getStart(), filter.getEnd());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }
    public String visit(Filter.BetweenNumericFilter filter) {
        final String BETWEEN_NUMERIC_FILTER = "%s:[%s TO %s]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(BETWEEN_NUMERIC_FILTER, solrFieldName, filter.getStart(), filter.getEnd());
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(BETWEEN_NUMERIC_FILTER, solrFieldName, filter.getStart(), filter.getEnd());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }

    public String visit(Filter.WithinBBoxFilter filter) {
        final String WITHIN_BBOX_FILTER = "%s:[%s TO %s]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(WITHIN_BBOX_FILTER, solrFieldName, filter.getUpperLeft(), filter.getLowerRight());
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(WITHIN_BBOX_FILTER, solrFieldName, filter.getUpperLeft(), filter.getLowerRight());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }

    public String visit(Filter.WithinCircleFilter filter) {
        final String WITHIN_CIRCLE_FILTER = "{!geofilt sfield=%s pt=%s d=%s}";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(WITHIN_CIRCLE_FILTER, solrFieldName, filter.getCenter(), filter.getDistance());
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(WITHIN_CIRCLE_FILTER, solrFieldName, filter.getCenter(), filter.getDistance());
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }
    public String visit(Filter.NotEmptyTextFilter filter) {
        final String NOT_EMPTY_TEXT_FILTER = "%s:['' TO *]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(NOT_EMPTY_TEXT_FILTER, solrFieldName);
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(NOT_EMPTY_TEXT_FILTER, solrFieldName);
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }
    public String visit(Filter.NotEmptyFilter filter) {
        final String NOT_EMPTY_FILTER = "%s:*";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(NOT_EMPTY_FILTER, solrFieldName);
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(NOT_EMPTY_FILTER, solrFieldName);
            return  String.format(CHILD_QUERY_TEMPLATE,
                    SolrUtils.Fieldname.TYPE,
                    this.parentFactory.getType(),
                    String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                    serializedFilter);
        }
    }
    public String visit(Filter.NotEmptyLocationFilter filter) {
        final String NOT_EMPTY_LOCATION_FILTER = "%s:[-90,-180 TO 90,180]";

        if (this.isHierarchical(filter.getField()) && !strict) {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.parentFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.parentFactory);
            final String parentTypeFilter = String.format(TYPE_FILTER, SolrUtils.Fieldname.TYPE, this.parentFactory.getType());
            final String filterSerialization = String.format(NOT_EMPTY_LOCATION_FILTER, solrFieldName);
            return "("+ String.join(" AND ", parentTypeFilter, filterSerialization) + ")";
        } else {
            final SolrUtils.Fieldname.UseCase useCase = SolrUtils.Fieldname.UseCase.valueOf(filter.getFilterScope(filter.getField(), this.childFactory).toString());
            final String solrFieldName = this.getFieldName(filter.getField(), this.searchContext, useCase, this.childFactory);
            final String serializedFilter = String.format(NOT_EMPTY_LOCATION_FILTER, solrFieldName);
            return  String.format(CHILD_QUERY_TEMPLATE,
                            SolrUtils.Fieldname.TYPE,
                            this.parentFactory.getType(),
                            String.format("%s:%s",SolrUtils.Fieldname.TYPE, this.childFactory.getType()),
                            serializedFilter);
        }
    }

    public String visit(Filter.ChildrenDocumentFilter filter) {
        if(StringUtils.isNotEmpty(filter.getNestedDocType())) {
            return String.format("{!parent which=\"%s:%s\"} (%s:* AND %s:%s)", SolrUtils.Fieldname.TYPE, filter.getParentDocType(), SolrUtils.Fieldname.ID, SolrUtils.Fieldname.TYPE, filter.getNestedDocType());
        } else {
            return String.format("{!parent which=\"%s:%s\"} (%s:* AND -%s:%s)", SolrUtils.Fieldname.TYPE, filter.getParentDocType(), SolrUtils.Fieldname.ID, SolrUtils.Fieldname.TYPE, filter.getParentDocType());
        }
    }


    private boolean isHierarchical(String fieldName) {
        if(Objects.nonNull(this.childFactory)){
            FieldDescriptor parentDescriptor = this.parentFactory.getField(fieldName);
            FieldDescriptor childDescriptor = this.childFactory.getField(fieldName);
            if(Objects.nonNull(parentDescriptor) && Objects.isNull(childDescriptor)){
                return true;
            }
            return false;
        }
        return true;
    }

    private String getFieldName(String name, String searchContext, SolrUtils.Fieldname.UseCase usecase, DocumentFactory factory) {
        FieldDescriptor descriptor = factory.getField(name);

        if(Objects.isNull(descriptor)){
            log.error("Unable to serialize solr filter: there is no field descriptor with name '{}'", name);
            throw new RuntimeException("Unable to serialize solr filter: there is no field descriptor with name '" + name +"'");
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
