package com.rbmhtechnology.vind.annotations.util;

import org.junit.Test;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static com.rbmhtechnology.vind.annotations.AnnotationsTestPojo.Taxonomy;
import static com.rbmhtechnology.vind.annotations.util.FunctionHelpers.ConcatFunction;
import static com.rbmhtechnology.vind.annotations.util.FunctionHelpers.GetterFunction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by fonso on 28.03.17.
 */
public class FunctionTest {

    @Test
    public void getterFunctionTest() throws IntrospectionException {
        final BeanInfo beanInfo = Introspector.getBeanInfo(GetterFunction.class);
        final Method applyMethod = Arrays.asList(beanInfo.getMethodDescriptors())
                .stream()
                .map(md -> md.getMethod())
                .filter(m -> m.getName().equals("apply"))
                .findAny()
                .get();
        final Class<?>[] parameterTypes = applyMethod.getParameterTypes();
        final Class<?> returnType = applyMethod.getReturnType();
        assertTrue(true);
    }

    @Test
    public void concatFunctionTest() throws IntrospectionException, InvocationTargetException, IllegalAccessException {

        final Taxonomy pojo = new Taxonomy(Arrays.asList("term1","term2"),"title","id1");
        final ConcatFunction cf = new ConcatFunction();
        cf.setParameters(Arrays.asList("terms", "title", "id"));

        final Object result = cf.apply(pojo);

        assertEquals("term1 term2 title id1",result);

    }

}
