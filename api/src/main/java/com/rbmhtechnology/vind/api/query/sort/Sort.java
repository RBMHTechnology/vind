package com.rbmhtechnology.vind.api.query.sort;

import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValuedComplexField;

/**
 * Sorting
 *
 * Abstract Class with static friendly user methods to create sorting query objects and to be implemented by basic sort
 * objects.
 */
public abstract class Sort {

    public enum Direction {
        Asc,
        Desc
    }

    protected Direction direction;

    private String type;

    @Override
    public abstract Sort clone();

    /**
     * Sets a {@link SpecialSort} object direction to ascending.
     * @param sort specific {@link SpecialSort} object.
     * @return {@link Sort} object with an ascending direction.
     */
    public static Sort asc(SpecialSort sort) {
        sort.setDirection(Direction.Asc);
        return sort;
    }
    /**
     * Sets a {@link SpecialSort} object direction to descending.
     * @param sort specific {@link SpecialSort} object.
     * @return {@link Sort} object with an descending direction.
     */
    public static Sort desc(SpecialSort sort) {
        sort.setDirection(Direction.Desc);
        return sort;
    }

    /**
     * Instantiates a new {@link SimpleSort} object with direction set to ascending.
     * @param field String name of the field to sort on.
     * @return {@link Sort} object with an ascending direction.
     */
    public static Sort asc(String field) {
        final Direction direction = Direction.Asc;
        return field(field, direction);
    }
    /**
     * Instantiates a new {@link DescriptorSort} object with direction set to ascending.
     * @param descriptor  {@link SingleValueFieldDescriptor} specifying the field to sort on.
     * @return {@link Sort} object with an ascending direction.
     */
    public static Sort asc(FieldDescriptor descriptor) {
        final Direction direction = Direction.Asc;
        return field(descriptor, direction);
    }

    /**
     * Instantiates a new {@link SimpleSort} object with direction set to ascending.
     * @param field String name of the field to sort on.
     * @param direction {@link Sort.Direction} to set the sorting direction (asc, desc).
     * @return {@link Sort} object with the given direction.
     */
    public static Sort field(String field, Direction direction) {
        return new SimpleSort(field, direction);
    }

    /**
     * Instantiates a new {@link DescriptorSort} object with direction set to ascending.
     * @param descriptor  {@link SingleValuedComplexField} specifying the field to sort on.
     * @param direction {@link Sort.Direction} to set the sorting direction (asc, desc).
     * @return {@link Sort} object with the given direction.
     */
    public static Sort field(SingleValuedComplexField descriptor, Direction direction) {
        return new DescriptorSort(descriptor, direction);
    }
    /**
     * Instantiates a new {@link DescriptorSort} object with direction set to ascending.
     * @param descriptor  {@link SingleValueFieldDescriptor} specifying the field to sort on.
     * @param direction {@link Sort.Direction} to set the sorting direction (asc, desc).
     * @return {@link Sort} object with the given direction.
     */
    public static Sort field(FieldDescriptor descriptor, Direction direction) {
        return new DescriptorSort(descriptor, direction);
    }
    /**
     * Instantiates a new {@link SimpleSort} object with direction set to descending.
     * @param field String name of the field to sort on.
     * @return {@link Sort} object with an descending direction.
     */
    public static Sort desc(String field) {
        final Direction direction = Direction.Desc;
        return field(field, direction);
    }
    /**
     * Instantiates a new {@link DescriptorSort} object with direction set to descending.
     * @param descriptor  {@link SingleValueFieldDescriptor} specifying the field to sort on.
     * @return {@link Sort} object with an descending direction.
     */
    public static Sort desc(FieldDescriptor descriptor) {
        final Direction direction = Direction.Desc;
        return field(descriptor, direction);
    }

    /**
     * Gets the sorting direction
     * @return {@link Sort.Direction}
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Gets the type of the sorting configuration
     * @return {@link String} class name of the sort
     */
    public String getType() {
        return this.getClass().getSimpleName();
    }

    /**
     * Abstract class to be implemented by complex sorting objects.
     */
    public static abstract class SpecialSort extends Sort {

        /**
         * Static method to instantiate a {@link Sort.SpecialSort.ScoredDate} sorting object.
         * @param descriptor {@link SingleValueFieldDescriptor} indicating the field to perform the sorting on.
         * @return {@link Sort.SpecialSort.ScoredDate} sort query object.
         */
        public static ScoredDate scoredDate(SingleValueFieldDescriptor descriptor) {
            return new ScoredDate(descriptor);
        }

