package com.dochiri.nplusguard.testsupport;

import com.dochiri.nplusguard.core.policy.GuardPolicy;
import com.dochiri.nplusguard.core.query.QuerySummary;
import com.dochiri.nplusguard.core.scope.GuardScope;
import com.dochiri.nplusguard.core.scope.ThreadLocalGuardScopeManager;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class QueryGuardAssertions {

    private QueryGuardAssertions() {
    }

    public static void assertWithin(
            ThreadLocalGuardScopeManager scopeManager,
            GuardPolicy policy,
            Runnable action
    ) {
        assertWithin(scopeManager, policy, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T assertWithin(
            ThreadLocalGuardScopeManager scopeManager,
            GuardPolicy policy,
            Supplier<T> action
    ) {
        requireNonNull(scopeManager, "scopeManager는 null일 수 없습니다");
        requireNonNull(policy, "policy는 null일 수 없습니다");
        requireNonNull(action, "action은 null일 수 없습니다");

        try (GuardScope scope = scopeManager.openScope()) {
            T result = action.get();
            QuerySummary summary = scope.summary();
            List<String> violations = policy.validate(summary);
            if (!violations.isEmpty()) {
                throw new AssertionError(formatMessage(summary, violations));
            }
            return result;
        }
    }

    private static String formatMessage(QuerySummary summary, List<String> violations) {
        return """
                쿼리 예산을 초과했습니다.
                total=%d, select=%d, maxRepeatedSelect=%d
                %s
                """.formatted(
                summary.totalCount(),
                summary.selectCount(),
                summary.maxRepeatedSelectCount(),
                String.join(System.lineSeparator(), violations)
        ).trim();
    }

}
