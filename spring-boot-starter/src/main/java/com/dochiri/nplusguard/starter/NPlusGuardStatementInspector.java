package com.dochiri.nplusguard.starter;

import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.scope.ThreadLocalGuardScopeManager;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.io.Serial;
import java.util.Objects;

public final class NPlusGuardStatementInspector implements StatementInspector {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ThreadLocalGuardScopeManager scopeManager;

    public NPlusGuardStatementInspector(ThreadLocalGuardScopeManager scopeManager) {
        this.scopeManager = Objects.requireNonNull(scopeManager, "scopeManager는 null일 수 없습니다");
    }

    @Override
    public String inspect(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }

        // 활성화된 테스트 scope가 있을 때만 SQL을 기록한다.
        scopeManager.currentScope().ifPresent(scope -> scope.record(QueryEvent.capture(sql)));
        return sql;
    }
}
