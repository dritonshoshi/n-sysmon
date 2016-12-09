package com.nsysmon.central.queries;

import java.util.HashMap;
import java.util.Map;

public class OverviewResponse {
    private long totalDataEntriesInDb;
    private Map<String, Long> entriesByType = new HashMap<>();

    public long getTotalDataEntriesInDb() {
        return totalDataEntriesInDb;
    }

    public void setTotalDataEntriesInDb(long totalDataEntriesInDb) {
        this.totalDataEntriesInDb = totalDataEntriesInDb;
    }

    public Map<String, Long> getEntriesByType() {
        return entriesByType;
    }
}
