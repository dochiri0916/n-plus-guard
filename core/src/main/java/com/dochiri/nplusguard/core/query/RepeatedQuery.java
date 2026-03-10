package com.dochiri.nplusguard.core.query;

import java.util.Objects;

public record RepeatedQuery(
        String fingerprint,
        String normalizedSql,
        int executions
) {

    public RepeatedQuery {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(normalizedSql, "normalizedSql must not be null");
        if (executions <= 0) {
            throw new IllegalArgumentException("executions must be positive");
        }
    }
}
