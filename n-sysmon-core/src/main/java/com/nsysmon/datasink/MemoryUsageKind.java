package com.nsysmon.datasink;

/**
 * Categorizes the kind of measurement data a data sink retains. This makes the memory usage report easier to read:
 *  a raw hierarchy and an aggregated statistics tree have very different memory profiles.
 *
 * @author arno
 */
public enum MemoryUsageKind {
    RAW_HIERARCHY("raw hierarchy"),
    AGGREGATED("aggregated statistics"),
    TIMED_SCALAR("timed scalar"),
    DERIVED("derived data"),
    QUEUE("queue"),
    OTHER("other");

    public final String label;

    MemoryUsageKind(String label) {
        this.label = label;
    }
}
