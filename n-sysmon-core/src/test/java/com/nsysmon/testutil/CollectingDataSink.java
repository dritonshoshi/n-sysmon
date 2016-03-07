package com.nsysmon.testutil;

import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arno
 */
public class CollectingDataSink implements ADataSink {
    public int numStarted = 0;
    public List<AHierarchicalDataRoot> data = new ArrayList<>();

    @Override public void onStartedHierarchicalMeasurement(String identifier) {
        if("Garbage Collection".equals(identifier)) {
            return;
        }

        numStarted += 1;
    }

    @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        if("Garbage Collection".equals(data.getRootNode().getIdentifier())) {
            return;
        }

        this.data.add(data);
    }

    @Override public void shutdown() {
    }
}
