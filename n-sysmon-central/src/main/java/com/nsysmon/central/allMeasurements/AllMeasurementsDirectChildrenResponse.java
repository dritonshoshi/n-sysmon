package com.nsysmon.central.allMeasurements;

import com.nsysmon.datasink.transfer.types.db.HierarchicalDataForStorage;

import java.util.ArrayList;
import java.util.List;

public class AllMeasurementsDirectChildrenResponse {
    private List<HierarchicalDataForStorage> children = new ArrayList<>();

    public List<HierarchicalDataForStorage> getChildren() {
        return children;
    }
}
