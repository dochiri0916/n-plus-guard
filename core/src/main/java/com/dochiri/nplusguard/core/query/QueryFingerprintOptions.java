package com.dochiri.nplusguard.core.query;

public record QueryFingerprintOptions(
        boolean stripAliases,
        boolean collapseWhitespace
) {

    public QueryFingerprintOptions {
    }

    public static QueryFingerprintOptions defaults() {
        return new QueryFingerprintOptions(true, true);
    }
}
