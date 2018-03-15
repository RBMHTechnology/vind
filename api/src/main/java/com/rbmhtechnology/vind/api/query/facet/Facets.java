package com.rbmhtechnology.vind.api.query.facet;

import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.facet.Facet.DateIntervalFacet.UtilDateIntervalFacet;
import com.rbmhtechnology.vind.api.query.facet.Facet.DateIntervalFacet.UtilDateMathIntervalFacet;
import com.rbmhtechnology.vind.api.query.facet.Facet.DateIntervalFacet.ZoneDateTimeDateMathIntervalFacet;
import com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet.DateMathRangeFacet;
import com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet.UtilDateRangeFacet;
import com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet.ZoneDateRangeFacet;
import com.rbmhtechnology.vind.api.query.facet.Facet.NumericIntervalFacet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.ComplexFieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;

/**
 *  Utility class offering static user friendly methods to create intuitively facets for a search query.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 29.06.16.
 */
public class Facets {

    /**
     * Creates a basic facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.TermFacet}) for
     * each {@link FieldDescriptor} provided as parameter.
     * @param descriptors FieldDescriptor[] group of fields to create facets for.
     * @return {@link HashMap} A map of {@link Facet.TermFacet} which key is the name of the field they apply to.
     */
    public static HashMap<String,Facet> term(FieldDescriptor<?>... descriptors) {
        HashMap<String, Facet> facets = new HashMap<>();
        for(FieldDescriptor<?> descriptor : descriptors) {
            Facet facet = new Facet.TermFacet(descriptor);
            facets.put(facet.getName(), facet);
        }
        return facets;
    }

    /**
     * Creates a basic facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.TermFacet}) for
     * each {@link FieldDescriptor} provided as parameter.
     * @param scope sets the scope where the facet will be done.
     * @param descriptors FieldDescriptor[] group of fields to create facets for.
     * @return {@link HashMap} A map of {@link Facet.TermFacet} which key is the name of the field they apply to.
     */
    public static HashMap<String,Facet> term(Scope scope, FieldDescriptor<?>... descriptors) {
        HashMap<String, Facet> facets = new HashMap<>();
        for(FieldDescriptor<?> descriptor : descriptors) {
            Facet facet = new Facet.TermFacet(descriptor).setScope(scope);
            facets.put(facet.getName(), facet);
        }
        return facets;
    }

    /**
     * Creates a basic facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.TermFacet}) for
     * each field provided as parameter.
     * @param names String group of name fields to create facets for.
     * @return {@link HashMap} A map of {@link Facet.TermFacet} which key is the name of the field they apply to.
     */
    public static HashMap<String,Facet> term(String ... names) {
        HashMap<String, Facet> facets = new HashMap<>();
        for(String name : names) {
            Facet facet = new Facet.TermFacet(name);
            facets.put(facet.getName(), facet);
        }
        return facets;
    }

    /**
     * Creates a basic facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.TermFacet}) for
     * each field provided as parameter.
     * @param scope sets the scope where the facet will be done.
     * @param names String group of name fields to create facets for.
     * @return {@link HashMap} A map of {@link Facet.TermFacet} which key is the name of the field they apply to.
     */
        public static HashMap<String,Facet> term(Scope scope, String ... names) {
        HashMap<String, Facet> facets = new HashMap<>();
        for(String name : names) {
            Facet facet = new Facet.TermFacet(name).setScope(scope);
            facets.put(facet.getName(), facet);
        }
        return facets;
    }

    /**
     * Creates a basic facet by document type ({@link com.rbmhtechnology.vind.api.query.facet.Facet.TypeFacet}).
     * @return {@link HashMap} A map of {@link Facet.TermFacet} which key is 'DocType'.
     */
    public static Facet type() {
        return new Facet.TypeFacet();
    }

    /**
     * Creates a numeric range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.NumericRangeFacet})
     * for each field provided as parameter.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link FieldDescriptor} to do the range query on. T must extend {@link Number}.
     * @param start T Starting value of the defined range. T must extend {@link Number}.
     * @param end T Ending value of the defined range. T must extend {@link Number}.
     * @param gap T Size of the steps in which the range is divided to facet the results. T must extend {@link Number}.
     * @param pivotNames
     * @return {@link Facet.NumericRangeFacet} object.
     */
    public static <T extends Number> Facet range(String name, FieldDescriptor<T> descriptor, T start, T end, T gap, String ...pivotNames) {
        return new Facet.NumericRangeFacet<>(name, descriptor, start, end, gap, pivotNames);
    }

