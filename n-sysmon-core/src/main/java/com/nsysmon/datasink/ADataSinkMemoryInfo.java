package com.nsysmon.datasink;

/**
 * Implemented by data sinks that retain measurement data in memory. It is deliberately separate from
 *  {@link ADataSink}: sinks that only forward, log or export data need not implement it.
 * <p>
 * The returned iterable is only read by the memory report. Implementations must return a view that is safe to iterate
 *  while measurements are being collected, e.g. a ring buffer or a concurrent collection. The report itself performs
 *  the size estimation, so sinks only have to expose the data they retain.
 *
 * @author arno
 */
public interface ADataSinkMemoryInfo {
    /**
     * @return a read-only view of the measurement data retained by this sink, including collection containers and map
     *  keys that are part of the retained memory. Must not be {@code null}.
     */
    Iterable<?> getStoredMeasurementData();

    /**
     * @return a short, stable name used in the memory report. The default is the simple class name, which is sufficient
     *  if there is only one instance of a sink class.
     */
    default String getMemoryUsageName() {
        return getClass().getSimpleName();
    }

    default MemoryUsageKind getMemoryUsageKind() {
        return MemoryUsageKind.OTHER;
    }
}
