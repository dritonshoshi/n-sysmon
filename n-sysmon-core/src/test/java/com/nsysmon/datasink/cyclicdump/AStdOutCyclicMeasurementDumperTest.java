package com.nsysmon.datasink.cyclicdump;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AStdOutCyclicMeasurementDumperTest {

    @Test void dumpWritesTheGivenLineToSystemOut() {
        final AStdOutCyclicMeasurementDumper dumper = new AStdOutCyclicMeasurementDumper(3600, 3600, 0);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream original = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            dumper.dump("Scalar Measurement: cpu = 42");
        } finally {
            System.setOut(original);
            dumper.shutdown();
        }

        assertEquals("Scalar Measurement: cpu = 42" + System.lineSeparator(), buffer.toString(StandardCharsets.UTF_8));
    }

    @Test void dumpWritesEveryLineSeparately() {
        final AStdOutCyclicMeasurementDumper dumper = new AStdOutCyclicMeasurementDumper(3600, 3600, 0);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final PrintStream original = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            dumper.dump("first");
            dumper.dump("second");
        } finally {
            System.setOut(original);
            dumper.shutdown();
        }

        assertEquals("first" + System.lineSeparator() + "second" + System.lineSeparator(), buffer.toString(StandardCharsets.UTF_8));
    }
}