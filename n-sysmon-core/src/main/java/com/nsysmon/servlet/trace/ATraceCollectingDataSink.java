package com.nsysmon.servlet.trace;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.datasink.ADataSinkMemoryInfo;
import com.nsysmon.datasink.MemoryUsageKind;


/**
 * @author arno
 */
class ATraceCollectingDataSink implements ADataSink, ADataSinkMemoryInfo {
    public boolean isStarted = true;
    private final ATraceFilter traceFilter;
    private final ARingBuffer<AHierarchicalDataRoot> data;

    ATraceCollectingDataSink(ATraceFilter traceFilter, int bufferSize) {
        this.traceFilter = traceFilter;
        this.data = new ARingBuffer<>(AHierarchicalDataRoot.class, bufferSize);
    }

    @Override public void onStartedHierarchicalMeasurement(String identifier) {
    }

    @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot trace) {
        if(isStarted && traceFilter.shouldCollect(trace)) {
            this.data.put(trace);
        }
    }

    @Override public void shutdown() throws Exception {
    }

    public void clear() {
        data.clear();
    }

    public Iterable<AHierarchicalDataRoot> getData() {
        return data;
    }

    @Override public Iterable<?> getStoredMeasurementData() {
        return data;
    }

    @Override public String getMemoryUsageName() {
        return "trace " + traceFilter.getId();
    }

    @Override public MemoryUsageKind getMemoryUsageKind() {
        return MemoryUsageKind.RAW_HIERARCHY;
    }
}
