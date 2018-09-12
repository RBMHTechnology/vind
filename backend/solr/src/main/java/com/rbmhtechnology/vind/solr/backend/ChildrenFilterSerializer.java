/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.solr.backend;

import com.google.common.collect.Sets;
import com.rbmhtechnology.vind.api.query.filter.FieldBasedFilter;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.Filer;
import java.util.*;
import java.util.stream.Collectors;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static com.rbmhtechnology.vind.solr.backend.SolrUtils.Fieldname.TYPE;

/**
 * Created on 10.08.18.
 */
public class ChildrenFilterSerializer {

    private static Logger log = LoggerFactory.getLogger(SolrFilterSerializer.class);

    private final DocumentFactory parentFactory;
    private final boolean strict;

    private final String TYPE_FILTER = "%s:%s";
    private final String CHILD_QUERY_TEMPLATE = "{!parent which='%s:%s' v='%s AND %s'}";

    private String searchContext;
    private final DocumentFactory childFactory;
    private final boolean childrenSearch;

    public ChildrenFilterSerializer(DocumentFactory parentFactory, DocumentFactory childFactory, String searchContext, boolean strict, boolean childrenSearch){
        this.parentFactory = parentFactory;
        this.childFactory = childFactory;
        this.searchContext = searchContext;
        this.strict = strict;
        this.childrenSearch = childrenSearch;
    }

    public String serialize(Filter filter){
        final Filter normalizedFilter = normalize(filter);

        if (AndFilter.class.isAssignableFrom(normalizedFilter.getClass()))
            return serialize((AndFilter)normalizedFilter);
        else if (OrFilter.class.isAssignableFrom(normalizedFilter.getClass()))
            return serialize((OrFilter)normalizedFilter);
        else if (isHierarchicalFilter(normalizedFilter)) {
             final String parentFilter =  new SolrFilterSerializer(parentFactory, strict).serialize(normalizedFilter,searchContext);
             return parentFilter;
        } else {
            final String childFilter = new SolrFilterSerializer(childFactory, strict).serialize(normalizedFilter,searchContext);
            return  String.format(CHILD_QUERY_TEMPLATE,
                    TYPE,
                    parentFactory.getType(),
                    String.format(TYPE_FILTER, TYPE, childFactory.getType()),
                    childFilter);
        }

    }

