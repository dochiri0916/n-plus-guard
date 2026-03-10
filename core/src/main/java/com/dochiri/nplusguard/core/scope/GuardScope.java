package com.dochiri.nplusguard.core.scope;

import java.time.Instant;

public interface GuardScope extends QueryCollector, AutoCloseable {

    String id();

    String name();

    Instant startedAt();

    boolean closed();

    GuardScopeSnapshot snapshot();

    @Override
    void close();
}
