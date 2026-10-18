package com.nsysmon.impl;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.datasink.MemoryUsageKind;
import com.nsysmon.testutil.CollectingDataSink;
import com.nsysmon.testutil.MemoryReportingDataSink;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NSysMonMemoryReportTest {
    private NSysMonImpl sysMon;

    @BeforeEach void setUp() {
        sysMon = (NSysMonImpl) TestSysMons.create();
    }

    @AfterEach void tearDown() {
        sysMon.shutdown();
    }

    private static AHierarchicalDataRoot measurement(String identifier) {
        return new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1L, 2L, identifier, Map.of("p", "v"), List.of(), false),
                List.of(), List.of());
    }

    @Test void containsOneRowPerRegisteredDataSink() {
        NSysMonConfigurer.addDataSink(sysMon, new MemoryReportingDataSink("my sink", MemoryUsageKind.RAW_HIERARCHY));
        NSysMonConfigurer.addDataSink(sysMon, new CollectingDataSink());

        final NSysMonMemoryReport.Report report = new NSysMonMemoryReport(sysMon).collect();

        assertEquals(2, report.dataSinks.size());
    }

    @Test void reportsTheStoredDataOfAMemoryAwareSink() {
        final MemoryReportingDataSink sink = new MemoryReportingDataSink("my sink", MemoryUsageKind.RAW_HIERARCHY);
        sink.add(measurement("jdbc: select 1"));
        NSysMonConfigurer.addDataSink(sysMon, sink);

        final NSysMonMemoryReport.Entry entry = entryOf(new NSysMonMemoryReport(sysMon).collect(), "my sink");

        assertTrue(entry.reporting);
        assertEquals(MemoryUsageKind.RAW_HIERARCHY, entry.kind);
        assertEquals(1, entry.numEntries);
        assertTrue(entry.estimatedBytes > 0);
    }

    @Test void listsSinksThatDoNotReportInsteadOfHidingThem() {
        NSysMonConfigurer.addDataSink(sysMon, new CollectingDataSink());

        final NSysMonMemoryReport.Entry entry = entryOf(new NSysMonMemoryReport(sysMon).collect(),
                "CollectingDataSink");

        assertFalse(entry.reporting);
        assertEquals(-1, entry.estimatedBytes);
    }

    @Test void reportsTheTimedScalarBuffers() {
        sysMon.addTimedScalarMeasurement(new AScalarDataPoint(1L, "a.b.c", 42L, 0));

        final NSysMonMemoryReport.Report report = new NSysMonMemoryReport(sysMon).collect();

        assertFalse(report.timedScalars.isEmpty());
        final NSysMonMemoryReport.Entry entry = report.timedScalars.stream()
                .filter(e -> e.name.contains("a.b.c"))
                .findFirst()
                .orElseThrow();
        assertEquals(MemoryUsageKind.TIMED_SCALAR, entry.kind);
        assertEquals(1, entry.numEntries);
        assertTrue(entry.estimatedBytes > 0);
    }

    @Test void totalIsTheSumOfAllReportingEntries() {
        final MemoryReportingDataSink sink = new MemoryReportingDataSink("my sink", MemoryUsageKind.RAW_HIERARCHY);
        sink.add(measurement("jdbc: select 1"));
        NSysMonConfigurer.addDataSink(sysMon, sink);

        final NSysMonMemoryReport.Report report = new NSysMonMemoryReport(sysMon).collect();
        final long expected = report.dataSinks.stream().mapToLong(e -> Math.max(0, e.estimatedBytes)).sum()
                + report.timedScalars.stream().mapToLong(e -> Math.max(0, e.estimatedBytes)).sum();

        assertEquals(expected, report.estimatedTotalBytes);
    }

    private static NSysMonMemoryReport.Entry entryOf(NSysMonMemoryReport.Report report, String name) {
        return report.dataSinks.stream()
                .filter(e -> e.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no entry named " + name + " in " + report.dataSinks));
    }
}
