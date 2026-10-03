package com.nsysmon.demo;


import java.lang.reflect.Proxy;

/**
 * This class creates a deadlock between two threads, just to showcase the monitoring of deadlocks.
 *
 * @author arno
 */
public class DeadlockThread extends Thread {
    // package-private so the test can assert the two threads really use distinct locks
    final Object lockA = new Object();
    final Object lockB = new Object();

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

            new Thread(wrapped).start();

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
