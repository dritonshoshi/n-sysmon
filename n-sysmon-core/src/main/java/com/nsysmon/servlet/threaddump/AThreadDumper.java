package com.nsysmon.servlet.threaddump;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;


/**
 * @author arno
 */
final class AThreadDumper {
    static final Comparator<ThreadInfo> THREAD_NAME_COMPARATOR = (o1, o2) -> Collator.getInstance().compare(o1.getThreadName(), o2.getThreadName());

    private static final ThreadMXBean MX_BEAN = ManagementFactory.getThreadMXBean();

    private AThreadDumper() {
    }

    public static Collection<ThreadInfo> getThreadInfo() {
        final Collection<ThreadInfo> result = new TreeSet<>(THREAD_NAME_COMPARATOR);

        result.addAll(Arrays.asList(MX_BEAN.dumpAllThreads(MX_BEAN.isObjectMonitorUsageSupported(), MX_BEAN.isSynchronizerUsageSupported())));

        return result;
    }

    private static final long[] NO_LONGS = new long[0];
    private static long[] nullToEmpty(long[] longs) {
        return longs != null ? longs : NO_LONGS;
    }

    public static Collection<Long> getDeadlockedThreads() {
        final Collection<Long> result = new HashSet<>();

        if(MX_BEAN != null) { // may happen, depending on JVM version
            for(long threadId: nullToEmpty(MX_BEAN.findDeadlockedThreads())) {
                result.add(threadId);
            }

            for(long threadId: nullToEmpty(MX_BEAN.findMonitorDeadlockedThreads())) {
                result.add(threadId);
            }
        }

        return result;
    }
}
