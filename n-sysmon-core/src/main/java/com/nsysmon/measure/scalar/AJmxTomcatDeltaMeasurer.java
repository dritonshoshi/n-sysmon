package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;
import org.apache.log4j.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;

public class AJmxTomcatDeltaMeasurer implements AScalarMeasurer {

    private static final Logger LOG = Logger.getLogger(AJmxTomcatDeltaMeasurer.class);

    private static final String KEY_PREFIX = "Tomcat";
    private static final String KEY_REQUEST_COUNT = KEY_PREFIX + "RequestCount";
    private static final String KEY_BYTES_RECEIVED  = KEY_PREFIX + "MBReceived";
    private static final String KEY_BYTES_SENT  = KEY_PREFIX + "MBSent";

    private static final String OBJECT_GLOBAL_REQUEST_PROCESSOR = "Tomcat:type=GlobalRequestProcessor,name=\"http-bio-8080\"";
    private static final String NAME_REQUEST_COUNT = "requestCount";
    private static final String NAME_BYTES_RECEIVED = "bytesReceived";
    private static final String NAME_BYTES_SENT = "bytesSent";

    private static int formerRequestCount = 0;
    private static long formerBytesReceived = 0;
    private static long formerBytesSent = 0;

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws Exception {
    }

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        try {
            final Object requestCount = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_REQUEST_COUNT);
            final Object bytesReceived = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_BYTES_RECEIVED);
            final Object bytesSent = server.getAttribute(new ObjectName(OBJECT_GLOBAL_REQUEST_PROCESSOR), NAME_BYTES_SENT);

            final int currentRequestCount = (Integer) requestCount;
            final long currentBytesReceived = (Long) bytesReceived;
            final long currentBytesSent = (Long) bytesSent;

            data.put(KEY_REQUEST_COUNT, new AScalarDataPoint(timestamp, KEY_REQUEST_COUNT, currentRequestCount - formerRequestCount, 0));
            data.put(KEY_BYTES_RECEIVED, new AScalarDataPoint(timestamp, KEY_BYTES_RECEIVED, formatToMegaBytes(currentBytesReceived - formerBytesReceived), 3));
            data.put(KEY_BYTES_SENT, new AScalarDataPoint(timestamp, KEY_BYTES_SENT, formatToMegaBytes(currentBytesSent - formerBytesSent), 3));

            formerRequestCount = currentRequestCount;
            formerBytesReceived = currentBytesReceived;
            formerBytesSent = currentBytesSent;
        } catch (Exception e) {
            LOG.error(e);
        }
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
}
