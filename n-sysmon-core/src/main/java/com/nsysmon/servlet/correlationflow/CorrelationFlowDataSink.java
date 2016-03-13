package com.nsysmon.servlet.correlationflow;

import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CorrelationFlowDataSink implements ADataSink {
    Map<String, List<String>> data = new ConcurrentHashMap<>();

    @Override
    public void onStartedHierarchicalMeasurement(String identifier) {
    }

    @Override
    public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot root) {
        processMeasurement(root.getStartedFlows());
        processMeasurement(root.getJoinedFlows()); //TODO FOX088S check if this is really the same
    }

    private void processMeasurement(Collection<ACorrelationId> flows) {
        for (ACorrelationId flow : flows) {
            if (flow.getIdParent() == null){
                data.putIfAbsent(flow.getId(), new ArrayList<>());
            }else {
                data.putIfAbsent(flow.getIdParent(), new ArrayList<>());
                data.get(flow.getIdParent()).add(flow.getId());
            }
        }
    }

    @Override
    public void shutdown() throws Exception {
        //nothing to do
    }
}
