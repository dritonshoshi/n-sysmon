package com.nsysmon.datasink.transfer.types;

import com.nsysmon.datasink.transfer.types.db.HierarchicalDataForStorage;
import com.nsysmon.datasink.transfer.types.db.HierarchicalDataRootForStorage;

import java.util.List;

public class InnerTransferMeasurementsRequest {
    private final HierarchicalDataRootForStorage root;
    private final List<HierarchicalDataForStorage> children;

    public InnerTransferMeasurementsRequest(HierarchicalDataRootForStorage root, List<HierarchicalDataForStorage> children) {
        this.root=root;
        this.children=children;
    }

    public HierarchicalDataRootForStorage getRoot() {
        return root;
    }

    public List<HierarchicalDataForStorage> getChildren() {
        return children;
    }
}
