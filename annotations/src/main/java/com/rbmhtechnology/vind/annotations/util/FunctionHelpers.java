package com.rbmhtechnology.vind.annotations.util;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by fonso on 28.03.17.
 */
public class FunctionHelpers {
    public static abstract  class ParameterFunction<T,R> implements Function<T, R> {

        private List parameters = ListUtils.EMPTY_LIST;
        public Function<T,R> setParameters(List parameters) {
           this.parameters = parameters;
           return this;
        }

        public List getParameters() {
            return parameters;
        }

    }
    public final static class GetterFunction extends ParameterFunction<Object, Object> {

        private static Logger log = LoggerFactory.getLogger(GetterFunction.class);

        @Override
        public Object apply(Object o) {
            if (CollectionUtils.isNotEmpty(super.parameters)) {
                final String propertyName = (String) getParameters().get(0);
                try {
                    final PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(o, propertyName);
                    final Method readMethod = PropertyUtils.getReadMethod(descriptor);
                    log.debug("Execute getter method '{}' of class [{}]", readMethod.getName(), o.getClass().getName());
                    return readMethod.invoke(o);

                } catch (InvocationTargetException e) {
                    log.error("Unable to find property '{}' in object from class [{}]", propertyName, o.getClass().getName(),e);
                    throw new RuntimeException("Unable to find property '"+propertyName+"' in object from class ["+o.getClass().getName()+"]");
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    log.error("Unable to find/access getter method for property '{}' in object from class [{}]", propertyName, o.getClass().getName(), e);
                    throw new RuntimeException("Unable to find/access getter method for property '"+propertyName+"' in object from class ["+o.getClass().getName()+"]");
                }
            } else return null;
        }
    }

    public final static class ConcatFunction extends ParameterFunction<Object, Object> {

        private static Logger log = LoggerFactory.getLogger(ConcatFunction.class);

        @Override
        public Object apply(Object o) {
            if (CollectionUtils.isNotEmpty(super.parameters)) {
                return getParameters().stream()
                        .map(propertyName -> {
                            try {
                                final PropertyDescriptor descriptor = PropertyUtils.getPropertyDescriptor(o, (String) propertyName);
                                final Method readMethod = PropertyUtils.getReadMethod(descriptor);
                                log.debug("Execute getter method '{}' of class [{}]", readMethod.getName(), o.getClass().getName());
                                final Object methodResult = readMethod.invoke(o);

                                if(Collection.class.isAssignableFrom(methodResult.getClass())) {
                                    return ((Collection) methodResult).stream().map(Object::toString).collect(Collectors.joining(" "));
                                }
                                return methodResult.toString();

                            } catch (InvocationTargetException e) {
                                log.error("Unable to find property '{}' in object from class [{}]", propertyName, o.getClass().getName(), e);
                                throw new RuntimeException("Unable to find property '" + propertyName + "' in object from class [" + o.getClass().getName() + "]");
                            } catch (NoSuchMethodException | IllegalAccessException e) {
                                log.error("Unable to find/access getter method for property '{}' in object from class [{}]", propertyName, o.getClass().getName(), e);
                                throw new RuntimeException("Unable to find/access getter method for property '" + propertyName + "' in object from class [" + o.getClass().getName() + "]");
                            }
                        })
                        .collect(Collectors.joining(" "));

            } else return null;
        }
    }
}
