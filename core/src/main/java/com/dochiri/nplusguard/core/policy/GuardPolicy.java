package com.dochiri.nplusguard.core.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record GuardPolicy(
        String name,
        GuardMode mode,
        int maxTotalQueries,
        int maxSelectQueries,
        int maxRepeatedSelectExecutions,
        List<String> excludedSqlPatterns
) {

    public GuardPolicy {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (maxTotalQueries < 0 || maxSelectQueries < 0 || maxRepeatedSelectExecutions < 0) {
            throw new IllegalArgumentException("guard policy limits must not be negative");
        }
        excludedSqlPatterns = excludedSqlPatterns == null ? List.of() : List.copyOf(excludedSqlPatterns);
    }

    public boolean hasTotalQueryLimit() {
        return maxTotalQueries > 0;
    }

    public boolean hasSelectQueryLimit() {
        return maxSelectQueries > 0;
    }

    public boolean hasRepeatedSelectLimit() {
        return maxRepeatedSelectExecutions > 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name = "guard-policy";
        private GuardMode mode = GuardMode.FAIL;
        private int maxTotalQueries;
        private int maxSelectQueries;
        private int maxRepeatedSelectExecutions;
        private final List<String> excludedSqlPatterns = new ArrayList<>();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder mode(GuardMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder maxTotalQueries(int maxTotalQueries) {
            this.maxTotalQueries = maxTotalQueries;
            return this;
        }

        public Builder maxSelectQueries(int maxSelectQueries) {
            this.maxSelectQueries = maxSelectQueries;
            return this;
        }

        public Builder maxRepeatedSelectExecutions(int maxRepeatedSelectExecutions) {
            this.maxRepeatedSelectExecutions = maxRepeatedSelectExecutions;
            return this;
        }

        public Builder addExcludedSqlPattern(String excludedSqlPattern) {
            this.excludedSqlPatterns.add(excludedSqlPattern);
            return this;
        }

        public Builder excludedSqlPatterns(List<String> excludedSqlPatterns) {
            this.excludedSqlPatterns.clear();
            if (excludedSqlPatterns != null) {
                this.excludedSqlPatterns.addAll(excludedSqlPatterns);
            }
            return this;
        }

        public GuardPolicy build() {
            return new GuardPolicy(
                    name,
                    mode,
                    maxTotalQueries,
                    maxSelectQueries,
                    maxRepeatedSelectExecutions,
                    excludedSqlPatterns
            );
        }
    }
}
