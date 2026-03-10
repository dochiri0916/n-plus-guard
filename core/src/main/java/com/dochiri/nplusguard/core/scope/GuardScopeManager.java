package com.dochiri.nplusguard.core.scope;

import java.util.Optional;

public interface GuardScopeManager {

    GuardScope openScope(String name);

    Optional<GuardScope> currentScope();
}
