package com.nsysmon.servlet.longestcalls;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongestCallsDataSinkTest {

    private static AHierarchicalData node(String identifier, long durationNanos, AHierarchicalData... children) {
        return new AHierarchicalData(true, 1L, durationNanos, identifier, Map.of(), List.of(children), false);
    }

    private static AHierarchicalDataRoot measurement(String rootIdentifier, long rootDurationNanos) {
        return new AHierarchicalDataRoot(node(rootIdentifier, rootDurationNanos),
                Collections.emptyList(), Collections.emptyList());
    }

    private static AHierarchicalDataRoot measurement(AHierarchicalData... children) {
        return new AHierarchicalDataRoot(node("root", 10_000L, children),
                Collections.emptyList(), Collections.emptyList());
    }

    private static List<String> identifiersOf(Iterable<AHierarchicalDataRoot> data) {
        final List<String> result = new ArrayList<>();
        for(AHierarchicalDataRoot d: data) {
            result.add(d.getRootNode().getIdentifier());
        }
        return result;
    }

    private static LongestCallsDataSink sink(boolean started, String findRegEx, Set<String> ignored) {
        return new LongestCallsDataSink(2, started, findRegEx, ignored);
    }

    @Test void isStartedWhenConfiguredThatWay() {
        assertTrue(sink(true, "", Set.of()).isStarted);
    }

    @Test void isStoppedWhenConfiguredThatWay() {
        assertFalse(sink(false, "", Set.of()).isStarted);
    }

    @Test void ignoresMeasurementsWhileStopped() {
        final LongestCallsDataSink dataSink = sink(false, "", Set.of());

        dataSink.onFinishedHierarchicalMeasurement(measurement("call", 100L));

        assertTrue(identifiersOf(dataSink.getData()).isEmpty());
    }

    @Test void toleratesTheLifecycleCallbacks() throws Exception {
        final LongestCallsDataSink dataSink = sink(true, "", Set.of());

        dataSink.onStartedHierarchicalMeasurement("whatever");
        dataSink.shutdown();

        assertTrue(identifiersOf(dataSink.getData()).isEmpty());
    }

    @Test void storesTheLongestCallsFirst() {
        final LongestCallsDataSink dataSink = sink(true, "", Set.of());
        dataSink.onFinishedHierarchicalMeasurement(measurement("short", 100L));

        dataSink.onFinishedHierarchicalMeasurement(measurement("long", 900L));

        assertEquals(List.of("long", "short"), identifiersOf(dataSink.getData()));
    }

    @Test void discardsTheShortestCallWhenTheBufferOverflows() {
        final LongestCallsDataSink dataSink = sink(true, "", Set.of());
        dataSink.onFinishedHierarchicalMeasurement(measurement("t1", 100L));
        dataSink.onFinishedHierarchicalMeasurement(measurement("t2", 200L));

        dataSink.onFinishedHierarchicalMeasurement(measurement("t3", 300L));

        assertEquals(List.of("t3", "t2"), identifiersOf(dataSink.getData()));
    }

    @Test void keepsTheBufferSizeWhenAShortCallIsRejected() {
        final LongestCallsDataSink dataSink = sink(true, "", Set.of());
        dataSink.onFinishedHierarchicalMeasurement(measurement("t1", 100L));
        dataSink.onFinishedHierarchicalMeasurement(measurement("t2", 200L));

        dataSink.onFinishedHierarchicalMeasurement(measurement("tooShort", 1L));

        assertEquals(List.of("t2", "t1"), identifiersOf(dataSink.getData()));
    }

    @Test void clearRemovesAllCalls() {
        final LongestCallsDataSink dataSink = sink(true, "", Set.of());
        dataSink.onFinishedHierarchicalMeasurement(measurement("call", 100L));

        dataSink.clear();

        assertTrue(identifiersOf(dataSink.getData()).isEmpty());
    }

    @Test void storesOnlyTheCallsMatchingTheFindRegEx() {
        final LongestCallsDataSink dataSink = sink(true, "jdbc:.*", Set.of());

        dataSink.onFinishedHierarchicalMeasurement(measurement(
                node("entry", 1_000L, node("other", 200L), node("jdbc: SELECT 1", 300L))));

        assertEquals(List.of("jdbc: SELECT 1"), identifiersOf(dataSink.getData()));
    }

    @Test void searchesTheWholeSubtreeForMatchingCalls() {
        final LongestCallsDataSink dataSink = sink(true, "jdbc:.*", Set.of());

        dataSink.onFinishedHierarchicalMeasurement(measurement(
                node("entry", 1_000L, node("service", 900L, node("jdbc: SELECT 1", 300L)))));

        assertEquals(List.of("jdbc: SELECT 1"), identifiersOf(dataSink.getData()));
    }

    @Test void theFindRegExIsCaseInsensitive() {
        final LongestCallsDataSink dataSink = sink(true, "JDBC:.*", Set.of());

        dataSink.onFinishedHierarchicalMeasurement(measurement("jdbc: SELECT 1", 300L));

        assertEquals(List.of("jdbc: SELECT 1"), identifiersOf(dataSink.getData()));
    }

    @Test void ignoresEntriesConfiguredToBeIgnored() {
        final LongestCallsDataSink dataSink = sink(true, "jdbc:.*", Set.of("SELECT 2"));

        dataSink.onFinishedHierarchicalMeasurement(measurement(
                node("jdbc: SELECT 1", 100L), node("jdbc: SELECT 2", 200L)));

        assertEquals(List.of("jdbc: SELECT 1"), identifiersOf(dataSink.getData()));
    }

    @Test void theIgnoredEntriesAreOnlyAppliedWhenFiltering() {
        final LongestCallsDataSink dataSink = sink(true, "", Set.of("SELECT 2"));

        dataSink.onFinishedHierarchicalMeasurement(measurement("jdbc: SELECT 2", 200L));

        assertEquals(List.of("jdbc: SELECT 2"), identifiersOf(dataSink.getData()));
    }
}
