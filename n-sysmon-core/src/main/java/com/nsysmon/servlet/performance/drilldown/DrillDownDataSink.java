package com.nsysmon.servlet.performance.drilldown;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.servlet.performance.AMinMaxAvgData;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author arno
 */
class DrillDownDataSink implements ADataSink {
    private volatile boolean isActive = false;

    private final ConcurrentHashMap<String, AMinMaxAvgData> rootMap = new ConcurrentHashMap<>();

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean isActive() {
        return isActive;
    }

    public synchronized void clear() {
        rootMap.clear();
    }

    public Map<String, AMinMaxAvgData> getData() {
        return Collections.unmodifiableMap(rootMap);
    }

    @Override public void onStartedHierarchicalMeasurement(String identifier) {
    }

    @Override
    public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        if(isActive) {
            synchronizedCollect(data.getRootNode());
        }
    }

    @Override public void shutdown() {
    }

    private synchronized void synchronizedCollect(AHierarchicalData newData) { //TODO change this to a non-blocking implementation
        recCollect(newData, rootMap);
    }

    private void recCollect(AHierarchicalData data, Map<String, AMinMaxAvgData> parentMap) {
        final AMinMaxAvgData prev = parentMap.get(data.getIdentifier());

        final Map<String, AMinMaxAvgData> childMap;

        if(prev == null) {
            final AMinMaxAvgData newData = new AMinMaxAvgData(data.isSerial(), data.getDurationNanos());
            childMap = newData.getChildren();
            parentMap.put(data.getIdentifier(), newData);
        }
        else {
            childMap = prev.getChildren();
            parentMap.put(data.getIdentifier(), prev.withDataPoint(data.isSerial(), data.getDurationNanos()));
        }

        for(AHierarchicalData childData: data.getChildren()) {
            recCollect(childData, childMap);
        }
    }
}
