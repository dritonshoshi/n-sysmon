package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.immutable.AList;
import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.AScalarDataPoint;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class TimedScalarMeasurer implements Runnable {
    private final Map<String, ARingBuffer<AScalarDataPoint>> dataBuffer = new TreeMap<>();
    private final int maxEntries;
    private AList<RobustScalarMeasurerWrapper> timedScalarMeasurers = AList.nil;
    private Map<String, Object> mementos = new TreeMap<>();

    public TimedScalarMeasurer(int maxEntries) {
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

        for (String keyNewData : data.keySet()) {
            if (!dataBuffer.containsKey(keyNewData)) {
                dataBuffer.put(keyNewData, new ARingBuffer<>(AScalarDataPoint.class, maxEntries));
            }
            dataBuffer.get(keyNewData).put(data.get(keyNewData));
        }


        //System.out.println(new Date() + " " + Thread.currentThread().getName() + " is Running Delayed Task, having " + timedScalarMeasurers.size() + " measurers.");
        //System.out.println(new Date());
    }

    public Map<String, ARingBuffer<AScalarDataPoint>> getMeasurements() {
        return dataBuffer;
        //TODO FOX088S make readonly-copy
    }

    public void refreshMeasurers(AList<RobustScalarMeasurerWrapper> timedScalarMeasurers) {
        for (RobustScalarMeasurerWrapper timedScalarMeasurer : timedScalarMeasurers) {
            timedScalarMeasurer.prepareMeasurements(mementos);
        }
        this.timedScalarMeasurers = timedScalarMeasurers;
    }

}
