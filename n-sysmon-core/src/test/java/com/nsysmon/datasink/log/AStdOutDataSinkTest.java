package com.nsysmon.datasink.log;

import com.nsysmon.data.AHierarchicalDataRoot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AStdOutDataSinkTest {

    private static String captureStdOut(AHierarchicalDataRoot data) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream original = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            new AStdOutDataSink().onFinishedHierarchicalMeasurement(data);
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test void writesMeasurementHierarchyToSystemOut() {
        final AHierarchicalDataRoot data = ALoggingDataSinkTest.traceOf(ALoggingDataSinkTest.node(true, 1000, 500, "root", ALoggingDataSinkTest.node(true, 1100, 200, "child")));

        final String captured = captureStdOut(data);

        assertEquals("+root@1000: 500 {}\n  +child@1100: 200 {}\n", captured);
    }

    @Test void writesNothingToSystemOutForStartedMeasurements() {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream original = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            new AStdOutDataSink().onStartedHierarchicalMeasurement("some-identifier");
        } finally {
            System.setOut(original);
        }

        assertEquals("", buffer.toString(StandardCharsets.UTF_8));
    }

    @Test void instanceIsTheSharedDataSinkUsedByConfiguration() {
        assertTrue(AStdOutDataSink.INSTANCE instanceof ALoggingDataSink);

        final String captured = captureStdOut(ALoggingDataSinkTest.traceOf(ALoggingDataSinkTest.node(true, 7, 8, "shared")));

        assertEquals("+shared@7: 8 {}\n", captured);
    }
}
