package com.rbmhtechnology.vind.model;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.value.LatLng;

import java.nio.ByteBuffer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;

/**
 * Abstract class t be implemented by the fields meant to be single valued.
 * Created by fonso on 6/24/16.
 */
public abstract class SingleValueFieldDescriptor<T> extends FieldDescriptor<T> {

    protected SingleValueFieldDescriptor(String fieldName, Class<T> type) {
        super(fieldName, type);
        this.sort = true;
    }

    /**
     * Class to instantiate fields containing one single numeric value.
     *
     * @param <T> The field content type, must extend Number.
     */
    public static class NumericFieldDescriptor<T extends Number> extends SingleValueFieldDescriptor<T> {

        protected NumericFieldDescriptor(String fieldName, Class<T> type) {
            super(fieldName, type);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param terms Number to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(Number... terms) {
            return Filter.terms((FieldDescriptor) this, terms);
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
         * Instantiates a new {@link Filter} to checking if the field value is greater than a numbers.
         * @param number Number to check against.
         * @return A configured filter for the field.
         */
        public Filter greaterThan(Number number) {
            return Filter.greaterThan(this.getName(), number);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is lower than a numbers.
         * @param number Number to check against.
         * @return A configured filter for the field.
         */
        public Filter lesserThan(Number number) {
            return Filter.lesserThan(this.getName(), number);
        }
    }

    /**
     * Class to instantiate {@link Document} fields containing one single date value.
     *
     * @param <T> The field content type, must extend ZoneDateTime.
     */
    public static class DateFieldDescriptor<T extends ZonedDateTime> extends SingleValueFieldDescriptor<T> {

        protected DateFieldDescriptor(String fieldName, Class<T> type) {
            super(fieldName, type);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param terms Date to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(ZonedDateTime... terms) {
            return Filter.terms((FieldDescriptor) this, terms);
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
         * Instantiates a new {@link Filter} to checking if the field value is between two points in time.
         * @param start Earliest {@link DateMathExpression} on the filter range check.
         * @param end Latest {@link DateMathExpression} in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(DateMathExpression start, DateMathExpression end) {
            return Filter.between(this.getName(), start, end);
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
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date {@link DateMathExpression} to check against.
         * @return A configured filter for the field.
         */
        public Filter before(DateMathExpression date) {
            return Filter.before(this.getName(), date);
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
         * @return A configured filter for the field.
         */
        public Filter before(Date date) {
            return Filter.before(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
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
         * @param date {@link DateMathExpression} to check against.
         * @return A configured filter for the field.
         */
        public Filter after(DateMathExpression date) {
            return Filter.after(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter after(Date date) {
            return Filter.after(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
        }

    }

    /**
     * Class to instantiate {@link Document} fields containing one single date value.
     *
     * @param <T> The field content type, must extend java.util.Date.
     */
    public static class UtilDateFieldDescriptor<T extends Date> extends SingleValueFieldDescriptor<T> {

        protected UtilDateFieldDescriptor(String fieldName, Class<T> type) {
            super(fieldName, type);
        }

        /**
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param terms Date to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(ZonedDateTime... terms) {
            return Filter.terms((FieldDescriptor) this, terms);
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
         * Instantiates a new {@link Filter} to checking if the field value is between two points in time.
         * @param start Earliest {@link DateMathExpression} on the filter range check.
         * @param end Latest {@link DateMathExpression} in the filter range check.
         * @return A configured filter for the field.
         */
        public Filter between(DateMathExpression start, DateMathExpression end) {
            return Filter.between(this.getName(), start, end);
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
         * Instantiates a new {@link Filter} to checking if the field value is before a date.
         * @param date {@link DateMathExpression} to check against.
         * @return A configured filter for the field.
         */
        public Filter before(DateMathExpression date) {
            return Filter.before(this.getName(), date);
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
         * @return A configured filter for the field.
         */
        public Filter before(Date date) {
            return Filter.before(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
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
         * @param date {@link DateMathExpression} to check against.
         * @return A configured filter for the field.
         */
        public Filter after(DateMathExpression date) {
            return Filter.after(this.getName(), date);
        }

        /**
         * Instantiates a new {@link Filter} to checking if the field value is after a date.
         * @param date Date to check against.
         * @return A configured filter for the field.
         */
        public Filter after(Date date) {
            return Filter.after(this.getName(), ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC")));
        }
    }

    /**
     * Class to instantiate {@link Document} fields containing one single text value.
     *
     * @param <T> The field content type, must extend CharSequence.
     */
    public static class TextFieldDescriptor<T extends CharSequence> extends SingleValueFieldDescriptor<T> {

        protected TextFieldDescriptor(String fieldName, Class<T> type) {
            super(fieldName, type);
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
         * Instantiates a new {@link Filter} which filters in all the documents matching at least one
         * of the given terms.
         * @param terms text to check against.
         * @return A configured filter for the field.
         */
        public Filter terms(String... terms) {
            return Filter.terms((FieldDescriptor) this, terms);
        }

    }


    /**
     * Class to instantiate {@link Document} fields containing multiple location values.
     *
     * @param <T> The field content type, must extend LatLng.
     */
    public static class LocationFieldDescriptor<T extends LatLng> extends SingleValueFieldDescriptor<T> {

        protected LocationFieldDescriptor(String fieldName, Class<T> type) {
            super(fieldName, type);
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

    /**
     * Class to instantiate {@link Document} fields containing one single binary field value.
     *
     * @param <T> The field content type, must extend ByteBuffer.
     */
    public static class BinaryFieldDescriptor<T extends ByteBuffer> extends SingleValueFieldDescriptor<T> {

        protected BinaryFieldDescriptor(String fieldName, Class<T> type) {
            super(fieldName, type);
        }
    }
}
