package com.dochiri.nplusguard.core.policy;

import com.dochiri.nplusguard.core.query.RepeatedQuery;

public final class GuardReportFormatter {

    public String format(GuardDecision decision) {
        StringBuilder report = new StringBuilder();
        report.append("Query guard ")
                .append(decision.status())
                .append(" for scope '")
                .append(decision.scopeName())
                .append("'");

        if (decision.hasViolations()) {
            report.append(System.lineSeparator()).append("Violations:");
            for (PolicyViolation violation : decision.violations()) {
                report.append(System.lineSeparator())
                        .append("- ")
                        .append(violation.code())
                        .append(": actual=")
                        .append(violation.actual())
                        .append(", limit=")
                        .append(violation.limit());
            }
        }

        report.append(System.lineSeparator())
                .append("Summary: total=")
                .append(decision.summary().totalCount())
                .append(", select=")
                .append(decision.summary().selectCount())
                .append(", insert=")
                .append(decision.summary().insertCount())
                .append(", update=")
                .append(decision.summary().updateCount())
                .append(", delete=")
                .append(decision.summary().deleteCount())
                .append(", other=")
                .append(decision.summary().otherCount());

        if (!decision.summary().repeatedSelects().isEmpty()) {
            report.append(System.lineSeparator()).append("Repeated SELECT patterns:");
            for (RepeatedQuery repeatedQuery : decision.summary().repeatedSelects()) {
                report.append(System.lineSeparator())
                        .append("- ")
                        .append(repeatedQuery.executions())
                        .append("x ")
                        .append(repeatedQuery.normalizedSql());
            }
        }

        return report.toString();
    }
}
