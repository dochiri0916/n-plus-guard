package com.dochiri.nplusguard.core.scope;

import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.query.QuerySummary;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class ThreadLocalGuardScopeManager implements GuardScopeManager {

    private final ThreadLocal<Deque<ManagedGuardScope>> scopeStack = ThreadLocal.withInitial(ArrayDeque::new);
    private final Supplier<String> scopeIdGenerator;

    public ThreadLocalGuardScopeManager() {
        this(() -> UUID.randomUUID().toString());
    }

    public ThreadLocalGuardScopeManager(Supplier<String> scopeIdGenerator) {
        this.scopeIdGenerator = Objects.requireNonNull(scopeIdGenerator, "scopeIdGenerator must not be null");
    }

    @Override
    public GuardScope openScope(String name) {
        String scopeName = (name == null || name.isBlank()) ? "guard-scope" : name;
        ManagedGuardScope scope = new ManagedGuardScope(scopeIdGenerator.get(), scopeName, Instant.now());
        scopeStack.get().push(scope);
        return scope;
    }

    @Override
    public Optional<GuardScope> currentScope() {
        return Optional.ofNullable(scopeStack.get().peek());
    }

    private void close(ManagedGuardScope scope) {
        Deque<ManagedGuardScope> stack = scopeStack.get();
        if (stack.isEmpty() || stack.peek() != scope) {
            throw new IllegalStateException("guard scopes must be closed in LIFO order");
        }

        stack.pop();
        scope.markClosed(Instant.now());

        if (stack.isEmpty()) {
            scopeStack.remove();
        }
    }

    private final class ManagedGuardScope implements GuardScope {
        private final String id;
        private final String name;
        private final Instant startedAt;
        private final List<QueryEvent> queryEvents = Collections.synchronizedList(new ArrayList<>());
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private volatile Instant closedAt;

        private ManagedGuardScope(String id, String name, Instant startedAt) {
            this.id = Objects.requireNonNull(id, "id must not be null");
            this.name = Objects.requireNonNull(name, "name must not be null");
            this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Instant startedAt() {
            return startedAt;
        }

        @Override
        public boolean closed() {
            return closed.get();
        }

        @Override
        public void record(QueryEvent queryEvent) {
            Objects.requireNonNull(queryEvent, "queryEvent must not be null");
            if (!id.equals(queryEvent.scopeId())) {
                throw new IllegalArgumentException("queryEvent scopeId does not match the active scope");
            }
            if (closed()) {
                throw new IllegalStateException("cannot record a queryEvent into a closed scope");
            }
            queryEvents.add(queryEvent);
        }

        @Override
        public GuardScopeSnapshot snapshot() {
            List<QueryEvent> eventSnapshot;
            synchronized (queryEvents) {
                eventSnapshot = List.copyOf(queryEvents);
            }
            Instant endedAt = closedAt == null ? Instant.now() : closedAt;
            return new GuardScopeSnapshot(id, name, startedAt, endedAt, eventSnapshot, QuerySummary.from(eventSnapshot));
        }

        @Override
        public void close() {
            if (!closed()) {
                ThreadLocalGuardScopeManager.this.close(this);
            }
        }

        private void markClosed(Instant closedAt) {
            closed.compareAndSet(false, true);
            this.closedAt = Objects.requireNonNull(closedAt, "closedAt must not be null");
        }
    }
}
