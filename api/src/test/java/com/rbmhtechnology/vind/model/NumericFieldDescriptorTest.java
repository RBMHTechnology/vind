package com.rbmhtechnology.vind.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by fonso on 6/22/16.
 */
public class NumericFieldDescriptorTest {
    
    private FieldDescriptorBuilder factory;
    private SingleValueFieldDescriptor.NumericFieldDescriptor numericFD;
    
    @Before
    public  void setup(){
        factory = new FieldDescriptorBuilder();
    }

    @Test
    public void numberBetweenTest(){
        numericFD = factory.buildNumericField("number");

        Integer start = 5;
        Double end = 6.7;
        Assert.assertEquals("number=[ 5 TO 6.7 ]", numericFD.between(start, end).toString());
    }

    @Test
    public void numberGreaterThanTest(){
        numericFD = factory.buildNumericField("number");
        int breakNumber = 1;
        Assert.assertEquals("number=[ 1 TO * ]",numericFD.greaterThan(breakNumber).toString());
    }

    @Test
    public void numberLesserThanTest(){
        numericFD = factory.buildNumericField("number");
        Number breakNumber = 1;
        Assert.assertEquals("number=[ * TO 1 ]", numericFD.lesserThan(breakNumber).toString());

    }

}
