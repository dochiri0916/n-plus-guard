package com.dochiri.nplusguard.core.scope;

import com.dochiri.nplusguard.core.query.DefaultQueryFingerprintStrategy;
import com.dochiri.nplusguard.core.query.DefaultQueryNormalizer;
import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.query.QueryOrigin;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadLocalGuardScopeManagerTest {

    private final DefaultQueryNormalizer queryNormalizer = new DefaultQueryNormalizer();
    private final DefaultQueryFingerprintStrategy fingerprintStrategy = new DefaultQueryFingerprintStrategy();

    @Test
    void opensNestedScopesAndRestoresParentScopeOnClose() {
        ThreadLocalGuardScopeManager scopeManager = new ThreadLocalGuardScopeManager(new SequenceScopeIdGenerator());

        GuardScope parentScope = scopeManager.openScope("parent");
        assertEquals("scope-1", parentScope.id());
        assertTrue(scopeManager.currentScope().isPresent());

        GuardScope childScope = scopeManager.openScope("child");
        childScope.record(queryEvent(childScope.id(), "select * from members where id = 1"));
        assertEquals("scope-2", scopeManager.currentScope().orElseThrow().id());

        childScope.close();

        assertEquals("scope-1", scopeManager.currentScope().orElseThrow().id());
        assertEquals(0, parentScope.snapshot().summary().totalCount());
        assertEquals(1, childScope.snapshot().summary().totalCount());

        parentScope.close();
        assertFalse(scopeManager.currentScope().isPresent());
    }

    @Test
    void recordsQueryEventsIntoTheActiveScope() {
        ThreadLocalGuardScopeManager scopeManager = new ThreadLocalGuardScopeManager(() -> "scope-1");

        try (GuardScope scope = scopeManager.openScope("test-scope")) {
            scope.record(queryEvent(scope.id(), "select * from members where id = 1"));
            scope.record(queryEvent(scope.id(), "update members set name = 'neo' where id = 1"));

            GuardScopeSnapshot snapshot = scope.snapshot();

            assertEquals("test-scope", snapshot.scopeName());
            assertEquals(2, snapshot.summary().totalCount());
            assertEquals(1, snapshot.summary().selectCount());
            assertEquals(1, snapshot.summary().updateCount());
        }
    }

    private QueryEvent queryEvent(String scopeId, String sql) {
        return QueryEvent.capture(
                sql,
                Duration.ofMillis(5),
                scopeId,
                QueryOrigin.JDBC,
                null,
                null,
                queryNormalizer,
                fingerprintStrategy
        );
    }

    private static final class SequenceScopeIdGenerator implements java.util.function.Supplier<String> {
        private int nextId = 1;

        @Override
        public String get() {
            return "scope-" + nextId++;
        }
    }
}
