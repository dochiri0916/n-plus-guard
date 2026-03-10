package com.dochiri.nplusguard.core.scope;

import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.query.QuerySummary;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GuardScopeSnapshot(
        String scopeId,
        String scopeName,
        Instant startedAt,
        Instant endedAt,
        List<QueryEvent> queryEvents,
        QuerySummary summary
) {

    public GuardScopeSnapshot {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(scopeName, "scopeName must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(endedAt, "endedAt must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        if (scopeId.isBlank()) {
            throw new IllegalArgumentException("scopeId must not be blank");
        }
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("endedAt must not be before startedAt");
        }

        queryEvents = queryEvents == null ? List.of() : List.copyOf(queryEvents);
    }
}
