package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonAware;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.data.AScalarDataPoint;
import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Similar to AJmxGcMeasurer but this one measures the average GC time per interval.
 */
public class AJmxGcAvgMeasurer implements AScalarMeasurer, NSysMonAware {
    public static final String IDENT_GC_TRACE_ROOT = "Garbage Collection";

    public static final String KEY_PREFIX_DURATION = "dur:";
    public static final String KEY_PREFIX_MIN = "min";
    public static final String KEY_PREFIX_AVG = "avg";
    public static final String KEY_PREFIX_MAX = "max";
    public static final String KEY_PREFIX_CNT = "cnt";

    public static final String KEY_MEM_TOTAL = "total";
    public static final String KEY_MEM_USED = "used";
    public static final String KEY_MEM_FREE = "free";

    private static final String SCALAR_PREFIX_GC_PER_INTERVAL = "gc-ival:";
    private static final String SCALAR_PREFIX_MEMORY_PER_INTERVAL = "mem-ival:";

    private volatile NSysMonApi sysMon;

    private final GcNotificationListener listener = new GcNotificationListener();

    private final Map<String, List<Long>> gcDurationsNsPerInterval = new ConcurrentHashMap<>();

    @Override
    public void setNSysMon(NSysMonApi sysMon) {
        this.sysMon = sysMon;

        for (GarbageCollectorMXBean gcbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            final NotificationEmitter emitter = (NotificationEmitter) gcbean;
            emitter.addNotificationListener(listener, null, null);
        }
    }

    @Override
    public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
        for (var entry : gcDurationsNsPerInterval.entrySet()) {
            List<Long> durationsPerInterval = entry.getValue();
            Optional<Long> min = durationsPerInterval.stream().min(Long::compare);
            Optional<Long> max = durationsPerInterval.stream().max(Long::compare);
            long minDuration = min.isPresent() ? min.get() : 0;
            long maxDuration = max.isPresent() ? max.get() : 0;
            long sumDuration = durationsPerInterval.stream().mapToLong(Long::longValue).sum();
            long avgDuration = sumDuration / durationsPerInterval.size();

            String name = SCALAR_PREFIX_GC_PER_INTERVAL + entry.getKey() + ":" + KEY_PREFIX_DURATION + KEY_PREFIX_MIN;
            data.put(name, new AScalarDataPoint(timestamp, name, minDuration, 0));
            name = SCALAR_PREFIX_GC_PER_INTERVAL + entry.getKey() + ":" + KEY_PREFIX_DURATION + KEY_PREFIX_AVG;
            data.put(name, new AScalarDataPoint(timestamp, name, avgDuration, 0));
            name = SCALAR_PREFIX_GC_PER_INTERVAL + entry.getKey() + ":" + KEY_PREFIX_DURATION + KEY_PREFIX_MAX;
            data.put(name, new AScalarDataPoint(timestamp, name, maxDuration, 0));
            name = SCALAR_PREFIX_GC_PER_INTERVAL + entry.getKey() + ":" + KEY_PREFIX_DURATION + KEY_PREFIX_CNT;
            data.put(name, new AScalarDataPoint(timestamp, name, durationsPerInterval.size(), 0));

        }
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        String name = SCALAR_PREFIX_MEMORY_PER_INTERVAL + KEY_MEM_TOTAL;
        data.put(name, new AScalarDataPoint(timestamp, name, totalMemory, 0));
        name = SCALAR_PREFIX_MEMORY_PER_INTERVAL + KEY_MEM_USED;
        data.put(name, new AScalarDataPoint(timestamp, name, usedMemory, 0));
        name = SCALAR_PREFIX_MEMORY_PER_INTERVAL + KEY_MEM_FREE;
        data.put(name, new AScalarDataPoint(timestamp, name, freeMemory, 0));
        gcDurationsNsPerInterval.clear();
    }

    @Override
    public void shutdown() throws Exception {
        for (GarbageCollectorMXBean gcbean : ManagementFactory.getGarbageCollectorMXBeans()) {
            final NotificationEmitter emitter = (NotificationEmitter) gcbean;
            try {
                emitter.removeNotificationListener(listener, null, null);
            } catch (ListenerNotFoundException e) { // ignore this to facilitate repeated shutdown
            }
        }
    }

    @Override
    public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }

    class GcNotificationListener implements NotificationListener {
        private AHierarchicalData toHierarchicalData(GarbageCollectionNotificationInfo info) {
            final long durationNanos = info.getGcInfo().getDuration() * 1000;
            final long measureTs = System.currentTimeMillis();
            final String gcType = shortenGcType(info.getGcAction());

            gcDurationsNsPerInterval.putIfAbsent(gcType, new ArrayList<>());
            gcDurationsNsPerInterval.get(gcType).add(durationNanos);

            final Map<String, String> paramMap = new HashMap<>();

            final AHierarchicalData byGcTypeNode = new AHierarchicalData(true, measureTs, durationNanos, gcType, Collections.emptyMap(), Collections.emptyList(), false);
            return new AHierarchicalData(true, measureTs, durationNanos, IDENT_GC_TRACE_ROOT, paramMap, Collections.singletonList(byGcTypeNode), false);
        }

        @Override
        public void handleNotification(Notification notification, Object handback) {
            //we only handle GARBAGE_COLLECTION_NOTIFICATION notifications here
            if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                final GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
                sysMon.injectSyntheticMeasurement(new AHierarchicalDataRoot(toHierarchicalData(info), Collections.emptyList(), Collections.emptyList()));
            }
        }
    }

    private String shortenGcType(String gcAction) {
        return gcAction
                .replace("start of ", "")
                .replace("end of ", "")
                .replace("GC", "")
                .trim();
    }

    @Override
    public String getGroupnameOfMeasurement(String measurement) {
        Map<String, AScalarDataPoint> data = new HashMap<>();
        contributeMeasurements(data, 0, Collections.emptyMap());
        if (data.containsKey(measurement)) {
            return "avg GC";
        }
        return null;
    }

    @Override
    public String getDescriptionOfMeasurement(String measurement) {
        return switch (measurement) {
            case "mem-ival:free" -> "free memory in reserved memory";
            case "mem-ival:used" -> "used memory in reserved memory";
            case "mem-ival:total" -> "memory the vm uses from system";
            case "gc-ival:minor:dur:min" -> "min duration";
            case "gc-ival:minor:dur:max" -> "max duration";
            case "gc-ival:minor:dur:avg" -> "avg duration";
            case "gc-ival:minor:dur:cnt" -> "no of calls";
            case "gc-ival:major:dur:min" -> "min duration";
            case "gc-ival:major:dur:max" -> "max duration";
            case "gc-ival:major:dur:avg" -> "avg duration";
            case "gc-ival:major:dur:cnt" -> "no of calls";
            default -> {
                if (measurement.startsWith(SCALAR_PREFIX_GC_PER_INTERVAL)
                        ||measurement.startsWith(SCALAR_PREFIX_MEMORY_PER_INTERVAL)) {
                    System.out.println("unknown measurement '" + measurement + "'");
                }
                yield null;
            }
        };
    }

}
