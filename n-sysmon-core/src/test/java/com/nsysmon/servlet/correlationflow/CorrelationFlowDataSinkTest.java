package com.nsysmon.servlet.correlationflow;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrelationFlowDataSinkTest {
    private static MockedStatic<NSysMon> withCorrelationFlowEnabled(boolean enabled) {
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getConfig()).thenReturn(TestSysMons.config(!enabled));

        final MockedStatic<NSysMon> result = Mockito.mockStatic(NSysMon.class);
        result.when(NSysMon::get).thenReturn(sysMon);
        return result;
    }

    private static AHierarchicalDataRoot measurement(ACorrelationId... startedFlows) {
        return new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1L, 1L, "root", Map.of(), Collections.emptyList(), false),
                List.of(startedFlows), Collections.emptyList());
    }

    private static List<String> qualifiersOf(CorrelationFlowDataSink sink) {
        final List<String> result = new ArrayList<>();
        for (Map.Entry<ACorrelationId, Set<ACorrelationId>> entry: sink.getDataAsMap().entrySet()) {
            result.add(entry.getKey().getQualifier());
        }
        return result;
    }

    @Test void storesNothingWhileCorrelationFlowIsDisabled() {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(false)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(10);

            sink.onFinishedHierarchicalMeasurement(measurement(new ACorrelationId("q", "1", null)));

            assertTrue(sink.getDataAsMap().isEmpty());
        }
    }

    @Test void storesARootFlow() {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(true)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(10);

            sink.onFinishedHierarchicalMeasurement(measurement(new ACorrelationId("q", "1", null)));

            assertEquals(List.of("q"), qualifiersOf(sink));
        }
    }

    @Test void linksAChildToItsParent() {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(true)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(10);
            final ACorrelationId parent = new ACorrelationId("parent", "1", null);
            final ACorrelationId child = new ACorrelationId("child", "2", "1");

            sink.onFinishedHierarchicalMeasurement(measurement(parent, child));

            assertEquals(Set.of(child), sink.getDataAsMap().get(parent));
        }
    }

    @Test void linksChildrenThatArriveInAnyOrder() {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(true)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(10);
            final ACorrelationId parent = new ACorrelationId("parent", "1", null);
            final ACorrelationId child = new ACorrelationId("child", "2", "1");

            sink.onFinishedHierarchicalMeasurement(measurement(child, parent));

            assertEquals(Set.of(child), sink.getDataAsMap().get(parent));
        }
    }

    @Test void discardsFlowsWhoseParentIsUnknown() {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(true)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(10);

            sink.onFinishedHierarchicalMeasurement(measurement(new ACorrelationId("orphan", "9", "does-not-exist")));

            assertTrue(sink.getDataAsMap().isEmpty());
        }
    }

    @Test void toleratesTheLifecycleCallbacks() throws Exception {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(true)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(10);

            sink.onStartedHierarchicalMeasurement("whatever");
            sink.shutdown();

            assertTrue(sink.getDataAsMap().isEmpty());
        }
    }

    @Test void clearDataRemovesAllFlows() {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(true)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(10);
            sink.onFinishedHierarchicalMeasurement(measurement(new ACorrelationId("q", "1", null)));

            sink.clearData();

            assertTrue(sink.getDataAsMap().isEmpty());
        }
    }

    @Test void getDataAsMapIsNotModifiable() {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(true)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(10);
            sink.onFinishedHierarchicalMeasurement(measurement(new ACorrelationId("q", "1", null)));

            assertThrows(UnsupportedOperationException.class, () -> sink.getDataAsMap().clear());
        }
    }

    @Test void dropsTheOldestFlowWhenTheBufferIsFull() {
        try (MockedStatic<NSysMon> ignored = withCorrelationFlowEnabled(true)) {
            final CorrelationFlowDataSink sink = new CorrelationFlowDataSink(2);

            sink.onFinishedHierarchicalMeasurement(measurement(new ACorrelationId("q1", "1", null)));
            sink.onFinishedHierarchicalMeasurement(measurement(new ACorrelationId("q2", "2", null)));
            sink.onFinishedHierarchicalMeasurement(measurement(new ACorrelationId("q3", "3", null)));

            assertEquals(2, sink.getDataAsMap().size());
        }
    }
}
