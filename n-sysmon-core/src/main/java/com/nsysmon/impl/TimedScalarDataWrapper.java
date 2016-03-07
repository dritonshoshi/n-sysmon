package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.immutable.AList;
import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.AScalarDataPoint;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class TimedScalarDataWrapper implements Runnable {
    private final Map<String, ARingBuffer<AScalarDataPoint>> dataBuffer = new TreeMap<>();
    private final int maxEntries;
    private AList<RobustScalarMeasurerWrapper> timedScalarMeasurers = AList.nil();
    private Map<String, Object> mementos = new TreeMap<>();

    public TimedScalarDataWrapper(int maxEntries) {
        this.maxEntries = maxEntries;
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

    public void refreshMeasurers(AList<RobustScalarMeasurerWrapper> timedScalarMeasurers) {
        for (RobustScalarMeasurerWrapper timedScalarMeasurer : timedScalarMeasurers) {
            timedScalarMeasurer.prepareMeasurements(mementos);
        }
        this.timedScalarMeasurers = timedScalarMeasurers;
    }

    public void addMeasurement(AScalarDataPoint... dataPoint) {
        for (AScalarDataPoint point : dataPoint) {
            String name = point.getName();
            if (!dataBuffer.containsKey(name)){
                dataBuffer.put(name, new ARingBuffer<>(AScalarDataPoint.class, maxEntries));
            }
            dataBuffer.get(name).put(point);
        }
    }
}
