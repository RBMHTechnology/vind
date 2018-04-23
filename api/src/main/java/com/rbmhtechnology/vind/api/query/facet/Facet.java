package com.rbmhtechnology.vind.api.query.facet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;

/**
 * Abstract class to be implemented by the specific facet query classes {@link TermFacet}, {@link NumericRangeFacet},
 * {@link DateRangeFacet}, {@link PivotFacet}, {@link QueryFacet} and {@link StatsFacet}.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Alfonso Noriega Meneses
 * @since 27.06.16.
 */
public abstract class Facet {

    private Logger log = LoggerFactory.getLogger(getClass());

    protected Scope scope = Scope.Facet;
    protected String name;
    protected String[] tagedPivots = new String[0];
    protected String facetName;

    public String getType() {
        return this.getClass().getSimpleName();
    }

    /**
     * Deep copy of the {@link Facet}.
     * @return A deep copy of the current {@link Facet} instance.
     */
    @Override
    public abstract Facet clone();

    /**
     * Returns the name of the facet. Use the new method {@link Facet#facetName}
     * @return String custom name of the specific facet.
     */
    @Deprecated
    public String getName() {
        return name;
    }

    public String[] getTagedPivots() {
        return tagedPivots;
    }

    /**
     * Returns the field values {@link Scope} which this facet will use.
     * @return {@link Scope} scope configured for this facet.
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Sets the field values {@link Scope} which this facet will use.
     * @return Returns the instance of this Facet with the new {@link Scope}.
     */
    public Facet setScope(Scope scope) {
        this.scope = scope;
        return this;
    }

    /**
     * Returns the name of the facet.
     * @return String custom name of the specific facet.
     */
    public String getFacetName() {
        return facetName;
    }


    /**
     * This class allows to perform the basic term facet query on one field.
     * @param <T> Spectated type of content from the field to be faceted.
     */
    public static class TermFacet<T> extends Facet {

        private FieldDescriptor<T> fieldDescriptor;
        private final String fieldName;

        public TermFacet(FieldDescriptor<T> fieldDescriptor) {
            this.fieldName = fieldDescriptor.getName();
            // Backwards compatibility
            this.name = this.fieldName;
            this.facetName = this.fieldName;
            this.fieldDescriptor = fieldDescriptor;
        }

        public TermFacet(String name) {
            this.fieldName =name;
            // Backwards compatibility
            this.name = name;
            this.facetName = name;
        }

        /**
         * Get the {@link FieldDescriptor} used to do the facet query.
         * @return {@link FieldDescriptor} describing the field in which the facet query will be perform.
         */
        public FieldDescriptor<T> getFieldDescriptor() {
            return fieldDescriptor;
        }

        public String getFieldName() {
            return this.fieldName;
        }

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                        "\"type\":\"TermFacet\"," +
                        "\"field\":\"%s\"" +
                    "}";
            return String.format(serializeFacet, this.facetName, this.facetName);
        }

