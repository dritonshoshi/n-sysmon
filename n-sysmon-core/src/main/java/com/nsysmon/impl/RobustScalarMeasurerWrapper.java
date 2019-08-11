package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.measure.scalar.ScalarMeasurerStatus;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class limits the impact on N-SysMon itself if some measurer runs into problems or uses large amounts of resources
 *
 * @author arno
 */
public class RobustScalarMeasurerWrapper {
    private static final NSysMonLogger log = NSysMonLogger.get(RobustScalarMeasurerWrapper.class);

    private final AScalarMeasurer inner;

    private final long timeoutNanos;
    private final int maxNumTimeouts;

    private final AtomicInteger numTimeouts = new AtomicInteger(0);
    private final AtomicInteger numExceptions = new AtomicInteger(0);

    public ScalarMeasurerStatus getStatus(String key) {
        if (!inner.isResponsibleForMeasurement(key)){
            return new ScalarMeasurerStatus(ScalarMeasurerStatus.Status.UNKNOWN);
        }
        if (strategy == DISABLED){
            return new ScalarMeasurerStatus(ScalarMeasurerStatus.Status.STOPPED);
        }
        return new ScalarMeasurerStatus(ScalarMeasurerStatus.Status.RUNNING);
    }

    public boolean restartIfResponsible(String key) {
        if (!inner.isResponsibleForMeasurement(key)){
            return false;
        }
        restart();
        return true;
    }

    private void restart() {
        log.warn("Restarting scalar measurer " + inner.getClass().getName() + " due user request.");
        strategy = ENABLED;
    }

    private interface Strategy {
        void prepareMeasurements(Map<String, Object> mementos);
        void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos);
    }

    private final Strategy ENABLED = new Strategy() {
        @Override public void prepareMeasurements(Map<String, Object> mementos) {
            try {
                final long start = System.nanoTime();
                inner.prepareMeasurements(mementos);
                handleDuration(System.nanoTime() - start, inner.getTimeoutInMilliSeconds());
                numExceptions.set(0);
            } catch (Exception e) {
                if (numExceptions.incrementAndGet() > maxNumTimeouts) {
                    log.warn("Disabling scalar measurer " + inner.getClass().getName() + " because an exception occurred " + maxNumTimeouts + " times in row", e);
                    strategy = DISABLED;
                }
            }
        }

        private void handleDuration(long durationNanos, AOption<Long> timeoutInMilliseconds) {
            final long timeout = timeoutInMilliseconds.isEmpty() ? timeoutNanos : timeoutInMilliseconds.get() * 1_000_000;

            if(durationNanos > timeout) {
                log.warn("Scalar measurer " + inner.getClass().getName() + " timed out (took " + durationNanos + "ns)");
                numTimeouts.incrementAndGet();
                strategy = TIMED_OUT;
            }
        }

        @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
            try {
                final long start = System.nanoTime();
                inner.contributeMeasurements(data, timestamp, mementos);
                handleDuration(System.nanoTime() - start, inner.getTimeoutInMilliSeconds());
                numExceptions.set(0);
            } catch (Exception e) {
                if (numExceptions.incrementAndGet() > maxNumTimeouts) {
                    log.warn("Disabling scalar measurer " + inner.getClass().getName() + " because an exception occurred " + maxNumTimeouts + " times in row", e);
                    strategy = DISABLED;
                }
            }
        }
    };

    private final Strategy TIMED_OUT = new Strategy() {
        @Override public void prepareMeasurements(Map<String, Object> mementos) {
            try {
                final long start = System.nanoTime();
                inner.prepareMeasurements(mementos);
                handleDuration(System.nanoTime() - start, inner.getTimeoutInMilliSeconds());
                numExceptions.set(0);
            } catch (Exception e) {
                if (numExceptions.incrementAndGet() > maxNumTimeouts) {
                    log.warn("Disabling scalar measurer " + inner.getClass().getName() + " because an exception occurred " + maxNumTimeouts + " times in row", e);
                    strategy = DISABLED;
                }
            }
        }

        private void handleDuration(long durationNanos, AOption<Long> timeoutInMilliseconds) {
            final long timeout = timeoutInMilliseconds.isEmpty() ? timeoutNanos : timeoutInMilliseconds.get() * 1_000_000;

            if(durationNanos > timeout) {
                log.warn("Scalar measurer " + inner.getClass().getName() + " timed out (took " + durationNanos + "ns)");
                if(numTimeouts.incrementAndGet() >= maxNumTimeouts) {
                    log.warn("Scalar measurer " + inner.getClass().getName() + " timed out " + maxNumTimeouts + " times in row - permanently disabling");
                    strategy = DISABLED;
                }
            } else {
                numTimeouts.set(0);
                strategy = ENABLED;
            }
        }

        @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
            try {
                final long start = System.nanoTime();
                inner.contributeMeasurements(data, timestamp, mementos);
                handleDuration(System.nanoTime() - start, inner.getTimeoutInMilliSeconds());
                numExceptions.set(0);
            } catch (Exception e) {
                if (numExceptions.incrementAndGet() > maxNumTimeouts) {
                    log.warn("Disabling scalar measurer " + inner.getClass().getName() + " because an exception occurred " + maxNumTimeouts + " times in row", e);
                    strategy = DISABLED;
                }
            }
        }
    };

    private static final Strategy DISABLED = new Strategy() {
        @Override public void prepareMeasurements(Map<String, Object> mementos) {
        }

        @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
        }
    };

    private volatile Strategy strategy = ENABLED;

    RobustScalarMeasurerWrapper(AScalarMeasurer inner, long timeoutNanos, int maxNumTimeouts) {
        this.inner = inner;
        this.timeoutNanos = timeoutNanos;
        this.maxNumTimeouts = maxNumTimeouts;
    }

    public void prepareMeasurements(Map<String, Object> mementos) {
        strategy.prepareMeasurements(mementos);
    }

    public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
        strategy.contributeMeasurements(data, timestamp, mementos);
    }

    public void shutdown() {
        try {
            inner.shutdown();
        } catch (Exception exc) {
            log.error("failed to shut down scalar measurer " + inner.getClass().getName() + '.', exc);
        }
    }
}
