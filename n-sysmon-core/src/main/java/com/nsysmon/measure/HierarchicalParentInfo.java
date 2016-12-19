package com.nsysmon.measure;

import com.ajjpj.afoundation.collection.mutable.ArrayStack;

public class HierarchicalParentInfo {
    private final String identifier;
    private final  long startTimeNanos;
    private final  boolean isTopEntry;


    public HierarchicalParentInfo(String identifier, long startTimeNanos, boolean isTopEntry, ArrayStack<ASimpleSerialMeasurementImpl> unfinished) {
        this.identifier = identifier;
        this.startTimeNanos = startTimeNanos;
        this.isTopEntry = isTopEntry;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isTopEntry() {
        return isTopEntry;
    }
}
