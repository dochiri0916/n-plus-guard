package com.dochiri.nplusguard.core.policy;

import com.dochiri.nplusguard.core.query.QuerySummary;

import java.util.List;
import java.util.Objects;

public record GuardDecision(
        GuardDecisionStatus status,
        String scopeId,
        String scopeName,
        GuardPolicy policy,
        QuerySummary summary,
        List<PolicyViolation> violations
) {

    public GuardDecision {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(scopeName, "scopeName must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public boolean passed() {
        return status == GuardDecisionStatus.PASS;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }
}
