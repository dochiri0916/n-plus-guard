package com.dochiri.nplusguard.core.query;

import static java.util.Objects.requireNonNull;

public record QueryEvent(
        String sql,
        String normalizedSql,
        String fingerprint,
        QueryType queryType
) {

    public QueryEvent {
        requireNonNull(sql, "sql은 null일 수 없습니다");
        requireNonNull(normalizedSql, "normalizedSql은 null일 수 없습니다");
        requireNonNull(fingerprint, "fingerprint는 null일 수 없습니다");
        requireNonNull(queryType, "queryType은 null일 수 없습니다");

        if (sql.isBlank()) {
            throw new IllegalArgumentException("sql은 비어 있을 수 없습니다");
        }
    }

    public static QueryEvent capture(
            String sql
    ) {
        String normalizedSql = DefaultQueryNormalizer.normalize(sql);
        String fingerprint = DefaultQueryFingerprintStrategy.fingerprint(normalizedSql);
        return new QueryEvent(
                sql,
                normalizedSql,
                fingerprint,
                QueryType.fromSql(normalizedSql)
        );
    }

}
