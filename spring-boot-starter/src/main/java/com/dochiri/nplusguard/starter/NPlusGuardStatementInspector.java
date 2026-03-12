package com.dochiri.nplusguard.starter;

import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.scope.ThreadLocalGuardScopeManager;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

public final class NPlusGuardStatementInspector implements StatementInspector {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ThreadLocalGuardScopeManager scopeManager;

    public NPlusGuardStatementInspector(ThreadLocalGuardScopeManager scopeManager) {
        this.scopeManager = requireNonNull(scopeManager, "scopeManager는 null일 수 없습니다");
    }

    @Override
    public String inspect(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        scopeManager.currentScope().ifPresent(scope -> scope.record(QueryEvent.capture(sql)));
        return sql;
    }

}
