package com.nsysmon.datasink.cyclicdump;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.config.appinfo.ADefaultApplicationInfoProvider;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ACyclicMeasurementDumperTest {

    private static final long AWAIT_TIMEOUT_MILLIS = 20_000;
    private static final int NO_REPEAT_SECONDS = 3600;

    /** Captures the dumped lines and signals once the expected number of them has arrived. */
    private static final class CapturingDumper extends ACyclicMeasurementDumper {
        private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
        private final int expectedLines;
        private final CountDownLatch allLinesDumped = new CountDownLatch(1);

        CapturingDumper(int initialDelaySeconds, int frequencyInSeconds, int averagingDelayMillis, int expectedLines) {
            super(initialDelaySeconds, frequencyInSeconds, averagingDelayMillis);
            this.expectedLines = expectedLines;
        }

        @Override protected void dump(String s) {
            lines.add(s);
            if(lines.size() >= expectedLines) {
                allLinesDumped.countDown();
            }
        }

        List<String> lines() {
            synchronized(lines) {
                return List.copyOf(lines);
            }
        }

        boolean awaitAllLines() throws InterruptedException {
            return allLinesDumped.await(AWAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    /** Contributes a single, fixed measurement so neither randomness nor the wall clock influences the result. */
    private static final class FixedScalarMeasurer implements AScalarMeasurer {
        static final String NAME = "fixed";

        @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
            data.put(NAME, new AScalarDataPoint(timestamp, NAME, 11, 0));
        }

        @Override public AOption<Long> getTimeoutInMilliSeconds() {
            return AOption.none();
        }

        @Override public void shutdown() {
        }
    }

    private static AScalarDataPoint point(String name, long value) {
        return new AScalarDataPoint(1234L, name, value, 0);
    }

    private static Map<String, AScalarDataPoint> measurementsOf(AScalarDataPoint... points) {
        final Map<String, AScalarDataPoint> result = new TreeMap<>();
        for (AScalarDataPoint point : points) {
            result.put(point.getName(), point);
        }
        return result;
    }

    @Test void dumpsEveryScalarMeasurementProvidedByTheApi() throws Exception {
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getScalarMeasurements(250)).thenReturn(measurementsOf(point("alpha", 42), point("beta", 7)));
        final CapturingDumper dumper = new CapturingDumper(0, NO_REPEAT_SECONDS, 250, 2);

        dumper.setNSysMon(sysMon);
        try {
            assertTrue(dumper.awaitAllLines());
            assertEquals(2, dumper.lines().size());
            assertTrue(dumper.lines().contains("Scalar Measurement: alpha = 42"), dumper.lines().toString());
            assertTrue(dumper.lines().contains("Scalar Measurement: beta = 7"), dumper.lines().toString());
        } finally {
            dumper.shutdown();
        }
    }

    @Test void requestsScalarMeasurementsWithTheConfiguredAveragingDelay() throws Exception {
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getScalarMeasurements(eq(4321))).thenReturn(measurementsOf(point("alpha", 1)));
        final CapturingDumper dumper = new CapturingDumper(0, NO_REPEAT_SECONDS, 4321, 1);

        dumper.setNSysMon(sysMon);
        try {
            assertTrue(dumper.awaitAllLines());
            assertEquals(1, dumper.lines().size());
        } finally {
            dumper.shutdown();
        }
        verify(sysMon, atLeastOnce()).getScalarMeasurements(4321);
    }

    @Test void dumpsNothingWhenTheApiReturnsNoMeasurements() {
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getScalarMeasurements(anyInt())).thenReturn(measurementsOf());
        final CapturingDumper dumper = new CapturingDumper(0, NO_REPEAT_SECONDS, 0, 1);

        dumper.setNSysMon(sysMon);
        try {
            // nothing can ever be dumped from an empty result, so it is safe to assert right after the
            // first poll has been observed
            verify(sysMon, timeout(AWAIT_TIMEOUT_MILLIS).atLeastOnce()).getScalarMeasurements(0);
            assertTrue(dumper.lines().isEmpty());
        } finally {
            dumper.shutdown();
        }
    }

    @Test void dumpsTheExceptionTextWhenTheApiFails() throws Exception {
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getScalarMeasurements(anyInt())).thenThrow(new IllegalStateException("no measurements available"));
        final CapturingDumper dumper = new CapturingDumper(0, NO_REPEAT_SECONDS, 0, 1);

        dumper.setNSysMon(sysMon);
        try {
            assertTrue(dumper.awaitAllLines());
            assertEquals(List.of("java.lang.IllegalStateException: no measurements available"), dumper.lines());
        } finally {
            dumper.shutdown();
        }
    }

    @Test void ignoresHierarchicalMeasurementCallbacks() {
        final CapturingDumper dumper = new CapturingDumper(0, NO_REPEAT_SECONDS, 0, 1);

        dumper.onStartedHierarchicalMeasurement("a");
        dumper.onFinishedHierarchicalMeasurement(null);

        assertTrue(dumper.lines().isEmpty());
    }

    @Test void rejectsRegistrationAfterShutdown() {
        final CapturingDumper dumper = new CapturingDumper(0, NO_REPEAT_SECONDS, 0, 1);
        dumper.shutdown();

        assertThrows(RejectedExecutionException.class, () -> dumper.setNSysMon(mock(NSysMonApi.class)));
    }

    @Test void shutdownIsIdempotent() {
        final CapturingDumper dumper = new CapturingDumper(0, NO_REPEAT_SECONDS, 0, 1);

        dumper.shutdown();
        dumper.shutdown();

        assertTrue(dumper.lines().isEmpty());
    }

    @Test void dumpsScalarMeasurementsCollectedByTheRealEngine() throws Exception {
        final NSysMonConfigBuilder configBuilder = new NSysMonConfigBuilder(new ADefaultApplicationInfoProvider("dummy", "version"));
        configBuilder.addScalarMeasurer(new FixedScalarMeasurer());
        final NSysMonApi sysMon = new NSysMonImpl(configBuilder.build());
        final CapturingDumper dumper = new CapturingDumper(0, NO_REPEAT_SECONDS, 0, 1);

        dumper.setNSysMon(sysMon);
        try {
            assertTrue(dumper.awaitAllLines());
            assertEquals(List.of("Scalar Measurement: fixed = 11"), dumper.lines());
        } finally {
            dumper.shutdown();
        }
    }
}
