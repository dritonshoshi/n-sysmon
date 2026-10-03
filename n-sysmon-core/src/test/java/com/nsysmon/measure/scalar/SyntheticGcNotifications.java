package com.nsysmon.measure.scalar;

import javax.management.Notification;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds <code>GarbageCollectionNotificationInfo</code> payloads without having to wait for a real GC. The JDK refuses to
 * deserialize arbitrary data, so the composite types have to mirror the ones the JVM publishes.
 */
final class SyntheticGcNotifications {

    private SyntheticGcNotifications() {
    }

    private static final String MEMORY_USAGE_TYPE = "java.lang.management.MemoryUsage";
    private static final String MAP_TYPE = "Map<java.lang.String,java.lang.management.MemoryUsage>";
    private static final String GC_INFO_TYPE = "com.sun.management.GcInfo";
    private static final String GC_NOTIFICATION_TYPE = "com.sun.management.gcnotification";

    private static final String[] MEMORY_USAGE_ITEMS = {"init", "used", "committed", "max"};
    private static final String[] MAP_ITEMS = {"key", "value"};

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CompositeType memoryUsageType() throws Exception {
        final OpenType[] types = {SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG};
        return new CompositeType(MEMORY_USAGE_TYPE, "memory usage", MEMORY_USAGE_ITEMS, MEMORY_USAGE_ITEMS, types);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CompositeType mapRowType() throws Exception {
        final OpenType[] types = {SimpleType.STRING, memoryUsageType()};
        return new CompositeType(MAP_TYPE, "map row", MAP_ITEMS, MAP_ITEMS, types);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TabularType memoryUsageMapType() throws Exception {
        return new TabularType(MAP_TYPE, "memory usage map", mapRowType(), new String[]{"key"});
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static TabularDataSupport toTabularData(Map<String, MemoryUsage> memoryUsages) throws Exception {
        final CompositeType rowType = mapRowType();
        final CompositeType memoryType = memoryUsageType();
        final TabularDataSupport result = new TabularDataSupport(memoryUsageMapType());
        for (Map.Entry<String, MemoryUsage> e : memoryUsages.entrySet()) {
            final MemoryUsage usage = e.getValue();
            result.put(new CompositeDataSupport(rowType, MAP_ITEMS, new Object[]{e.getKey(),
                    new CompositeDataSupport(memoryType, MEMORY_USAGE_ITEMS,
                            new Object[]{usage.getInit(), usage.getUsed(), usage.getCommitted(), usage.getMax()})}));
        }
        return result;
    }

    /**
     * @return a CompositeData that can be fed to
     * {@link com.sun.management.GarbageCollectionNotificationInfo#from(CompositeData)} and
     * {@link javax.management.Notification#getUserData()}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static CompositeData compositeData(String gcName, String gcAction, String gcCause, long id, long startTime,
                                       long endTime, Map<String, MemoryUsage> before, Map<String, MemoryUsage> after) throws Exception {
        final String[] gcInfoItems = {"id", "startTime", "endTime", "duration", "memoryUsageBeforeGc", "memoryUsageAfterGc"};
        final TabularType mapType = memoryUsageMapType();
        final OpenType[] gcInfoTypes = {SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, mapType, mapType};
        final CompositeType gcInfoType = new CompositeType(GC_INFO_TYPE, "gc info", gcInfoItems, gcInfoItems, gcInfoTypes);

        final CompositeData gcInfo = new CompositeDataSupport(gcInfoType, gcInfoItems, new Object[]{
                id, startTime, endTime, endTime - startTime, toTabularData(before), toTabularData(after)});

        final String[] items = {"gcName", "gcAction", "gcCause", "gcInfo"};
        final OpenType[] types = {SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, gcInfoType};
        final CompositeType notificationType = new CompositeType(GC_NOTIFICATION_TYPE, "gc notification", items, items, types);

        return new CompositeDataSupport(notificationType, items, new Object[]{gcName, gcAction, gcCause, gcInfo});
    }

    static Notification notification(String gcName, String gcAction, String gcCause, long id, long startTime,
                                      long endTime, Map<String, MemoryUsage> before, Map<String, MemoryUsage> after) throws Exception {
        return notification(com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION,
                gcName, gcAction, gcCause, id, startTime, endTime, before, after);
    }

    static Notification notification(String type, String gcName, String gcAction, String gcCause, long id,
                                      long startTime, long endTime, Map<String, MemoryUsage> before,
                                      Map<String, MemoryUsage> after) throws Exception {
        final Notification result = new Notification(type, SyntheticGcNotifications.class.getName(), 1L);
        result.setUserData(compositeData(gcName, gcAction, gcCause, id, startTime, endTime, before, after));
        return result;
    }

    static Map<String, MemoryUsage> memoryUsage(String key, long init, long used, long committed, long max) {
        final Map<String, MemoryUsage> result = new LinkedHashMap<>();
        result.put(key, new MemoryUsage(init, used, committed, max));
        return result;
    }

    static Notification minorGcNotification(long startTime, long endTime) throws Exception {
        return notification("G1 Young Generation", "end of minor GC", "G1 Evacuation Pause",
                7L, startTime, endTime, memoryUsage("Eden Space", 0, 100, 200, 300),
                memoryUsage("Eden Space", 0, 40, 150, 300));
    }
}
