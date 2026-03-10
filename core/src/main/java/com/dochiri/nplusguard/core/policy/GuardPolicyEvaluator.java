package com.dochiri.nplusguard.core.policy;

import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.query.QuerySummary;
import com.dochiri.nplusguard.core.scope.GuardScopeSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class GuardPolicyEvaluator {

    private final ConcurrentHashMap<String, Pattern> excludedPatternCache = new ConcurrentHashMap<>();

    public GuardDecision evaluate(GuardScopeSnapshot snapshot, GuardPolicy policy) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        List<QueryEvent> filteredEvents = snapshot.queryEvents().stream()
                .filter(queryEvent -> !isExcluded(queryEvent, policy))
                .toList();

        QuerySummary summary = QuerySummary.from(filteredEvents);
        List<PolicyViolation> violations = evaluateViolations(summary, policy);

        GuardDecisionStatus status = determineStatus(policy.mode(), violations);
        return new GuardDecision(status, snapshot.scopeId(), snapshot.scopeName(), policy, summary, violations);
    }

    private List<PolicyViolation> evaluateViolations(QuerySummary summary, GuardPolicy policy) {
        List<PolicyViolation> violations = new ArrayList<>();

        if (policy.hasTotalQueryLimit() && summary.totalCount() > policy.maxTotalQueries()) {
            violations.add(new PolicyViolation(
                    "TOTAL_QUERIES_EXCEEDED",
                    "total queries exceeded the configured limit",
                    summary.totalCount(),
                    policy.maxTotalQueries()
            ));
        }

        if (policy.hasSelectQueryLimit() && summary.selectCount() > policy.maxSelectQueries()) {
            violations.add(new PolicyViolation(
                    "SELECT_QUERIES_EXCEEDED",
                    "select queries exceeded the configured limit",
                    summary.selectCount(),
                    policy.maxSelectQueries()
            ));
        }

        int maxRepeatedSelectCount = summary.maxRepeatedSelectCount();
        if (policy.hasRepeatedSelectLimit() && maxRepeatedSelectCount > policy.maxRepeatedSelectExecutions()) {
            violations.add(new PolicyViolation(
                    "REPEATED_SELECTS_EXCEEDED",
                    "repeated select executions exceeded the configured limit",
                    maxRepeatedSelectCount,
                    policy.maxRepeatedSelectExecutions()
            ));
        }

        return violations;
    }

    private GuardDecisionStatus determineStatus(GuardMode mode, List<PolicyViolation> violations) {
        if (violations.isEmpty() || mode == GuardMode.OFF) {
            return GuardDecisionStatus.PASS;
        }
        return switch (mode) {
            case WARN -> GuardDecisionStatus.WARN;
            case FAIL -> GuardDecisionStatus.FAIL;
            case OFF -> GuardDecisionStatus.PASS;
        };
    }

    private boolean isExcluded(QueryEvent queryEvent, GuardPolicy policy) {
        if (policy.excludedSqlPatterns().isEmpty()) {
            return false;
        }

        for (String excludedSqlPattern : policy.excludedSqlPatterns()) {
            Pattern pattern = excludedPatternCache.computeIfAbsent(excludedSqlPattern, Pattern::compile);
            if (pattern.matcher(queryEvent.normalizedSql()).find() || pattern.matcher(queryEvent.sql()).find()) {
                return true;
            }
        }
        return false;
    }
}
