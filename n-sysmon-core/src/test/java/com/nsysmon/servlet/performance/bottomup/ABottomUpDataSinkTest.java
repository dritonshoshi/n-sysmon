package com.nsysmon.servlet.performance.bottomup;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.servlet.performance.AMinMaxAvgData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ABottomUpDataSinkTest {
    private static final ABottomUpLeafFilter JDBC_LEAVES = data -> data.getIdentifier().startsWith("jdbc:");

    private ABottomUpDataSink sink;

    @BeforeEach void setUp() {
        sink = new ABottomUpDataSink(JDBC_LEAVES);
    }

    private static AHierarchicalData node(String identifier, long durationNanos, AHierarchicalData... children) {
        return new AHierarchicalData(true, 1L, durationNanos, identifier, Map.of(), List.of(children), false);
    }

    private static AHierarchicalDataRoot measurement(AHierarchicalData... children) {
        return new AHierarchicalDataRoot(node("service", 1_000L, children),
                Collections.emptyList(), Collections.emptyList());
    }

    @Test void isInactiveByDefault() {
        assertFalse(sink.isActive());
    }

    @Test void ignoresMeasurementsWhileInactive() {
        sink.onFinishedHierarchicalMeasurement(measurement(node("jdbc: SELECT 1", 100L)));

        assertTrue(sink.getData().isEmpty());
    }

    @Test void toleratesTheLifecycleCallbacks() {
        sink.onStartedHierarchicalMeasurement("whatever");
        sink.shutdown();

        assertTrue(sink.getData().isEmpty());
    }

    @Test void collectsNothingWhenNoNodeMatchesTheLeafFilter() {
        sink.setActive(true);

        sink.onFinishedHierarchicalMeasurement(measurement(node("not-jdbc", 100L)));

        assertTrue(sink.getData().isEmpty());
    }

    @Test void storesTheLeafNodeAsRootOfTheAggregatedTree() {
        sink.setActive(true);

        sink.onFinishedHierarchicalMeasurement(measurement(node("jdbc: SELECT 1", 400L)));

        assertEquals(List.of("jdbc: SELECT 1"), List.copyOf(sink.getData().keySet()));
        assertEquals(400L, sink.getData().get("jdbc: SELECT 1").getTotalNanos());
    }

    @Test void storesTheCallStackFromLeafToEntryPointBelowIt() {
        sink.setActive(true);

        sink.onFinishedHierarchicalMeasurement(
                measurement(node("entryPoint", 2_000L, node("service", 1_000L, node("jdbc: SELECT 1", 400L)))));

        final AMinMaxAvgData leaf = sink.getData().get("jdbc: SELECT 1");
        final AMinMaxAvgData service = leaf.getChildren().get("service");
        assertEquals(1_000L, service.getTotalNanos());
        assertEquals(2_000L, service.getChildren().get("entryPoint").getTotalNanos());
    }

    @Test void aggregatesRepeatedCallsPerContext() {
        sink.setActive(true);
        sink.onFinishedHierarchicalMeasurement(measurement(node("jdbc: SELECT 1", 400L)));
        sink.onFinishedHierarchicalMeasurement(measurement(node("jdbc: SELECT 1", 600L)));

        final AMinMaxAvgData leaf = sink.getData().get("jdbc: SELECT 1");
        assertEquals(2, leaf.getTotalNumInContext());
        assertEquals(1_000L, leaf.getTotalNanos());
        assertEquals(500L, leaf.getAvgNanos());
    }

    @Test void keepsStatisticsSeparatePerContext() {
        sink.setActive(true);

        sink.onFinishedHierarchicalMeasurement(measurement(node("jdbc: SELECT 1", 400L)));
        sink.onFinishedHierarchicalMeasurement(measurement(
                node("otherService", 3_000L, node("jdbc: SELECT 1", 400L))));

        final AMinMaxAvgData leaf = sink.getData().get("jdbc: SELECT 1");
        assertEquals(800L, leaf.getTotalNanos());
        assertEquals(1, leaf.getChildren().get("service").getTotalNumInContext());
        assertEquals(1, leaf.getChildren().get("otherService").getTotalNumInContext());
    }

    @Test void clearRemovesAllMeasurements() {
        sink.setActive(true);
        sink.onFinishedHierarchicalMeasurement(measurement(node("jdbc: SELECT 1", 400L)));

        sink.clear();

        assertTrue(sink.getData().isEmpty());
    }

    @Test void getDataIsNotModifiable() {
        sink.setActive(true);
        sink.onFinishedHierarchicalMeasurement(measurement(node("jdbc: SELECT 1", 400L)));

        assertThrows(UnsupportedOperationException.class, () -> sink.getData().clear());
    }
}
