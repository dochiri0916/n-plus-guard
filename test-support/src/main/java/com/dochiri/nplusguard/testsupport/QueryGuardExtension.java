package com.dochiri.nplusguard.testsupport;

import com.dochiri.nplusguard.core.policy.GuardPolicy;
import com.dochiri.nplusguard.core.query.QuerySummary;
import com.dochiri.nplusguard.core.scope.GuardScope;
import com.dochiri.nplusguard.core.scope.ThreadLocalGuardScopeManager;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

public final class QueryGuardExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(QueryGuardExtension.class);
    private static final String SCOPE_KEY = "scope";
    private static final String POLICY_KEY = "policy";

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        Optional<QueryBudget> budget = findBudget(context);
        if (budget.isEmpty()) {
            return;
        }

        GuardPolicy policy = toPolicy(budget.orElseThrow());
        ThreadLocalGuardScopeManager scopeManager = SpringExtension.getApplicationContext(context)
                .getBean(ThreadLocalGuardScopeManager.class);

        GuardScope scope = scopeManager.openScope();
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        store.put(SCOPE_KEY, scope);
        store.put(POLICY_KEY, policy);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        GuardScope scope = store.remove(SCOPE_KEY, GuardScope.class);
        GuardPolicy policy = store.remove(POLICY_KEY, GuardPolicy.class);
        if (scope == null || policy == null) {
            return;
        }

        try (scope) {
            if (context.getExecutionException().isPresent()) {
                return;
            }

            QuerySummary summary = scope.summary();
            List<String> violations = policy.validate(summary);
            if (!violations.isEmpty()) {
                throw new AssertionError(formatMessage(summary, violations));
            }
        }
    }

    private static Optional<QueryBudget> findBudget(ExtensionContext context) {
        QueryBudget methodBudget = context.getRequiredTestMethod().getAnnotation(QueryBudget.class);
        if (methodBudget != null) {
            return Optional.of(methodBudget);
        }

        Class<?> testClass = context.getRequiredTestClass();
        QueryBudget classBudget = testClass.getAnnotation(QueryBudget.class);
        return Optional.ofNullable(classBudget);
    }

    private static GuardPolicy toPolicy(QueryBudget budget) {
        return new GuardPolicy(
                budget.maxTotalQueries(),
                budget.maxSelectQueries(),
                budget.maxRepeatedSelectExecutions()
        );
    }

    private static String formatMessage(QuerySummary summary, List<String> violations) {
        return """
                @QueryBudget 제한을 초과했습니다.
                total=%d, select=%d, maxRepeatedSelect=%d
                %s
                """.formatted(
                summary.totalCount(),
                summary.selectCount(),
                summary.maxRepeatedSelectCount(),
                String.join(System.lineSeparator(), violations)
        ).trim();
    }

}
