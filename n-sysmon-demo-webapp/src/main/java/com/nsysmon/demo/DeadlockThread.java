package com.nsysmon.demo;


import java.lang.reflect.Proxy;

/**
 * This class creates a deadlock between two threads, just to showcase the monitoring of deadlocks.
 * <p>
 * Both threads are daemon threads: a deadlock is meant to be observable in the UI, not to keep the
 * JVM alive after the container has shut down.
 *
 * @author arno
 */
public class DeadlockThread extends Thread {
    static final String NAME = "nsysmon-demo-deadlock";

    // package-private so the test can assert the two threads really use distinct locks
    final Object lockA = new Object();
    final Object lockB = new Object();

    public DeadlockThread() {
        super(NAME);
        setDaemon(true);
    }

    @Override public void run() {
        synchronized(lockA) {
            final Runnable r = () -> {
                synchronized(lockB) {
                    synchronized(lockA) {
                        System.out.println("..."); // should never happen
                    }
                }
            };

            final Runnable wrapped = (Runnable) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{Runnable.class}, (proxy, method, args) -> method.invoke(r, args));

            final Thread partner = new Thread(wrapped, NAME + "-partner");
            partner.setDaemon(true);
            partner.start();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            synchronized(lockB) {
                System.out.println("..."); // should never happen
            }
        }
    }
}
