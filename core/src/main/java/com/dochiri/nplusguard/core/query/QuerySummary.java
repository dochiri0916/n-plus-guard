package com.dochiri.nplusguard.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record QuerySummary(
        int totalCount,
        int selectCount,
        int insertCount,
        int updateCount,
        int deleteCount,
        int otherCount,
        List<RepeatedQuery> repeatedSelects
) {

    public QuerySummary {
        if (totalCount < 0 || selectCount < 0 || insertCount < 0 || updateCount < 0 || deleteCount < 0 || otherCount < 0) {
            throw new IllegalArgumentException("query counts must not be negative");
        }
        repeatedSelects = repeatedSelects == null ? List.of() : List.copyOf(repeatedSelects);
    }

    public int count(QueryType queryType) {
        return switch (Objects.requireNonNull(queryType, "queryType must not be null")) {
            case SELECT -> selectCount;
            case INSERT -> insertCount;
            case UPDATE -> updateCount;
            case DELETE -> deleteCount;
            case OTHER -> otherCount;
        };
    }

    public int maxRepeatedSelectCount() {
        return repeatedSelects.stream()
                .mapToInt(RepeatedQuery::executions)
                .max()
                .orElse(0);
    }

    public static QuerySummary from(Collection<QueryEvent> events) {
        Objects.requireNonNull(events, "events must not be null");

        int selectCount = 0;
        int insertCount = 0;
        int updateCount = 0;
        int deleteCount = 0;
        int otherCount = 0;
        Map<String, MutableRepeatedQuery> repeatedSelectAccumulator = new LinkedHashMap<>();

        for (QueryEvent event : events) {
            switch (event.queryType()) {
                case SELECT -> {
                    selectCount++;
                    repeatedSelectAccumulator
                            .computeIfAbsent(event.fingerprint(),
                                    ignored -> new MutableRepeatedQuery(event.fingerprint(), event.normalizedSql()))
                            .increment();
                }
                case INSERT -> insertCount++;
                case UPDATE -> updateCount++;
                case DELETE -> deleteCount++;
                case OTHER -> otherCount++;
            }
        }

        List<RepeatedQuery> repeatedSelects = new ArrayList<>();
        for (MutableRepeatedQuery query : repeatedSelectAccumulator.values()) {
            if (query.executions > 1) {
                repeatedSelects.add(new RepeatedQuery(query.fingerprint, query.normalizedSql, query.executions));
            }
        }

        repeatedSelects.sort(Comparator
                .comparingInt(RepeatedQuery::executions)
                .reversed()
                .thenComparing(RepeatedQuery::normalizedSql));

        int totalCount = selectCount + insertCount + updateCount + deleteCount + otherCount;
        return new QuerySummary(totalCount, selectCount, insertCount, updateCount, deleteCount, otherCount, repeatedSelects);
    }

    private static final class MutableRepeatedQuery {
        private final String fingerprint;
        private final String normalizedSql;
        private int executions;

        private MutableRepeatedQuery(String fingerprint, String normalizedSql) {
            this.fingerprint = fingerprint;
            this.normalizedSql = normalizedSql;
        }

        private void increment() {
            executions++;
        }
    }
}
