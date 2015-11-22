package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.ajjpj.afoundation.function.AFunction1;
import com.ajjpj.afoundation.io.AFile;
import com.nsysmon.data.AScalarDataPoint;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author arno
 */
public class CpuTotalAndUserMeasurer implements AScalarMeasurer {
    private static final AFile PROC_STAT_FILE = new AFile("/proc/stat", Charset.defaultCharset());
    private static final AFile PROC_SELF_STAT_FILE = new AFile("/proc/self/stat", Charset.defaultCharset());

    private static final String CPU_TOTAL = "SystemCpuTotal";
    private static final String CPU_USER = "SystemCpuUser";
    private static final Pattern COMPILE = Pattern.compile("\\s+");

    private final boolean isWindows;

    public CpuTotalAndUserMeasurer(){
        isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws IOException {
        //this measurement isn't working on windows
        if (isWindows){
            return;
        }
        mementos.put(CPU_TOTAL, createSnapshotTotal());
        mementos.put(CPU_USER, createSnapshotUser());
    }

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws IOException {
        //this measurement isn't working on windows
        if (isWindows){
            return;
        }
        contributeCpuTotal(data, timestamp, mementos);
        contributeCpuUser(data, timestamp, mementos);
    }

    private void contributeCpuTotal(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws IOException {
        final Map<String, SnapshotTotal> allCurrent = createSnapshotTotal();
        @SuppressWarnings("unchecked")
        final Map<String, SnapshotTotal> allPrev = (Map<String, SnapshotTotal>) mementos.get(CPU_TOTAL);

        final int numCpus = allCurrent.size() - 1;
        final SnapshotTotal current = allCurrent.get("cpu");
        final SnapshotTotal prev = allPrev.get("cpu");

        final long diffTime = current.timestamp - prev.timestamp;
        if(diffTime <= 0) {
            return;
        }

        final long idleJiffies   = current.idle   - prev.idle;
        final long stolenJiffies = current.stolen - prev.stolen;

        // 'baseline' - 100% for a single CPU, <# cpus>*100% for 'total'
        final long fullJiffies = diffTime * numCpus / 10;

        // reduce the theoretical 'full' capacity by 'stolen' cycles
        final long availJiffies = fullJiffies - stolenJiffies;

        final long usedJiffies = availJiffies - idleJiffies;

        final long usedPerMill = usedJiffies * 10;

        data.put(CPU_TOTAL, new AScalarDataPoint(timestamp, CPU_TOTAL, usedPerMill, 1));
    }

    private void contributeCpuUser(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws IOException {
        //this measurement isn't working on windows
        if (isWindows){
            return;
        }
        final SnapshotUser prev = (SnapshotUser) mementos.get(CPU_USER);
        final SnapshotUser current = createSnapshotUser();

        final long diffTime = current.timestamp - prev.timestamp;

        final long userJiffies = current.userJiffies - prev.userJiffies;

        final long userPerMill = userJiffies * 10*1000 / diffTime;

        data.put(CPU_USER, new AScalarDataPoint(timestamp, CPU_USER, userPerMill, 1));
    }

    private Map<String, SnapshotTotal> createSnapshotTotal() throws IOException {
        return PROC_STAT_FILE.iterate(new AFunction1<Iterator<String>, Map<String, SnapshotTotal>, RuntimeException>() { //TODO nothrow
            @Override public Map<String, SnapshotTotal> apply(Iterator<String> iter) {
                final Map<String, SnapshotTotal> result = new HashMap<>();

                while(iter.hasNext()) {
                    final String line = iter.next();
                    final String[] split = COMPILE.split(line);

                    if(split[0].startsWith("cpu")) {
                        final long idle = Long.valueOf(split[4]);
                        final long stolen = split.length >= 8 ? Long.valueOf(split[8]) : 0;
                        result.put(split[0], new SnapshotTotal(idle, stolen));
                    }
                }

                return result;
            }
        });
    }

    private SnapshotUser createSnapshotUser() throws IOException {
        final String raw = PROC_SELF_STAT_FILE.lines().get(0);
        final String[] split = COMPILE.split(raw);

        final long userJiffies = Long.valueOf(split[13]);
        final long kernelJiffies = Long.valueOf(split[14]);

        return new SnapshotUser(userJiffies, kernelJiffies);
    }

    private static class SnapshotUser {
        final long timestamp = System.currentTimeMillis();
        final long userJiffies;
        final long kernelJiffies;

        SnapshotUser(long userJiffies, long kernelJiffies) {
            this.userJiffies = userJiffies;
            this.kernelJiffies = kernelJiffies;
        }
    }

    @Override public void shutdown() throws Exception {
    }

    static class SnapshotTotal {
        public final long timestamp = System.currentTimeMillis();
        public final long idle;
        public final long stolen;

        SnapshotTotal(long idle, long stolen) {
            this.idle = idle;
            this.stolen = stolen;
        }
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }
}
