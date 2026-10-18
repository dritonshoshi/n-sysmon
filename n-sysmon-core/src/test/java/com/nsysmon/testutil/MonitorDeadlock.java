package com.nsysmon.testutil;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Creates a real monitor deadlock, so that {@link java.lang.management.ThreadMXBean#findDeadlockedThreads()} has
 *  something to report. The two threads created here stay deadlocked for the rest of the JVM's life; they are daemons,
 *  so they never prevent the JVM from exiting.
 */
public final class MonitorDeadlock {
    private static final long DETECTION_TIMEOUT_MILLIS = 30_000;

    private MonitorDeadlock() {
    }

    public static List<Thread> create(String name1, String name2) {
        final Object lockA = new Object();
        final Object lockB = new Object();
        final Object[] firstLocks = {lockA, lockB};
        final Object[] secondLocks = {lockB, lockA};

        final CountDownLatch bothHoldFirstLock = new CountDownLatch(2);
        final CountDownLatch goForSecondLock = new CountDownLatch(1);

        final Thread first = createDeadlockingThread(name1, firstLocks[0], secondLocks[0], bothHoldFirstLock, goForSecondLock);
        final Thread second = createDeadlockingThread(name2, firstLocks[1], secondLocks[1], bothHoldFirstLock, goForSecondLock);

        first.start();
        second.start();

        awaitLatch(bothHoldFirstLock);
        goForSecondLock.countDown();

        awaitDetection(first, second);

        return List.of(first, second);
    }

    private static Thread createDeadlockingThread(String name, Object firstLock, Object secondLock,
                                                 CountDownLatch bothHoldFirstLock, CountDownLatch goForSecondLock) {
        final Thread result = new Thread(() -> {
            synchronized (firstLock) {
                bothHoldFirstLock.countDown();
                awaitLatch(goForSecondLock);
                synchronized (secondLock) {
                    // never entered
                }
            }
        }, name);
        result.setDaemon(true);
        return result;
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(DETECTION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("timed out waiting for " + latch);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void awaitDetection(Thread first, Thread second) {
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(DETECTION_TIMEOUT_MILLIS);
        while (System.nanoTime() < deadline) {
            if (isDetectedAsDeadlocked(first) && isDetectedAsDeadlocked(second)) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
        throw new IllegalStateException("JVM did not report the deadlock of " + first.getName() + " and " + second.getName());
    }

    private static boolean isDetectedAsDeadlocked(Thread thread) {
        final long[] deadlocked = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        if (deadlocked == null) {
            return false;
        }
        for (long threadId: deadlocked) {
            if (threadId == thread.getId()) {
                return true;
            }
        }
        return false;
    }
}
