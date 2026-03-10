package com.dochiri.nplusguard.core.scope;

import com.dochiri.nplusguard.core.query.QueryEvent;

@FunctionalInterface
public interface QueryCollector {

    void record(QueryEvent queryEvent);
}
