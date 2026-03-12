package com.dochiri.nplusguard.core.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultQueryNormalizerTest {

    @Test
    void normalizesWhitespaceCommentsAndLiteralValues() {
        String sql = """
                SELECT o.id, o.status
                FROM orders o
                -- fetch one order
                WHERE o.member_id = 42
                  AND o.status = 'READY'
                  AND o.id IN (1, 2, 3);
                """;

        String normalized = DefaultQueryNormalizer.normalize(sql);

        assertEquals(
                "select o.id, o.status from orders o where o.member_id = ? and o.status = ? and o.id in (?)",
                normalized
        );
    }
}
