package com.dochiri.nplusguard.core.scope;

import java.util.Objects;
import java.util.Optional;

public final class ThreadLocalGuardScopeManager {

    private final ThreadLocal<GuardScope> currentScope = new ThreadLocal<>();

    public GuardScope openScope() {
        if (currentScope.get() != null) {
            throw new IllegalStateException("이미 활성화된 scope가 있습니다");
        }

        GuardScope scope = new GuardScope(this::clear);
        currentScope.set(scope);
        return scope;
    }

    public Optional<GuardScope> currentScope() {
        return Optional.ofNullable(currentScope.get());
    }

    private void clear(GuardScope scope) {
        GuardScope activeScope = currentScope.get();
        if (!Objects.equals(activeScope, scope)) {
            throw new IllegalStateException("현재 활성 scope와 닫으려는 scope가 다릅니다");
        }
        currentScope.remove();
    }

}
