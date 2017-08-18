package com.rbmhtechnology.vind.api.result.facet;

import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to store the stats facet response.
 * @author Fonso
 * @since 13.07.16.
 */
public class StatsFacetResult<T> implements FacetResult<T> {

    private FieldDescriptor field;
    private T min;
    private T max;
    private T sum;
    private Long count;
    private Long missing;
    private Double sumOfSquares;
    private T mean;
    private Double stddev;
    private Map<Double, Double> percentiles = new HashMap<>();
    private List<T> distinctValues;
    private Long countDistinct;
    private Long cardinality;

    /**
     * Creates a new instance of {@link StatsFacetResult}.
     * @param field {@link FieldDescriptor} of the document on which the stats are calculated.
     * @param min Minimum value of the field.
     * @param max Maximum value of the field
     * @param sum Summation of all values  of the field. Valid when T extends Number, T extends Date, or T extends ZoneDateTime.
     * @param count Count of documents containing the field.
     * @param missing Count of documents missing the field.
     * @param sumOfSquares Summation of the squares of all the field values. Valid when T extends Number, T extends Date, or T extends ZoneDateTime.
     * @param mean Average calculation oof the field values. Valid when T extends Number, T extends Date, or T extends ZoneDateTime.
     * @param stddev Standard deviation of the field values. Valid when T extends Number, T extends Date, or T extends ZoneDateTime.
     * @param percentiles Map of percentile values for each given in query percentage of sample. Valid when T extends Number.
     * @param distinctValues List of distinct values on the field.
     * @param countDistinct Number of distinct values on the field.
     * @param cardinality Statistical approximation to the number of different values on the field.
     */
    public StatsFacetResult(FieldDescriptor<T> field,
                            T min,
                            T max,
                            T sum,
                            Long count,
                            Long missing,
                            Double sumOfSquares,
                            T mean,
                            Double stddev,
                            Map<Double, Double> percentiles,
                            List<T> distinctValues,
                            Long countDistinct,
                            Long cardinality) {
        this.field = field;
        this.min = min;
        this.max = max;
        this.sum = sum;
        this.count = count;
        this.missing = missing;
        this.sumOfSquares = sumOfSquares;
        this.mean = mean;
        this.stddev = stddev;
        this.percentiles = percentiles;
        this.distinctValues = distinctValues;
        this.countDistinct = countDistinct;
        this.cardinality = cardinality;
    }
    /**
     * Gets the {@link FieldDescriptor} of this {@link PivotFacetResult}.
     * @return {@link FieldDescriptor}
     */
    public FieldDescriptor getField() {
        return field;
    }

    /**
     * Gets the Minimum value of the field.
     * @return Minimum value of the field.
     */
    public T getMin() {
        return min;
    }
    /**
     * Gets the Maximum value of the field.
     * @return Maximum value of the field.
     */
    public T getMax() {
        return max;
    }
    /**
     * Gets the Summation value of the field.
     * @return Summation value of the field.
     */
    public T getSum() {
        return sum;
    }
    /**
     * Gets the Count of documents containing the field.
     * @return Count of documents containing the field.
     */
    public Long getCount() {
        return count;
    }
    /**
     * Gets the count of documents missing the field.
     * @return Count of documents missing the field.
     */
    public Long getMissing() {
        return missing;
    }
    /**
     * Gets the Summation of the squares of all the field values.
     * @return Summation of the squares of all the field values.
     */
    public Double getSumOfSquares() {
        return sumOfSquares;
    }
    /**
     * Gets the Average calculation oof the field values.
     * @return Average calculation oof the field values.
     */
    public T getMean() {
        return mean;
    }
    /**
     * Gets the Standard deviation calculation oof the field values.
     * @return Standard deviation calculation oof the field values.
     */
    public Double getStddev() {
        return stddev;
    }
    /**
     * Gets the Map of percentile values for each given in query percentage of sample.
     * @return Map of percentile values for each given in query percentage of sample.
     */
    public Map<Double, Double> getPercentiles() {
        return percentiles;
    }
    /**
     * Gets the List of distinct values on the field.
     * @return List of distinct values on the field.
     */
    public List<T> getDistinctValues() {
        return distinctValues;
    }
    /**
     * Gets the Number of distinct values on the field.
     * @return Number of distinct values on the field.
     */
    public Long getCountDistinct() {
        return countDistinct;
    }
    /**
     * Gets the Statistical approximation to the number of different values on the field.
     * @return Statistical approximation to the number of different values on the field.
     */
    public Long getCardinality() {
        return cardinality;
    }
}
