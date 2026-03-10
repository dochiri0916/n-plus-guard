package com.dochiri.nplusguard.core.policy;

import com.dochiri.nplusguard.core.query.DefaultQueryFingerprintStrategy;
import com.dochiri.nplusguard.core.query.DefaultQueryNormalizer;
import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.query.QueryOrigin;
import com.dochiri.nplusguard.core.scope.GuardScope;
import com.dochiri.nplusguard.core.scope.GuardScopeSnapshot;
import com.dochiri.nplusguard.core.scope.ThreadLocalGuardScopeManager;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardPolicyEvaluatorTest {

    private final DefaultQueryNormalizer queryNormalizer = new DefaultQueryNormalizer();
    private final DefaultQueryFingerprintStrategy fingerprintStrategy = new DefaultQueryFingerprintStrategy();
    private final GuardPolicyEvaluator policyEvaluator = new GuardPolicyEvaluator();

    @Test
    void failsWhenSelectBudgetAndRepeatedSelectBudgetAreExceeded() {
        GuardScopeSnapshot snapshot = snapshotWithQueries(
                "select m.id, m.name from members m where m.id = 1",
                "select m1_0.id, m1_0.name from members m1_0 where m1_0.id = 2",
                "select m2_0.id, m2_0.name from members m2_0 where m2_0.id = 3",
                "select count(*) from members"
        );

        GuardPolicy policy = GuardPolicy.builder()
                .name("member-fetch")
                .mode(GuardMode.FAIL)
                .maxTotalQueries(3)
                .maxSelectQueries(2)
                .maxRepeatedSelectExecutions(1)
                .addExcludedSqlPattern("select count\\(\\*\\).*")
                .build();

        GuardDecision decision = policyEvaluator.evaluate(snapshot, policy);

        assertEquals(GuardDecisionStatus.FAIL, decision.status());
        assertEquals(3, decision.summary().totalCount());
        assertEquals(2, decision.violations().size());
        assertEquals(3, decision.summary().maxRepeatedSelectCount());
        assertTrue(decision.violations().stream().anyMatch(violation -> violation.code().equals("SELECT_QUERIES_EXCEEDED")));
        assertTrue(decision.violations().stream().anyMatch(violation -> violation.code().equals("REPEATED_SELECTS_EXCEEDED")));
    }

    @Test
    void downgradesViolationsToWarningInWarnMode() {
        GuardScopeSnapshot snapshot = snapshotWithQueries(
                "select * from orders where id = 1",
                "select * from orders where id = 2"
        );

        GuardPolicy policy = GuardPolicy.builder()
                .name("warn-only")
                .mode(GuardMode.WARN)
                .maxSelectQueries(1)
                .build();

        GuardDecision decision = policyEvaluator.evaluate(snapshot, policy);

        assertEquals(GuardDecisionStatus.WARN, decision.status());
        assertEquals(1, decision.violations().size());
    }

    private GuardScopeSnapshot snapshotWithQueries(String... sqlStatements) {
        ThreadLocalGuardScopeManager scopeManager = new ThreadLocalGuardScopeManager(() -> "scope-1");
        GuardScope scope = scopeManager.openScope("test-scope");
        for (String sqlStatement : sqlStatements) {
            scope.record(QueryEvent.capture(
                    sqlStatement,
                    Duration.ofMillis(3),
                    scope.id(),
                    QueryOrigin.JDBC,
                    null,
                    null,
                    queryNormalizer,
                    fingerprintStrategy
            ));
        }

        GuardScopeSnapshot snapshot = scope.snapshot();
        scope.close();
        return snapshot;
    }
}
