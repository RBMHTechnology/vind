package com.rbmhtechnology.vind.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by fonso on 6/22/16.
 */
public class TextFieldDescriptorTest {
    
    private FieldDescriptorBuilder factory;
    private SingleValueFieldDescriptor.TextFieldDescriptor textFD;
    
    @Before
    public  void setup(){
        factory = new FieldDescriptorBuilder();
    }

    @Test
    public void textEqualsTest(){
        textFD = factory.buildTextField("text");

        String text = "sample text";
        Assert.assertEquals("text='sample text'", textFD.equals(text).toString());
    }

    @Test
    public void textPrefixTest(){
        textFD = factory.buildTextField("text");
        String prefix = "pref_";
        Assert.assertEquals("text=pref_*", textFD.prefix(prefix).toString());
    }


}
