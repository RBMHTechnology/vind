package com.rbmhtechnology.vind.annotations;

import com.rbmhtechnology.vind.annotations.language.Language;
import org.junit.Assert;
import org.junit.Test;

/**
 */
public class AnnotationsTest {

    @Test
    public void testAnnotations() throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        AnnotationsTestPojo testPojo = new AnnotationsTestPojo();

        /* Class Annotations Tests */
        Assert.assertEquals(1,testPojo.getClass().getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().isAnnotationPresent(Type.class));
        Assert.assertEquals("TypeTestAnnotation",testPojo.getClass().getAnnotation(Type.class).name());

        /* Method Annotations Tests */
        // idAnnotated field
        Assert.assertEquals(1,testPojo.getClass().getField("idAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("idAnnotated").isAnnotationPresent(Id.class));
        Assert.assertEquals("pref", testPojo.getClass().getField("idAnnotated").getAnnotation(Id.class).prefix());

        // testFieldAnnotated field
        Assert.assertEquals(1,testPojo.getClass().getField("testFieldAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("testFieldAnnotated").isAnnotationPresent(Field.class));
        Assert.assertEquals("FieldAnnotated", testPojo.getClass().getField("testFieldAnnotated").getAnnotation(Field.class).name());
        Assert.assertFalse(testPojo.getClass().getField("testFieldAnnotated").getAnnotation(Field.class).indexed());
        Assert.assertFalse(testPojo.getClass().getField("testFieldAnnotated").getAnnotation(Field.class).stored());

        // testFieldDefaultAnnotated field
        Assert.assertEquals(1,testPojo.getClass().getField("testFieldDefaultAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("testFieldDefaultAnnotated").isAnnotationPresent(Field.class));
        Assert.assertEquals("", testPojo.getClass().getField("testFieldDefaultAnnotated").getAnnotation(Field.class).name());
        Assert.assertTrue(testPojo.getClass().getField("testFieldDefaultAnnotated").getAnnotation(Field.class).indexed());
        Assert.assertTrue(testPojo.getClass().getField("testFieldDefaultAnnotated").getAnnotation(Field.class).stored());

        // testFullTextdAnnotated field
        Assert.assertEquals(1,testPojo.getClass().getField("testFullTextdAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("testFullTextdAnnotated").isAnnotationPresent(FullText.class));
        Assert.assertEquals(Language.English, testPojo.getClass().getField("testFullTextdAnnotated").getAnnotation(FullText.class).language());

        // testihnoretAnnotated field
        Assert.assertEquals(1,testPojo.getClass().getField("testihnoretAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("testihnoretAnnotated").isAnnotationPresent(Ignore.class));

        // testFacetDefaultAnnotated field
        Assert.assertEquals(1,testPojo.getClass().getField("testFacetDefaultAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("testFacetDefaultAnnotated").isAnnotationPresent(Facet.class));
        Assert.assertTrue(testPojo.getClass().getField("testFacetDefaultAnnotated").getAnnotation(Facet.class).suggestion());

        // testFacetAnnotated field
        Assert.assertEquals(1, testPojo.getClass().getField("testFacetAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("testFacetAnnotated").isAnnotationPresent(Facet.class));
        Assert.assertFalse(testPojo.getClass().getField("testFacetAnnotated").getAnnotation(Facet.class).suggestion());

        // testMultipleAnnotated field
        Assert.assertEquals(2,testPojo.getClass().getField("testMultipleAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("testMultipleAnnotated").isAnnotationPresent(Field.class));
        Assert.assertTrue(testPojo.getClass().getField("testMultipleAnnotated").isAnnotationPresent(Facet.class));

        // test score field
        Assert.assertEquals(1,testPojo.getClass().getField("testScoreAnnotated").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("testScoreAnnotated").isAnnotationPresent(Score.class));

        // test complex field
        Assert.assertEquals(1,testPojo.getClass().getField("taxonomy").getAnnotations().length);
        Assert.assertTrue(testPojo.getClass().getField("taxonomy").isAnnotationPresent(ComplexField.class));
        Assert.assertEquals("title",testPojo.getClass().getField("taxonomy").getAnnotation(ComplexField.class).store().fieldName()[0]);
    }
}
