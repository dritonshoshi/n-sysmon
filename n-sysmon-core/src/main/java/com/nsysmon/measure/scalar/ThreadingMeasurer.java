package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;

public class ThreadingMeasurer implements AScalarMeasurer {

    private static final String THREAD_COUNT = "JVM: Current Thread Count";
    private static final String DAEMON_THREAD_COUNT = "JVM: Daemon Thread Count";
    private static final String THREADS_CREATED = "JVM: Threads Created";

    private long formerTotalStartedThreadCount = 0;

    public ThreadingMeasurer(){
    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws IOException {
    }

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws IOException {
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final long totalStartedThreadCount = threadMXBean.getTotalStartedThreadCount();

        data.put(THREAD_COUNT, new AScalarDataPoint(timestamp, THREAD_COUNT, threadMXBean.getThreadCount(), 0));
        data.put(DAEMON_THREAD_COUNT, new AScalarDataPoint(timestamp, DAEMON_THREAD_COUNT, threadMXBean.getDaemonThreadCount(), 0));
        data.put(THREADS_CREATED, new AScalarDataPoint(timestamp, THREADS_CREATED, totalStartedThreadCount - formerTotalStartedThreadCount, 0));

        formerTotalStartedThreadCount = totalStartedThreadCount;
    }

    @Override public void shutdown() throws Exception {
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }
}
