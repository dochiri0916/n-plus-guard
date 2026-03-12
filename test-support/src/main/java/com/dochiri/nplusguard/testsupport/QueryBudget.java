package com.dochiri.nplusguard.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryBudget {

    int maxTotalQueries() default 0;

    int maxSelectQueries() default 0;

    int maxRepeatedSelectExecutions() default 0;

}
