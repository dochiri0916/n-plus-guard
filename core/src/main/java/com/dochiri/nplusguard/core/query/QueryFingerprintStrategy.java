package com.dochiri.nplusguard.core.query;

@FunctionalInterface
public interface QueryFingerprintStrategy {

    String fingerprint(String normalizedSql);
}
