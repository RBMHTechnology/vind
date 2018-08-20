package com.rbmhtechnology.vind.model;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.value.LatLng;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.function.Function;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;

/**
 * Created by fonso on 02.03.17.
 */
public class SingleValuedComplexField<T,F,S> extends ComplexFieldDescriptor {

    protected SingleValuedComplexField(String fieldName, Class type, Class facet, Class storedType) {
        super(fieldName, type, facet, storedType);
    }

    public Function<T, S> getSortFunction(){
        return (Function<T, S>)this.sortFunction;
    }
    /**
     * Class to instantiate {@link Document} complex fields containing text facet value.
     *
     * @param <T> The field original model type.
     * @param <F> The field facet type, must extend CharSequence.
     */
    public static class TextComplexField <T ,F extends CharSequence,S> extends SingleValuedComplexField<T,F,S> {

        /**
         * Creates an instance of a {@link SingleValuedComplexField.TextComplexField}.
         * @param fieldName String name of the created field.
         * @param type The field original model type.
         * @param facet The field facet type, must extend CharSequence.
         */
        protected TextComplexField(String fieldName, Class type, Class facet, Class suggestion) {
            super(fieldName, type, facet, suggestion);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param terms text to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(String... terms) {
            return Filter.terms(this, terms);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @param terms text to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(Scope scope, String... terms) {
            return Filter.terms(this, scope, terms);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is equals to a text.
         * @param text text to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter equals(String text, Scope scope) {
            return eq(this.getName(), text, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is equals to a text.
         * @param text text to check against.
         * @return A configured filter for the field.
         */
        public Filter equals(String text) {
            return eq(this.getName(), text);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value has as prefix a text.
         * @param prefix text to check against.
         * @return A configured filter for the field.
         */
        public Filter prefix(String prefix) {
            return Filter.prefix(this.getName(), prefix);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value has as prefix a text.
         * @param prefix text to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter prefix(String prefix, Scope scope) {
            return Filter.prefix(this.getName(), prefix, scope);
        }
        
    }

    /**
     * Class to instantiate {@link Document} complex fields containing Numeric facet value.
     *
     * @param <T> The field original model type.
     * @param <F> The field facet type, must extend Number.
     */
    public static class NumericComplexField <T,F extends Number,S> extends SingleValuedComplexField<T,F,S> {

        /**
         * Creates an instance of a {@link SingleValuedComplexField.NumericComplexField}.
         * @param fieldName String name of the created field.
         * @param type The field original model type.
         * @param facet The field facet type, must extend Number.
         * @param store The field store type.
         */
        protected NumericComplexField(String fieldName, Class type, Class facet, Class store) {
            super(fieldName, type, facet, store);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param terms number to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(Number... terms) {
            return Filter.terms(this, terms);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @param terms number to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(Scope scope, Number... terms) {
            return Filter.terms(this, scope, terms);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two numbers.
         * @param start Lower number on the filter range check.
         * @param end Greater number in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(Number start, Number end) {
            return Filter.between(this.getName(), start, end);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two numbers.
         * @param start Lower number on the filter range check.
         * @param end Greater number in the filter range check.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter between(Number start, Number end, Scope scope) {
            return Filter.between(this.getName(), start, end, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is greater than a numbers.
         * @param number Number to check against.
         * @return A configured filter for the field.
         */
        public Filter greaterThan(Number number) {
            return Filter.greaterThan(this.getName(), number);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is greater than a numbers.
         * @param number Number to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter greaterThan(Number number, Scope scope) {
            return Filter.greaterThan(this.getName(), number, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is lower than a numbers.
         * @param number Number to check against.
         * @return A configured filter for the field.
         */
        public Filter lesserThan(Number number) {
            return Filter.lesserThan(this.getName(), number);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is lower than a numbers.
         * @param number Number to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter lesserThan(Number number, Scope scope) {
            return Filter.lesserThan(this.getName(), number, scope);
        }
    }

    /**
     * Class to instantiate {@link Document} complex fields containing ZoneDateTime facet value.
     *
     * @param <T> The field original model type.
     * @param <F> The field facet type, must extend ZoneDateTime.
     */
    public static class DateComplexField <T ,F extends ZonedDateTime,S> extends SingleValuedComplexField<T,F,S> {

        /**
         * Creates an instance of a {@link SingleValuedComplexField.DateComplexField}.
         * @param fieldName String name of the created field.
         * @param type The field original model type.
         * @param facet The field facet type, must extend ZoneDateTime.
         * @param store The field store type.
         */
        protected DateComplexField(String fieldName, Class type, Class facet, Class store) {
            super(fieldName, type, facet, store);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param terms date to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(ZonedDateTime... terms) {
            return Filter.terms(this, terms);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @param terms date to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(Scope scope, ZonedDateTime... terms) {
            return Filter.terms(this, scope, terms);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two dates.
         * @param start Earliest date on the filter range check.
         * @param end Latest date in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(ZonedDateTime start, ZonedDateTime end) {
            return Filter.between(this.getName(), start, end);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two dates.
         * @param start Earliest date on the filter range check.
         * @param end Latest date in the filter range check.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter between(ZonedDateTime start, ZonedDateTime end, Scope scope) {
            return Filter.between(this.getName(), start, end, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two points in time.
         * @param start Earliest {@link DateMathExpression} on the filter range check.
         * @param end Latest {@link DateMathExpression} in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(DateMathExpression start, DateMathExpression end) {
            return Filter.between(this.getName(), start, end);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two points in time.
         * @param start Earliest {@link DateMathExpression} on the filter range check.
         * @param end Latest {@link DateMathExpression} in the filter range check.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter between(DateMathExpression start, DateMathExpression end, Scope scope) {
            return Filter.between(this.getName(), start, end, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two dates.
         * @param start Earliest date on the filter range check.
         * @param end Latest date in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(Date start, Date end) {
            return Filter.between(this.getName(), ZonedDateTime.ofInstant(start.toInstant(), ZoneId.of("UTC")), ZonedDateTime.ofInstant(end.toInstant(), ZoneId.of("UTC")));
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two dates.
         * @param start Earliest date on the filter range check.
         * @param end Latest date in the filter range check.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter between(Date start, Date end, Scope scope) {
            return Filter.between(this.getName(), ZonedDateTime.ofInstant(start.toInstant(), ZoneId.of("UTC")), ZonedDateTime.ofInstant(end.toInstant(), ZoneId.of("UTC")), scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date {@link DateMathExpression} to check against.
         * @return A configured filter for the field.
         */
        public Filter before(DateMathExpression date) {
            return Filter.before(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date {@link DateMathExpression} to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter before(DateMathExpression date, Scope scope) {
            return Filter.before(this.getName(), date, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter before(ZonedDateTime date) {
            return Filter.before(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date Date to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter before(ZonedDateTime date, Scope scope) {
            return Filter.before(this.getName(), date, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter before(Date date) {
            return Filter.before(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date Date to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter before(Date date, Scope scope) {
            return Filter.before(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")), scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter after(ZonedDateTime date) {
            return Filter.after(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter after(ZonedDateTime date, Scope scope) {
            return Filter.after(this.getName(), date, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date {@link DateMathExpression} to check against.
         * @return A configured filter for the field.
         */
        public Filter after(DateMathExpression date) {
            return Filter.after(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date {@link DateMathExpression} to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter after(DateMathExpression date, Scope scope) {
            return Filter.after(this.getName(), date, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter after(Date date) {
            return Filter.after(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter after(Date date, Scope scope) {
            return Filter.after(this.getName(),ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")),scope);
        }
    }

    /**
     * Class to instantiate {@link Document} complex fields containing Date facet value.
     *
     * @param <T> The field original model type.
     * @param <F> The field facet type, must extend Date.
     */
    public static class UtilDateComplexField <T ,F extends Date,S> extends SingleValuedComplexField<T,F,S> {

        /**
         * Creates an instance of a {@link SingleValuedComplexField.UtilDateComplexField}.
         *
         * @param fieldName String name of the created field.
         * @param type      The field original model type.
         * @param facet     The field facet type, must extend Date.
         */
        protected UtilDateComplexField(String fieldName, Class type, Class facet, Class suggestion) {
            super(fieldName, type, facet, suggestion);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param terms date to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(ZonedDateTime... terms) {
            return Filter.terms(this, terms);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @param terms date to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(Scope scope, ZonedDateTime... terms) {
            return Filter.terms(this, scope, terms);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two dates.
         * @param start Earliest date on the filter range check.
         * @param end Latest date in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(ZonedDateTime start, ZonedDateTime end) {
            return Filter.between(this.getName(), start, end);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two dates.
         * @param start Earliest date on the filter range check.
         * @param end Latest date in the filter range check.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter between(ZonedDateTime start, ZonedDateTime end, Scope scope) {
            return Filter.between(this.getName(), start, end, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two points in time.
         * @param start Earliest {@link DateMathExpression} on the filter range check.
         * @param end Latest {@link DateMathExpression} in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(DateMathExpression start, DateMathExpression end) {
            return Filter.between(this.getName(), start, end);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two points in time.
         * @param start Earliest {@link DateMathExpression} on the filter range check.
         * @param end Latest {@link DateMathExpression} in the filter range check.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter between(DateMathExpression start, DateMathExpression end, Scope scope) {
            return Filter.between(this.getName(), start, end, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two dates.
         * @param start Earliest date on the filter range check.
         * @param end Latest date in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(Date start, Date end) {
            return Filter.between(this.getName(),ZonedDateTime.ofInstant(start.toInstant(), ZoneId.of("UTC")), ZonedDateTime.ofInstant(end.toInstant(), ZoneId.of("UTC")));
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is between two dates.
         * @param start Earliest date on the filter range check.
         * @param end Latest date in the filter range check.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter between(Date start, Date end, Scope scope) {
            return Filter.between(this.getName(), ZonedDateTime.ofInstant(start.toInstant(), ZoneId.of("UTC")), ZonedDateTime.ofInstant(end.toInstant(), ZoneId.of("UTC")), scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date {@link DateMathExpression} to check against.
         * @return A configured filter for the field.
         */
        public Filter before(DateMathExpression date) {
            return Filter.before(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date {@link DateMathExpression} to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter before(DateMathExpression date, Scope scope) {
            return Filter.before(this.getName(), date, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter before(ZonedDateTime date) {
            return Filter.before(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date Date to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter before(ZonedDateTime date, Scope scope) {
            return Filter.before(this.getName(), date, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter before(Date date) {
            return Filter.before(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date Date to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter before(Date date, Scope scope) {
            return Filter.before(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")), scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter after(ZonedDateTime date) {
            return Filter.after(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter after(ZonedDateTime date, Scope scope) {
            return Filter.after(this.getName(), date, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date {@link DateMathExpression} to check against.
         * @return A configured filter for the field.
         */
        public Filter after(DateMathExpression date) {
            return Filter.after(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date {@link DateMathExpression} to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter after(DateMathExpression date, Scope scope) {
            return Filter.after(this.getName(), date, scope);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter after(Date date) {
            return Filter.after(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @param scope Enum {@link Scope} describing the scope to perform the filter on.
         * @return A configured filter for the field.
         */
        public Filter after(Date date, Scope scope) {
            return Filter.after(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")),scope);
        }
    }

    /**
     * Class to instantiate {@link Document} complex fields containing location type facet values.
     *
     * @param <T> The field content type, must extend LatLng.
     */
    public static class LocationComplexFieldDescriptor <T ,F extends LatLng,S> extends SingleValuedComplexField<T,F,S> {

        protected LocationComplexFieldDescriptor(String fieldName, Class<T> type, Class<F> facetType, Class<S> suggestion) {
            super(fieldName, type, facetType, suggestion);
        }

        /**
         * Instantiates a new {@link Filter} to checking if a field value is within a bounding box.
         *
         * @param upperLeft  the upper left corner of the box
         * @param lowerRight the lower left corner of the box
         * @return A configured filter for the field.
         */
        public Filter withinBBox(LatLng upperLeft, LatLng lowerRight) {
            return Filter.withinBBox(this.getName(), upperLeft, lowerRight);
        }

        /**
         * Instantiates a new {@link Filter} to checking if a field value is within a circle.
         *
         * @param center   the of teh circle
         * @param distance the radius of the circle
         * @return A configured filter for the field.
         */
        public Filter withinCircle(LatLng center, double distance) {
            return Filter.withinCircle(this.getName(), center, distance);
        }
    }
}
