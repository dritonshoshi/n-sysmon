package com.nsysmon.demo;

import java.util.Random;

/**
 * Triggers garbage collections at irregular intervals, so the n-sysmon GC views have something to
 * show.
 * <p>
 * A daemon thread: it is started and interrupted by {@link ArtificialLoadServletContextListener}
 * and must never be able to keep the JVM alive.
 *
 * @author arno
 */
class GcChurnThread extends Thread {
    static final String NAME = "nsysmon-demo-artificial-gc";

    private final int minIntervalMillis;
    private final int intervalSpreadMillis;
    private final Random random = new Random();

    GcChurnThread(int minIntervalMillis, int intervalSpreadMillis) {
        super(NAME);
        setDaemon(true);

        this.minIntervalMillis = minIntervalMillis;
        this.intervalSpreadMillis = intervalSpreadMillis;
    }

    @Override public void run() {
        while (!isInterrupted()) {
            System.gc();

            try {
                Thread.sleep(minIntervalMillis + random.nextInt(intervalSpreadMillis));
            } catch (InterruptedException exc) {
                interrupt();
            }
        }
    }
}
