package com.nsysmon.demo;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

/**
 * Allocates throw-away objects at irregular intervals, so heap usage and collector activity in the
 * n-sysmon memory views actually move.
 * <p>
 * A daemon thread: it is started and interrupted by {@link ArtificialLoadServletContextListener}
 * and must never be able to keep the JVM alive.
 *
 * @author arno
 */
class MemoryChurnThread extends Thread {
    static final String NAME = "nsysmon-demo-artificial-memory";

    private final int minObjectsPerRound;
    private final int objectSpread;
    private final int minIntervalMillis;
    private final int intervalSpreadMillis;
    private final Random random = new Random();

    MemoryChurnThread(int minObjectsPerRound, int objectSpread, int minIntervalMillis, int intervalSpreadMillis) {
        super(NAME);
        setDaemon(true);

        this.minObjectsPerRound = minObjectsPerRound;
        this.objectSpread = objectSpread;
        this.minIntervalMillis = minIntervalMillis;
        this.intervalSpreadMillis = intervalSpreadMillis;
    }

    @Override public void run() {
        final List<Object> allocations = new ArrayList<>();

        while (!isInterrupted()) {
            allocations.clear();
            final int numObjects = minObjectsPerRound + random.nextInt(objectSpread);
            for(int i = 0; i < numObjects; i++) {
                allocations.add(new GregorianCalendar());
            }

            try {
                Thread.sleep(minIntervalMillis + random.nextInt(intervalSpreadMillis));
            } catch (InterruptedException exc) {
                interrupt();
            }
        }
    }
}
