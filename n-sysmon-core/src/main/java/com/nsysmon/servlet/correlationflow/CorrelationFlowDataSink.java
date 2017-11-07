package com.nsysmon.servlet.correlationflow;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.NSysMon;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CorrelationFlowDataSink implements ADataSink {
	private static final NSysMonLogger LOG = NSysMonLogger.get(CorrelationFlowDataSink.class);
	private ARingBuffer<CorrelationFlowDetails> dataBuffer;

	public CorrelationFlowDataSink(int maxNumDetails){
		dataBuffer = new ARingBuffer<>(CorrelationFlowDetails.class, maxNumDetails);
	}

    @Override
    public void onStartedHierarchicalMeasurement(String identifier) {
    }

    @Override
    public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot root) {
        processMeasurement(root.getStartedFlows(), root.getJoinedFlows());
    }

    private void processMeasurement(Collection<ACorrelationId> startedFlows,Collection<ACorrelationId> joinedFlows) {
		if (NSysMon.get().getConfig().correlationFlowDisabled) {
			return;
		}
	    Set<ACorrelationId> flowsToProcess = new HashSet<>(startedFlows);
	    flowsToProcess.addAll(joinedFlows);
		int loops = flowsToProcess.size();
	    for (int i = 0; i< loops; i++) {
		    Set<ACorrelationId> flowsWithParent = findFlowsWithParent(flowsToProcess);
		    flowsToProcess.removeAll(flowsWithParent);
		    //System.out.println(flowsWithParent.size());
		    addToData(flowsWithParent);
	    }
		Set<ACorrelationId> flowsWithParent = findFlowsWithParent(flowsToProcess);
		flowsToProcess.removeAll(flowsWithParent);

	    flowsToProcess.forEach(aCorrelationId -> {
		    LOG.warn("Correlation " + aCorrelationId.getId() + " with description '" + aCorrelationId.getQualifier() + "' could not be saved, because parent isn't stored!");
	    });
    }

	private Set<ACorrelationId> findFlowsWithParent(Collection<ACorrelationId> flows) {
		Set<ACorrelationId> rc = new HashSet<>();
		for (ACorrelationId flow : flows) {
			if (flow.getIdParent()==null){
				rc.add(flow);
			}else{
				dataBuffer.forEach(correlationFlowDetails -> {
					if (flow.getIdParent().equals(correlationFlowDetails.getaCorrelationId().getId())){
						rc.add(flow);
					}
				});
			}
		}
		return rc;
	}

	private void addToData(Collection<ACorrelationId> flows) {
		for (ACorrelationId flow : flows) {
			if (flow.getIdParent() == null){
				dataBuffer.put(new CorrelationFlowDetails(flow, new HashSet<>()));
		    }else {
				dataBuffer.put(new CorrelationFlowDetails(flow, new HashSet<>()));
		        ACorrelationId parent = findParent(flow);
		        if (parent != null){
			        dataBuffer.forEach(correlationFlowDetails -> {
				        if (correlationFlowDetails.getaCorrelationId().equals(parent)){
					        correlationFlowDetails.getChilds().add(flow);
				        }
			        });
		        }
		    }
		}
	}

	private ACorrelationId findParent(ACorrelationId flow) {
        if (flow == null || flow.getIdParent() == null){
            return null;
        }
        for (CorrelationFlowDetails flowDetail : dataBuffer) {
            if (flowDetail.getaCorrelationId() != null && flowDetail.getaCorrelationId().getId() != null && flowDetail.getaCorrelationId().getId().equals(flow.getIdParent())){
                return flowDetail.getaCorrelationId();
            }
        }
        return null;
    }

    @Override
    public void shutdown() throws Exception {
        //nothing to do
    }

    public Map<ACorrelationId, Set<ACorrelationId>> getDataAsMap() {

	    //this way it is easier to parse for the page
	    Map<ACorrelationId, Set<ACorrelationId>> rc = new HashMap<>();
	    for (CorrelationFlowDetails correlationFlowDetails : dataBuffer) {
		    rc.put(correlationFlowDetails.getaCorrelationId(), correlationFlowDetails.getChilds());
	    }
	    return Collections.unmodifiableMap(rc);
    }

	public void clearData() {
		dataBuffer.clear();
	}
}
