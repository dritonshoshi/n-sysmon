package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.NSysMonApi;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.datasink.ADataSinkMemoryInfo;
import com.nsysmon.datasink.MemoryUsageKind;
import com.nsysmon.util.AHeapSizeEstimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds a per-data-sink memory usage report for one {@link NSysMonApi} instance.
 * <p>
 * The report is deliberately outside {@link NSysMonImpl}: N-SysMon itself only exposes its registered sinks. Estimation,
 *  grouping and the timed scalar buffers are handled here.
 * <p>
 * Per-sink numbers are upper bounds. The same {@code AHierarchicalDataRoot} instance is passed to every data sink, so
 *  shared measurement trees may be counted in more than one row. The total is therefore an estimate as well.
 *
 * @author arno
 */
public final class NSysMonMemoryReport {
    private final NSysMonApi sysMon;

    public NSysMonMemoryReport(NSysMonApi sysMon) {
        this.sysMon = sysMon;
    }

    public Report collect() {
        final List<Entry> dataSinks = collectDataSinks();
        final List<Entry> timedScalars = collectTimedScalars();

        return new Report(dataSinks, timedScalars, sum(dataSinks) + sum(timedScalars));
    }

    private List<Entry> collectDataSinks() {
        final List<Entry> result = new ArrayList<>();

        for (ADataSink sink : getDataSinks()) {
            if (sink instanceof ADataSinkMemoryInfo memoryInfo) {
                result.add(entryFor(sink, memoryInfo));
            } else {
                result.add(new Entry(sink.getClass().getSimpleName(), sink.getClass().getName(),
                        MemoryUsageKind.OTHER, -1, -1, false));
            }
        }
        return result;
    }

    private Entry entryFor(ADataSink sink, ADataSinkMemoryInfo memoryInfo) {
        final Iterable<?> data = memoryInfo.getStoredMeasurementData();

        if (data == null) {
            return new Entry(memoryInfo.getMemoryUsageName(), sink.getClass().getName(),
                    memoryInfo.getMemoryUsageKind(), -1, -1, false);
        }

        final long numEntries = AHeapSizeEstimator.count(data);
        final long estimatedBytes = new AHeapSizeEstimator().estimate(data);
        return new Entry(memoryInfo.getMemoryUsageName(), sink.getClass().getName(),
                memoryInfo.getMemoryUsageKind(), numEntries, estimatedBytes, true);
    }

    private List<ADataSink> getDataSinks() {
        if (!(sysMon instanceof NSysMonImpl)) {
            return Collections.emptyList();
        }
        return ((NSysMonImpl) sysMon).getDataSinksForDirectAccess();
    }

    private List<Entry> collectTimedScalars() {
        final List<Entry> result = new ArrayList<>();

        addTimedScalarEntries(result, sysMon.getTimedScalarMeasurements(), "timed scalar");
        addTimedScalarEntries(result, sysMon.getTimedScalarMeasurementsForMonitoring(), "timed scalar (monitoring)");
        return result;
    }

    private void addTimedScalarEntries(List<Entry> result, Map<String, ARingBuffer<AScalarDataPoint>> buffers,
                                       String prefix) {
        if (buffers == null) {
            return;
        }

        for (Map.Entry<String, ARingBuffer<AScalarDataPoint>> bufferEntry : buffers.entrySet()) {
            final ARingBuffer<AScalarDataPoint> buffer = bufferEntry.getValue();
            result.add(new Entry(prefix + " " + bufferEntry.getKey(), ARingBuffer.class.getName(),
                    MemoryUsageKind.TIMED_SCALAR, AHeapSizeEstimator.count(buffer),
                    new AHeapSizeEstimator().estimate(buffer), true));
        }
    }

    private static long sum(List<Entry> entries) {
        long result = 0;
        for (Entry entry : entries) {
            if (entry.estimatedBytes > 0) {
                result += entry.estimatedBytes;
            }
        }
        return result;
    }

    /**
     * One row in the report. A non-reporting sink has {@code reporting == false} and {@code -1} for the measurements;
     *  this keeps it visible on the page instead of silently omitting it.
     */
    public static final class Entry {
        public final String name;
        public final String className;
        public final MemoryUsageKind kind;
        public final long numEntries;
        public final long estimatedBytes;
        public final boolean reporting;

        Entry(String name, String className, MemoryUsageKind kind, long numEntries, long estimatedBytes,
              boolean reporting) {
            this.name = name;
            this.className = className;
            this.kind = kind;
            this.numEntries = numEntries;
            this.estimatedBytes = estimatedBytes;
            this.reporting = reporting;
        }
    }

    public static final class Report {
        public final List<Entry> dataSinks;
        public final List<Entry> timedScalars;
        public final long estimatedTotalBytes;

        Report(List<Entry> dataSinks, List<Entry> timedScalars, long estimatedTotalBytes) {
            this.dataSinks = List.copyOf(dataSinks);
            this.timedScalars = List.copyOf(timedScalars);
            this.estimatedTotalBytes = estimatedTotalBytes;
        }
    }
}
