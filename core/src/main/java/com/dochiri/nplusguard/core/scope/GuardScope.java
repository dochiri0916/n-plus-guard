package com.dochiri.nplusguard.core.scope;

import com.dochiri.nplusguard.core.query.QueryEvent;
import com.dochiri.nplusguard.core.query.QuerySummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public final class GuardScope implements AutoCloseable {

    private final Consumer<GuardScope> onClose;
    private final List<QueryEvent> queryEvents = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean closed = new AtomicBoolean(false);

    GuardScope(Consumer<GuardScope> onClose) {
        this.onClose = requireNonNull(onClose, "onClose는 null일 수 없습니다");
    }

    public boolean closed() {
        return closed.get();
    }

    public void record(QueryEvent queryEvent) {
        requireNonNull(queryEvent, "queryEvent는 null일 수 없습니다");
        if (closed()) {
            throw new IllegalStateException("닫힌 scope에는 queryEvent를 기록할 수 없습니다");
        }
        queryEvents.add(queryEvent);
    }

    public QuerySummary summary() {
        synchronized (queryEvents) {
            return QuerySummary.from(List.copyOf(queryEvents));
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            onClose.accept(this);
        }
    }

}
