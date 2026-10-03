package com.nsysmon.servlet.performance.drilldown;

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

class DrillDownDataSinkTest {
    private DrillDownDataSink sink;

    @BeforeEach void setUp() {
        sink = new DrillDownDataSink();
    }

    private static AHierarchicalDataRoot measurement(AHierarchicalData... children) {
        final AHierarchicalData root = new AHierarchicalData(true, 1L, 1_000L, "root",
                Map.of(), List.of(children), false);
        return new AHierarchicalDataRoot(root, Collections.emptyList(), Collections.emptyList());
    }

    private static AHierarchicalData node(String identifier, long durationNanos, boolean isSerial,
                                          AHierarchicalData... children) {
        return new AHierarchicalData(isSerial, 1L, durationNanos, identifier, Map.of(), List.of(children), false);
    }

    @Test void isInactiveByDefault() {
        assertFalse(sink.isActive());
    }

    @Test void ignoresMeasurementsWhileInactive() {
        sink.onFinishedHierarchicalMeasurement(measurement(node("child", 100L, true)));

        assertTrue(sink.getData().isEmpty());
    }

    @Test void collectsMeasurementsWhileActive() {
        sink.setActive(true);

        sink.onFinishedHierarchicalMeasurement(measurement(node("child", 100L, true)));

        assertEquals(1, sink.getData().size());
        assertEquals(1_000L, sink.getData().get("root").getTotalNanos());
        assertEquals(100L, sink.getData().get("root").getChildren().get("child").getTotalNanos());
    }

    @Test void recursesIntoChildren() {
        sink.setActive(true);

        sink.onFinishedHierarchicalMeasurement(measurement(node("child", 100L, true, node("grandChild", 40L, true))));

        final AMinMaxAvgData root = sink.getData().get("root");
        assertEquals(1, root.getChildren().size());
        assertEquals(1_000L, root.getTotalNanos());
        assertEquals(40L, root.getChildren().get("child").getChildren().get("grandChild").getTotalNanos());
    }

    @Test void aggregatesRepeatedMeasurements() {
        sink.setActive(true);
        sink.onFinishedHierarchicalMeasurement(measurement(node("child", 100L, true)));
        sink.onFinishedHierarchicalMeasurement(measurement(node("child", 300L, true)));

        final AMinMaxAvgData root = sink.getData().get("root");
        assertEquals(2, root.getTotalNumInContext());
        assertEquals(2_000L, root.getTotalNanos());
        assertEquals(1_000L, root.getAvgNanos());
    }

    @Test void clearRemovesAllMeasurements() {
        sink.setActive(true);
        sink.onFinishedHierarchicalMeasurement(measurement(node("child", 100L, true)));

        sink.clear();

        assertTrue(sink.getData().isEmpty());
    }

    @Test void getDataIsNotModifiable() {
        sink.setActive(true);
        sink.onFinishedHierarchicalMeasurement(measurement(node("child", 100L, true)));

        assertThrows(UnsupportedOperationException.class, () -> sink.getData().remove("root"));
    }

    @Test void toleratesTheLifecycleCallbacks() {
        sink.onStartedHierarchicalMeasurement("whatever");
        sink.shutdown();

        assertTrue(sink.getData().isEmpty());
    }
}
