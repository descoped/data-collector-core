package io.descoped.dc.core.handler;

import io.descoped.dc.api.PositionObserver;
import io.descoped.dc.api.Termination;
import io.descoped.dc.core.health.HealthWorkerMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class PrefetchAlgorithm {

    static final Logger LOG = LoggerFactory.getLogger(PrefetchAlgorithm.class);

    final int prefetchThreshold;
    final Runnable prefetchRunnable;
    final Termination termination;
    final HealthWorkerMonitor monitor;

    final AtomicLong expectedPositionCounter = new AtomicLong();
    final AtomicLong positionCompletedCounter = new AtomicLong();
    final AtomicLong pendingPrefetches = new AtomicLong(1);

    public PrefetchAlgorithm(int prefetchThreshold, Runnable prefetchRunnable, Termination termination, HealthWorkerMonitor monitor) {
        this.prefetchThreshold = prefetchThreshold;
        this.prefetchRunnable = prefetchRunnable;
        this.termination = termination;
        this.monitor = monitor;
    }

    public PositionObserver getPositionObserver() {
        return new PositionObserver(expectedConsumerFunction(), completedConsumerFunction());
    }

    private Consumer<Integer> expectedConsumerFunction() {
        return expectedCount -> {
            long total = expectedPositionCounter.addAndGet(expectedCount);
            if (LOG.isTraceEnabled()) {
                LOG.trace("expected observed: added={}, total={}", expectedCount, total);
            }

            if (monitor != null) {
                monitor.request().updateTotalExpectedCount(expectedCount);
            }

            long countAfterDecrement = pendingPrefetches.decrementAndGet();
            if (countAfterDecrement < 0) {
                throw new IllegalStateException("count-after-decrement < 0");
            }

            prefetchIfBelowThreshold(expectedPositionCounter.get() - positionCompletedCounter.get());
        };
    }

    private Consumer<Integer> completedConsumerFunction() {
        return completedCount -> {
            long totalCompletedCount = positionCompletedCounter.addAndGet(completedCount);
            if (LOG.isTraceEnabled()) {
                LOG.trace("completed observed: added={}, total={}", completedCount, totalCompletedCount);
            }

            if (monitor != null) {
                monitor.request().updateTotalCompletedCount(completedCount);
            }

            if (pendingPrefetches.get() > 0) {
                return; // threshold will be checked when expected counter is increased
            }

            prefetchIfBelowThreshold(expectedPositionCounter.get() - totalCompletedCount);
        };
    }

    private void prefetchIfBelowThreshold(long pendingPositions) {
        if (pendingPositions < prefetchThreshold) {

            if (!pendingPrefetches.compareAndSet(0, 1)) {
                return; // a concurrent thread won the race to start a pre-fetch
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("Pre-fetching next-page...");
            }

            prefetchRunnable.run();
        }
    }
}
