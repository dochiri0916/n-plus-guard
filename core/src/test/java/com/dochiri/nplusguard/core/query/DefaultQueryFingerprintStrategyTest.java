package com.dochiri.nplusguard.core.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultQueryFingerprintStrategyTest {

    private final DefaultQueryFingerprintStrategy fingerprintStrategy = new DefaultQueryFingerprintStrategy();

    @Test
    void stripsGeneratedAliasesFromEquivalentQueries() {
        String firstQuery = "select o.id, o.status from orders o where o.member_id = ?";
        String secondQuery = "select o1_0.id, o1_0.status from orders o1_0 where o1_0.member_id = ?";

        assertEquals(
                fingerprintStrategy.fingerprint(firstQuery),
                fingerprintStrategy.fingerprint(secondQuery)
        );
        assertEquals(
                "select id, status from orders where member_id = ?",
                fingerprintStrategy.fingerprint(firstQuery)
        );
    }
}
