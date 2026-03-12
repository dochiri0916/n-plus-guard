package com.dochiri.nplusguard.core.query;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DefaultQueryNormalizer {

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("(?m)--.*$");
    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:''|[^'])*'");
    private static final Pattern NUMERIC_LITERAL = Pattern.compile("(?<![\\w.])-?\\d+(?:\\.\\d+)?(?![\\w.])");
    private static final Pattern TRAILING_SEMICOLONS = Pattern.compile(";+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern COLLAPSIBLE_IN_LIST =
            Pattern.compile("\\bin\\s*\\((?:\\s*\\?(?:\\s*,\\s*\\?)+\\s*)\\)");

    private DefaultQueryNormalizer() {
    }

    public static String normalize(String sql) {
        if (sql == null || sql.isBlank()) {
            return "";
        }

        String normalized = sql.strip();

        normalized = BLOCK_COMMENT.matcher(normalized).replaceAll(" ");
        normalized = LINE_COMMENT.matcher(normalized).replaceAll(" ");

        normalized = TRAILING_SEMICOLONS.matcher(normalized).replaceAll("");

        normalized = STRING_LITERAL.matcher(normalized).replaceAll("?");
        normalized = NUMERIC_LITERAL.matcher(normalized).replaceAll("?");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").trim();
        normalized = COLLAPSIBLE_IN_LIST.matcher(normalized).replaceAll("in (?)");
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").trim();

        return normalized;
    }
}
