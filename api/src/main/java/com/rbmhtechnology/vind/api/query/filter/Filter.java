package com.rbmhtechnology.vind.api.query.filter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.value.LatLng;
import com.rbmhtechnology.vind.utils.SerializerVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Abstract class to be implemented by filter query objects ({@link AndFilter}, {@link OrFilter},
 * {@link NotFilter}, {@link TermFilter}, {@link DescriptorFilter},
 * {@link BetweenDatesFilter}, {@link BeforeFilter}, {@link AfterFilter},
 * {@link BetweenNumericFilter}, {@link GreaterThanFilter}, {@link LowerThanFilter}) and providing
 * static user friendly methods to create them.
 */
public abstract class Filter {

    private Logger logger = LoggerFactory.getLogger(Filter.class);

    public enum Scope {
        Facet,
        Suggest,
        Filter
    }

    public static Collector<Filter, ImmutableSet.Builder<Filter>, Filter> AndCollector = new AndCollector();

    public static Collector<Filter, ImmutableSet.Builder<Filter>, Filter> OrCollector = new OrCollector();

    public static final Scope DEFAULT_SCOPE = Scope.Facet;

    protected Scope filterScope;

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public Scope getFilterScope() {
        return this.getFilterScope(null);
    }

    public Scope getFilterScope(String fieldname, DocumentFactory factory){
        return getFilterScope(factory.getField(fieldname));
    }

    public Scope getFilterScope(FieldDescriptor fd){
        if(Objects.nonNull(this.filterScope)) {
            return this.filterScope;
        } else {
            if (fd == null) {
                logger.debug(
                        "Unable to get custom scope from filter or field descriptor: fall back to default filter scope '{}'",
                        DEFAULT_SCOPE);
                return DEFAULT_SCOPE;
            } else {
                if (fd.isFacet() && !fd.isSuggest()) return Scope.Facet;
                if (fd.isSuggest() && !fd.isFacet()) return Scope.Suggest;
                else return DEFAULT_SCOPE;
            }
        }
    }

    @Override
    public abstract Filter clone();

    /**
     * Static method which creates a {@link AndFilter} out of a group of filters.
     * @param a Required {@link Filter} to be added to the AND operation.
     * @param b Required {@link Filter} to be added to the AND operation.
     * @param filters Optional Filters to be added to the AND operation.
     * @return {@link AndFilter} query object on the Filters provided as parameters.
     */
    public static Filter and(Filter a, Filter b,Filter ... filters) {
        Set<Filter> andFilters = new HashSet<>();
        andFilters.add(a);
        andFilters.add(b);
        andFilters.addAll(Sets.newHashSet(filters));
        return  AndFilter.fromSet(andFilters);
    }

    /**
     * Static method which creates a {@link OrFilter} out of a group of filters.
     * @param a Required {@link Filter} to be added to the OR operation.
     * @param b Required {@link Filter} to be added to the OR operation.
     * @param filters Optional Filters to be added to the OR operation.
     * @return {@link OrFilter} query object on the Filters provided as parameters.
     */
    public static Filter or(Filter a, Filter b, Filter ... filters) {
        Set<Filter> orFilters = new HashSet<>();
        orFilters.add(a);
        orFilters.add(b);
        return OrFilter.fromSet(orFilters);
    }

    /**
     * Static method which instantiates a {@link NotFilter} object out of a Filter.
     * @param a Required {@link Filter} to be negated by the NOT operation.
     * @return {@link NotFilter} query object on the Filters provided as parameter.
     */
    public static Filter not(Filter a) {
        return new NotFilter(a);
    }

    /**
     * Static method to instantiate a {@link TermFilter} object based on a given field name parameter and a value.
     * @param field String name of the field to build the filter over.
     * @param term String Value of the field to build the filter over.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link TermFilter} query object on the parameters provided.
     */
    public static Filter eq(String field, String term, Scope scope) {
        return new TermFilter(field, term, scope);
    }

    /**
     * Static method to instantiate a {@link TermFilter} object based on a given field name parameter and a value.
     * @param field String name of the field to build the filter over.
     * @param term String Value of the field to build the filter over.
     * @return {@link TermFilter} query object on the parameters provided.
     */
    public static Filter eq(String field, String term) {
        return new TermFilter(field, term, null);
    }

    /**
     * Static method to instantiate a {@link DescriptorFilter} object based on a given field descriptor
     * parameter and a value.
     * @param descriptor {@link FieldDescriptor} indicating which field should be filterd by.
     * @param term Value of the field of type T.
     * @param <T> Type of content the field can store.
     * @return {@link DescriptorFilter} query object on the parameters provided.
     */
    public static <T> Filter eq(FieldDescriptor<T> descriptor, T term) {
        return new DescriptorFilter(descriptor, term, null);
    }

    /**
     * Static method to instantiate a {@link DescriptorFilter} object based on a given field descriptor
     * parameter and a value.
     * @param descriptor {@link FieldDescriptor} indicating which field should be filterd by.
     * @param term Value of the field of type T.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @param <T> Type of content the field can store.
     * @return {@link DescriptorFilter} query object on the parameters provided.
     */
    public static <T> Filter eq(FieldDescriptor<T> descriptor, T term, Scope scope) {
        return new DescriptorFilter(descriptor, term, scope);
    }

    /**
     * Static method to instantiate a {@link TermsQueryFilter} object based on a given field descriptor
     * and a list of values.
     * @param field
     * @param values
     * @param <T>
     * @return
     */
    public static <T> Filter terms(FieldDescriptor<T> field, T... values) {
        return new TermsQueryFilter(field, values,null);
    }

    /**
     * Static method to instantiate a {@link TermsQueryFilter} object based on a given field descriptor
     * and a list of values.
     * @param field
     * @param values
     * @param scope
     * @param <T>
     * @return
     */
    public static <T> Filter terms(FieldDescriptor<T> field, Scope scope, T... values) {

        return new TermsQueryFilter(field, values, scope);
    }