    private String serialize(AndFilter filter){

        //Get the filters which apply to the parent
        final String parentFilters = filter.getChildren().stream()
                .filter(f -> isHierarchicalFilter(f))
                .map( f -> new SolrFilterSerializer(parentFactory, strict).serialize(f,searchContext))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" AND "));

        //Get the filter which apply to the children
        final String childrenFilters = filter.getChildren().stream()
                .filter(f -> !isHierarchicalFilter(f))
                .map( f -> new SolrFilterSerializer(childFactory, strict).serialize(f,searchContext))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" AND "));

        if (StringUtils.isNotBlank(childrenFilters) && StringUtils.isNotBlank(parentFilters)){
            return String.join(" AND ",
                    String.format(TYPE_FILTER, TYPE, parentFactory.getType()),
                    parentFilters,
                    String.format(CHILD_QUERY_TEMPLATE,
                            TYPE,
                            parentFactory.getType(),
                            String.format(TYPE_FILTER, TYPE, childFactory.getType()),
                            childrenFilters));
        }

        if ( StringUtils.isNotBlank(parentFilters)){
            return String.join(" AND ",
                    String.format(TYPE_FILTER, TYPE, parentFactory.getType()),
                    parentFilters);
        }

        return  String.format(CHILD_QUERY_TEMPLATE,
                            TYPE,
                            parentFactory.getType(),
                            String.format(TYPE_FILTER, TYPE, childFactory.getType()),
                            childrenFilters);


    }

    private String serialize(OrFilter filter) {
      final String andFilters =
              filter.getChildren().stream()
                      .filter( f-> AndFilter.class.isAssignableFrom(f.getClass()))
                      .filter(Objects::nonNull)
                      .map(this::serialize)
                      .map( andF -> "(" + andF +" )")
                      .collect(Collectors.joining(" OR "));

      final String basicFilters =
              filter.getChildren().stream()
                .filter( f-> !AndFilter.class.isAssignableFrom(f.getClass()))
                .filter(Objects::nonNull)
                .map(this::serialize)
                .collect(Collectors.joining(" OR "));

      if (StringUtils.isNotBlank(basicFilters) && StringUtils.isNotBlank(andFilters)){
          return String.join(" OR ", basicFilters, andFilters);
      }

      if ( StringUtils.isNotBlank(andFilters)){
          return andFilters;
      }

      return basicFilters;
    }

    private Filter normalize(AndFilter filter){

        final Set<Filter> normalizedFilters = filter.getChildren().stream()
                .map(this::normalize)
                .collect(Collectors.toSet());

        //Get the basic filters already normalized
        final Set<Filter> normalizedChildren = normalizedFilters.stream()
                .filter(f -> !f.getType().equals(filter.getType()) && !f.getType().equals("OrFilter"))
                .collect(Collectors.toSet());

        //Get the And filters and promote the children to this level
        normalizedFilters.stream().
                filter(f -> f.getType().equals(filter.getType()))
                .forEach( af -> normalizedChildren.addAll(((AndFilter)af).getChildren()));

        final Set<Filter> orChildren = normalizedFilters.stream().
                filter(f -> f.getType().equals("OrFilter"))
                .map( of -> normalize((OrFilter) of))
                .collect(Collectors.toSet());


        final Filter orFilterPivot = orChildren.stream()
                .findFirst()
                .orElse(null);

        if(Objects.nonNull(orFilterPivot)) {
            orChildren.remove(orFilterPivot);

            final Set<Filter> andResultFilters= ((OrFilter) orFilterPivot).getChildren().stream()
                .map( f-> AndFilter.fromSet(Sets.union( normalizedChildren, Sets.newHashSet(f))))
                .map( af -> AndFilter.fromSet(Sets.union( orChildren, Sets.newHashSet(af))))
                .collect(Collectors.toSet());

            final Set<Filter> andResultNormalizedFilters = andResultFilters.stream()
            .map(f -> normalize(f))
            .collect(Collectors.toSet());

            if(CollectionUtils.isNotEmpty(andResultNormalizedFilters)) {
                return  OrFilter.fromSet(andResultNormalizedFilters);
            }
        }


        if(CollectionUtils.isNotEmpty(orChildren)) {
            return  OrFilter.fromSet(orChildren);
        }

        return AndFilter.fromSet(normalizedChildren);
    }

    private Filter normalize(OrFilter filter){

        final List<Filter> normalizedFilters = filter.getChildren().stream()
                .map(this::normalize)
                .collect(Collectors.toList());

        //Get the basic filters already normalized
        final Set<Filter> normalizedChildren = normalizedFilters.stream()
                .filter(f -> !f.getType().equals(filter.getType()) && !f.getType().equals("AndFilter"))
                .collect(Collectors.toSet());

        normalizedFilters.stream().
                filter(f -> f.getType().equals(filter.getType()))
                .forEach( of -> normalizedChildren.addAll(((OrFilter)of).getChildren()));

        normalizedFilters.stream().
                filter( f -> f.getType().equals("AndFilter"))
                .forEach( f -> normalizedChildren.add(f));

        return OrFilter.fromSet(normalizedChildren);
    }

    private Filter normalize(NotFilter filter){

        final Filter normalizedDelegate = normalize(filter.getDelegate());

        if(AndFilter.class.isAssignableFrom(normalizedDelegate.getClass())) {
            return OrFilter.fromSet(((AndFilter)normalizedDelegate).getChildren().stream()
                    .map(f -> new NotFilter(f))
                    .collect(Collectors.toSet()));
        } else if(OrFilter.class.isAssignableFrom(normalizedDelegate.getClass())) {
            return AndFilter.fromSet(((OrFilter)normalizedDelegate).getChildren().stream()
                    .map(f -> new NotFilter(f))
                    .collect(Collectors.toSet()));
        } else if(NotFilter.class.isAssignableFrom(normalizedDelegate.getClass())) {
            return ((NotFilter)normalizedDelegate).getDelegate();
        }

        return filter;
    }

    protected Filter normalize(Filter filter){

        if (AndFilter.class.isAssignableFrom(filter.getClass()))
            return normalize((AndFilter)filter);
        else if (OrFilter.class.isAssignableFrom(filter.getClass()))
            return normalize((OrFilter)filter);
        else if (NotFilter.class.isAssignableFrom(filter.getClass()))
            return normalize((NotFilter) filter);
        else return filter;
    }

    private boolean isHierarchicalFilter(Filter filter) {

        if(filter instanceof FieldBasedFilter) return isHierarchicalField(((FieldBasedFilter) filter).getField());
        if(filter instanceof Filter.NotFilter) return isHierarchicalFilter((((NotFilter) filter).getDelegate()));
        if(filter instanceof Filter.ChildrenDocumentFilter) return true;

        throw new RuntimeException("Error parsing filter: Filter '" + filter.getClass() + "' not supported!");
    }

    private boolean isHierarchicalField(String fieldName) {
        if(Objects.nonNull(this.childFactory)){
            final FieldDescriptor parentDescriptor = this.parentFactory.getField(fieldName);
            final FieldDescriptor childDescriptor = this.childFactory.getField(fieldName);

            //Check if the field descriptor belongs to the parent and not to the children in a children search
            if(Objects.nonNull(parentDescriptor) && Objects.isNull(childDescriptor) && childrenSearch){
                log.debug("The field [{}] is a parent property", fieldName);
                return true;
            }
            //Check if the field belongs to the parent in a parent search
            if(Objects.nonNull(parentDescriptor) && !childrenSearch) {
                log.debug("The field [{}] is a parent property", fieldName);
                return true;
            }

            return false;
        }
        log.debug("There is no Children factory define therefore field [{}] is a parent property", fieldName);
        return true;
    }
}