        @Override
        public Facet clone() {
            final TermFacet<T> copy = new TermFacet<>(this.fieldName);
            copy.setScope(this.scope);
            copy.fieldDescriptor = this.fieldDescriptor;
            return copy;
        }
    }

    /**
     * This class allows to perform a basic faceting based on document type.
     */
    public static class TypeFacet extends Facet {

        public TypeFacet() {
            // Backwards compatibility
            this.name = "DocType";
            this.facetName = "DocType";
        }

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                    "\"type\":\"TypeFacet\""+
                    "}";
            return String.format(serializeFacet,this.facetName);
        }

        @Override
        public Facet clone() {
            final TypeFacet copy = new TypeFacet();
            copy.setScope(this.scope);

            return copy;
        }
    }

    /**
     * This class allows to perform a subdocument faceting based on document type.
     */
    public static class SubdocumentFacet extends Facet {

        public SubdocumentFacet(DocumentFactory factory) {
            this.facetName = factory.getType();
            // Backwards compatibility
            this.name = factory.getType();}

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                    "\"type\":\"SubDocumentFacet\""+
                    "}";
            return String.format(serializeFacet,this.facetName);
        }

        @Override
        public Facet clone() {
            final SubdocumentFacet copy = new SubdocumentFacet(new DocumentFactoryBuilder(this.facetName).build());
            copy.setScope(this.scope);
            return copy;
        }
    }



    /**
     * This class allows to perform ranged facet query on a numeric field. A facet ranged query returns all the
     * documents which have the specified field with a value included in a range defined by
     * {@link NumericRangeFacet#start} and {@link NumericRangeFacet#end}. The result documents are
     * faceted in the range steps of the size described by the property {@link NumericRangeFacet#gap}.
     * @param <T> Spectated type of content from the field to be faceted. T should extend {@link Number}.
     */
    public static class NumericRangeFacet<T extends Number> extends Facet {

        private FieldDescriptor fieldDescriptor;
        private T start,end,gap;

        /**
         * Constructor of the {@link NumericRangeFacet} providing all the needed configuration.
         * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
         * @param fieldDescriptor {@link FieldDescriptor} to do the range query on. T must extend {@link Number}.
         * @param start T Starting value of the defined range. T must extend {@link Number}.
         * @param end T Ending value of the defined range. T must extend {@link Number}.
         * @param gap T Size of the steps in which the range is divided to facet the results. T must extend {@link Number}.
         * @param pivotNames Name of the pivots using this facet.
         */
        public NumericRangeFacet(String name, FieldDescriptor<T> fieldDescriptor, T start, T end, T gap, String ... pivotNames) {
            this.facetName = name;
            this.fieldDescriptor = fieldDescriptor;
            // Backwards compatibility
            this.name = name;
            this.start = start;
            this.end = end;
            this.gap = gap;
            this.tagedPivots = pivotNames;
        }

        public NumericRangeFacet(String name, ComplexFieldDescriptor<?,T,?> fieldDescriptor, T start, T end, T gap, String ... pivotNames) {
            this.facetName = name;
            this.fieldDescriptor = fieldDescriptor;
            // Backwards compatibility
            this.name = name;
            this.start = start;
            this.end = end;
            this.gap = gap;
            this.tagedPivots = pivotNames;
        }

        /**
         * Get the {@link FieldDescriptor} used to do the facet query.
         * @return {@link FieldDescriptor} describing the field in which the facet query will be perform.
         */
        public FieldDescriptor getFieldDescriptor() {
            return fieldDescriptor;
        }

        public String getFieldName() {
            return fieldDescriptor.getName();
        }

        /**
         * Get the starting value of the range.
         * @return T Numeric value. T must extend {@link Number}.
         */
        public T getStart() {
            return start;
        }

        /**
         * Get the ending value of the range.
         * @return T Numeric value. T must extend {@link Number}.
         */
        public T getEnd() {
            return end;
        }

        /**
         * Get the step size value of the range.
         * @return T Numeric value. T must extend {@link Number}.
         */
        public T getGap() {
            return gap;
        }

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                    "\"type\":\"%s\","+
                    "\"field\":\"%s\","+
                    "\"start\":%s,"+
                    "\"end\":%s,"+
                    "\"gap\":%s"+
                    "}";
            return String.format(serializeFacet,this.facetName,this.getClass().getSimpleName(),this.fieldDescriptor.getName(),this.start,this.end,this.gap);
        }

        @Override
        public Facet clone() {
            final NumericRangeFacet copy = new NumericRangeFacet(this.facetName,this.fieldDescriptor,this.start, this.end, this.gap, this.tagedPivots);
            copy.setScope(this.scope);
            return copy;
        }
    }

    /**
     * This abstract class provides the foundations to be implemented by date type specific classes in order to perform
     * ranged facet query on a date field. A facet ranged query returns all the documents which have the specified
     * field with a value included in a range defined by {@link DateRangeFacet#start} and {@link DateRangeFacet#end}.
     * The result documents are faceted in the range steps of the size described by the property
     * {@link DateRangeFacet#gap}.
     * @param <T> Spectated type of content from the field to be faceted.
     */
    public abstract static class DateRangeFacet<T> extends Facet {

        protected FieldDescriptor fieldDescriptor;
        protected T start,end;
        protected Long gap;
        protected final TimeUnit gapUnits = TimeUnit.MILLISECONDS;

        /**
         * Get the {@link FieldDescriptor} used to do the facet query.
         * @return {@link FieldDescriptor} describing the field in which the facet query will be perform.
         */
        public FieldDescriptor getFieldDescriptor() {
            return fieldDescriptor;
        }

        public String getFieldName() {
            return fieldDescriptor.getName();
        }

        /**
         * Get the starting value of the range.
         * @return T value.
         */
        public T getStart() {
            return start;
        }

        /**
         * Get the ending value of the range.
         * @return T value.
         */
        public T getEnd() {
            return end;
        }

        /**
         * Get the step size value of the range in milliseconds.
         * @return Long value in millisecond .
         */
        public Long getGap() {
            return gap;
        }

        /**
         * Get the units in wihch the step size is messured.
         * @return {@link TimeUnit} units of the step size in the range.
         */
        public TimeUnit getGapUnits() {
            return gapUnits;
        }

        /**
         * Get the step size value of the range as {@link Duration} object.
         * @return {@link Duration} of the step in the range.
         */
        public Duration getGapDuration() {
            return Duration.ofMillis(gap);
        }

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                    "\"type\":\"%s\","+
                    "\"field\":\"%s\","+
                    "\"start\":\"%s\","+
                    "\"end\":\"%s\","+
                    "\"gap\":%s,"+
                    "\"units\":\"%s\""+
                    "}";
            return String.format(serializeFacet,this.facetName,this.getClass().getSimpleName(),this.fieldDescriptor.getName(),this.start,this.end,this.gap,this.gapUnits.toString());
        }

        @Override
        public abstract Facet clone();

        /**
         * Class which allows to perform ranged facet query on a {@link ZonedDateTime} typed date field. A facet ranged query
         * returns all the documents which have the specified field with a value included in a range defined by
         * {@link DateRangeFacet#start} and {@link DateRangeFacet#end}. The result documents are faceted in the range
         * steps of the size described by the property {@link DateRangeFacet#gap}.
         * @param <T> Spectated type of content from the field to be faceted.
         */
        public static class ZoneDateRangeFacet<T extends ZonedDateTime> extends DateRangeFacet {

            /**
             * Constructor of the {@link ZoneDateRangeFacet} providing all the needed configuration.
             * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
             * @param fieldDescriptor {@link FieldDescriptor} to do the range query on. T must extend {@link ZonedDateTime}.
             * @param start T Starting value of the defined range. T must extend {@link Number}.
             * @param end T Ending value of the defined range. T must extend {@link Number}.
             * @param gap Duration Size of the steps in which the range is divided to facet the results.
             * @param pivotNames Name of the pivots using this facet.
             */
            public ZoneDateRangeFacet(String name, FieldDescriptor fieldDescriptor, T start, T end, Duration gap, String... pivotNames) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.start = start;
                this.end = end;
                this.gap = gap.toMillis();
                this.tagedPivots = pivotNames;
            }

            public long getTimeStampStart() {
                return ((ZonedDateTime)this.getStart()).toInstant().getEpochSecond();
            }

            public long getTimeStampEnd() {
                return ((ZonedDateTime)getEnd()).toInstant().getEpochSecond();
            }


            @Override
            public Facet clone() {
                return new ZoneDateRangeFacet<ZonedDateTime>(this.facetName,this.fieldDescriptor, (ZonedDateTime)this.start, (ZonedDateTime)this.end, Duration.ofMillis(this.gap), this.tagedPivots);
            }
        }

        /**
         * Class which allows to perform ranged facet query on a {@link Date} typed date field. A facet ranged query
         * returns all the documents which have the specified field with a value included in a range defined by
         * {@link DateRangeFacet#start} and {@link DateRangeFacet#end}. The result documents are faceted in the range
         * steps of the size described by the property {@link DateRangeFacet#gap}.
         * @param <T> Spectated type of content from the field to be faceted.
         */
        public static class UtilDateRangeFacet<T extends Date> extends DateRangeFacet {

            /**
             * Constructor of the {@link UtilDateRangeFacet} providing all the needed configuration.
             * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
             * @param fieldDescriptor {@link FieldDescriptor} to do the range query on. T must extend {@link Date}.
             * @param start T Starting value of the defined range. T must extend {@link Date}.
             * @param end T Ending value of the defined range. T must extend {@link Date}.
             * @param gap Long Size of the steps in which the range is divided to facet the results.
             * @param timeUnit TimeUnit Units in which the gap is given.
             * @param pivotNames Name of the pivots using this facet.
             */
            public UtilDateRangeFacet(String name, FieldDescriptor fieldDescriptor, T start, T end, Long gap,TimeUnit timeUnit, String ... pivotNames) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.start = start;
                this.end = end;
                this.gap = timeUnit.toMillis(gap);
                this.tagedPivots = pivotNames;
            }

            public long getTimeStampStart() {
                return ((Date)this.getStart()).toInstant().getEpochSecond();
            }

            public long getTimeStampEnd() {
                return ((Date)getEnd()).toInstant().getEpochSecond();
            }

            @Override
            public Facet clone() {
                return new UtilDateRangeFacet<Date>(this.facetName,this.fieldDescriptor, (Date)this.start, (Date)this.end, this.gap, this.gapUnits, this.tagedPivots);
            }
        }

        /**
         * Class which allows to perform ranged facet query on a {@link DateMathExpression} typed date field. A facet ranged query
         * returns all the documents which have the specified field with a value included in a range defined by
         * {@link DateRangeFacet#start} and {@link DateRangeFacet#end}. The result documents are faceted in the range
         * steps of the size described by the property {@link DateRangeFacet#gap}.
         * @param <T> Spectated type of content from the field to be faceted.
         */
        public static class DateMathRangeFacet<T extends DateMathExpression> extends DateRangeFacet {

            /**
             * Constructor of the {@link DateMathRangeFacet} providing all the needed configuration.
             * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
             * @param fieldDescriptor {@link FieldDescriptor} to do the range query on. T must extend {@link Date}.
             * @param start T Starting value of the defined range. T must extend {@link DateMathExpression}.
             * @param end T Ending value of the defined range. T must extend {@link DateMathExpression}.
             * @param gap Long Size of the steps in which the range is divided to facet the results.
             * @param timeUnit TimeUnit Units in which the gap is given.
             * @param pivotNames Name of the pivots using this facet.
             */
            public DateMathRangeFacet(String name, FieldDescriptor<? extends Date> fieldDescriptor, T start, T end, Long gap,TimeUnit timeUnit, String ... pivotNames) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.start = start;
                this.end = end;
                this.gap = timeUnit.toMillis(gap);
                this.tagedPivots = pivotNames;
            }

            public DateMathRangeFacet(String name, ComplexFieldDescriptor<?,? extends Date,?> fieldDescriptor, T start, T end, Long gap,TimeUnit timeUnit, String ... pivotNames) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.start = start;
                this.end = end;
                this.gap = timeUnit.toMillis(gap);
                this.tagedPivots = pivotNames;
            }

            /**
             * Constructor of the {@link DateMathRangeFacet} providing all the needed configuration.
             * @param name String with a custom name for the instance of the facet. It should be alphanumeric.
             * @param fieldDescriptor {@link FieldDescriptor} to do the range query on. T must extend {@link ZonedDateTime}.
             * @param start T Starting value of the defined range. T must extend {@link Number}.
             * @param end T Ending value of the defined range. T must extend {@link Number}.
             * @param gap Duration Size of the steps in which the range is divided to facet the results.
             * @param pivotNames Name of the pivots using this facet.
             */
            public DateMathRangeFacet(String name, FieldDescriptor<? extends ZonedDateTime> fieldDescriptor, T start, T end, Duration gap, String... pivotNames) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.start = start;
                this.end = end;
                this.gap = gap.toMillis();
                this.tagedPivots = pivotNames;
            }

            public DateMathRangeFacet(String name, ComplexFieldDescriptor<?,? extends ZonedDateTime,?> fieldDescriptor, T start, T end, Duration gap, String... pivotNames) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.start = start;
                this.end = end;
                this.gap = gap.toMillis();
                this.tagedPivots = pivotNames;
            }

            private DateMathRangeFacet() {}

            public long getTimeStampStart() {
                return ((DateMathExpression)this.getStart()).getTimeStamp();
            }

            public long getTimeStampEnd() {
                return ((DateMathExpression)this.getEnd()).getTimeStamp();
            }

            @Override
            public Facet clone() {
                final DateMathRangeFacet<T> copy = new DateMathRangeFacet<>();
                // Backwards compatibility
                copy.name = this.name;
                copy.facetName = this.facetName;
                copy.fieldDescriptor = this.fieldDescriptor;
                copy.start = this.start;
                copy.end = this.end;
                copy.gap = this.gap;
                copy.tagedPivots = this.tagedPivots;
                return copy;
            }
        }

    }

    public static abstract class IntervalFacet<T> extends Facet {

        protected FieldDescriptor<T> fieldDescriptor;
        protected Set<Interval<T>> intervals;

        /**
         * Get the {@link FieldDescriptor} used to do the facet query.
         * @return {@link FieldDescriptor} describing the field in which the facet query will be perform.
         */
        public FieldDescriptor<T> getFieldDescriptor() {
            return fieldDescriptor;
        }

        public String getFieldName() {
            return fieldDescriptor.getName();
        }

        public Set<Interval<T>> getIntervals() {
            return intervals;
        }

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                    "\"type\":\"%s\","+
                    "\"field\":\"%s\","+
                    "\"interval\":{%s}"+
                    "}";
            return String.format(serializeFacet,
                    this.facetName,
                    this.getClass().getSimpleName(),
                    this.fieldDescriptor.getName(),
                    this.intervals.stream().map(i -> i.toString()).collect(Collectors.joining(","))
            );
        }
    }

    public static class NumericIntervalFacet<T extends Number> extends IntervalFacet<T> {

        public NumericIntervalFacet(String name, FieldDescriptor<T> fieldDescriptor, Interval.NumericInterval<T>... intervals) {
            this.facetName = name;
            this.fieldDescriptor = fieldDescriptor;
            // Backwards compatibility
            this.name = name;
            this.intervals = Sets.newHashSet(intervals);
        }

        public <S extends Serializable> NumericIntervalFacet(String name, ComplexFieldDescriptor<S,T,?> fieldDescriptor, Interval.NumericInterval<T>... intervals) {
            this.facetName = name;
            this.fieldDescriptor = (FieldDescriptor<T>) fieldDescriptor;
            // Backwards compatibility
            this.name = name;
            this.intervals = Sets.newHashSet(intervals);
        }

        /**
         * Get the {@link FieldDescriptor} used to do the facet query.
         * @return {@link FieldDescriptor} describing the field in which the facet query will be perform.
         */
        public FieldDescriptor getFieldDescriptor() {
            return fieldDescriptor;
        }

        public String getFieldName() {
            return fieldDescriptor.getName();
        }

        private NumericIntervalFacet() {}

        @Override
        public Facet clone() {
            final NumericIntervalFacet copy = new NumericIntervalFacet();
            copy.fieldDescriptor = this.fieldDescriptor;
            copy.facetName = this.facetName;
            // Backwards compatibility
            copy.name = this.name;
            copy.scope = this.scope;
            copy.intervals = this.intervals; //FIXME clone intervals
            return copy;
        }
    }

    public abstract static class DateIntervalFacet<T> extends IntervalFacet<T> {

        //TODO set interval

        public static class ZoneDateTimeIntervalFacet<T extends ZonedDateTime> extends DateIntervalFacet {

            public ZoneDateTimeIntervalFacet(String name, FieldDescriptor<T> fieldDescriptor, Interval.ZonedDateTimeInterval<T>... intervals) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.intervals = Sets.newHashSet(intervals);
            }

            public ZoneDateTimeIntervalFacet(String name, ComplexFieldDescriptor<?,T,?> fieldDescriptor, Interval.ZonedDateTimeInterval<T>... intervals) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.intervals = Sets.newHashSet(intervals);
            }

            private ZoneDateTimeIntervalFacet(){}

            @Override
            public Facet clone() {
                final ZoneDateTimeIntervalFacet copy = new ZoneDateTimeIntervalFacet();
                copy.fieldDescriptor = this.fieldDescriptor;
                // Backwards compatibility
                copy.name = this.name;
                copy.facetName = this.facetName;
                copy.scope = this.scope;
                copy.intervals = this.intervals; //FIXME clone intervals
                return copy;
            }

        }

        public static class UtilDateIntervalFacet<T extends Date> extends DateIntervalFacet<T> {

            public UtilDateIntervalFacet(String name, FieldDescriptor<T> fieldDescriptor, Interval.UtilDateInterval<T>... intervals) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.intervals = Sets.newHashSet(intervals);
            }

            public UtilDateIntervalFacet(String name, ComplexFieldDescriptor<?,T,?> fieldDescriptor, Interval.UtilDateInterval<T>... intervals) {
                this.facetName = name;
                this.fieldDescriptor = (FieldDescriptor<T>)  fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.intervals = Sets.newHashSet(intervals);
            }

            private UtilDateIntervalFacet(){}

            @Override
            public Facet clone() {
                final UtilDateIntervalFacet copy = new UtilDateIntervalFacet();
                copy.fieldDescriptor = this.fieldDescriptor;
                // Backwards compatibility
                copy.name = this.name;
                copy.facetName = this.facetName;
                copy.scope = this.scope;
                copy.intervals = this.intervals; //FIXME clone intervals
                return copy;
            }
        }

        public static class UtilDateMathIntervalFacet<T extends Date> extends DateIntervalFacet<T> {
            public UtilDateMathIntervalFacet(String name, FieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.intervals = Sets.newHashSet(intervals);
            }

            public UtilDateMathIntervalFacet(String name, ComplexFieldDescriptor<?,T,?> fieldDescriptor, Interval.DateMathInterval... intervals) {
                this.facetName = name;
                this.fieldDescriptor = (FieldDescriptor<T>) fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.intervals = Sets.newHashSet(intervals);
            }

            private UtilDateMathIntervalFacet(){}

            @Override
            public Facet clone() {
                final UtilDateMathIntervalFacet copy = new UtilDateMathIntervalFacet();
                copy.fieldDescriptor = this.fieldDescriptor;
                // Backwards compatibility
                copy.name = this.name;
                copy.facetName = this.facetName;
                copy.scope = this.scope;
                copy.intervals = this.intervals; //FIXME clone intervals
                return copy;
            }

        }

        public static class ZoneDateTimeDateMathIntervalFacet<T extends ZonedDateTime> extends DateIntervalFacet<T> {
            public ZoneDateTimeDateMathIntervalFacet(String name, FieldDescriptor<T> fieldDescriptor, Interval.DateMathInterval... intervals) {
                this.facetName = name;
                this.fieldDescriptor = fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.intervals = Sets.newHashSet(intervals);
            }

            public ZoneDateTimeDateMathIntervalFacet(String name, ComplexFieldDescriptor<?,T,?> fieldDescriptor, Interval.DateMathInterval... intervals) {
                this.facetName = name;
                this.fieldDescriptor =(FieldDescriptor<T>) fieldDescriptor;
                // Backwards compatibility
                this.name = name;
                this.intervals = Sets.newHashSet(intervals);
            }

            private ZoneDateTimeDateMathIntervalFacet(){}

            @Override
            public Facet clone() {
                final ZoneDateTimeDateMathIntervalFacet copy = new ZoneDateTimeDateMathIntervalFacet();
                copy.fieldDescriptor = this.fieldDescriptor;
                // Backwards compatibility
                copy.name = this.name;
                copy.facetName = this.facetName;
                copy.scope = this.scope;
                copy.intervals = this.intervals; //FIXME clone intervals
                return copy;
            }
        }
    }

    /**
     * Allows to configure basic pivot facet queries on a group of fields. A basic pivot facet query returns a decision
     * tree of the specified fields.
     */
    public static class PivotFacet extends Facet {

        private List<FieldDescriptor<?>> fieldDescriptors;

        /**
         * Creates a new instance of the {@link PivotFacet} class.
         * @param name String with a custom name for the new instance.
         * @param descriptors A group of {@link FieldDescriptor} objects on which perform the pivot query
         */
        public PivotFacet(String name, FieldDescriptor<?>... descriptors ) {
            this.facetName = name;
            // Backwards compatibility
            this.name = name;
            this.fieldDescriptors = Lists.newArrayList(descriptors);
        }

        /**
         * Get the {@link FieldDescriptor} used to do the facet query.
         * @return {@link FieldDescriptor} describing the field in which the pivot facet query will be perform.
         */
        public List<FieldDescriptor<?>> getFieldDescriptors() {
            return fieldDescriptors;
        }

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                    "\"type\":\"%s\","+
                    "\"field\":[%s]"+
                    "}";
            return String.format(serializeFacet,
                    this.facetName,
                    this.getClass().getSimpleName(),
                    this.fieldDescriptors.stream().map(d -> "\"" + d.getName() + "\"").collect(Collectors.joining(","))
            );
        }

        @Override
        public Facet clone() {
            return new PivotFacet(this.facetName, this.fieldDescriptors.toArray(new FieldDescriptor<?>[this.fieldDescriptors.size()]));
        }
    }

    /**
     * Allows to configure a query facet. A query facet restricts the facet results based on a group of filters.
     */
    public static class QueryFacet extends Facet {

        private Filter filter;

        /**
         * Creates a new instance of {@link QueryFacet}.
         * @param name String with a custom name for the new instance.
         * @param filter {@link Filter} to apply to the facet results.
         * @param pivotNames Name of the pivots using this facet.
         */
        public QueryFacet(String name,  Filter filter, String ... pivotNames) {
            this.facetName = name;
            // Backwards compatibility
            this.name = name;
            this.filter = filter;
            this.tagedPivots = pivotNames;
        }

        /**
         * Get the {@link Filter} used to do the query facet.
         * @return {@link Filter} describing the filters to be applied on the facet result.
         */
        public Filter getFilter() {
            return filter;
        }

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                    "\"type\":\"%s\","+
                    "\"query\":\"%s\""+
                    "}";
            return String.format(serializeFacet,
                    this.facetName,
                    this.getClass().getSimpleName(),
                    this.filter
            );
        }

        @Override
        public Facet clone() {
            return new QueryFacet(this.facetName, this.filter.clone(),this.tagedPivots);
        }
    }

    /**
     * Allows to configure a basic stats facet query. A stats facet query returns statistics calculated based on documents
     * for an specific document field any type T. This class covers the statistics which can be perform on any kind of
     * content.
     */
    public static class StatsFacet<T> extends Facet {

        protected  FieldDescriptor<T> field;
        protected Boolean min = false;
        protected Boolean max = false;
        protected Boolean sum = false;
        protected Boolean count = false;
        protected Boolean missing = false;
        protected Boolean sumOfSquares = false;
        protected Boolean mean = false;
        protected Boolean stddev = false;
        protected Double[] percentiles = new Double[0];
        protected Boolean distinctValues = false;
        protected Boolean countDistinct = false;
        protected Boolean cardinality = false;

        /**
         * Create a new instance of {@link StatsFacet}.
         * @param name String with a custom name for the new instance.
         * @param field {@link FieldDescriptor} to calculate the statistics on.
         * @param pivotNames Name of the pivots using this facet.
         */
        public StatsFacet(String name,FieldDescriptor field, String ... pivotNames) {
            this.facetName = name;
            this.field = field;
            // Backwards compatibility
            this.name = name;
            this.tagedPivots = pivotNames;
        }

        /**
         * Get the {@link FieldDescriptor} used to do the facet query.
         * @return {@link FieldDescriptor} describing the field in which the pivot facet query will be perform.
         */
        public FieldDescriptor<T> getField() {
            return field;
        }


        /**
         * Activate the min statistics, which will give as result the minimum value for the specified field
         * @return {@link StatsFacet} with the min statistics enabled.
         */
        public StatsFacet<T> min() {
            this.min=true;
            return this;
        }

        /**
         * Activate the max statistics, which will give as result the maximum value for the specified field
         * @return {@link StatsFacet} with the max statistics enabled.
         */
        public StatsFacet<T> max() {
            this.max=true;
            return this;
        }

        /**
         * Activate the count statistics, which will give as result the count of documents with the specified field
         * @return {@link StatsFacet} with the count statistics enabled.
         */
        public StatsFacet<T> count() {
            this.count=true;
            return this;
        }

        /**
         * Activate the missing statistics, which will give as result the number of elements without the specified field
         * @return {@link StatsFacet} with the missing statistics enabled.
         */
        public StatsFacet<T> missing() {
            this.missing=true;
            return this;
        }

        /**
         * Activate the distinctValues statistics, which will give as result the set of distinct values for the specified field
         * @return {@link StatsFacet} with the min statistics enabled.
         */
        public StatsFacet<T> distinctValues() {
            this.distinctValues=true;
            return this;
        }

        /**
         * Activate the countDistinct statistics, which will give as result the count of distinct values for the specified field
         * @return {@link StatsFacet} with the min statistics enabled.
         */
        public StatsFacet<T> countDistinct() {
            this.countDistinct=true;
            return this;
        }

        //TODO: Solr doc specifies cardinality stats works for every kind of field but when trying with text we get:
        //  java.lang.NoSuchMethodError: com.google.common.hash.HashFunction.hashString(Ljava/lang/CharSequence;)Lcom/google/common/hash/HashCode;
        //      at org.apache.solr.handler.component.StringStatsValues.hash(StatsValuesFactory.java:826)
        //At the moment this method will be moved to numeric and data specific statsFacets
        /*public StatsFacet<T> cardinality() {
            this.cardinality=true;
            return this;
        }*/

        public Boolean getMin() {
            return min;
        }

        public Boolean getMax() {
            return max;
        }

        public Boolean getSum() {
            return sum;
        }

        public Boolean getCount() {
            return count;
        }

        public Boolean getMissing() {
            return missing;
        }

        public Boolean getSumOfSquares() {
            return sumOfSquares;
        }

        public Boolean getMean() {
            return mean;
        }

        public Boolean getStddev() {
            return stddev;
        }

        public Double[] getPercentiles() {
            return percentiles;
        }

        public Boolean getDistinctValues() {
            return distinctValues;
        }

        public Boolean getCountDistinct() {
            return countDistinct;
        }

        public Boolean getCardinality() {
            return cardinality;
        }

        @Override
        public String toString(){
            final String serializeFacet = "" +
                    "\"%s\":{" +
                    "\"type\":\"%s\","+
                    "\"field\":\"%s\""+
                    "}";
            return String.format(serializeFacet,
                    this.facetName,
                    this.getClass().getSimpleName(),
                    this.field.getName()
            );
        }

        @Override
        public Facet clone() {
            final StatsFacet copy = new StatsFacet(this.facetName, this.field, this.tagedPivots);
            copy.scope = this.scope;
            copy.min = this.min;
            copy.max = this.max;
            copy.sum = this.sum;
            copy.count = this.count;
            copy.missing = this.missing;
            copy.sumOfSquares = this.sumOfSquares;
            copy.mean = this.mean;
            copy.stddev = this.stddev;
            copy.percentiles = this.percentiles;
            copy.distinctValues = this.distinctValues;
            copy.countDistinct = this.countDistinct;
            copy.cardinality = this.cardinality;
            return copy;
        }

        @Override
        public StatsFacet<T> setScope(Scope scope) {
            this.scope = scope;
            return this;

        }

        static public StatsFacet createFacet(String name, FieldDescriptor field, String ...pivotNames) {

            if (MultiValueFieldDescriptor.NumericFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                return new StatsNumericFacet(name,(MultiValueFieldDescriptor.NumericFieldDescriptor)field,pivotNames);
            } else
            if (SingleValueFieldDescriptor.NumericFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                return new StatsNumericFacet(name,(SingleValueFieldDescriptor.NumericFieldDescriptor)field,pivotNames);
            } else
            if (MultiValueFieldDescriptor.DateFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                return new StatsDateFacet(name,(MultiValueFieldDescriptor.DateFieldDescriptor)field,pivotNames);
            } else
            if (SingleValueFieldDescriptor.DateFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                return new StatsDateFacet(name,(MultiValueFieldDescriptor.DateFieldDescriptor)field,pivotNames);
            } else
            if (MultiValueFieldDescriptor.UtilDateFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                return new StatsUtilDateFacet(name,(MultiValueFieldDescriptor.UtilDateFieldDescriptor)field,pivotNames);
            } else
            if (SingleValueFieldDescriptor.UtilDateFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                return new StatsUtilDateFacet(name,(MultiValueFieldDescriptor.UtilDateFieldDescriptor)field,pivotNames);
            } else {
                Class facetType = ((ComplexFieldDescriptor) field).getFacetType();
                if (Number.class.isAssignableFrom(facetType)) {
                    return new StatsNumericFacet(name, (ComplexFieldDescriptor)field,pivotNames);
                } else
                if (Date.class.isAssignableFrom(facetType)) {
                    return new StatsUtilDateFacet(name, (ComplexFieldDescriptor)field,pivotNames);
                } else
                if (ZonedDateTime.class.isAssignableFrom(facetType)) {
                    return new StatsDateFacet(name, (ComplexFieldDescriptor)field,pivotNames);
                } else
                if (CharSequence.class.isAssignableFrom(facetType)) {
                    return new StatsFacet(name, field,pivotNames);
                }

            }
            throw new RuntimeException("Unable to create Stats facet");
        }
    }

    /**
     * Allows to configure a numeric stats facet query. A stats facet query returns statistics calculated based on
     * documents for an specific document field any type T. This class covers the specific statistics which can be
     * perform on a numeric field.
     */
    public static class StatsNumericFacet<T extends Number> extends StatsFacet<T> {

        /**
         * Create a new instance of {@link StatsNumericFacet} for a multivalued field.
         * @param name String with a custom name for the new instance.
         * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.NumericFieldDescriptor} to calculate the statistics on.
         * @param pivotNames Name of the pivots using this facet.
         */
        public StatsNumericFacet(String name, MultiValueFieldDescriptor.NumericFieldDescriptor field, String ...pivotNames) {
            super(name,field,pivotNames);
        }

        /**
         * Create a new instance of {@link StatsNumericFacet} for a single valued field.
         * @param name String with a custom name for the new instance.
         * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.NumericFieldDescriptor} to calculate the statistics on.
         * @param pivotNames Name of the pivots using this facet.
         */
        public StatsNumericFacet(String name, SingleValueFieldDescriptor.NumericFieldDescriptor field, String ...pivotNames) {
            super(name,field,pivotNames);
        }

        public StatsNumericFacet(String name, ComplexFieldDescriptor<?,T,?> field, String ...pivotNames) {
            super(name,(FieldDescriptor<T>)field,pivotNames);
        }

        @Override
        public StatsNumericFacet<T> min() {
            this.min=true;
            return this;
        }
        @Override
        public StatsNumericFacet<T> max() {
            this.max=true;
            return this;
        }
        @Override
        public StatsNumericFacet<T> count() {
            this.count=true;
            return this;
        }
        @Override
        public StatsNumericFacet<T> missing() {
            this.missing=true;
            return this;
        }
        @Override
        public StatsNumericFacet<T> distinctValues() {
            this.distinctValues=true;
            return this;
        }
        @Override
        public StatsNumericFacet<T> countDistinct() {
            this.countDistinct=true;
            return this;
        }

        /**
         * Activate the cardinality statistics, which will give as result an statistical aproximation to the number of
         * distinct values int he field.
         * @return {@link StatsFacet} with the cardinality statistics enabled.
         */
        public StatsNumericFacet<T> cardinality() {
            this.cardinality=true;
            return this;
        }

        /**
         * Activate the sum statistics, which will give as result the addition of all values for the specified field
         * @return {@link StatsFacet} with the sum statistics enabled.
         */
        public StatsNumericFacet<T> sum() {
            this.sum=true;
            return this;
        }

        /**
         * Activate the sumOfSquares statistics, which will give as result the addition of all values squared for the
         * specified field
         * @return {@link StatsFacet} with the sumOfSquares statistics enabled.
         */
        public StatsNumericFacet<T> sumOfSquares() {
            this.sumOfSquares=true;
            return this;
        }

        /**
         * Activate the mean statistics, which will give as result the average value for the specified field
         * @return {@link StatsFacet} with the mean statistics enabled.
         */
        public StatsNumericFacet<T> mean() {
            this.mean=true;
            return this;
        }

        /**
         * Activate the stddev statistics, which will give as result the standard deviation for the specified field
         * @return {@link StatsFacet} with the stddev statistics enabled.
         */
        public StatsNumericFacet<T> stddev() {
            this.stddev=true;
            return this;
        }

        /**
         * Activate the percentiles statistics, which will give as result a set of percentile values for the selected
         * field based on the points specified as parameters.
         * @param percentiles {@link Double} values of the points to calculate the percentiles.
         * @return {@link StatsFacet} with the percentiles statistics enabled.
         */
        public StatsNumericFacet<T> percentiles(Double...percentiles) {
            this.percentiles=percentiles;
            return this;
        }

        @Override
        public StatsNumericFacet<T> setScope(Scope scope) {
            this.scope = scope;
            return this;

        }

    }

    /**
     * Allows to configure a date stats facet query. A stats facet query returns statistics calculated based on
     * documents for an specific document field any type T extending {@link ZonedDateTime}. This class covers the specific statistics which can be
     * perform on a date field.
     */
    public static class StatsDateFacet<T extends ZonedDateTime> extends StatsFacet<T> {

        /**
         * Create a new instance of {@link StatsDateFacet} for a multivalued field.
         * @param name String with a custom name for the new instance.
         * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.DateFieldDescriptor} to calculate the statistics on.
         * @param pivotNames Name of the pivots using this facet.
         */
        public StatsDateFacet(String name, MultiValueFieldDescriptor.DateFieldDescriptor field, String ...pivotNames) {
            super(name,field,pivotNames);
        }

        /**
         * Create a new instance of {@link StatsDateFacet} for a multivalued field.
         * @param name String with a custom name for the new instance.
         * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.DateFieldDescriptor} to calculate the statistics on.
         * @param pivotNames Name of the pivots using this facet.
         */
        public StatsDateFacet(String name, SingleValueFieldDescriptor.DateFieldDescriptor field, String ...pivotNames) {
            super(name,field,pivotNames);
        }

        public StatsDateFacet(String name, ComplexFieldDescriptor<?,T,?> field, String ...pivotNames) {
            super(name,(FieldDescriptor<T>)field,pivotNames);
        }

        @Override
        public StatsDateFacet<T> min() {
            this.min=true;
            return this;
        }
        @Override
        public StatsDateFacet<T> max() {
            this.max=true;
            return this;
        }
        @Override
        public StatsDateFacet<T> count() {
            this.count=true;
            return this;
        }
        @Override
        public StatsDateFacet<T> missing() {
            this.missing=true;
            return this;
        }
        @Override
        public StatsDateFacet<T> distinctValues() {
            this.distinctValues=true;
            return this;
        }
        @Override
        public StatsDateFacet<T> countDistinct() {
            this.countDistinct=true;
            return this;
        }
        /**
         * Activate the cardinality statistics, which will give as result an statistical aproximation to the number of
         * distinct values int he field.
         * @return {@link StatsFacet} with the cardinality statistics enabled.
         */
        public StatsDateFacet<T> cardinality() {
            this.cardinality=true;
            return this;
        }
        /**
         * Activate the sum statistics, which will give as result the addition of all values for the specified field
         * @return {@link StatsFacet} with the sum statistics enabled.
         */
        public StatsDateFacet<T> sum() {
            this.sum=true;
            return this;
        }
        /**
         * Activate the sumOfSquares statistics, which will give as result the addition of all values squared for the
         * specified field
         * @return {@link StatsFacet} with the sumOfSquares statistics enabled.
         */
        public StatsDateFacet<T> sumOfSquares() {
            this.sumOfSquares=true;
            return this;
        }
        /**
         * Activate the mean statistics, which will give as result the average value for the specified field
         * @return {@link StatsFacet} with the mean statistics enabled.
         */
        public StatsDateFacet<T> mean() {
            this.mean=true;
            return this;
        }
        /**
         * Activate the stddev statistics, which will give as result the standard deviation for the specified field
         * @return {@link StatsFacet} with the stddev statistics enabled.
         */
        public StatsDateFacet<T> stddev() {
            this.stddev=true;
            return this;
        }

        @Override
        public StatsDateFacet<T> setScope(Scope scope) {
            this.scope = scope;
            return this;

        }
    }

    /**
     * Allows to configure a date stats facet query. A stats facet query returns statistics calculated based on
     * documents for an specific document field any type T extending {@link Date}. This class covers the specific statistics which can be
     * perform on a date field.
     */
    public static class StatsUtilDateFacet<T extends Date> extends StatsFacet<T> {

        /**
         * Create a new instance of {@link StatsUtilDateFacet} for a multivalued field.
         * @param name String with a custom name for the new instance.
         * @param field {@link com.rbmhtechnology.vind.model.MultiValueFieldDescriptor.UtilDateFieldDescriptor} to calculate the statistics on.
         * @param pivotNames Name of the pivots using this facet.
         */
        public StatsUtilDateFacet(String name, MultiValueFieldDescriptor.UtilDateFieldDescriptor field, String ...pivotNames) {
            super(name,field,pivotNames);
        }

        /**
         * Create a new instance of {@link StatsUtilDateFacet} for a multivalued field.
         * @param name String with a custom name for the new instance.
         * @param field {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.UtilDateFieldDescriptor} to calculate the statistics on.
         * @param pivotNames Name of the pivots using this facet.
         */
        public StatsUtilDateFacet(String name, SingleValueFieldDescriptor.UtilDateFieldDescriptor field, String ...pivotNames) {
            super(name,field,pivotNames);
        }

        public StatsUtilDateFacet(String name,ComplexFieldDescriptor<?,T,?> field, String ...pivotNames) {
            super(name,(FieldDescriptor<T>)field,pivotNames);
        }

        @Override
        public StatsUtilDateFacet<T> min() {
            this.min=true;
            return this;
        }
        @Override
        public StatsUtilDateFacet<T> max() {
            this.max=true;
            return this;
        }
        @Override
        public StatsUtilDateFacet<T> count() {
            this.count=true;
            return this;
        }
        @Override
        public StatsUtilDateFacet<T> missing() {
            this.missing=true;
            return this;
        }
        @Override
        public StatsUtilDateFacet<T> distinctValues() {
            this.distinctValues=true;
            return this;
        }
        @Override
        public StatsUtilDateFacet<T> countDistinct() {
            this.countDistinct=true;
            return this;
        }
        /**
         * Activate the cardinality statistics, which will give as result an statistical aproximation to the number of
         * distinct values int he field.
         * @return {@link StatsUtilDateFacet} with the cardinality statistics enabled.
         */
        public StatsUtilDateFacet<T> cardinality() {
            this.cardinality=true;
            return this;
        }
        /**
         * Activate the sum statistics, which will give as result the addition of all values for the specified field
         * @return {@link StatsUtilDateFacet} with the sum statistics enabled.
         */
        public StatsUtilDateFacet<T> sum() {
            this.sum=true;
            return this;
        }
        /**
         * Activate the sumOfSquares statistics, which will give as result the addition of all values squared for the
         * specified field
         * @return {@link StatsUtilDateFacet} with the sumOfSquares statistics enabled.
         */
        public StatsUtilDateFacet<T> sumOfSquares() {
            this.sumOfSquares=true;
            return this;
        }
        /**
         * Activate the mean statistics, which will give as result the average value for the specified field
         * @return {@link StatsUtilDateFacet} with the mean statistics enabled.
         */
        public StatsUtilDateFacet<T> mean() {
            this.mean=true;
            return this;
        }
        /**
         * Activate the stddev statistics, which will give as result the standard deviation for the specified field
         * @return {@link StatsUtilDateFacet} with the stddev statistics enabled.
         */
        public StatsUtilDateFacet<T> stddev() {
            this.stddev=true;
            return this;
        }

        @Override
        public StatsUtilDateFacet<T> setScope(Scope scope) {
            this.scope = scope;
            return this;

        }
    }

}
