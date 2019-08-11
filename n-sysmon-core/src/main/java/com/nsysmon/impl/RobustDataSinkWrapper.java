package com.nsysmon.impl;

import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class limits the impact on N-SysMon itself if some data sink runs into problems or uses large amounts of resources
 *
 * @author arno
 */
class RobustDataSinkWrapper { //TO_CONSIDER provide a means for a data sink to specify its own timeout? --> dumping...
    private static final NSysMonLogger log = NSysMonLogger.get(RobustDataSinkWrapper.class);

    private final ADataSink inner;

    private final long timeoutNanos;
    private final int maxNumTimeouts;

    private final AtomicInteger numTimeouts = new AtomicInteger(0);
    private final AtomicInteger numExceptions = new AtomicInteger(0);

    private interface Strategy {
        void onStartedHierarchicalMeasurement(String identifier);
        void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data);
    }

    private final Strategy ENABLED = new Strategy() {
        @Override public void onStartedHierarchicalMeasurement(String identifier) {
            try {
                final long start = System.nanoTime();
                inner.onStartedHierarchicalMeasurement(identifier);
                handleDuration(System.nanoTime() - start);
                numExceptions.set(0);
            } catch (Exception e) {
                if (numExceptions.incrementAndGet() > maxNumTimeouts) {
                    log.warn("Disabling data sink " + inner.getClass().getName() + " because an exception occurred " + maxNumTimeouts + " times in row", e);
                    strategy = DISABLED;
                }
            }
        }

        @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
            try {
                final long start = System.nanoTime();
                inner.onFinishedHierarchicalMeasurement(data);
                handleDuration(System.nanoTime() - start);
                numExceptions.set(0);
            } catch (Exception e) {
                if (numExceptions.incrementAndGet() > maxNumTimeouts) {
                    log.warn("Disabling data sink " + inner.getClass().getName() + " because an exception occurred " + maxNumTimeouts + " times in row", e);
                    strategy = DISABLED;
                }
            }
        }

        private void handleDuration(long durationNanos) {
            if(durationNanos > timeoutNanos) {
                log.warn("Data sink " + inner.getClass().getName() + " timed out (took " + durationNanos + "ns)");
                numTimeouts.incrementAndGet();
                strategy = TIMED_OUT;
            }
        }
    };

    private final Strategy TIMED_OUT = new Strategy() {
        @Override public void onStartedHierarchicalMeasurement(String identifier) {
            try {
                final long start = System.nanoTime();
                inner.onStartedHierarchicalMeasurement(identifier);
                handleDuration(System.nanoTime() - start);
                numExceptions.set(0);
            } catch (Exception e) {
                if (numExceptions.incrementAndGet() > maxNumTimeouts) {
                    log.warn("Disabling data sink " + inner.getClass().getName() + " because an exception occurred " + maxNumTimeouts + " times in row", e);
                    strategy = DISABLED;
                }
            }
        }

        @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
            try {
                final long start = System.nanoTime();
                inner.onFinishedHierarchicalMeasurement(data);
                handleDuration(System.nanoTime() - start);
                numExceptions.set(0);
            } catch (Exception e) {
                if (numExceptions.incrementAndGet() > maxNumTimeouts) {
                    log.warn("Disabling data sink " + inner.getClass().getName() + " because an exception occurred " + maxNumTimeouts + " times in row", e);
                    strategy = DISABLED;
                }
            }
        }

        private void handleDuration(long durationNanos) {
            if(durationNanos > timeoutNanos) {
                log.warn("Data sink " + inner.getClass().getName() + " timed out (took " + durationNanos + "ns)");
                if(numTimeouts.incrementAndGet() >= maxNumTimeouts) {
                    log.warn("Data Sink " + inner.getClass().getName() + " timed out " + maxNumTimeouts + " times in row - permanently disabling");
                    strategy = DISABLED;
                }
            } else {
                numTimeouts.set(0);
                strategy = ENABLED;
            }
        }
    };

    private final Strategy DISABLED = new Strategy() {
        @Override public void onStartedHierarchicalMeasurement(String identifier) {
        }

        @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        }
    };

    private volatile Strategy strategy = ENABLED;

    RobustDataSinkWrapper(ADataSink inner, long timeoutNanos, int maxNumTimeouts) {
        this.inner = inner;
        this.timeoutNanos = timeoutNanos;
        this.maxNumTimeouts = maxNumTimeouts;
    }

    public void onStartedHierarchicalMeasurement(String identifier) {
        strategy.onStartedHierarchicalMeasurement(identifier);
    }

    public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        strategy.onFinishedHierarchicalMeasurement(data);
    }

    public void shutdown() {
        try {
            inner.shutdown();
        } catch (Exception exc) {
            log.error("failed to shut down data sink " + inner.getClass().getName() + ".", exc);
        }
    }
}
