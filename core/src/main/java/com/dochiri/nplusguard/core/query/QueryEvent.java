package com.dochiri.nplusguard.core.query;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record QueryEvent(
        String sql,
        String normalizedSql,
        String fingerprint,
        QueryType queryType,
        Instant startedAt,
        Duration duration,
        long threadId,
        String scopeId,
        QueryOrigin origin,
        Long rowCount,
        Map<String, String> metadata
) {

    public QueryEvent {
        Objects.requireNonNull(sql, "sql must not be null");
        Objects.requireNonNull(normalizedSql, "normalizedSql must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(queryType, "queryType must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(origin, "origin must not be null");

        if (sql.isBlank()) {
            throw new IllegalArgumentException("sql must not be blank");
        }
        if (scopeId.isBlank()) {
            throw new IllegalArgumentException("scopeId must not be blank");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }

        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static QueryEvent capture(
            String sql,
            Instant startedAt,
            Duration duration,
            String scopeId,
            QueryOrigin origin,
            Long rowCount,
            Map<String, String> metadata,
            QueryNormalizer queryNormalizer,
            QueryFingerprintStrategy fingerprintStrategy
    ) {
        Objects.requireNonNull(queryNormalizer, "queryNormalizer must not be null");
        Objects.requireNonNull(fingerprintStrategy, "fingerprintStrategy must not be null");

        String normalizedSql = queryNormalizer.normalize(sql);
        String fingerprint = fingerprintStrategy.fingerprint(normalizedSql);
        return new QueryEvent(
                sql,
                normalizedSql,
                fingerprint,
                QueryType.fromSql(normalizedSql),
                startedAt,
                duration,
                Thread.currentThread().threadId(),
                scopeId,
                origin,
                rowCount,
                metadata
        );
    }

    public static QueryEvent capture(
            String sql,
            Duration duration,
            String scopeId,
            QueryOrigin origin,
            Long rowCount,
            Map<String, String> metadata,
            QueryNormalizer queryNormalizer,
            QueryFingerprintStrategy fingerprintStrategy
    ) {
        return capture(
                sql,
                Instant.now(),
                duration,
                scopeId,
                origin,
                rowCount,
                metadata,
                queryNormalizer,
                fingerprintStrategy
        );
    }
}