    /**
     * Creates a numeric range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.NumericRangeFacet})
     * for each field provided as parameter.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link FieldDescriptor} to do the range query on. T must extend {@link Number}.
     * @param start T Starting value of the defined range. T must extend {@link Number}.
     * @param end T Ending value of the defined range. T must extend {@link Number}.
     * @param gap T Size of the steps in which the range is divided to facet the results. T must extend {@link Number}.
     * @param pivotNames
     * @return {@link Facet.NumericRangeFacet} object.
     */
    public static <T extends Number> Facet range(Scope scope, String name, FieldDescriptor<T> descriptor, T start, T end, T gap, String ...pivotNames) {
        return new Facet.NumericRangeFacet<>(name, descriptor, start, end, gap, pivotNames).setScope(scope);
    }

   /* public static <T extends Number> Facet range(String name, ComplexFieldDescriptor<?,T,?> descriptor, T start, T end, T gap, String ...pivotNames) {
        return new Facet.NumericRangeFacet<>(name, descriptor, start, end, gap, pivotNames);
    }*/

    /**
     * Creates a date range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet})
     * for the multivalued field provided as parameter.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.DateFieldDescriptor} to do the range query on. T must extend {@link ZonedDateTime}.
     * @param start T Starting value of the defined range. T must extend {@link ZonedDateTime}.
     * @param end T Ending value of the defined range. T must extend {@link ZonedDateTime}.
     * @param gap Duration Size of the steps in which the range is divided to facet the results.
     * @param pivotNames
     * @return {@link ZoneDateRangeFacet} object.
     */
    public static <T extends ZonedDateTime> Facet range(String name, MultiValueFieldDescriptor.DateFieldDescriptor<T> descriptor, T start, T end, Duration gap, String ...pivotNames) {
        return new ZoneDateRangeFacet<>(name, descriptor, start, end, gap, pivotNames);
    }

    /**
     * Creates a date range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet})
     * for the multivalued field provided as parameter.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.DateFieldDescriptor} to do the range query on. T must extend {@link ZonedDateTime}.
     * @param start T Starting value of the defined range. T must extend {@link ZonedDateTime}.
     * @param end T Ending value of the defined range. T must extend {@link ZonedDateTime}.
     * @param gap Duration Size of the steps in which the range is divided to facet the results.
     * @param pivotNames
     * @return {@link ZoneDateRangeFacet} object.
     */
    public static <T extends ZonedDateTime> Facet range(Scope scope, String name, MultiValueFieldDescriptor.DateFieldDescriptor<T> descriptor, T start, T end, Duration gap, String ...pivotNames) {
        return new ZoneDateRangeFacet<>(name, descriptor, start, end, gap, pivotNames).setScope(scope);
    }

    public static <T extends ZonedDateTime> Facet range(String name, ComplexFieldDescriptor<?,T,?> descriptor, T start, T end, Duration gap, String ...pivotNames) {
        return new ZoneDateRangeFacet<>(name, descriptor, start, end, gap, pivotNames);
    }

    public static <T extends ZonedDateTime> Facet range(Scope scope,String name, ComplexFieldDescriptor<?,T,?> descriptor, T start, T end, Duration gap, String ...pivotNames) {
        return new ZoneDateRangeFacet<>(name, descriptor, start, end, gap, pivotNames).setScope(scope);
    }
    /**
     * Creates a date range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet})
     * for the singlevalued field provided as parameter.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.DateFieldDescriptor} to do the range query on. T must extend {@link ZonedDateTime}.
     * @param start T Starting value of the defined range. T must extend {@link ZonedDateTime}.
     * @param end T Ending value of the defined range. T must extend {@link ZonedDateTime}.
     * @param gap Duration Size of the steps in which the range is divided to facet the results.
     * @param pivotNames
     * @return {@link ZoneDateRangeFacet} object.
     */
    public static <T extends ZonedDateTime> Facet range(String name, SingleValueFieldDescriptor.DateFieldDescriptor<T> descriptor, T start, T end, Duration gap, String ...pivotNames) {
        return new ZoneDateRangeFacet<>(name, descriptor, start, end, gap, pivotNames);
    }

