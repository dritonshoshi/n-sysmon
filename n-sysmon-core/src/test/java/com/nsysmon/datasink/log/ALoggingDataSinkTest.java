package com.nsysmon.datasink.log;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ALoggingDataSinkTest {

    /** Records everything the sink logs, so the rendered lines can be asserted on. */
    private static final class RecordingDataSink extends ALoggingDataSink {
        private final List<String> lines = new ArrayList<>();
        private boolean loggingEnabled = true;

        @Override protected void log(String s) {
            lines.add(s);
        }

        @Override protected boolean isLoggingEnabled() {
            return loggingEnabled;
        }

        List<String> lines() {
            return lines;
        }
    }

    /** Exercises the default {@code isLoggingEnabled()}. */
    private static final class DefaultRecordingDataSink extends ALoggingDataSink {
        private final List<String> lines = new ArrayList<>();

        @Override protected void log(String s) {
            lines.add(s);
        }

        List<String> lines() {
            return lines;
        }
    }

    static AHierarchicalData node(boolean serial, long startTimeMillis, long durationNanos, String identifier, AHierarchicalData... children) {
        return new AHierarchicalData(serial, startTimeMillis, durationNanos, identifier, Collections.emptyMap(), Arrays.asList(children), false);
    }

    static AHierarchicalData nodeWithParameters(boolean serial, String identifier, Map<String, String> parameters) {
        return new AHierarchicalData(serial, 1000, 500, identifier, parameters, Collections.emptyList(), false);
    }

    static AHierarchicalDataRoot traceOf(AHierarchicalData rootNode) {
        return new AHierarchicalDataRoot(rootNode, Collections.emptyList(), Collections.emptyList());
    }

    @Test void isLoggingEnabledByDefault() {
        final DefaultRecordingDataSink sink = new DefaultRecordingDataSink();

        assertTrue(sink.isLoggingEnabled());
    }

    @Test void logsNothingWhenStartedAMeasurement() {
        final RecordingDataSink sink = new RecordingDataSink();

        sink.onStartedHierarchicalMeasurement("some-identifier");

        assertTrue(sink.lines().isEmpty());
    }

    @Test void logsOneLinePerNodeOfTheHierarchy() {
        final RecordingDataSink sink = new RecordingDataSink();
        final AHierarchicalData rootNode = node(true, 1000, 500, "root", node(true, 1100, 200, "child"));

        sink.onFinishedHierarchicalMeasurement(traceOf(rootNode));

        assertEquals(2, sink.lines().size());
        assertEquals("+root@1000: 500 {}", sink.lines().get(0));
        assertEquals("  +child@1100: 200 {}", sink.lines().get(1));
    }

    @Test void indentsTwoSpacesPerHierarchyLevel() {
        final RecordingDataSink sink = new RecordingDataSink();
        final AHierarchicalData rootNode = node(true, 1, 2, "a", node(true, 3, 4, "b", node(true, 5, 6, "c")));

        sink.onFinishedHierarchicalMeasurement(traceOf(rootNode));

        assertEquals(List.of("+a@1: 2 {}", "  +b@3: 4 {}", "    +c@5: 6 {}"), sink.lines());
    }

    @Test void prefixesSerialNodesWithPlusAndParallelNodesWithMinus() {
        final RecordingDataSink sink = new RecordingDataSink();
        final AHierarchicalData rootNode = node(true, 1, 2, "serial", node(false, 3, 4, "parallel"));

        sink.onFinishedHierarchicalMeasurement(traceOf(rootNode));

        assertEquals("+serial@1: 2 {}", sink.lines().get(0));
        assertEquals("  -parallel@3: 4 {}", sink.lines().get(1));
    }

    @Test void writesTheParametersOfEveryNode() {
        final RecordingDataSink sink = new RecordingDataSink();
        final Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("jdbc.url", "jdbc:h2:mem:demo");
        final AHierarchicalData rootNode = nodeWithParameters(true, "withParams", parameters);

        sink.onFinishedHierarchicalMeasurement(traceOf(rootNode));

        assertEquals(List.of("+withParams@1000: 500 {jdbc.url=jdbc:h2:mem:demo}"), sink.lines());
    }

    @Test void writesEmptyParameterMapForNodesWithoutParameters() {
        final RecordingDataSink sink = new RecordingDataSink();
        final AHierarchicalData rootNode = nodeWithParameters(true, "noParams", Collections.emptyMap());

        sink.onFinishedHierarchicalMeasurement(traceOf(rootNode));

        assertEquals(List.of("+noParams@1000: 500 {}"), sink.lines());
    }

    @Test void logsNothingWhenLoggingIsDisabled() {
        final RecordingDataSink sink = new RecordingDataSink();
        sink.loggingEnabled = false;

        sink.onFinishedHierarchicalMeasurement(traceOf(node(true, 1000, 500, "root")));

        assertTrue(sink.lines().isEmpty());
    }

    @Test void shutdownDoesNotLogAnything() {
        final RecordingDataSink sink = new RecordingDataSink();

        sink.shutdown();

        assertTrue(sink.lines().isEmpty());
    }
}