    /**
     * Static method to instantiate a {@link PrefixFilter} object based on a given field name parameter and a
     * prefix.
     * @param field String name of the field to build the filter over.
     * @param prefix String prefix to filter by the documents.
     * @return {@link PrefixFilter} query object on the parameters provided.
     */
    public static Filter prefix(String field, String prefix) {
        return new PrefixFilter(field, prefix, Scope.Facet);
    }

    /**
     * Static method to instantiate a {@link PrefixFilter} object based on a given field name parameter and a
     * prefix.
     * @param field String name of the field to build the filter over.
     * @param prefix String prefix to filter by the documents.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link PrefixFilter} query object on the parameters provided.
     */
    public static Filter prefix(String field, String prefix, Scope scope) {
        return new PrefixFilter(field, prefix, scope);
    }

    /**
     * Static method to instantiate a {@link BetweenDatesFilter} object based on a given field name, a start date
     * and an end date.
     * @param field String name of the field to build the filter over.
     * @param start ZoneDateTime object indicating the lower limit of a range.
     * @param end ZoneDateTime object indicating the greater limit of a range.
     * @return {@link BetweenDatesFilter} query object on the parameters provided.
     */
    public static Filter between(String field, ZonedDateTime start, ZonedDateTime end) { return new BetweenDatesFilter(field, new DateMathExpression(start),  new DateMathExpression(end), Scope.Facet);}

    /**
     * Static method to instantiate a {@link BetweenDatesFilter} object based on a given field name, a start date
     * and an end date.
     * @param field String name of the field to build the filter over.
     * @param start ZoneDateTime object indicating the lower limit of a range.
     * @param end ZoneDateTime object indicating the greater limit of a range.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link BetweenDatesFilter} query object on the parameters provided.
     */
    public static Filter between(String field, ZonedDateTime start, ZonedDateTime end, Scope scope) { return new BetweenDatesFilter(field, new DateMathExpression(start), new DateMathExpression(end), scope);}

