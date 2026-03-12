package com.dochiri.nplusguard.core.query;

import static java.util.Objects.requireNonNull;

public record RepeatedQuery(
        String fingerprint,
        String normalizedSql,
        int executions
) {

    public RepeatedQuery {
        requireNonNull(fingerprint, "fingerprint는 null일 수 없습니다");
        requireNonNull(normalizedSql, "normalizedSql은 null일 수 없습니다");
        if (executions <= 0) {
            throw new IllegalArgumentException("executions는 1 이상이어야 합니다");
        }
    }

}
