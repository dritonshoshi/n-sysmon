package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class SystemLoadOneMinuteMeasurer implements AScalarMeasurer {
    private static final String IDENT_LOAD_1_MIN = "Load";

    private final File procFile = new File("/proc/loadavg");
    private final boolean isWindows;

    public SystemLoadOneMinuteMeasurer(){
        isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) {
    }

    @Override
    public void contributeMeasurements(Map<String, AScalarDataPoint> result, long timestamp, Map<String, Object> mementos) throws IOException {
        //this measurement isn't working on windows
        if (isWindows){
            return;
        }

        final BufferedReader in = new BufferedReader(new FileReader(procFile));
        final String[] raw = in.readLine().split(" ");

        final int load1 = (int)(Double.parseDouble(raw[0])*100);

        result.put(IDENT_LOAD_1_MIN, new AScalarDataPoint(timestamp, IDENT_LOAD_1_MIN, load1, 2));
    }

    @Override public void shutdown() {
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }

    @Override
    public String getGroupnameOfMeasurement(String measurement) {
        if (IDENT_LOAD_1_MIN.equalsIgnoreCase(measurement)) {
            return "System";
        }
        return null;
    }

    @Override
    public String getDescriptionOfMeasurement(String measurement) {
        if (IDENT_LOAD_1_MIN.equalsIgnoreCase(measurement)) {
            return "Average of system load value for the last time period.";
        }
        return null;
    }
}
