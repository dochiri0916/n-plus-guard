package com.dochiri.nplusguard.core.query;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    OTHER;

    private static final Pattern LEADING_QUERY_TYPE =
            Pattern.compile("\\b(select|insert|update|delete)\\b");

    public static QueryType fromSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return OTHER;
        }

        String normalized = sql.strip().toLowerCase(Locale.ROOT);
        Matcher matcher = LEADING_QUERY_TYPE.matcher(normalized);
        if (!matcher.find()) {
            return OTHER;
        }

        return switch (matcher.group(1)) {
            case "select" -> SELECT;
            case "insert" -> INSERT;
            case "update" -> UPDATE;
            case "delete" -> DELETE;
            default -> OTHER;
        };
    }
}
