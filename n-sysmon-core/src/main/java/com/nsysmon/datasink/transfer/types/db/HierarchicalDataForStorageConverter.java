package com.nsysmon.datasink.transfer.types.db;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;

import java.util.List;

/**
 * Created by torsten on 11.12.2016.
 */
public class HierarchicalDataForStorageConverter {


    public static HierarchicalDataRootForStorage fromRoot(AHierarchicalDataRoot data) {
        return new HierarchicalDataRootForStorage(data.getJoinedFlows(), data.getStartedFlows(), data.isKilled());
    }

    public static HierarchicalDataForStorage fromChild(AHierarchicalData data, long level) {
        return new HierarchicalDataForStorage(data.getIdentifier(), data.getStartTimeMillis(), data.getParameters(), data.getChildren().isEmpty(), level, Long.toString(level), null);
    }

    public static void fromChilds(List<AHierarchicalData> dataEntries, List<HierarchicalDataForStorage> rc, long level, String localIdentifier, String parentIdentifier) {
        int cnt = 0;
        for (AHierarchicalData dataEntry : dataEntries) {
            HierarchicalDataForStorage result = new HierarchicalDataForStorage(dataEntry.getIdentifier(), dataEntry.getStartTimeMillis(), dataEntry.getParameters(), dataEntry.getChildren().isEmpty(), level, localIdentifier, parentIdentifier);
            rc.add(result);
            fromChilds(dataEntry.getChildren(), rc, level + 1, localIdentifier + "." + cnt++, localIdentifier);
        }
    }
}
