package com.nsysmon.servlet.threaddump;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ARunningThreadTrackingDataSinkTest {

    private static AHierarchicalDataRoot someMeasurement() {
        return new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1L, 1L, "a", Map.of(), Collections.emptyList(), false),
                Collections.emptyList(), Collections.emptyList());
    }

    @Test void recordsTheStartTimestampOfTheCurrentThread() {
        final ARunningThreadTrackingDataSink sink = new ARunningThreadTrackingDataSink();

        sink.onStartedHierarchicalMeasurement("a");

        assertTrue(sink.getStartTimestamps().containsKey(Thread.currentThread().getName()));
    }

    @Test void forgetsTheTimestampWhenTheMeasurementIsFinished() {
        final ARunningThreadTrackingDataSink sink = new ARunningThreadTrackingDataSink();
        sink.onStartedHierarchicalMeasurement("a");

        sink.onFinishedHierarchicalMeasurement(someMeasurement());

        assertFalse(sink.getStartTimestamps().containsKey(Thread.currentThread().getName()));
    }

    @Test void timestampsOfOtherThreadsAreUnaffected() throws InterruptedException {
        final ARunningThreadTrackingDataSink sink = new ARunningThreadTrackingDataSink();
        final ARunningThreadTrackingDataSink otherSink = new ARunningThreadTrackingDataSink();
        final Thread thread = new Thread(() -> otherSink.onStartedHierarchicalMeasurement("a"), "some-other-thread");
        thread.start();
        thread.join();

        sink.onStartedHierarchicalMeasurement("a");

        assertTrue(otherSink.getStartTimestamps().containsKey("some-other-thread"));
        assertFalse(sink.getStartTimestamps().containsKey("some-other-thread"));
    }

    @Test void getStartTimestampsReturnsAnIndependentSnapshot() {
        final ARunningThreadTrackingDataSink sink = new ARunningThreadTrackingDataSink();
        sink.onStartedHierarchicalMeasurement("a");

        sink.getStartTimestamps().clear();

        assertEquals(1, sink.getStartTimestamps().size());
    }

    @Test void nestedMeasurementsShareOneTimestamp() {
        final ARunningThreadTrackingDataSink sink = new ARunningThreadTrackingDataSink();
        sink.onStartedHierarchicalMeasurement("outer");

        sink.onStartedHierarchicalMeasurement("inner");
        final Long firstTimestamp = sink.getStartTimestamps().get(Thread.currentThread().getName());

        assertEquals(firstTimestamp, sink.getStartTimestamps().get(Thread.currentThread().getName()));
        assertEquals(List.of(Thread.currentThread().getName()), List.copyOf(sink.getStartTimestamps().keySet()));
    }

    @Test void discardsAllTimestampsWhenTheMemoryLimitIsExceeded() throws Exception {
        final ARunningThreadTrackingDataSink sink = new ARunningThreadTrackingDataSink();
        final Map<String, Long> timestamps = startTimestampsOf(sink);
        for (int i = 0; i <= 100_000; i++) {
            timestamps.put("gone-" + i, 1L);
        }

        sink.onStartedHierarchicalMeasurement("theTrigger");

        assertEquals(1, sink.getStartTimestamps().size());
        assertTrue(sink.getStartTimestamps().containsKey(Thread.currentThread().getName()));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Long> startTimestampsOf(ARunningThreadTrackingDataSink sink) throws Exception {
        final java.lang.reflect.Field field =
                ARunningThreadTrackingDataSink.class.getDeclaredField("startTimestamps");
        field.setAccessible(true);
        return (Map<String, Long>) field.get(sink);
    }

    @Test void toleratesShutdownWithoutData() throws Exception {
        final ARunningThreadTrackingDataSink sink = new ARunningThreadTrackingDataSink();

        sink.shutdown();

        assertTrue(sink.getStartTimestamps().isEmpty());
    }
}
