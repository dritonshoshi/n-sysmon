package com.nsysmon.datasink.log;

import com.nsysmon.data.AHierarchicalDataRoot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AStdErrDataSinkTest {

    @Test void writesMeasurementHierarchyToSystemErr() {
        final AHierarchicalDataRoot data = ALoggingDataSinkTest.traceOf(ALoggingDataSinkTest.node(true, 1000, 500, "root", ALoggingDataSinkTest.node(false, 1100, 200, "child")));

        final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        final PrintStream originalErr = System.err;
        final PrintStream originalOut = System.out;
        System.setErr(new PrintStream(errBuffer, true, StandardCharsets.UTF_8));
        System.setOut(new PrintStream(outBuffer, true, StandardCharsets.UTF_8));
        try {
            new AStdErrDataSink().onFinishedHierarchicalMeasurement(data);
        } finally {
            System.setErr(originalErr);
            System.setOut(originalOut);
        }

        assertEquals("+root@1000: 500 {}\n  -child@1100: 200 {}\n", errBuffer.toString(StandardCharsets.UTF_8));
        assertEquals("", outBuffer.toString(StandardCharsets.UTF_8));
    }

    @Test void writesNothingToSystemErrForStartedMeasurements() {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream original = System.err;
        System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            new AStdErrDataSink().onStartedHierarchicalMeasurement("some-identifier");
        } finally {
            System.setErr(original);
        }

        assertEquals("", buffer.toString(StandardCharsets.UTF_8));
    }
}
