package com.nsysmon.servlet.performance;

import com.nsysmon.config.log.NSysMonLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author arno
 */
public class AMinMaxAvgData {
    private final boolean isSerial;
    private final int totalNumInContext;
    private final long minNanos;
    private final long maxNanos;
    private final long avgNanos;
    private final long totalNanos;

    private final Map<String, AMinMaxAvgData> children;

    private static final NSysMonLogger log = NSysMonLogger.get(AMinMaxAvgData.class);

    public AMinMaxAvgData(boolean isSerial, long initialNanos) {
        this(isSerial, 1, initialNanos, initialNanos, initialNanos, initialNanos, new HashMap<>(0));
    }

    public AMinMaxAvgData(boolean isSerial, int totalNumInContext, long minNanos, long maxNanos, long avgNanos, long totalNanos, Map<String, AMinMaxAvgData> children) {
        this.isSerial = isSerial;
        this.totalNumInContext = totalNumInContext;
        this.minNanos = minNanos;
        this.maxNanos = maxNanos;
        this.avgNanos = avgNanos;
        this.totalNanos = totalNanos;

        this.children = children;
        //TODO FOX088S check if it is save to change this to local array with new instance like below
        //        this.children.clear();
        //        this.children.putAll(children);
    }

    public AMinMaxAvgData withDataPoint(boolean isSerial, long durationNanos) {
        if(isSerial != this.isSerial) {
            log.error (new IllegalArgumentException("both parallel and serial measurements at the same level with the same identifier - ignoring measurement"));
            return this;
        }

        return new AMinMaxAvgData(isSerial,
                totalNumInContext+1,
                Math.min(minNanos, durationNanos),
                Math.max(maxNanos, durationNanos),
                (avgNanos * totalNumInContext + durationNanos) / (totalNumInContext + 1), // this should be safe against overflow
                totalNanos + durationNanos,
                children);
    }

    public boolean isSerial() {
        return isSerial;
    }

    public long getTotalNanos() {
        return totalNanos;
    }

    public int getTotalNumInContext() {
        return totalNumInContext;
    }

    public long getMinNanos() {
        return minNanos;
    }

    public long getMaxNanos() {
        return maxNanos;
    }

    public long getAvgNanos() {
        return avgNanos;
    }

    public Map<String, AMinMaxAvgData> getChildren() {
        return children;
    }
}
