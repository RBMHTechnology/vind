package com.rbmhtechnology.vind.utils.mam;

import com.rbmhtechnology.vind.model.*;
import org.apache.solr.search.SyntaxError;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by fonso on 02.02.17.
 */
public class FacetMapperTest {

    @Test
    public void test() throws SyntaxError {

        HashMap<String, String> dateIntervals = new HashMap<>();
        dateIntervals.put("after","[NOW+23DAYS/DAY TO *]");
        dateIntervals.put("before","[* TO NOW+23DAYS/DAY]");

        HashMap<String, String> numberIntervals = new HashMap<>();
        numberIntervals.put("bigger","[23 TO *]");
        numberIntervals.put("smaller","[* TO 22]");

        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> testDateField = new FieldDescriptorBuilder().buildUtilDateField("test1");
        FieldDescriptor<Float> testNumericField = new FieldDescriptorBuilder().buildNumericField("numericTest", Float.class);

        Assert.assertTrue(FacetMapper.stringQuery2FacetMapper(testDateField, "dateFacet",dateIntervals).getName().equals("dateFacet"));
        Assert.assertTrue(FacetMapper.stringQuery2FacetMapper(testNumericField, "numericFacet", numberIntervals).getName().equals("numericFacet"));

        Assert.assertTrue(true);
    }

    @Test
    public void testComplexField() throws SyntaxError {

        HashMap<String, String> dateIntervals = new HashMap<>();
        dateIntervals.put("after","[NOW+23DAYS/DAY TO *]");
        dateIntervals.put("before","[* TO NOW+23DAYS/DAY]");

        HashMap<String, String> numberIntervals = new HashMap<>();
        numberIntervals.put("bigger","[23 TO *]");
        numberIntervals.put("smaller","[* TO 22]");

        SingleValuedComplexField.UtilDateComplexField<Taxonomy,Date,Date> complexDateField = new ComplexFieldDescriptorBuilder<Taxonomy,Date,Date>()
                .setFacet(true, tax -> Arrays.asList(tax.getDate()))
                .buildUtilDateComplexField("complexDateTax", Taxonomy.class, Date.class, Date.class);

        SingleValuedComplexField.NumericComplexField<Taxonomy,Number,Number> complexNumberField = new ComplexFieldDescriptorBuilder<Taxonomy,Number,Number>()
                .setFacet(true, tax -> Arrays.asList(tax.getTerm()))
                .buildNumericComplexField("complexNumberTax", Taxonomy.class, Number.class, Number.class);

        Assert.assertTrue(FacetMapper.stringQuery2FacetMapper(complexDateField, "dateFacet",dateIntervals).getName().equals("dateFacet"));
        Assert.assertTrue(FacetMapper.stringQuery2FacetMapper(complexNumberField, "numericFacet", numberIntervals).getName().equals("numericFacet"));

        Assert.assertTrue(true);
    }

    public class Taxonomy implements Serializable {
        public Number term;
        public Date date;

        public Number getTerm() {
            return term;
        }

        public Date getDate() {
            return date;
        }
    }

}
