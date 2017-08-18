package com.rbmhtechnology.vind.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Created by fonso on 6/22/16.
 */
public class DateFieldDescriptorTest {
    
    private FieldDescriptorBuilder factory;
    private SingleValueFieldDescriptor.DateFieldDescriptor dateFD;
    
    @Before
    public  void setup(){
        
        factory = new FieldDescriptorBuilder();
    }
    @Test
    public void dateBetweenTest(){
        dateFD = factory.buildDateField("now");

        ZonedDateTime start = ZonedDateTime.of(2015, 01, 01, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime end = ZonedDateTime.of(2016, 01, 01, 0, 0, 0, 0, ZoneId.of("UTC"));
        Assert.assertEquals("now=[ 2015-01-01T00:00:00Z TO 2016-01-01T00:00:00Z ]", dateFD.between(start, end).toString());
        
    }

    @Test
    public void dateBeforeTest(){
        dateFD = factory.buildDateField("now");

        ZonedDateTime breakDate = ZonedDateTime.of(2015, 01, 01, 0, 0, 0, 0, ZoneId.of("UTC"));

        Assert.assertEquals("now=[ * TO 2015-01-01T00:00:00Z ]",dateFD.before(breakDate).toString());

    }

    @Test
    public void dateAfterTest(){
        dateFD = factory.buildDateField("now");

        ZonedDateTime breakDate = ZonedDateTime.of(2015, 01, 01, 0, 0, 0, 0, ZoneId.of("UTC"));

        Assert.assertEquals("now=[ 2015-01-01T00:00:00Z TO * ]", dateFD.after(breakDate).toString());

    }
        
}
