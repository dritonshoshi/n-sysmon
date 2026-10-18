package com.nsysmon.testutil;

import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.datasink.ADataSinkMemoryInfo;
import com.nsysmon.datasink.MemoryUsageKind;

import java.util.ArrayList;
import java.util.List;

/**
 * A data sink that stores a configurable list of measurements and reports them to the memory usage report.
 */
public class MemoryReportingDataSink implements ADataSink, ADataSinkMemoryInfo {
    private final String name;
    private final MemoryUsageKind kind;
    private final List<AHierarchicalDataRoot> data = new ArrayList<>();

    public MemoryReportingDataSink(String name, MemoryUsageKind kind) {
        this.name = name;
        this.kind = kind;
    }

    public void add(AHierarchicalDataRoot root) {
        data.add(root);
    }

    @Override public Iterable<?> getStoredMeasurementData() {
        return data;
    }

    @Override public String getMemoryUsageName() {
        return name;
    }

    @Override public MemoryUsageKind getMemoryUsageKind() {
        return kind;
    }

    @Override public void onStartedHierarchicalMeasurement(String identifier) {
    }

    @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
    }

    @Override public void shutdown() {
    }
}
