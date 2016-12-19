package com.nsysmon.impl;

import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.HierarchicalParentInfo;

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

    private interface Strategy {
        void onStartedHierarchicalMeasurement(String identifier);

        void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data);

        void onWorkingStep(AHierarchicalDataRoot trace, HierarchicalParentInfo parentInfo);

        void handleDuration(long durationNanos);
    }

    private void commonOnStartedHierarchicalMeasurement(String identifier, final Strategy myStrategy) {
        try {
            final long start = System.nanoTime();
            inner.onStartedHierarchicalMeasurement(identifier);
            myStrategy.handleDuration(System.nanoTime() - start);
        } catch (Exception e) {
            log.warn("Disabling data sink " + inner.getClass().getName() + " because an exception occurred", e);
            strategy = DISABLED;
        }
    }

    private void commonOnFinishedHierarchicalMeasurement(AHierarchicalDataRoot data, final Strategy myStrategy) {
        try {
            final long start = System.nanoTime();
            inner.onFinishedHierarchicalMeasurement(data);
            myStrategy.handleDuration(System.nanoTime() - start);
        } catch (Exception e) {
            log.warn("Disabling data sink " + inner.getClass().getName() + " because an exception occurred", e);
            strategy = DISABLED;
        }
    }

    private void commonOnWorkingStep(AHierarchicalDataRoot data, final Strategy myStrategy, HierarchicalParentInfo parentInfo) {
        try {
            final long start = System.nanoTime();
            inner.onWorkingStep(data, parentInfo);
            myStrategy.handleDuration(System.nanoTime() - start);
        } catch (Exception e) {
            log.warn("Disabling data sink " + inner.getClass().getName() + " because an exception occurred", e);
            strategy = DISABLED;
        }
    }


    private final Strategy ENABLED = new Strategy() {
        @Override
        public void onStartedHierarchicalMeasurement(String identifier) {
            commonOnStartedHierarchicalMeasurement(identifier, this);
        }

        @Override
        public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
            commonOnFinishedHierarchicalMeasurement(data, this);
        }

        @Override
        public void onWorkingStep(AHierarchicalDataRoot data, HierarchicalParentInfo parentInfo) {
            commonOnWorkingStep(data, this, parentInfo);
        }

        @Override
        public void handleDuration(long durationNanos) {
            if (durationNanos > timeoutNanos) {
                log.warn("Data sink " + inner.getClass().getName() + " timed out (took " + durationNanos + "ns)");
                numTimeouts.incrementAndGet();
                strategy = TIMED_OUT;
            }
        }
    };

    private final Strategy TIMED_OUT = new Strategy() {
        @Override
        public void onStartedHierarchicalMeasurement(String identifier) {
            commonOnStartedHierarchicalMeasurement(identifier, this);
        }

        @Override
        public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
            commonOnFinishedHierarchicalMeasurement(data, this);
        }

        @Override
        public void onWorkingStep(AHierarchicalDataRoot data, HierarchicalParentInfo parentInfo) {
            commonOnWorkingStep(data, this, parentInfo);
        }

        @Override
        public void handleDuration(long durationNanos) {
            if (durationNanos > timeoutNanos) {
                if (numTimeouts.incrementAndGet() >= maxNumTimeouts) {
                    log.warn("Data Sink " + inner.getClass().getName() + " timed out " + maxNumTimeouts + " times in row - permanently disabling");
                    strategy = DISABLED;
                } else {
                    strategy = ENABLED;
                }
            }
        }
    };

    private final Strategy DISABLED = new Strategy() {
        @Override
        public void onStartedHierarchicalMeasurement(String identifier) {
        }

        @Override
        public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        }

        @Override
        public void onWorkingStep(AHierarchicalDataRoot trace, HierarchicalParentInfo parentInfo) {
        }

        @Override
        public void handleDuration(long durationNanos) {
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

    public void onWorkingStep(AHierarchicalDataRoot data, HierarchicalParentInfo parentInfo) {
        strategy.onWorkingStep(data, parentInfo);
    }

    public void shutdown() {
        try {
            inner.shutdown();
        } catch (Exception exc) {
            log.error("failed to shut down data sink " + inner.getClass().getName() + ".", exc);
        }
    }
}
