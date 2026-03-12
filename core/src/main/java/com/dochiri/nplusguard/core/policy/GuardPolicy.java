package com.dochiri.nplusguard.core.policy;

import com.dochiri.nplusguard.core.query.QuerySummary;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public record GuardPolicy(
        int maxTotalQueries,
        int maxSelectQueries,
        int maxRepeatedSelectExecutions
) {

    public GuardPolicy {
        if (maxTotalQueries < 0 || maxSelectQueries < 0 || maxRepeatedSelectExecutions < 0) {
            throw new IllegalArgumentException("guard policy 제한 값은 음수일 수 없습니다");
        }
    }

    public List<String> validate(QuerySummary summary) {
        requireNonNull(summary, "summary는 null일 수 없습니다");

        List<String> violations = new ArrayList<>();

        if (maxTotalQueries > 0 && summary.totalCount() > maxTotalQueries) {
            violations.add("전체 쿼리 수가 제한을 초과했습니다. actual=%d, limit=%d"
                    .formatted(summary.totalCount(), maxTotalQueries));
        }

        if (maxSelectQueries > 0 && summary.selectCount() > maxSelectQueries) {
            violations.add("SELECT 쿼리 수가 제한을 초과했습니다. actual=%d, limit=%d"
                    .formatted(summary.selectCount(), maxSelectQueries));
        }

        int maxRepeatedSelectCount = summary.maxRepeatedSelectCount();
        if (maxRepeatedSelectExecutions > 0 && maxRepeatedSelectCount > maxRepeatedSelectExecutions) {
            violations.add("반복 SELECT 실행 횟수가 제한을 초과했습니다. actual=%d, limit=%d"
                    .formatted(maxRepeatedSelectCount, maxRepeatedSelectExecutions));
        }

        return List.copyOf(violations);
    }

    public boolean isSatisfiedBy(QuerySummary summary) {
        return validate(summary).isEmpty();
    }

    public void check(QuerySummary summary) {
        List<String> violations = validate(summary);
        if (!violations.isEmpty()) {
            throw new IllegalStateException(String.join(System.lineSeparator(), violations));
        }
    }
}
