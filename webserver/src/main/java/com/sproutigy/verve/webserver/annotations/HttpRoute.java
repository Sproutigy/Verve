package com.sproutigy.verve.webserver.annotations;

import java.lang.annotation.*;

@Repeatable(HttpRoutes.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpRoute {
    String[] method() default {};
    String[] path() default {};
    int order() default 0;
    Class inputType() default void.class;
    Class outputType() default void.class;
}
