package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.ajjpj.afoundation.function.AFunction1;
import com.ajjpj.afoundation.io.AFile;
import com.nsysmon.NSysMon;
import com.nsysmon.data.AScalarDataPoint;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.Charset;
import java.util.*;


/**
 * @author arno
 */
public class ACpuUtilizationMeasurer implements AScalarMeasurer {
    public static final AFile PROC_STAT_FILE = new AFile("/proc/stat", Charset.defaultCharset());
    public static final AFile PROC_STAT_FILE_MACOS_ = new AFile("sysctl -a hw", Charset.defaultCharset());

    public static final AFile PROC_CPUINFO_FILE = new AFile("/proc/cpuinfo", Charset.defaultCharset());

    public static final String KEY_PREFIX = "cpu:";
    public static final String KEY_MEMENTO = KEY_PREFIX;
    public static final String KEY_AVAILABLE = KEY_PREFIX + "available";
    public static final String KEY_ALL_USED = KEY_PREFIX + "all-used";
    public static final String KEY_PREFIX_MHZ = KEY_PREFIX + "freq-mhz:";
    public static final String KEY_SELF_KERNEL = KEY_PREFIX + "self-kernel";

    @Override
    public void prepareMeasurements(Map<String, Object> mementos) throws IOException {
        //this measurement isn't working on windows
        if (NSysMon.isWindows()) {
            return;
        }
        if (NSysMon.isMacOS()) {
            return;
        }
        mementos.put(KEY_MEMENTO, createSnapshot());
    }

    private void fillForWindows(Map<String, AScalarDataPoint> data, long timestamp) {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
            long value = bean.getAvailableProcessors() * 1000L;
            data.put(KEY_AVAILABLE, new AScalarDataPoint(timestamp, KEY_AVAILABLE, Math.max(0, value), 1));
            value = (long) (bean.getSystemCpuLoad() * 1000);
            data.put(KEY_SELF_KERNEL, new AScalarDataPoint(timestamp, KEY_SELF_KERNEL, Math.max(0, value), 1));
        }
    }

    @Override
    public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws IOException {
        //this measurement isn't working on windows
        if (NSysMon.isWindows()) {
            fillForWindows(data, timestamp);
            return;
        }
        if (NSysMon.isMacOS()) {
            fillForWindows(data, timestamp);
            return;
        }
        final Map<String, Snapshot> allCurrent = createSnapshot();
        @SuppressWarnings("unchecked") final Map<String, Snapshot> allPrev = (Map<String, Snapshot>) mementos.get(KEY_MEMENTO);

        final int numCpus = allCurrent.size() - 1;
        final Snapshot current = allCurrent.get("cpu");

        final Snapshot prev = (allPrev != null ? allPrev.get("cpu") : null);

        final long diffTime = current.timestamp - (prev != null ? prev.timestamp : 0);
        if (diffTime <= 0) {
            return;
        }
        final long idleJiffies = current.idle - (prev != null ? prev.idle : 0);
        final long stolenJiffies = current.stolen - (prev != null ? prev.stolen : 0);

        // 'baseline' - 100% for a single CPU, <# cpus>*100% for 'total'
        final long fullJiffies = diffTime * numCpus / 10;

        // reduce the theoretical 'full' capacity by 'stolen' cycles
        final long availJiffies = fullJiffies - stolenJiffies;

        final long usedJiffies = availJiffies - idleJiffies;

        final long usedPerMill = usedJiffies * 10;

        data.put(KEY_AVAILABLE, new AScalarDataPoint(timestamp, KEY_AVAILABLE, availJiffies / (diffTime / 10) * 1000, 1));
        data.put(KEY_ALL_USED, new AScalarDataPoint(timestamp, KEY_ALL_USED, usedPerMill, 1));

        contributeFreq(data, timestamp);
    }

    private void contributeFreq(Map<String, AScalarDataPoint> data, long timestamp) throws IOException {
        final Map<String, Integer> frequencies = new HashMap<>();

        String cpuId = null;
        String mhz = null;

        for (String line : PROC_CPUINFO_FILE.lines()) {
            if (line.contains("processor")) {
                cpuId = line.substring(line.indexOf(':') + 1).trim();
            }
            if (line.contains("MHz")) {
                mhz = line.substring(line.indexOf(':') + 1).trim();
            }
            if (!Objects.isNull(cpuId) && !Objects.isNull(mhz)) {
                frequencies.put(cpuId, Float.valueOf(mhz).intValue());
                cpuId = null;
                mhz = null;
            }
        }

        for (String cpu : frequencies.keySet()) {
            final String key = KEY_PREFIX_MHZ + cpu;
            data.put(key, new AScalarDataPoint(timestamp, key, frequencies.get(cpu), 0));
        }
    }

    private Map<String, Snapshot> createSnapshot() throws IOException {
        return PROC_STAT_FILE.iterate((AFunction1<Iterator<String>, Map<String, Snapshot>, RuntimeException>) iter -> {
            final Map<String, Snapshot> result = new HashMap<>();

            while (iter.hasNext()) {
                final String line = iter.next();
                final String[] split = line.split("\\s+");

                if (split[0].startsWith("cpu")) {
                    final long idle = Long.parseLong(split[4]);
                    final long stolen = split.length >= 8 ? Long.parseLong(split[8]) : 0;
                    result.put(split[0], new Snapshot(idle, stolen));
                }
            }

            return result;
        });
    }

    @Override
    public void shutdown() throws Exception {
    }

    static class Snapshot {
        public final long timestamp = System.currentTimeMillis();
        public final long idle;
        public final long stolen;

        Snapshot(long idle, long stolen) {
            this.idle = idle;
            this.stolen = stolen;
        }
    }

    @Override
    public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }

    @Override
    public List<String> getConfigurationParameters() {
        return Arrays.asList(KEY_SELF_KERNEL, KEY_ALL_USED);
    }

    @Override
    public String getGroupnameOfMeasurement(String measurement) {
        Map<String, AScalarDataPoint> data = new HashMap<>();
        try {
            Map<String, Object> mementos = new HashMap<>();
            contributeMeasurements(data, 0, mementos);
            if (data.containsKey(measurement)) {
                return "CPU";
            }
        } catch (IOException e) {
            // not relevant, it is not this group
        }
        return null;
    }

    @Override
    public String getDescriptionOfMeasurement(String measurement) {
        switch (measurement) {
            case KEY_SELF_KERNEL:
                return "CPU utilization of the NSysMon process kernel mode (0.0 - 1000.0 per CPU)";
            case KEY_AVAILABLE:
                return "Overall available CPU capacity (0.0 - 1000.0 per CPU)";
            case KEY_ALL_USED:
                return "Overall CPU utilization (0.0 - 1000.0 per CPU)";
            default:
                if (measurement.startsWith(KEY_PREFIX_MHZ)) {
                    return "CPU frequency in MHz";
                }
                return null;
        }
    }
}
