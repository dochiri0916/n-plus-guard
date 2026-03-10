package com.dochiri.nplusguard.core.query;

public record QueryNormalizationOptions(
        boolean stripComments,
        boolean replaceStringLiterals,
        boolean replaceNumericLiterals,
        boolean collapseInLists,
        boolean lowercase,
        boolean collapseWhitespace
) {

    public QueryNormalizationOptions {
    }

    public static QueryNormalizationOptions defaults() {
        return new QueryNormalizationOptions(true, true, true, true, true, true);
    }
}
