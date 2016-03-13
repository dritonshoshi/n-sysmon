package com.nsysmon.servlet.correlationflow;

import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CorrelationFlowDataSink implements ADataSink {
    //TODO FOX088S change to round robing + constructor with size
    //TODO FOX088S replace List with Set
    private Map<ACorrelationId, List<ACorrelationId>> data = new ConcurrentHashMap<>();

    @Override
    public void onStartedHierarchicalMeasurement(String identifier) {
    }

    @Override
    public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot root) {
        processMeasurement(root.getStartedFlows());
        processMeasurement(root.getJoinedFlows()); //TODO FOX088S check if this is really the same
    }

    private void processMeasurement(Collection<ACorrelationId> flows) {
	    Set<ACorrelationId> flowsToProcess = new HashSet<>(flows);
	    for (int i = 0; i<flows.size();i++) {
		    Set<ACorrelationId> flowsWithParent = findFlowsWithParent(flowsToProcess);
		    flowsToProcess.removeAll(flowsWithParent);
		    //System.out.println(flowsWithParent.size());
		    addToData(flowsWithParent);
	    }
    }

	private Set<ACorrelationId> findFlowsWithParent(Collection<ACorrelationId> flows) {
		Set<ACorrelationId> rc = new HashSet<>();
		for (ACorrelationId flow : flows) {
			if (flow.getIdParent()==null){
				rc.add(flow);
			}else{
				for (ACorrelationId aCorrelationId : data.keySet()) {
					if (flow.getIdParent().equals(aCorrelationId.getId())){
						rc.add(flow);
					}
				}
			}
		}
		return rc;
	}

	private void addToData(Collection<ACorrelationId> flows) {
		for (ACorrelationId flow : flows) {
			if (flow.getIdParent() == null){
		        data.putIfAbsent(flow, new ArrayList<>());
		    }else {
		        data.putIfAbsent(flow, new ArrayList<>());
		        ACorrelationId parent = findParent(flow);
		        if (parent != null){
		            data.get(parent).add(flow);
		        }
		    }
		}
	}

	private ACorrelationId findParent(ACorrelationId flow) {
        if (flow.getIdParent() == null){
            return null;
        }
        for (ACorrelationId aCorrelationId : data.keySet()) {
            if (aCorrelationId.getId().equals(flow.getIdParent())){
                return aCorrelationId;
            }
        }
        //TODO FOX088S throw error.
        return null;
    }

    @Override
    public void shutdown() throws Exception {
        //nothing to do
    }

    public Map<ACorrelationId, List<ACorrelationId>> getData() {
        return Collections.unmodifiableMap(data);
    }

	public void clearData() {
		data.clear();
	}
}
