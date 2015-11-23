package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;
import com.sun.management.OperatingSystemMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;

public class CpuTotalAndUserMeasurer implements AScalarMeasurer {

    private static final String CPU_TOTAL = "SystemCpuTotal";
    private static final String CPU_USER = "SystemCpuUser";

    private final boolean isWindows;

    public CpuTotalAndUserMeasurer(){
        isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws IOException {
    }

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws IOException {
        //this measurement isn't working on windows
        if (isWindows){
            return;
        }

        final short numFracDigits = 2;
        com.sun.management.OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        final int processCpu = Double.valueOf((Math.round(osBean.getProcessCpuLoad() * 100 * Math.pow(10, numFracDigits)) / Math.pow(10, numFracDigits)) * 100).intValue();
        final int totalCpu = Double.valueOf((Math.round(osBean.getSystemCpuLoad()* 100 * Math.pow(10, numFracDigits)) / Math.pow(10, numFracDigits)) * 100).intValue();

        data.put(CPU_USER, new AScalarDataPoint(timestamp, CPU_USER, processCpu, numFracDigits));
        data.put(CPU_TOTAL, new AScalarDataPoint(timestamp, CPU_TOTAL, totalCpu, numFracDigits));
    }


    @Override public void shutdown() throws Exception {
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }
}
