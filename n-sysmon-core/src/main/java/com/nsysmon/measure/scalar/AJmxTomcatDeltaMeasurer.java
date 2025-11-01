package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.ADefaultConfigFactory;
import com.nsysmon.config.NSysMonAware;
import com.nsysmon.data.AScalarDataPoint;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;

public class AJmxTomcatDeltaMeasurer implements AScalarMeasurer, NSysMonAware {

    private static final String KEY_REQUEST_COUNT = "Request Count";
    private static final String KEY_BYTES_RECEIVED  = "Received MB";
    private static final String KEY_BYTES_SENT  = "Sent MB";
    private static final String KEY_THREADS_BUSY  = "Threads Busy";

    private static String OBJECT_GLOBAL_REQUEST_PROCESSOR;
    private static String OBJECT_THREAD_POOL;
    private static final String NAME_REQUEST_COUNT = "requestCount";
    private static final String NAME_BYTES_RECEIVED = "bytesReceived";
    private static final String NAME_BYTES_SENT = "bytesSent";
    private static final String NAME_THREADS_BUSY = "currentThreadsBusy";

    private static int formerRequestCount = 0;
    private static long formerBytesReceived = 0;
    private static long formerBytesSent = 0;

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final Object requestCount = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_REQUEST_COUNT);
        final Object bytesReceived = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_BYTES_RECEIVED);
        final Object bytesSent = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_BYTES_SENT);
        final Object currentThreadsBusy = server.getAttribute(new ObjectName(OBJECT_THREAD_POOL), NAME_THREADS_BUSY);

        final int currentRequestCount = (Integer) requestCount;
        final long currentBytesReceived = (Long) bytesReceived;
        final long currentBytesSent = (Long) bytesSent;

        data.put(KEY_REQUEST_COUNT, new AScalarDataPoint(timestamp, KEY_REQUEST_COUNT, currentRequestCount - formerRequestCount, 0));
        data.put(KEY_BYTES_RECEIVED, new AScalarDataPoint(timestamp, KEY_BYTES_RECEIVED, formatToMegaBytes(currentBytesReceived - formerBytesReceived), 3));
        data.put(KEY_BYTES_SENT, new AScalarDataPoint(timestamp, KEY_BYTES_SENT, formatToMegaBytes(currentBytesSent - formerBytesSent), 3));
        data.put(KEY_THREADS_BUSY, new AScalarDataPoint(timestamp, KEY_THREADS_BUSY, (Integer) currentThreadsBusy, 0));

        formerRequestCount = currentRequestCount;
        formerBytesReceived = currentBytesReceived;
        formerBytesSent = currentBytesSent;
    }

    private long formatToMegaBytes(final long bytes) {
        final double megaByes = bytes / 1024;
        double megaBytesWithoutBytes = megaByes * 1000;
        long test = Math.round(megaBytesWithoutBytes);
        return test / 1024;
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }

    @Override public void shutdown() throws Exception {
    }

    @Override public void setNSysMon(NSysMonApi sysMon) {
        OBJECT_GLOBAL_REQUEST_PROCESSOR = sysMon.getConfig().additionalConfigurationParameters.get(ADefaultConfigFactory.KEY_TOMCAT_GLOBAL_REQUEST_PROCESSOR);
        OBJECT_THREAD_POOL = sysMon.getConfig().additionalConfigurationParameters.get(ADefaultConfigFactory.KEY_TOMCAT_THREAD_POOL);
    }

    @Override
    public String getGroupnameOfMeasurement(String measurement) {
        if (KEY_REQUEST_COUNT.equalsIgnoreCase(measurement) ||
                KEY_BYTES_RECEIVED.equalsIgnoreCase(measurement) ||
                KEY_BYTES_SENT.equalsIgnoreCase(measurement) ||
                KEY_THREADS_BUSY.equalsIgnoreCase(measurement)) {
            return "Tomcat";
        }
        return null;
    }

    @Override
    public String getDescriptionOfMeasurement(String measurement) {
        if (KEY_REQUEST_COUNT.equalsIgnoreCase(measurement)) {
            return "Number of requests to tomcat.";
        } else if (KEY_BYTES_RECEIVED.equalsIgnoreCase(measurement)) {
            return "Bytes received by tomcat.";
        } else if (KEY_BYTES_SENT.equalsIgnoreCase(measurement)) {
            return "Bytes sent by tomcat.";
        } else if (KEY_THREADS_BUSY.equalsIgnoreCase(measurement)) {
            return "Tomcat busy threads.";
        }
        return null;
    }
}
