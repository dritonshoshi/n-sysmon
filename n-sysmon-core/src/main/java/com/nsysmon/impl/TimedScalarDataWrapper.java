package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.immutable.AList;
import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.AScalarDataPoint;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

class TimedScalarDataWrapper implements Runnable {
    private final Map<String, ARingBuffer<AScalarDataPoint>> dataBuffer = new TreeMap<>();
    private final Map<String, ARingBuffer<AScalarDataPoint>> dataBufferMonitoring = new TreeMap<>();
    private final int maxEntriesTotal;
    private final int maxEntriesMonitoring;
    private AList<RobustScalarMeasurerWrapper> timedScalarMeasurers = AList.nil();
    private final Map<String, Object> mementos = new TreeMap<>();

    TimedScalarDataWrapper(int maxEntriesTotal, int maxEntriesMonitoring) {
        this.maxEntriesTotal = maxEntriesTotal;
        this.maxEntriesMonitoring = maxEntriesMonitoring;
    }

    @Override
    public void run() {
        if (NSysMonConfig.isGloballyDisabled()){
            //do nothing it nsysmon is disabled
            return;
        }
        long timestamp = new Date().getTime();
        Map<String, AScalarDataPoint> data = new TreeMap<>();

        for (RobustScalarMeasurerWrapper timedScalarMeasurer : timedScalarMeasurers) {
            timedScalarMeasurer.contributeMeasurements(data, timestamp, mementos);
        }

        data.values().forEach(this::addMeasurement);
    }

    public Map<String, ARingBuffer<AScalarDataPoint>> getMeasurements() {
        return Collections.unmodifiableMap(dataBuffer);
    }

    public Map<String, ARingBuffer<AScalarDataPoint>> getMeasurementsForMonitoring() {
        return Collections.unmodifiableMap(dataBufferMonitoring);
    }

    void refreshMeasurers(AList<RobustScalarMeasurerWrapper> timedScalarMeasurers) {
        for (RobustScalarMeasurerWrapper timedScalarMeasurer : timedScalarMeasurers) {
            timedScalarMeasurer.prepareMeasurements(mementos);
        }
        this.timedScalarMeasurers = timedScalarMeasurers;
    }

    void addMeasurement(AScalarDataPoint... dataPoint) {
        for (AScalarDataPoint point : dataPoint) {
            String name = point.getName();
            if (!dataBuffer.containsKey(name)){
                dataBuffer.put(name, new ARingBuffer<>(AScalarDataPoint.class, maxEntriesTotal));
            }
            dataBuffer.get(name).put(point);

            if (!dataBufferMonitoring.containsKey(name)){
                dataBufferMonitoring.put(name, new ARingBuffer<>(AScalarDataPoint.class, maxEntriesMonitoring));
            }
            dataBufferMonitoring.get(name).put(point);
        }
    }

    public AList<RobustScalarMeasurerWrapper> getTimedScalarMeasurers() {
        return timedScalarMeasurers;
    }
}
