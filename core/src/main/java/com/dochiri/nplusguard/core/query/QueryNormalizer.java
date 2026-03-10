package com.dochiri.nplusguard.core.query;

@FunctionalInterface
public interface QueryNormalizer {

    String normalize(String sql);
}