        /**
         * Static method to instantiate a {@link com.rbmhtechnology.vind.api.query.distance.Distance} object.
         * Be sure that geoDistance is set in search!
         * @return {@link Sort.SpecialSort.ScoredDate} sort query object.
         */
        public static DistanceSort distance() {
            return new DistanceSort();
        }

        /**
         * Sorting class which modifies the document score based on the date.
         */
        public static class ScoredDate extends SpecialSort {

            private SingleValueFieldDescriptor descriptor;

            /**
             * Creates a new instance of {@link Sort.SpecialSort.ScoredDate} for a given field.
             * @param descriptor {@link SingleValueFieldDescriptor} indicating the field to perform the sorting on.
             */
            protected ScoredDate(SingleValueFieldDescriptor descriptor) {
                this.descriptor = descriptor;
            }

            /**
             * Gets the {@link FieldDescriptor}.
             * @return {@link SingleValueFieldDescriptor}
             */
            public FieldDescriptor getDescriptor() {
                return descriptor;
            }

            /**
             * Gets the {@link String} name of the field descriptor.
             * @return {@link String} name of the field descriptor.
             */
            public String getField() {return descriptor.getName();}

            @Override
            public void setDirection(Direction direction) {
                this.direction = direction;
            }

            @Override
            public String toString(){
                final String scoreString = "{" +
                        "\"direction\":\"%s\"," +
                        "\"field\":\"%s\"" +
                        "}";
                return String.format(scoreString,this.direction,this.descriptor.getName());
            }

            @Override
            public Sort clone() {
                final ScoredDate copy = new ScoredDate(this.descriptor);
                copy.direction = this.direction;
                return copy;
            }
        }

        public static class DistanceSort extends SpecialSort {

            public DistanceSort() {
                this.direction = Direction.Asc;
            }

            @Override
            public void setDirection(Direction direction) {
                this.direction = direction;
            }

            @Override
            public String toString(){
                final String scoreString = "{" +
                        "\"direction\":\"%s\"," +
                        "\"field\":\"location\"" +
                        "}";
                return String.format(scoreString,this.direction);
            }
        }

        public abstract void setDirection(Direction direction);

        @Override
        public Sort clone() {
            final DistanceSort copy = new DistanceSort();
            copy.direction = this.direction;
            return copy;
        }
    }

    /**
     * Sorting class which sorts results based on a field.
     */
    public static class SimpleSort extends Sort {

        private String field;

        /**
         * Creates a new instance of {@link SimpleSort}.
         * @param field String name of the field to sort on.
         * @param direction {@link Sort.Direction} of the sorting results.
         */
        public SimpleSort(String field, Direction direction) {
            this.field = field;
            this.direction = direction;
        }

        /**
         * Gets the name of the field to sort on.
         * @return String field name.
         */
        public String getField() {
            return field;
        }

        @Override
        public String toString(){
            final String scoreString = "{" +
                    "'direction':'%s'," +
                    "'field':'%s'" +
                    "}";
            return String.format(scoreString,this.direction,this.field);
        }

        @Override
        public Sort clone() {
            final SimpleSort copy = new SimpleSort(new String(this.field), this.direction);
            return copy;
        }
    }
    /**
     * Sorting class which sorts results based on a field.
     */
    public static class DescriptorSort extends Sort {
        private FieldDescriptor descriptor;

        /**
         * Creates a new instance of {@link DescriptorSort}.
         * @param descriptor {@link FieldDescriptor} indicating the field to sort on.
         * @param direction {@link Sort.Direction} of the sorting results.
         */
        public DescriptorSort(FieldDescriptor descriptor, Direction direction) {
            this.descriptor = descriptor;
            this.direction = direction;
        }
        /**
         * Gets the field description of the field to sort on.
         * @return {@link FieldDescriptor} of the sorting field.
         */
        public FieldDescriptor getDescriptor() {
            return descriptor;
        }

        /**
         * Gets the {@link String} name of the field descriptor.
         * @return {@link String} name of the field descriptor.
         */
        public String getField() {return descriptor.getName();}

        @Override
        public String toString(){
            final String scoreString = "{" +
                    "\"direction\":\"%s\"," +
                    "\"field\":\"%s\"" +
                    "}";
            return String.format(scoreString,this.direction,this.descriptor.getName());
        }

        @Override
        public Sort clone() {
            final DescriptorSort copy = new DescriptorSort(this.descriptor, this.direction);
            return copy;
        }
    }

}
