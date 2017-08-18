package com.rbmhtechnology.vind.annotations;

import java.util.function.Function;

/**
 * Created by fonso on 28.03.17.
 */
public @interface Operator {
    Class<? extends Function> function ();
    Class<?> returnType () default String.class;
    String[] fieldName () default {};


}