    /**
     * Static method to instantiate a {@link BetweenDatesFilter} object based on a given field name, a start date
     * and an end date.
     * @param field {@link String} name of the field to build the filter over.
     * @param start {@link Date} object indicating the lower limit of a range.
     * @param end {@link Date} object indicating the greater limit of a range.
     * @return {@link BetweenDatesFilter} query object on the parameters provided.
     */
    public static Filter between(String field, Date start, Date end) { return new BetweenDatesFilter(field, new DateMathExpression(ZonedDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC)),  new DateMathExpression(ZonedDateTime.ofInstant(end.toInstant(), ZoneOffset.UTC)), Scope.Facet);}

    /**
     * Static method to instantiate a {@link BetweenDatesFilter} object based on a given field name, a start date
     * and an end date.
     * @param field {@link String} name of the field to build the filter over.
     * @param start {@link Date} object indicating the lower limit of a range.
     * @param end {@link Date} object indicating the greater limit of a range.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link BetweenDatesFilter} query object on the parameters provided.
     */
    public static Filter between(String field, Date start, Date end, Scope scope) { return new BetweenDatesFilter(field, new DateMathExpression(ZonedDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC)),  new DateMathExpression(ZonedDateTime.ofInstant(end.toInstant(), ZoneOffset.UTC)), scope);}


    /**
     * Static method to instantiate a {@link BetweenDatesFilter} object based on a given field name, a start date
     * and an end date.
     * @param field String name of the field to build the filter over.
     * @param start DateMathExpression object indicating the lower limit of a range.
     * @param end DateMathExpression object indicating the greater limit of a range.
     * @return {@link BetweenDatesFilter} query object on the parameters provided.
     */
    public static Filter between(String field, DateMathExpression start, DateMathExpression end) { return new BetweenDatesFilter(field, start,  end, Scope.Facet);}

    /**
     * Static method to instantiate a {@link BetweenDatesFilter} object based on a given field name, a start date
     * and an end date.
     * @param field String name of the field to build the filter over.
     * @param start DateMathExpression object indicating the lower limit of a range.
     * @param end DateMathExpression object indicating the greater limit of a range.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link BetweenDatesFilter} query object on the parameters provided.
     */
    public static Filter between(String field, DateMathExpression start, DateMathExpression end, Scope scope) { return new BetweenDatesFilter(field, start,  end, scope);}


    /**
     * Static method to instantiate a {@link BetweenNumericFilter} object based on a given field name, a start
     * number and an end number.
     * @param field String name of the field to build the filter over.
     * @param start Number object indicating the lower limit of a range.
     * @param end Number object indicating the greater limit of a range.
     * @return {@link BetweenNumericFilter} query object on the parameters provided.
     */
    public static Filter between(String field, Number start, Number end) { return new BetweenNumericFilter(field, start,  end, Scope.Facet);}

    /**
     * Static method to instantiate a {@link BetweenNumericFilter} object based on a given field name, a start
     * number and an end number.
     * @param field String name of the field to build the filter over.
     * @param start Number object indicating the lower limit of a range.
     * @param end Number object indicating the greater limit of a range.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link BetweenNumericFilter} query object on the parameters provided.
     */
    public static Filter between(String field, Number start, Number end, Scope scope) { return new BetweenNumericFilter(field, start,  end, scope);}

    /**
     * Static method to instantiate a {@link BeforeFilter} object based on a given field name and a date.
     * @param field String name of the field to build the filter over.
     * @param date ZoneDateTime object setting the point in time where all field values later than it are filtered.
     * @return {@link BeforeFilter} query object on the parameters provided.
     */
    public static Filter before(String field, ZonedDateTime date) { return new BeforeFilter(field, new DateMathExpression(date), Scope.Facet);}

    /**
     * Static method to instantiate a {@link BeforeFilter} object based on a given field name and a date.
     * @param field String name of the field to build the filter over.
     * @param date ZoneDateTime object setting the point in time where all field values later than it are filtered.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link BeforeFilter} query object on the parameters provided.
     */
    public static Filter before(String field, ZonedDateTime date, Scope scope) { return new BeforeFilter(field, new DateMathExpression(date), scope);}

    /**
     * Static method to instantiate a {@link BeforeFilter} object based on a given field name and a date.
     * @param field {@link String} name of the field to build the filter over.
     * @param date {@link Date} object setting the point in time where all field values later than it are filtered.
     * @return {@link BeforeFilter} query object on the parameters provided.
     */
    public static Filter before(String field, Date date) { return new BeforeFilter(field, new DateMathExpression(ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC)), Scope.Facet);}

    /**
     * Static method to instantiate a {@link BeforeFilter} object based on a given field name and a date.
     * @param field {@link String} name of the field to build the filter over.
     * @param date {@link Date} object setting the point in time where all field values later than it are filtered.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link BeforeFilter} query object on the parameters provided.
     */
    public static Filter before(String field, Date date, Scope scope) { return new BeforeFilter(field, new DateMathExpression(ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC)), scope);}


    /**
     * Static method to instantiate a {@link BeforeFilter} object based on a given field name and a date math expression.
     * @param field String name of the field to build the filter over.
     * @param date {@link DateMathExpression} object setting the point in time where all field values later than it are filtered.
     * @return {@link BeforeFilter} query object on the parameters provided.
     */
    public static Filter before(String field, DateMathExpression date) { return new BeforeFilter(field, date, Scope.Facet);}

    /**
     * Static method to instantiate a {@link BeforeFilter} object based on a given field name and a date math expression.
     * @param field String name of the field to build the filter over.
     * @param date {@link DateMathExpression} object setting the point in time where all field values later than it are filtered.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link BeforeFilter} query object on the parameters provided.
     */
    public static Filter before(String field, DateMathExpression date,Scope scope) { return new BeforeFilter(field, date, scope);}


    /**
     * Static method to instantiate a {@link GreaterThanFilter} object based on a given field name and a number.
     * @param field String name of the field to build the filter over.
     * @param number Number object setting the value where all field values smaller than it are filtered.
     * @return {@link GreaterThanFilter} query object on the parameters provided.
     */
    public static Filter greaterThan(String field, Number number) { return new GreaterThanFilter(field, number, Scope.Facet);}

    /**
     * Static method to instantiate a {@link GreaterThanFilter} object based on a given field name and a number.
     * @param field String name of the field to build the filter over.
     * @param number Number object setting the value where all field values smaller than it are filtered.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link GreaterThanFilter} query object on the parameters provided.
     */
    public static Filter greaterThan(String field, Number number, Scope scope) { return new GreaterThanFilter(field, number, scope);}

    /**
     * Static method to instantiate a {@link AfterFilter} object based on a given field name and a date.
     * @param field String name of the field to build the filter over.
     * @param date ZoneDateTime object setting the point in time where all field values earlier than it are filtered.
     * @return {@link AfterFilter} query object on the parameters provided.
     */
    public static Filter after(String field, ZonedDateTime date) { return new AfterFilter(field, new DateMathExpression(date), Scope.Facet);}

    /**
     * Static method to instantiate a {@link AfterFilter} object based on a given field name and a date.
     * @param field String name of the field to build the filter over.
     * @param date ZoneDateTime object setting the point in time where all field values earlier than it are filtered.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link AfterFilter} query object on the parameters provided.
     */
    public static Filter after(String field, ZonedDateTime date, Scope scope) { return new AfterFilter(field, new DateMathExpression(date), scope);}

    /**
     * Static method to instantiate a {@link AfterFilter} object based on a given field name and a date.
     * @param field String name of the field to build the filter over.
     * @param date ZoneDateTime object setting the point in time where all field values earlier than it are filtered.
     * @return {@link AfterFilter} query object on the parameters provided.
     */
    public static Filter after(String field, Date date) { return new AfterFilter(field, new DateMathExpression(ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC)), Scope.Facet);}

    /**
     * Static method to instantiate a {@link AfterFilter} object based on a given field name and a date.
     * @param field String name of the field to build the filter over.
     * @param date ZoneDateTime object setting the point in time where all field values earlier than it are filtered.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link AfterFilter} query object on the parameters provided.
     */
    public static Filter after(String field, Date date, Scope scope) { return new AfterFilter(field, new DateMathExpression(ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC)), scope);}

    /**
     * Static method to instantiate a {@link AfterFilter} object based on a given field name and a point in time.
     * @param field String name of the field to build the filter over.
     * @param date {@link DateMathExpression} object setting the point in time where all field values earlier than it are filtered.
     * @return {@link AfterFilter} query object on the parameters provided.
     */
    public static Filter after(String field, DateMathExpression date) { return new AfterFilter(field, date, Scope.Facet);}

    /**
     * Static method to instantiate a {@link AfterFilter} object based on a given field name and a point in time.
     * @param field String name of the field to build the filter over.
     * @param date {@link DateMathExpression} object setting the point in time where all field values earlier than it are filtered.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link AfterFilter} query object on the parameters provided.
     */
    public static Filter after(String field, DateMathExpression date, Scope scope) { return new AfterFilter(field, date, scope);}

    /**
     * Static method to instantiate a {@link LowerThanFilter} object based on a given field name and a number.
     * @param field String name of the field to build the filter over.
     * @param number Number object setting the value where all field values greater than it are filtered.
     * @return {@link LowerThanFilter} query object on the parameters provided.
     */
    public static Filter lesserThan(String field, Number number) { return new LowerThanFilter(field, number, Scope.Facet);}

    /**
     * Static method to instantiate a {@link LowerThanFilter} object based on a given field name and a number.
     * @param field String name of the field to build the filter over.
     * @param number Number object setting the value where all field values greater than it are filtered.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link LowerThanFilter} query object on the parameters provided.
     */
    public static Filter lesserThan(String field, Number number, Scope scope) { return new LowerThanFilter(field, number, scope);}

    /**
     * Static method to instantiate a {@link WithinBBoxFilter}
     * @param field String name of the field to build the filter over.
     * @param upperLeft the upper left corner of the bounding box
     * @param lowerRight the lower right corner of the bounding box
     * @return {@link WithinBBoxFilter} query object on the parameters provided.
     */
    public static Filter withinBBox(String field, LatLng upperLeft, LatLng lowerRight) {
        return new WithinBBoxFilter(field, upperLeft, lowerRight, null);
    }

    /**
     * Static method to instantiate a {@link WithinBBoxFilter}
     * @param field String name of the field to build the filter over.
     * @param upperLeft the upper left corner of the bounding box
     * @param lowerRight the lower right corner of the bounding box
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link WithinBBoxFilter} query object on the parameters provided.
     */
    public static Filter withinBBox(String field, LatLng upperLeft, LatLng lowerRight, Scope scope) {
        return new WithinBBoxFilter(field, upperLeft, lowerRight, scope);
    }

    /**
     * Static method to instantiate a {@link WithinCircleFilter}
     * @param field String name of the field to build the filter over.
     * @param center the center of the circle
     * @param distance the radius of the circle in kilometers
     * @return {@link WithinCircleFilter} query object on the parameters provided.
     */
    public static Filter withinCircle(String field, LatLng center, double distance) {
        return new WithinCircleFilter(field, center, distance, null);
    }

    /**
     * Static method to instantiate a {@link WithinCircleFilter}
     * @param field String name of the field to build the filter over.
     * @param center the center of the circle
     * @param distance the radius of the circle in kilometers
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return {@link WithinCircleFilter} query object on the parameters provided.
     */
    public static Filter withinCircle(String field, LatLng center, double distance, Scope scope) {
        return new WithinCircleFilter(field, center, distance, scope);
    }

    /**
     * Static method to instantiate a {@link ChildrenDocumentFilter}
     * @param parentType {@link String} value of the parent type.
     * @return {@link ChildrenDocumentFilter} query object.
     */
    public static Filter hasChildrenDocuments(String parentType) {
        return new ChildrenDocumentFilter(parentType);
    }

    /**
     * Static method to instantiate a {@link ChildrenDocumentFilter}
     * @param docFactory {@link DocumentFactory} of the parent document.
     * @return {@link ChildrenDocumentFilter} query object on the parameters provided.
     */
    public static Filter hasChildrenDocuments(DocumentFactory docFactory) {
        return new ChildrenDocumentFilter(docFactory);
    }

    /**
     * Filter Class implementing the query AND logic operation.
     */
    public static class AndFilter extends Filter {
        private final Set<Filter> children = new HashSet<>();

        /**
         * Creates an instance of {@link AndFilter} of two given filters.
         * @param a {@link Filter} to be one part of the AND query.
         * @param b {@link Filter} to be one part of the AND query.
         */
        public AndFilter(Filter a, Filter b) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);
            addChild(a);
            addChild(b);
        }

        /**
         * Adds another {@link Filter} to the AND query.
         * @param f {@link Filter} to be added to the AND query.
         */
        private void addChild(Filter f) {
            if (f instanceof AndFilter) {
                children.addAll(((AndFilter) f).children);
            } else {
                children.add(f);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            for (Filter child : children) {
                if (sb.length() > 0) {
                    sb.append(" AND ");
                }
                if (child instanceof NotFilter || child instanceof TermFilter) {
                    sb.append(child.toString());
                } else {
                    sb.append('(').append(child.toString()).append(')');
                }
            }

            return sb.toString();
        }

        /**
         * Get all the {@link Filter} composing the AND filter query.
         * @return {@code Set<Filter>} forming this AND filter query.
         */
        public Set<Filter> getChildren() {
            return children;
        }

        /**
         * Static method to create a new {@link AndFilter} from a set of {@link Filter}.
         * @param build The group of Filter, given as a {@code Set<Filter>}, which will be part of the AND filter query.
         * @return {@link AndFilter} query object on the filters provided.
         */
        public static Filter fromSet(Set<Filter> build) {
            if(build.isEmpty()) return null;//TODO
            if(build.size() < 2) return build.iterator().next();

            Iterator<Filter> filters = build.iterator();
            AndFilter filter = new AndFilter(filters.next(), filters.next());

            while(filters.hasNext()) {
                filter.addChild(filters.next());
            }

            return filter;
        }

        @Override
        public Filter clone() {
            final Set<Filter> childCopy = this.getChildren().stream()
                    .map(f -> f.clone())
                    .collect(Collectors.toSet());

            final Filter copy = AndFilter.fromSet(childCopy);
            return copy;
        }
    }

    /**
     * Filter Class implementing the query OR logic operation.
     */
    public static class OrFilter extends Filter {
        private final Set<Filter> children = new HashSet<>();

        /**
         * Creates an instance of {@link OrFilter} of two given filters.
         * @param a {@link Filter} to be one part of the OR query.
         * @param b {@link Filter} to be one part of the OR query.
         */
        public OrFilter(Filter a, Filter b) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);
            addChild(a);
            addChild(b);
        }

        /**
         * Adds another {@link Filter} to the or query.
         * @param f {@link Filter} to be added to the or query.
         */
        private void addChild(Filter f) {
            if (f instanceof OrFilter) {
                children.addAll(((OrFilter) f).children);
            } else {
                children.add(f);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            for (Filter child : children) {
                if (sb.length() > 0) {
                    sb.append(" OR ");
                }
                if (child instanceof NotFilter || child instanceof TermFilter) {
                    sb.append(child.toString());
                } else {
                    sb.append('(').append(child.toString()).append(')');
                }
            }

            return sb.toString();
        }

        /**
         * Get all the {@link Filter} composing the OR filter query.
         * @return {@code Set<Filter>} forming this OR filter query.
         */
        public Set<Filter> getChildren() {
            return children;
        }

        /**
         * Static method to create a new {@link OrFilter} from a set of {@link Filter}.
         * @param build The group of Filter, given as a {@code Set<Filter>}, which will be part of the OR filter query.
         * @return {@link OrFilter} query object on the filters provided.
         */
        public static Filter fromSet(Set<Filter> build) {
            if(build.isEmpty()) return null;//TODO
            if(build.size() < 2) return build.iterator().next();

            Iterator<Filter> filters = build.iterator();
            OrFilter filter = new OrFilter(filters.next(), filters.next());

            while(filters.hasNext()) {
                filter.addChild(filters.next());
            }

            return filter;
        }

        @Override
        public Filter clone() {
            final Set<Filter> childCopy = this.getChildren().stream()
                    .map(f -> f.clone())
                    .collect(Collectors.toSet());

            final Filter copy = OrFilter.fromSet(childCopy);
            return copy;
        }
    }

    /**
     * Filter Class implementing the query NOT logic operation.
     */
    public static class NotFilter extends Filter {
        private final Filter delegate;

        /**
         * Creates an instance of {@link NotFilter} of a given filter.
         * @param a {@link com.rbmhtechnology.vind.api.query.filter.Filter} to be one part of the NOT query.
         */
        public NotFilter(Filter a) {
            Objects.requireNonNull(a);
            delegate = a;
        }

        @Override
        public String toString() {
            return "NOT(" + delegate.toString() + ")";
        }

        /**
         * Gets the {@link com.rbmhtechnology.vind.api.query.filter.Filter} negated by the NOT operation
         * @return {@link Filter} negated filter.
         */
        public Filter getDelegate() {
            return delegate;
        }

        @Override
        public Filter clone() {
            final Filter copy = new NotFilter(this.getDelegate().clone());
            return copy;
        }
    }

    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with  this {@link TermFilter#field} and this value {@link TermFilter#term}.
     */
    public static class TermFilter extends FieldBasedFilter { //TODO should we allow this?
        private final String field;
        private final String term;

        /**
         * Creates a {@link TermFilter} object based on a given field name parameter and a value.
         * @param field String name of the field to build the filter over.
         * @param term String Value of the field to build the filter over.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public TermFilter(String field, String term, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(term);
            this.field = field;
            this.term = term;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s='%s'", field, term);
        }

        /**
         * Get the filtered field name
         * @return String {@link TermFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }

        /**
         * Get the filtered value for the field.
         * @return String {@link TermFilter#term} with the value to filter by.
         */
        public String getTerm() {
            return term;
        }

        @Override
        public Filter clone() {
            final Filter copy = new TermFilter(this.field, this.term, super.filterScope);
            return copy;
        }
    }

    /**
     * Filter Class implementing a filter by prefix value. A query performed with this filter should return all the
     * documents with  this {@link PrefixFilter#field} and starting
     * by this value {@link PrefixFilter#term}.
     */
    public static class PrefixFilter extends FieldBasedFilter { //TODO should we allow this?
        private final String field;
        private final String term;

        /**
         * Creates a {@link PrefixFilter} object based on a given
         * field name parameter and a value.
         * @param field String name of the field to build the filter over.
         * @param term String Value of the field to build the filter over.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public PrefixFilter(String field, String term, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(term);
            this.field = field;
            this.term = term;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=%s*", field, term);
        }

        @Override
        public Filter clone() {
            final PrefixFilter copy = new PrefixFilter(this.field, this.term, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link PrefixFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }

        /**
         * Get the filtered value for the field.
         * @return String {@link PrefixFilter#term} with the prefix to filter by.
         */
        public String getTerm() {
            return term;
        }
    }

    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with  this {@link DescriptorFilter#descriptor} and this value {@link DescriptorFilter#term}.
     * @param <T> The type of the field content to be filtered by.
     */
    public static class DescriptorFilter<T> extends FieldBasedFilter { //TODO should we allow this?

        private final T term;
        private final FieldDescriptor descriptor;
        private final String field;

        /**
         * Creates a {@link DescriptorFilter} object based on a given field descriptor parameter and a value.
         * @param descriptor {@link FieldDescriptor} of the field to build the filter over.
         * @param term T Value of the field to build the filter over.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public DescriptorFilter(FieldDescriptor descriptor, T term, Scope scope) {
            Objects.requireNonNull(descriptor);
            Objects.requireNonNull(term);
            this.descriptor = descriptor;
            this.term = term;
            super.filterScope = scope;
            this.field = descriptor.getName();
        }

        @Override
        public Scope getFilterScope() {
            return this.getFilterScope(this.descriptor);
        }

        @Override
        public String toString() {
            return String.format("%s='%s'", descriptor.getName(), term);
        }

        /**
         * Get the filtered field name
         * @return FieldDescriptor {@link DescriptorFilter#descriptor} with the field description.
         */
        public FieldDescriptor getDescriptor() {
            return descriptor;
        }

        /**
         * Get the filtered value for the field.
         * @return T {@link DescriptorFilter#term} with the value to filter by.
         */
        public T getTerm() {
            return term;
        }

        /**
         * Get the name of the field.
         * @return  {@link String} with the value to field descriptor name.
         */
        @Override
        public String getField() {
            return field;
        }

        @Override
        public Filter clone() {
            final DescriptorFilter<T> copy = new DescriptorFilter<T>(this.descriptor,this.term, super.filterScope);
            return copy;
        }
    }

    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with  this {@link BetweenDatesFilter#field} and value between
     * {@link BetweenDatesFilter#start} and {@link BetweenDatesFilter#end}.
     */
    public static class BetweenDatesFilter extends FieldBasedFilter {
        private final String field;
        private final DateMathExpression start;
        private final DateMathExpression end;

        /**
         * Creates a {@link BetweenDatesFilter} object based on a given field name parameter, start date and an
         * end date.
         * @param field String name of the field to build the filter over.
         * @param start {@link DateMathExpression} setting the starting time point of the period to be filtered by.
         * @param end {@link DateMathExpression} setting the ending time point of the period to be filtered by.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
         public BetweenDatesFilter(String field, DateMathExpression start, DateMathExpression end, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            this.field = field;
            this.start = start;
            this.end = end;
            super.filterScope = scope;
        }

        /**
         * Calculates the epoch millis of the start position for the filter
         * @return start date expressed in  epoch millis
         */
        public long getTimeStampStart() {
            return this.getStart().getTimeStamp();
        }

        /**
         * Calculates the epoch millis of the end position for the filter
         * @return end date expressed in  epoch millis
         */
        public long getTimeStampEnd() {
            return this.getEnd().getTimeStamp();
        }

        @Override
        public String toString() {
            return String.format("%s=[ %s TO %s ]", field, start.toString(), end.toString());
        }

        @Override
        public Filter clone() {
            final BetweenDatesFilter copy = new BetweenDatesFilter(this.field,this.start, this.end, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link BetweenDatesFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }

        /**
         * Get the start date of the filter period.
         * @return ZonedDateTime start date.
         */
        public DateMathExpression getStart() {
            return start;
        }

        /**
         * Get the end date of the filter period.
         * @return ZonedDateTime end date.
         */
        public DateMathExpression getEnd() {
            return end;
        }
    }

    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with  this {@link BeforeFilter#field} and value before {@link BeforeFilter#date}.
     */
    public static class BeforeFilter extends FieldBasedFilter {
        private final String field;
        private final DateMathExpression date;

        /**
         * Creates a {@link BeforeFilter} object based on a given field name parameter and a date.
         * @param field String name of the field to build the filter over.
         * @param date ZoneDateTime date setting the time point before which all documents are filtered by.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public BeforeFilter(String field, DateMathExpression date, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(date);

            this.field = field;
            this.date = date;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=[ * TO %s ]", field, date);
        }

        @Override
        public Filter clone() {
            final BeforeFilter copy = new BeforeFilter(this.field,this.date, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link BeforeFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }
        /**
         * Get the date of the filter.
         * @return ZonedDateTime filter before date.
         */
        public DateMathExpression getDate() {
            return date;
        }
    }

    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with  this {@link AfterFilter#field} and value after {@link AfterFilter#date}.
     */
    public static class AfterFilter extends FieldBasedFilter {
        private final String field;
        private final DateMathExpression date;

        /**
         * Creates a {@link AfterFilter} object based on a given field name parameter and a date.
         * @param field String name of the field to build the filter over.
         * @param date {@link DateMathExpression} setting the time point after which all documents are filtered by.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public AfterFilter(String field, DateMathExpression date, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(date);

            this.field = field;
            this.date = date;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=[ %s TO * ]", field, date);
        }

        @Override
        public Filter clone() {
            final AfterFilter copy = new AfterFilter(this.field,this.date, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link AfterFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }
        /**
         * Get the date of the filter.
         * @return ZonedDateTime filter after date.
         */
        public DateMathExpression getDate() {
            return date;
        }
    }
    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with this {@link BetweenNumericFilter#field} and value between
     * {@link BetweenNumericFilter#start} and {@link BetweenNumericFilter#end}.
     */
    public static class BetweenNumericFilter extends FieldBasedFilter {
        private final String field;
        private final Number start;
        private final Number end;
        /**
         * Creates a {@link BetweenNumericFilter} object based on a given field name parameter, start number and
         * an end number.
         * @param field String name of the field to build the filter over.
         * @param start Number date setting the starting point of the period to be filtered by.
         * @param end Number date setting the ending point of the period to be filtered by.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public BetweenNumericFilter(String field, Number start, Number end, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            this.field = field;
            this.start = start;
            this.end = end;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=[ %s TO %s ]", field, start,end);
        }

        @Override
        public Filter clone() {
            final BetweenNumericFilter copy = new BetweenNumericFilter(this.field,this.start, this.end, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link BetweenNumericFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }
        /**
         * Get the start number of the filter period.
         * @return Number start number.
         */
        public Number getStart() {
            return start;
        }
        /**
         * Get the end number of the filter period.
         * @return Number end number.
         */
        public Number getEnd() {
            return end;
        }
    }
    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with  this {@link GreaterThanFilter#field} and value bigger than
     * {@link GreaterThanFilter#number}.
     */
    public static class GreaterThanFilter extends FieldBasedFilter {
        private final String field;
        private final Number number;
        /**
         * Creates a {@link GreaterThanFilter} object based on a given field name parameter and a number.
         * @param field String name of the field to build the filter over.
         * @param number Number setting the point which all documents with a lower value are filtered by.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public GreaterThanFilter(String field, Number number, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(number);
            this.field = field;
            this.number = number;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=[ %s TO * ]", field, number);
        }

        @Override
        public Filter clone() {
            final GreaterThanFilter copy = new GreaterThanFilter(this.field,this.number, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link GreaterThanFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }
        /**
         * Get the number of the filter.
         * @return Number lower limit number.
         */
        public Number getNumber() {
            return number;
        }
    }
    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with  this {@link LowerThanFilter#field} and value lower than
     * {@link LowerThanFilter#number}.
     */
    public static class LowerThanFilter extends FieldBasedFilter {
        private final String field;
        private final Number number;
        /**
         * Creates a {@link LowerThanFilter} object based on a given field name parameter and a number.
         * @param field String name of the field to build the filter over.
         * @param number Number setting the point which all documents with a greater value are filtered by.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public LowerThanFilter(String field, Number number, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(number);

            this.field = field;
            this.number = number;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=[ * TO %s ]", field, number);
        }

        @Override
        public Filter clone() {
            final LowerThanFilter copy = new LowerThanFilter(this.field,this.number, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link LowerThanFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }

        /**
         * Get the number of the filter.
         * @return Number greater limit number.
         */
        public Number getNumber() {
            return number;
        }
    }

    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with the value of this {@link WithinBBoxFilter#field}
     * is within a bounding box (defined by {@link WithinBBoxFilter#upperLeft}
     * and {@link WithinBBoxFilter#lowerRight})
     */
    public static class WithinBBoxFilter extends FieldBasedFilter {
        private final String field;
        private final LatLng upperLeft, lowerRight;
        /**
         * Creates a {@link LowerThanFilter} object based on a given
         * field name parameter and two {@link com.rbmhtechnology.vind.model.value.LatLng} values.
         * @param field String name of the field to build the filter over.
         * @param upperLeft The upper left corner of the bounding box
         * @param lowerRight The lower right corner of the bounding box
         */
        public WithinBBoxFilter(String field, LatLng upperLeft, LatLng lowerRight) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(upperLeft);
            Objects.requireNonNull(lowerRight);

            this.field = field;
            this.upperLeft = upperLeft;
            this.lowerRight = lowerRight;
            super.filterScope = null;
        }

        /**
         * Creates a {@link LowerThanFilter} object based on a given
         * field name parameter and two {@link com.rbmhtechnology.vind.model.value.LatLng} values.
         * @param field String name of the field to build the filter over.
         * @param upperLeft The upper left corner of the bounding box
         * @param lowerRight The lower right corner of the bounding box
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public WithinBBoxFilter(String field, LatLng upperLeft, LatLng lowerRight, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(upperLeft);
            Objects.requireNonNull(lowerRight);

            this.field = field;
            this.upperLeft = upperLeft;
            this.lowerRight = lowerRight;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=[ %s TO %s ]", field, upperLeft, lowerRight);
        }

        @Override
        public Filter clone() {
            final WithinBBoxFilter copy = new WithinBBoxFilter(this.field,this.upperLeft, this.lowerRight, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link WithinBBoxFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }

        /**
         * Get the upper left value of the bbox
         * @return LatLng {@link WithinBBoxFilter#upperLeft}.
         */
        public LatLng getUpperLeft() {
            return upperLeft;
        }

        /**
         * Get the lower right value of the bbox
         * @return LatLng {@link WithinBBoxFilter#lowerRight}.
         */
        public LatLng getLowerRight() {
            return lowerRight;
        }
    }

    /**
     * Filter Class implementing a filter by field value. A query performed with this filter should return all the
     * documents with the value of this {@link WithinBBoxFilter#field}
     * is within a circle (defined by {@link WithinBBoxFilter#field}
     */
    public static class WithinCircleFilter extends FieldBasedFilter {
        private final String field;
        private final LatLng center;
        private final double distance;
        /**
         * Creates a {@link LowerThanFilter} object based on a given
         * field name parameter and two {@link com.rbmhtechnology.vind.model.value.LatLng} values.
         * @param field String name of the field to build the filter over.
         * @param center The center of the circle
         * @param distance The radius of the circle
         */
        public WithinCircleFilter(String field, LatLng center, double distance) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(center);
            Objects.requireNonNull(distance);

            this.field = field;
            this.center = center;
            this.distance = distance;
            super.filterScope = null;
        }

        /**
         * Creates a {@link LowerThanFilter} object based on a given
         * field name parameter and two {@link com.rbmhtechnology.vind.model.value.LatLng} values.
         * @param field String name of the field to build the filter over.
         * @param center The center of the circle
         * @param distance The radius of the circle
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public WithinCircleFilter(String field, LatLng center, double distance, Scope scope) {
            Objects.requireNonNull(field);
            Objects.requireNonNull(center);
            Objects.requireNonNull(distance);

            this.field = field;
            this.center = center;
            this.distance = distance;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("&fq={!geofilt sfield=%s}&pt=%s&d=%s", field, center, distance);
        }

        @Override
        public Filter clone() {
            final WithinCircleFilter copy = new WithinCircleFilter(this.field,this.center, this.distance, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link WithinBBoxFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }

        /**
         * Get the center of the circle
         * @return LatLng {@link WithinCircleFilter#center}.
         */
        public LatLng getCenter() {
            return center;
        }

        /**
         * Get the radius of teh cricle
         * @return double {@link WithinCircleFilter#distance}.
         */
        public double getDistance() {
            return distance;
        }
    }

    /**
     * Filter Class implementing a text field empty filter. A query performed with this filter should return all the
     * documents with have the {@link NotEmptyTextFilter#field}
     * with a value.
     */
    public static class NotEmptyTextFilter extends FieldBasedFilter {

        private final String field;

        /**
         * Creates a {@link NotEmptyTextFilter} object based on a given
         * field name parameter.
         * @param field String name of the field to build the filter over.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public NotEmptyTextFilter(String field, Scope scope) {
            Objects.requireNonNull(field);

            this.field = field;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=*", field);
        }

        @Override
        public Filter clone() {
            final NotEmptyTextFilter copy = new NotEmptyTextFilter(this.field, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link NotEmptyTextFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }
    }

    /**
     * Filter Class implementing a field empty filter. A query performed with this filter should return all the
     * documents with have the {@link NotEmptyFilter#field}
     * with a value.
     */
    public static class NotEmptyFilter extends FieldBasedFilter {

        private final String field;

        /**
         * Creates a {@link NotEmptyFilter} object based on a given
         * field name parameter.
         * @param field String name of the field to build the filter over.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public NotEmptyFilter(String field, Scope scope) {
            Objects.requireNonNull(field);

            this.field = field;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=*", field);
        }

        @Override
        public Filter clone() {
            final NotEmptyFilter copy = new NotEmptyFilter(this.field, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link NotEmptyFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }
    }

    /**
     * Filter Class implementing a location field empty filter. A query performed with this filter should return all the
     * documents with have the {@link NotEmptyLocationFilter#field}
     * with a value.
     */
    public static class NotEmptyLocationFilter extends FieldBasedFilter {

        private final String field;

        /**
         * Creates a {@link NotEmptyLocationFilter} object based on a given
         * field name parameter.
         * @param field String name of the field to build the filter over.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public NotEmptyLocationFilter(String field, Scope scope) {
            Objects.requireNonNull(field);

            this.field = field;
            super.filterScope = scope;
        }

        @Override
        public String toString() {
            return String.format("%s=*", field);
        }

        @Override
        public Filter clone() {
            final NotEmptyLocationFilter copy = new NotEmptyLocationFilter(this.field, super.filterScope);
            return copy;
        }

        /**
         * Get the filtered field name
         * @return String {@link NotEmptyLocationFilter#field} with the field name.
         */
        @Override
        public String getField() {
            return field;
        }
    }

    /**
     * Filter Class implementing a has nested documents filter. A query performed with this filter should return all the
     * documents which have related nested documents of type {@link ChildrenDocumentFilter#nestedDocType}.
     */
    public static class ChildrenDocumentFilter extends Filter {

        private final String parentDocType;
        private String nestedDocType;


        /**
         * Creates a {@link ChildrenDocumentFilter} object based on a given
         * {@link DocumentFactory} parameter specifying the parent document type.
         * @param documentFactory {@link DocumentFactory} of the nested document.
         */
        public ChildrenDocumentFilter(DocumentFactory documentFactory) {
            Objects.requireNonNull(documentFactory);
            this.parentDocType = documentFactory.getType();
        }

        /**
         * Creates a {@link ChildrenDocumentFilter} object based on a given
         * String value of the parent document type parameter.
         * @param docType String type of the nested document.
         */
        public ChildrenDocumentFilter(String docType) {
            Objects.requireNonNull(docType);
            this.parentDocType = docType;
        }

        /**
         * Creates a {@link ChildrenDocumentFilter} object based on a given
         * {@link DocumentFactory} parameter specifying the parent document type.
         * @param documentFactory {@link DocumentFactory} of the parent document.
         * @param childDocumentFactory {@link DocumentFactory} of the nested document.
         */
        public ChildrenDocumentFilter(DocumentFactory documentFactory, DocumentFactory childDocumentFactory) {
            Objects.requireNonNull(documentFactory);
            Objects.requireNonNull(childDocumentFactory);
            this.parentDocType = documentFactory.getType();
            this.nestedDocType = childDocumentFactory.getType();
        }

        /**
         * Creates a {@link ChildrenDocumentFilter} object based on a given
         * String value of the parent document type parameter.
         * @param docType String type of the parent document.
         * @param childType String type of the nested document.
         */
        public ChildrenDocumentFilter(String docType, String childType) {
            Objects.requireNonNull(docType);
            Objects.requireNonNull(childType);
            this.parentDocType = docType;
            this.nestedDocType = childType;
        }

        @Override
        public String toString() {
            return String.format("(parentType=%s & nestedType=%s)", parentDocType,nestedDocType);
        }

        @Override
        public Filter clone() {
            final ChildrenDocumentFilter copy = new ChildrenDocumentFilter(this.parentDocType, this.nestedDocType);
            return copy;
        }

        /**
         * Get the filtered parent type
         * @return String {@link ChildrenDocumentFilter#parentDocType} with the type name.
         */
        public String getParentDocType() {
            return this.parentDocType;
        }

        /**
         * Get the filtered children type
         * @return String {@link ChildrenDocumentFilter#nestedDocType} with the type name.
         */
        public String getNestedDocType() {
            return this.nestedDocType;
        }
    }

    public static class TermsQueryFilter<T> extends FieldBasedFilter {

        private final T[] terms;
        private final FieldDescriptor descriptor;
        private final String field;

        /**
         * Creates a {@link TermsQueryFilter} object based on a given field descriptor parameter
         * and a list of values.
         * @param descriptor {@link FieldDescriptor} of the field to build the filter over.
         * @param terms T Value of the field to build the filter over.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         */
        public TermsQueryFilter(FieldDescriptor<T> descriptor, T[] terms, Scope scope) {
            Objects.requireNonNull(descriptor);
            Objects.requireNonNull(terms);
            this.descriptor = descriptor;
            this.terms = terms;
            super.filterScope = scope;
            this.field = descriptor.getName();
        }

        @Override
        public Scope getFilterScope() {
            return this.getFilterScope(this.descriptor);
        }

        @Override
        public String toString() {
            return String.format("%s='%s'",
                    descriptor.getName(),
                    Arrays.asList(terms).stream().map(Object::toString).collect(Collectors.joining(", ")));
        }

        /**
         * Get the filtered field name
         * @return FieldDescriptor {@link TermsQueryFilter#descriptor} with the field description.
         */
        public FieldDescriptor getDescriptor() {
            return descriptor;
        }

        /**
         * Get the filtered value for the field.
         * @return T {@link TermsQueryFilter#terms} with the value to filter by.
         */
        public List<T> getTerm() {
            return Arrays.asList(terms);
        }

        /**
         * Get the name of the field.
         * @return  {@link String} with the value to field descriptor name.
         */
        @Override
        public String getField() {
            return field;
        }

        @Override
        public Filter clone() {
            final TermsQueryFilter<T> copy =
                    new TermsQueryFilter<T>(this.descriptor,this.terms, super.filterScope);
            return copy;
        }

    }
    private static class OrCollector extends FilterCollector {
        @Override
        public Function<ImmutableSet.Builder<Filter>, Filter> finisher() {
            return new Function<ImmutableSet.Builder<Filter>, Filter>(){

                @Override
                public Filter apply(ImmutableSet.Builder<Filter> filterBuilder) {
                    return OrFilter.fromSet(filterBuilder.build());
                }

                @Override
                public <V> Function<V, Filter> compose(Function<? super V, ? extends ImmutableSet.Builder<Filter>> before) {
                    return null;//TODO
                }

                @Override
                public <V> Function<ImmutableSet.Builder<Filter>, V> andThen(Function<? super Filter, ? extends V> after) {
                    return null;//TODO
                }
            };
        }

    }

    private static class AndCollector extends FilterCollector {
        @Override
        public Function<ImmutableSet.Builder<Filter>, Filter> finisher() {
            return new Function<ImmutableSet.Builder<Filter>, Filter>(){

                @Override
                public Filter apply(ImmutableSet.Builder<Filter> filterBuilder) {
                    return AndFilter.fromSet(filterBuilder.build());
                }

                @Override
                public <V> Function<V, Filter> compose(Function<? super V, ? extends ImmutableSet.Builder<Filter>> before) {
                    return null;//TODO
                }

                @Override
                public <V> Function<ImmutableSet.Builder<Filter>, V> andThen(Function<? super Filter, ? extends V> after) {
                    return null;//TODO
                }
            };
        }

    }
    private static abstract class FilterCollector implements Collector<Filter, ImmutableSet.Builder<Filter>, Filter> {

        @Override
        public Supplier<ImmutableSet.Builder<Filter>> supplier() {
            return ImmutableSet::builder;
        }

        @Override
        public BiConsumer<ImmutableSet.Builder<Filter>, Filter> accumulator() {
            return ImmutableSet.Builder::add;
        }

        @Override
        public BinaryOperator<ImmutableSet.Builder<Filter>> combiner() {
            return (left, right) -> {
                left.addAll(right.build());
                return left;
            };
        }
        @Override
        public Set<Collector.Characteristics> characteristics() {
            return EnumSet.of(Collector.Characteristics.UNORDERED);
        }

    }
}
