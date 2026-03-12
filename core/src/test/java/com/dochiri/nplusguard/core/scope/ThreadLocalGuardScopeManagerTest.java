package com.dochiri.nplusguard.core.scope;

import com.dochiri.nplusguard.core.query.QueryEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadLocalGuardScopeManagerTest {

    @Test
    void opensSingleActiveScopeAndClearsItOnClose() {
        ThreadLocalGuardScopeManager scopeManager = new ThreadLocalGuardScopeManager();

        GuardScope scope = scopeManager.openScope();
        assertTrue(scopeManager.currentScope().isPresent());
        assertThrows(IllegalStateException.class, scopeManager::openScope);

        scope.close();
        assertFalse(scopeManager.currentScope().isPresent());
    }

    @Test
    void recordsQueryEventsIntoTheActiveScope() {
        ThreadLocalGuardScopeManager scopeManager = new ThreadLocalGuardScopeManager();

        try (GuardScope scope = scopeManager.openScope()) {
            scope.record(queryEvent("select * from members where id = 1"));
            scope.record(queryEvent("update members set name = 'neo' where id = 1"));

            assertEquals(2, scope.summary().totalCount());
            assertEquals(1, scope.summary().selectCount());
            assertEquals(1, scope.summary().updateCount());
        }
    }

    private QueryEvent queryEvent(String sql) {
        return QueryEvent.capture(sql);
    }
}
