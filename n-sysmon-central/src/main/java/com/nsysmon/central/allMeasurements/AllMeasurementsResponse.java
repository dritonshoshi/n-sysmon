package com.nsysmon.central.allMeasurements;

import com.nsysmon.datasink.transfer.types.db.HierarchicalDataForStorage;

import java.util.ArrayList;
import java.util.List;

public class AllMeasurementsResponse {
    private List<HierarchicalDataForStorage> entries = new ArrayList<>();

    public List<HierarchicalDataForStorage> getEntries() {
        return entries;
    }

    public void setEntries(List<HierarchicalDataForStorage> entries) {
        this.entries = entries;
    }
}
