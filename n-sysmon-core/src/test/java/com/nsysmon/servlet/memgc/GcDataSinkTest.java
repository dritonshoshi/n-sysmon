package com.nsysmon.servlet.memgc;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.measure.scalar.AJmxGcMeasurer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GcDataSinkTest {

    private static Map<String, String> gcParams(String memKind, long usedAfter, long committedAfter,
                                                long usedDelta, long committedDelta) {
        final Map<String, String> params = new HashMap<>();
        params.put(AJmxGcMeasurer.KEY_ID, "42");
        params.put(AJmxGcMeasurer.KEY_CAUSE, "G1 Evacuation Pause");
        params.put(AJmxGcMeasurer.KEY_ALGORITHM, "G1");
        params.put(AJmxGcMeasurer.KEY_PREFIX_MEM + memKind + AJmxGcMeasurer.KEY_SUFFIX_USED, String.valueOf(usedAfter));
        params.put(AJmxGcMeasurer.KEY_PREFIX_MEM + memKind + AJmxGcMeasurer.KEY_SUFFIX_COMMITTED,
                String.valueOf(committedAfter));
        params.put(AJmxGcMeasurer.KEY_PREFIX_MEM + memKind + AJmxGcMeasurer.KEY_SUFFIX_USED_DELTA,
                String.valueOf(usedDelta));
        params.put(AJmxGcMeasurer.KEY_PREFIX_MEM + memKind + AJmxGcMeasurer.KEY_SUFFIX_COMMITTED_DELTA,
                String.valueOf(committedDelta));
        return params;
    }

    private static AHierarchicalDataRoot gcRoot(Map<String, String> params, AHierarchicalData... children) {
        final List<AHierarchicalData> childList = new ArrayList<>(List.of(children));
        return new AHierarchicalDataRoot(new AHierarchicalData(true, 1_000L, 99L,
                AJmxGcMeasurer.IDENT_GC_TRACE_ROOT, params, childList, false),
                Collections.emptyList(), Collections.emptyList());
    }

    private static AHierarchicalData gcNode(String gcType, long startMillis, long durationNanos) {
        return new AHierarchicalData(true, startMillis, durationNanos, gcType, Map.of(), List.of(), false);
    }

    private static List<GcDetails> drain(Iterable<GcDetails> details) {
        final List<GcDetails> result = new ArrayList<>();
        details.forEach(result::add);
        return result;
    }

    @Test void ignoresMeasurementsThatAreNotGarbageCollections() {
        final GcDataSink sink = new GcDataSink(10);
        sink.onFinishedHierarchicalMeasurement(new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1L, 1L, "someOtherMeasurement", Map.of(), List.of(gcNode("G1", 1L, 1L)), false),
                Collections.emptyList(), Collections.emptyList()));

        assertTrue(drain(sink.getData()).isEmpty());
    }

    @Test void createsOneDetailsRecordPerChildNode() {
        final GcDataSink sink = new GcDataSink(10);
        sink.onFinishedHierarchicalMeasurement(gcRoot(Map.of(), gcNode("G1 New", 1_000L, 5L), gcNode("G1 Old", 2_000L, 7L)));

        final List<GcDetails> details = drain(sink.getData());

        assertEquals(2, details.size());
        assertEquals("G1 New", details.get(0).gcType);
        assertEquals(1_000L, details.get(0).startMillis);
        assertEquals(5L, details.get(0).durationNanos);
        assertEquals("G1 Old", details.get(1).gcType);
        assertEquals(7L, details.get(1).durationNanos);
    }

    @Test void takesCauseAndAlgorithmFromTheRootParameters() {
        final GcDataSink sink = new GcDataSink(10);
        sink.onFinishedHierarchicalMeasurement(gcRoot(gcParams("heap", 0L, 0L, 0L, 0L), gcNode("G1 New", 1L, 2L)));

        final GcDetails details = drain(sink.getData()).get(0);

        assertEquals("G1 Evacuation Pause", details.cause);
        assertEquals("G1", details.algorithm);
    }

    @Test void computesTheMemoryValuesBeforeTheGc() {
        final GcDataSink sink = new GcDataSink(10);
        sink.onFinishedHierarchicalMeasurement(
                gcRoot(gcParams("heap", 1_000L, 4_000L, 100L, 250L), gcNode("G1 New", 1L, 2L)));

        final GcMemDetails mem = drain(sink.getData()).get(0).memDetails.get(0);

        assertEquals("heap", mem.memKind);
        assertEquals(1_000L, mem.usedAfter);
        assertEquals(900L, mem.usedBefore);
        assertEquals(4_000L, mem.committedAfter);
        assertEquals(3_750L, mem.committedBefore);
    }

    @Test void createsOneMemoryRecordPerMemoryKind() {
        final GcDataSink sink = new GcDataSink(10);
        final Map<String, String> params = gcParams("heap", 1L, 2L, 3L, 4L);
        params.put(AJmxGcMeasurer.getUsedAfterKey("PS Old Gen"), "10");
        params.put(AJmxGcMeasurer.getCommittedAfterKey("PS Old Gen"), "20");
        params.put(AJmxGcMeasurer.getUsedDeltaKey("PS Old Gen"), "5");
        params.put(AJmxGcMeasurer.getCommittedDeltaKey("PS Old Gen"), "6");
        sink.onFinishedHierarchicalMeasurement(gcRoot(params, gcNode("G1 Old", 1L, 2L)));

        final List<GcMemDetails> mem = drain(sink.getData()).get(0).memDetails;

        assertEquals(2, mem.size());
        assertTrue(mem.stream().anyMatch(m -> m.memKind.equals("heap") && m.usedAfter == 1L));
        assertTrue(mem.stream().anyMatch(m -> m.memKind.equals("PS Old Gen") && m.usedAfter == 10L
                && m.usedBefore == 5L && m.committedAfter == 20L && m.committedBefore == 14L));
    }

    @Test void discardsTheOldestRecordsWhenTheBufferIsFull() {
        final GcDataSink sink = new GcDataSink(2);
        sink.onFinishedHierarchicalMeasurement(
                gcRoot(Map.of(), gcNode("gc0", 1L, 1L), gcNode("gc1", 2L, 2L), gcNode("gc2", 3L, 3L)));

        final List<GcDetails> details = drain(sink.getData());

        assertEquals(2, details.size());
        assertEquals("gc1", details.get(0).gcType);
        assertEquals("gc2", details.get(1).gcType);
    }

    @Test void toleratesTheLifecycleCallbacksWithoutData() throws Exception {
        final GcDataSink sink = new GcDataSink(1);

        sink.onStartedHierarchicalMeasurement("whatever");
        sink.shutdown();

        assertTrue(drain(sink.getData()).isEmpty());
    }
}
