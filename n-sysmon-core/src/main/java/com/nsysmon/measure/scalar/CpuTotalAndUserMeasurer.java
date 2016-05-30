package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;
import com.sun.management.OperatingSystemMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;

public class CpuTotalAndUserMeasurer implements AScalarMeasurer {

    private static final String CPU_TOTAL = "CPU Total";
    private static final String CPU_USER = "CPU User";
    private static final String CPU_AVAILABLE = "cpu:available";
    private static final String CPU_SELF_KERNEL = "cpu:self-kernel";

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

    @Override
    public String getGroupnameOfMeasurement(String measurement) {
        if (CPU_TOTAL.equalsIgnoreCase(measurement)) {
            return "System";
        } else if (CPU_USER.equalsIgnoreCase(measurement)) {
            return "System";
        } else if (CPU_AVAILABLE.equalsIgnoreCase(measurement)) {
            return "System";
        } else if (CPU_SELF_KERNEL.equalsIgnoreCase(measurement)) {
            return "System";
        }
        return null;
    }

    @Override
    public String getDescriptionOfMeasurement(String measurement) {
        if (CPU_TOTAL.equalsIgnoreCase(measurement)) {
            return "Total usage of the CPU including everything on the system.";
        } else if (CPU_USER.equalsIgnoreCase(measurement)) {
            return "CPU usage by this user.";
        } else if (CPU_AVAILABLE.equalsIgnoreCase(measurement)) {
            return "CPU available overall.";
        } else if (CPU_SELF_KERNEL.equalsIgnoreCase(measurement)) {
            return "CPU usage of kernel-threads.";
        }
        return null;
    }

}