    /**
     * Creates a date range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet})
     * for the singlevalued field provided as parameter.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.DateFieldDescriptor} to do the range query on. T must extend {@link ZonedDateTime}.
     * @param start T Starting value of the defined range. T must extend {@link ZonedDateTime}.
     * @param end T Ending value of the defined range. T must extend {@link ZonedDateTime}.
     * @param gap Duration Size of the steps in which the range is divided to facet the results.
     * @param pivotNames
     * @return {@link ZoneDateRangeFacet} object.
     */
    public static <T extends ZonedDateTime> Facet range(Scope scope,String name, SingleValueFieldDescriptor.DateFieldDescriptor<T> descriptor, T start, T end, Duration gap, String ...pivotNames) {
        return new ZoneDateRangeFacet<>(name, descriptor, start, end, gap, pivotNames).setScope(scope);
    }

    public static <T extends ZonedDateTime> Facet range(String name, FieldDescriptor<T> descriptor, DateMathExpression start, DateMathExpression end, Duration gap, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, pivotNames);
    }

    public static <T extends ZonedDateTime> Facet range(Scope scope, String name, FieldDescriptor<T> descriptor, DateMathExpression start, DateMathExpression end, Duration gap, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, pivotNames).setScope(scope);
    }

    public static <T extends ZonedDateTime> Facet range(String name, ComplexFieldDescriptor<?,T,?> descriptor, DateMathExpression start, DateMathExpression end, Duration gap, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, pivotNames);
    }

    public static <T extends ZonedDateTime> Facet range(Scope scope, String name, ComplexFieldDescriptor<?,T,?> descriptor, DateMathExpression start, DateMathExpression end, Duration gap, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, pivotNames).setScope(scope);
    }

    /**
     * Creates a date range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet})
     * for the multivalued field provided as parameter.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.UtilDateFieldDescriptor} to do the range query on. T must extend {@link Date}.
     * @param start T Starting value of the defined range. T must extend {@link Date}.
     * @param end T Ending value of the defined range. T must extend {@link Date}.
     * @param gap Long Size of the steps in which the range is divided to facet the results.
     * @param timeUnit TimeUnit Units in which the gap is given.
     * @param pivotNames
     * * @return {@link UtilDateRangeFacet} object.
     */
    public static <T extends Date> Facet range(String name, MultiValueFieldDescriptor.UtilDateFieldDescriptor<T> descriptor, T start, T end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new UtilDateRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames);
    }

    /**
     * Creates a date range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet})
     * for the multivalued field provided as parameter.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.UtilDateFieldDescriptor} to do the range query on. T must extend {@link Date}.
     * @param start T Starting value of the defined range. T must extend {@link Date}.
     * @param end T Ending value of the defined range. T must extend {@link Date}.
     * @param gap Long Size of the steps in which the range is divided to facet the results.
     * @param timeUnit TimeUnit Units in which the gap is given.
     * @param pivotNames
     * * @return {@link UtilDateRangeFacet} object.
     */
    public static <T extends Date> Facet range(Scope scope, String name, MultiValueFieldDescriptor.UtilDateFieldDescriptor<T> descriptor, T start, T end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new UtilDateRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames).setScope(scope);
    }

    /**
     * Creates a date range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet})
     * for the singlevalued field provided as parameter.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.UtilDateFieldDescriptor} to do the range query on. T must extend {@link Date}.
     * @param start T Starting value of the defined range. T must extend {@link Date}.
     * @param end T Ending value of the defined range. T must extend {@link Date}.
     * @param gap Long Size of the steps in which the range is divided to facet the results.
     * @param timeUnit TimeUnit Units in which the gap is given.
     * @param pivotNames
     * * @return {@link UtilDateRangeFacet} object.
     */
    public static <T extends Date> Facet range(String name, SingleValueFieldDescriptor.UtilDateFieldDescriptor<T> descriptor, T start, T end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new UtilDateRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames);
    }

    /**
     * Creates a date range facet query object ({@link com.rbmhtechnology.vind.api.query.facet.Facet.DateRangeFacet})
     * for the singlevalued field provided as parameter.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
     * @param descriptor {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.UtilDateFieldDescriptor} to do the range query on. T must extend {@link Date}.
     * @param start T Starting value of the defined range. T must extend {@link Date}.
     * @param end T Ending value of the defined range. T must extend {@link Date}.
     * @param gap Long Size of the steps in which the range is divided to facet the results.
     * @param timeUnit TimeUnit Units in which the gap is given.
     * @param pivotNames
     * * @return {@link UtilDateRangeFacet} object.
     */
    public static <T extends Date> Facet range(Scope scope, String name, SingleValueFieldDescriptor.UtilDateFieldDescriptor<T> descriptor, T start, T end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new UtilDateRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames).setScope(scope);
    }

    public static <T extends Date> Facet range(String name, ComplexFieldDescriptor<?,T,?> descriptor, T start, T end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new UtilDateRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames);
    }

    public static <T extends Date> Facet range(Scope scope, String name, ComplexFieldDescriptor<?,T,?> descriptor, T start, T end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new UtilDateRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames).setScope(scope);
    }

    public static <T extends Date> Facet range(String name, SingleValueFieldDescriptor.UtilDateFieldDescriptor<T> descriptor, DateMathExpression start, DateMathExpression end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames);
    }

    public static <T extends Date> Facet range(Scope scope,String name, SingleValueFieldDescriptor.UtilDateFieldDescriptor<T> descriptor, DateMathExpression start, DateMathExpression end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames).setScope(scope);
    }
    public static <T extends Date> Facet range(String name, ComplexFieldDescriptor<?,T,?> descriptor, DateMathExpression start, DateMathExpression end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames);
    }

    public static <T extends Date> Facet range(Scope scope, String name, ComplexFieldDescriptor<?,T,?> descriptor, DateMathExpression start, DateMathExpression end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames).setScope(scope);
    }

    public static <T extends Date> Facet range(String name, MultiValueFieldDescriptor.UtilDateFieldDescriptor<T> descriptor, DateMathExpression start, DateMathExpression end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames);
    }

    public static <T extends Date> Facet range(Scope scope, String name, MultiValueFieldDescriptor.UtilDateFieldDescriptor<T> descriptor, DateMathExpression start, DateMathExpression end, Long gap, TimeUnit timeUnit, String ...pivotNames) {
        return new DateMathRangeFacet<>(name, descriptor, start, end, gap, timeUnit, pivotNames).setScope(scope);
    }

    public static <T extends Number> Facet interval(String name, FieldDescriptor<T> fieldDescriptor, Interval.NumericInterval<T>... intervals) {
        return new NumericIntervalFacet<>(name, fieldDescriptor, intervals);
    }

    public static <T extends Number> Facet interval(Scope scope, String name, FieldDescriptor<T> fieldDescriptor, Interval.NumericInterval<T>... intervals) {
        return new NumericIntervalFacet<>(name, fieldDescriptor, intervals).setScope(scope);
    }

    public static <T extends Date> Facet interval(String name, FieldDescriptor<T> fieldDescriptor, Interval.UtilDateInterval<T>... intervals) {
        return new UtilDateIntervalFacet<>(name, fieldDescriptor, intervals);
    }

    public static <T extends Date> Facet interval(Scope scope, String name, FieldDescriptor<T> fieldDescriptor, Interval.UtilDateInterval<T>... intervals) {
        return new UtilDateIntervalFacet<>(name, fieldDescriptor, intervals).setScope(scope);
    }

    public static <T extends ZonedDateTime> Facet interval(String name, FieldDescriptor<T> fieldDescriptor, Interval.ZonedDateTimeInterval<T>... intervals) {
        return new Facet.DateIntervalFacet.ZoneDateTimeIntervalFacet<>(name, fieldDescriptor, intervals);
    }

    public static <T extends ZonedDateTime> Facet interval(Scope scope, String name, FieldDescriptor<T> fieldDescriptor, Interval.ZonedDateTimeInterval<T>... intervals) {
        return new Facet.DateIntervalFacet.ZoneDateTimeIntervalFacet<>(name, fieldDescriptor, intervals).setScope(scope);
    }

    //TODO: Find out how to support General Field descriptors
    public static <T extends ZonedDateTime> Facet interval(String name, SingleValueFieldDescriptor.DateFieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new ZoneDateTimeDateMathIntervalFacet<>(name, fieldDescriptor, intervals);
    }

    public static <T extends ZonedDateTime> Facet interval(Scope scope, String name, SingleValueFieldDescriptor.DateFieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new ZoneDateTimeDateMathIntervalFacet<>(name, fieldDescriptor, intervals).setScope(scope);
    }

    public static <T extends Date> Facet interval(String name, SingleValueFieldDescriptor.UtilDateFieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new UtilDateMathIntervalFacet<>(name, fieldDescriptor, intervals);
    }

    public static <T extends Date> Facet interval(Scope scope, String name, SingleValueFieldDescriptor.UtilDateFieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new UtilDateMathIntervalFacet<>(name, fieldDescriptor, intervals).setScope(scope);
    }

    public static <T extends ZonedDateTime> Facet interval(String name, MultiValueFieldDescriptor.DateFieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new ZoneDateTimeDateMathIntervalFacet<>(name, fieldDescriptor, intervals);
    }

    public static <T extends ZonedDateTime> Facet interval(Scope scope, String name, MultiValueFieldDescriptor.DateFieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new ZoneDateTimeDateMathIntervalFacet<>(name, fieldDescriptor, intervals).setScope(scope);
    }

    public static <T extends Date> Facet interval(String name, MultiValueFieldDescriptor.UtilDateFieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new UtilDateMathIntervalFacet<>(name, fieldDescriptor, intervals);
    }

    public static <T extends Date> Facet interval(Scope scope, String name, MultiValueFieldDescriptor.UtilDateFieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new UtilDateMathIntervalFacet<>(name, fieldDescriptor, intervals).setScope(scope);
    }

    //TODO: MBDN-454 Complex fields - Hpw to support both and be type safe
   /* public static <T extends ZonedDateTime> Facet interval(String name, ComplexFieldDescriptor<?,T,?> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new ZoneDateTimeDateMathIntervalFacet<>(name, fieldDescriptor, intervals);
    }*/
    public static <T extends Date> Facet interval(String name, ComplexFieldDescriptor<?,T,?> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new UtilDateMathIntervalFacet<>(name, fieldDescriptor, intervals);
    }

    public static <T extends Date> Facet interval(Scope scope, String name, ComplexFieldDescriptor<?,T,?> fieldDescriptor, Interval.DateMathInterval... intervals) {
        return new UtilDateMathIntervalFacet<>(name, fieldDescriptor, intervals).setScope(scope);
    }


    /**
     * Creates a basic pivot facet query on a group of fields. A basic pivot facet query returns a decision
     * tree of the specified fields.
     * @param name String with a custom name for the new instance.
     * @param fieldDescriptors A group of {@link FieldDescriptor} objects on which perform the pivot query
     * @return {@link Facet.PivotFacet} object.
     */
    public static Facet pivot(String name,  FieldDescriptor<?>... fieldDescriptors) {
        return new Facet.PivotFacet(name, fieldDescriptors);
    }
    /**
     * Creates a basic pivot facet query on a group of fields. A basic pivot facet query returns a decision
     * tree of the specified fields.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param fieldDescriptors A group of {@link FieldDescriptor} objects on which perform the pivot query
     * @return {@link Facet.PivotFacet} object.
     */
    public static Facet pivot(Scope scope, String name,  FieldDescriptor<?>... fieldDescriptors) {
        return new Facet.PivotFacet(name, fieldDescriptors).setScope(scope);
    }

    /**
     * Creates a query facet. A query facet restricts the facet results based on a group of filters.
     * @param name String with a custom name for the new instance.
     * @param filter {@link Filter} to apply to the facet results.
     * @param pivotNames Name of the pivots using this facet.
     * @return {@link Facet.QueryFacet} object.
     */
    public static Facet query(String name,  Filter filter, String ...pivotNames) {
        return new Facet.QueryFacet(name, filter, pivotNames);
    }

    /**
     * Creates a query facet. A query facet restricts the facet results based on a group of filters.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param filter {@link Filter} to apply to the facet results.
     * @param pivotNames Name of the pivots using this facet.
     * @return {@link Facet.QueryFacet} object.
     */
    public static Facet query(Scope scope, String name,  Filter filter, String ...pivotNames) {
        return new Facet.QueryFacet(name, filter, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a multivalued text field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.TextFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend CharSequence
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends CharSequence> Facet.StatsFacet<T> stats(String name,  MultiValueFieldDescriptor.TextFieldDescriptor<T> field, String ...pivotNames) {
        return new Facet.StatsFacet(name, field, pivotNames);
    }

    /**
     * Creates a basic stats facet query for a multivalued text field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.TextFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend CharSequence
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends CharSequence> Facet.StatsFacet<T> stats(Scope scope, String name,  MultiValueFieldDescriptor.TextFieldDescriptor<T> field, String ...pivotNames) {
        return new Facet.StatsFacet(name, field, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a single valued text field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.TextFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend CharSequence
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends CharSequence> Facet.StatsFacet<T> stats(String name,  SingleValueFieldDescriptor.TextFieldDescriptor<T> field, String ...pivotNames) {
        return new Facet.StatsFacet(name, field, pivotNames);
    }

    /**
     * Creates a basic stats facet query for a single valued text field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.TextFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend CharSequence
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends CharSequence> Facet.StatsFacet<T> stats(Scope scope, String name,  SingleValueFieldDescriptor.TextFieldDescriptor<T> field, String ...pivotNames) {
        return new Facet.StatsFacet(name, field, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a multivalued numeric field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.NumericFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend Number
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends Number> Facet.StatsNumericFacet<T> stats(String name,  MultiValueFieldDescriptor.NumericFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsNumericFacet(name, field, pivotNames);
    }
    /**
     * Creates a basic stats facet query for a multivalued numeric field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.NumericFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend Number
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends Number> Facet.StatsNumericFacet<T> stats(Scope scope, String name,  MultiValueFieldDescriptor.NumericFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsNumericFacet(name, field, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a multivalued date field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.DateFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend ZonedDateTime
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T  extends ZonedDateTime> Facet.StatsDateFacet<T> stats(String name,  MultiValueFieldDescriptor.DateFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsDateFacet(name, field, pivotNames);
    }

    /**
     * Creates a basic stats facet query for a multivalued date field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.DateFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend ZonedDateTime
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T  extends ZonedDateTime> Facet.StatsDateFacet<T> stats(Scope scope, String name,  MultiValueFieldDescriptor.DateFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsDateFacet(name, field, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a multivalued date field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.UtilDateFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend Date
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends Date> Facet.StatsUtilDateFacet<T> stats(String name,  MultiValueFieldDescriptor.UtilDateFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsUtilDateFacet<>(name, field, pivotNames);
    }
    /**
     * Creates a basic stats facet query for a multivalued date field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.UtilDateFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend Date
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends Date> Facet.StatsUtilDateFacet<T> stats(Scope scope, String name,  MultiValueFieldDescriptor.UtilDateFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsUtilDateFacet<T>(name, field, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a single valued numeric field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.NumericFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend Number
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends Number> Facet.StatsNumericFacet<T> stats(String name,  SingleValueFieldDescriptor.NumericFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsNumericFacet(name, field, pivotNames);
    }

    /**
     * Creates a basic stats facet query for a single valued numeric field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.NumericFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend Number
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends Number> Facet.StatsNumericFacet<T> stats(Scope scope, String name,  SingleValueFieldDescriptor.NumericFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsNumericFacet(name, field, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a single valued date field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.DateFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend ZonedDateTime
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends ZonedDateTime> Facet.StatsDateFacet<T> stats(String name,  SingleValueFieldDescriptor.DateFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsDateFacet(name, field, pivotNames);
    }

    /**
     * Creates a basic stats facet query for a single valued date field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.DateFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend ZonedDateTime
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends ZonedDateTime> Facet.StatsDateFacet<T> stats(Scope scope, String name,  SingleValueFieldDescriptor.DateFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsDateFacet(name, field, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a single valued date field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.UtilDateFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend Date
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends Date> Facet.StatsUtilDateFacet<T> stats(String name,  SingleValueFieldDescriptor.UtilDateFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsUtilDateFacet(name, field, pivotNames);
    }

    /**
     * Creates a basic stats facet query for a single valued date field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.UtilDateFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend Date
     * @return {@link Facet.StatsFacet} object.
     */
    public static <T extends Date> Facet.StatsUtilDateFacet<T> stats(Scope scope, String name,  SingleValueFieldDescriptor.UtilDateFieldDescriptor field, String ...pivotNames) {
        return new Facet.StatsUtilDateFacet(name, field, pivotNames).setScope(scope);
    }

    /**
     * Creates a basic stats facet query for a complex field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.ComplexFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend one of the supported types
     * @return {@link Facet.StatsFacet} object.
     */
    //TODO: Not yet type safe
    public static <T extends Object> Facet.StatsFacet<T> stats(String name, ComplexFieldDescriptor<?,T,?> field, String ...pivotNames) {
        return Facet.StatsFacet.createFacet(name,field,pivotNames);
    }

    /**
     * Creates a basic stats facet query for a complex field. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     * @param scope in which the facet will be done.
     * @param name String with a custom name for the new instance.
     * @param field {@link com.rbmhtechnology.vind.model.ComplexFieldDescriptor} to calculate the statistics on.
     * @param <T> T class must extend one of the supported types
     * @return {@link Facet.StatsFacet} object.
     */
    //TODO: Not yet type safe
    public static <T extends Object> Facet.StatsFacet<T> stats(Scope scope, String name, ComplexFieldDescriptor<?,T,?> field, String ...pivotNames) {
        return Facet.StatsFacet.createFacet(name,field,pivotNames).setScope(scope);
    }
}
