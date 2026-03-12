package com.dochiri.nplusguard.core.policy;

import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.query.QuerySummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardPolicyTest {

    @Test
    void reportsViolationsWhenQueryBudgetsAreExceeded() {
        QuerySummary summary = summaryWithQueries(
                "select m.id, m.name from members m where m.id = 1",
                "select m1_0.id, m1_0.name from members m1_0 where m1_0.id = 2",
                "select m2_0.id, m2_0.name from members m2_0 where m2_0.id = 3"
        );

        GuardPolicy policy = new GuardPolicy(2, 2, 1);

        List<String> violations = policy.validate(summary);

        assertEquals(3, summary.totalCount());
        assertEquals(3, summary.maxRepeatedSelectCount());
        assertEquals(3, violations.size());
        assertTrue(violations.stream().anyMatch(message -> message.contains("전체 쿼리 수")));
        assertTrue(violations.stream().anyMatch(message -> message.contains("SELECT 쿼리 수")));
        assertTrue(violations.stream().anyMatch(message -> message.contains("반복 SELECT 실행 횟수")));
    }

    @Test
    void returnsNoViolationsWhenSummaryIsWithinLimits() {
        QuerySummary summary = summaryWithQueries(
                "select * from orders where id = 1",
                "update orders set status = 'PAID' where id = 1"
        );

        GuardPolicy policy = new GuardPolicy(2, 1, 1);

        assertTrue(policy.isSatisfiedBy(summary));
        assertTrue(policy.validate(summary).isEmpty());
    }

    @Test
    void throwsWhenSummaryViolatesThePolicy() {
        QuerySummary summary = summaryWithQueries(
                "select * from orders where id = 1",
                "select * from orders where id = 2"
        );

        GuardPolicy policy = new GuardPolicy(0, 1, 1);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> policy.check(summary));

        assertFalse(exception.getMessage().isBlank());
        assertTrue(exception.getMessage().contains("SELECT 쿼리 수가 제한을 초과했습니다"));
    }

    private QuerySummary summaryWithQueries(String... sqlStatements) {
        List<QueryEvent> events = java.util.Arrays.stream(sqlStatements)
                .map(QueryEvent::capture)
                .toList();
        return QuerySummary.from(events);
    }
}
