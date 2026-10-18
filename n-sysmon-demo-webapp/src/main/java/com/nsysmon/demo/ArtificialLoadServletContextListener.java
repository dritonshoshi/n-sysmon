package com.nsysmon.demo;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabricates the load the demo needs: a pair of deliberately deadlocked threads, a garbage
 * collection churn and an allocation churn.
 * <p>
 * This lives in the demo webapp instead of in a container specific launcher, so the very same
 * webapp behaves identically wherever it is deployed - embedded Jetty in {@code n-sysmon-demo},
 * Open Liberty in {@code n-sysmon-demo-liberty} - and so that a container that has no
 * {@code main()} of its own still gets the artificial load.
 *
 * @author arno
 */
public class ArtificialLoadServletContextListener implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(ArtificialLoadServletContextListener.class);

    /*
     * The intervals are the ones the demo used while this still lived in NsysmonDemoMain.main().
     */
    private static final int GC_MIN_INTERVAL_MILLIS = 10000;
    private static final int GC_INTERVAL_SPREAD_MILLIS = 20000;
    private static final int MIN_OBJECTS_PER_ROUND = 10000;
    private static final int OBJECT_SPREAD = 10000;
    private static final int MEMORY_MIN_INTERVAL_MILLIS = 500;
    private static final int MEMORY_INTERVAL_SPREAD_MILLIS = 500;

    private GcChurnThread gcChurnThread;
    private MemoryChurnThread memoryChurnThread;

    @Override public void contextInitialized(ServletContextEvent sce) {
        // No reference is kept to the deadlock threads: they are never stopped, and a running thread
        // cannot be garbage collected anyway.
        new DeadlockThread().start();

        gcChurnThread = new GcChurnThread(GC_MIN_INTERVAL_MILLIS, GC_INTERVAL_SPREAD_MILLIS);
        gcChurnThread.start();

        memoryChurnThread = new MemoryChurnThread(MIN_OBJECTS_PER_ROUND, OBJECT_SPREAD,
                MEMORY_MIN_INTERVAL_MILLIS, MEMORY_INTERVAL_SPREAD_MILLIS);
        memoryChurnThread.start();

        log.info("artificial demo load started: a deadlock, a GC churn every {}-{} ms and an allocation churn every {}-{} ms",
                GC_MIN_INTERVAL_MILLIS, GC_MIN_INTERVAL_MILLIS + GC_INTERVAL_SPREAD_MILLIS,
                MEMORY_MIN_INTERVAL_MILLIS, MEMORY_MIN_INTERVAL_MILLIS + MEMORY_INTERVAL_SPREAD_MILLIS);
    }

    @Override public void contextDestroyed(ServletContextEvent sce) {
        interrupt(memoryChurnThread);
        interrupt(gcChurnThread);

        // The two deadlock threads cannot be stopped - they are the exhibit, and they are the
        // reason the "Deadlock" page of the UI is never empty. They sleep in the JVM until it ends.
        log.info("artificial demo load stopped, the deadlocked threads stay deadlocked");
    }

    private void interrupt(Thread thread) {
        if(thread != null) {
            thread.interrupt();
        }
    }
}
