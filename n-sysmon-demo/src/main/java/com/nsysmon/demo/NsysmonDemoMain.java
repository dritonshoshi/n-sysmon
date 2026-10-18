package com.nsysmon.demo;


import org.eclipse.jetty.compression.gzip.GzipCompression;
import org.eclipse.jetty.compression.server.CompressionConfig;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * @author arno
 */
public final class NsysmonDemoMain {

    private NsysmonDemoMain() {
        // entry point holder
    }

    public static void main(String[] args) throws Exception {
        new DeadlockThread().start();

//        System.setProperty("com.nsysmon.globallydisabled", "true");

//        NSysMonConfigurer.addDataSink(NSysMon.get(), new AHttpJsonOffloadingDataSink(NSysMon.get(), "http://localhost:8899/upload", "demo", "the-instance", 100, 1000, 1, 10*1000));

        final Server server = new Server(8181);

        final WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar("n-sysmon-demo/src/main/resources");

        server.setHandler(createCompressionHandler(webapp));

        generateArtificialGcs();

        System.out.println("access http://localhost:8181/nsysmon for n-sysmon");
        System.out.println("access any url starting with http://localhost:8181/content/ to simulate 'regular' system access");
        System.out.println("access any url starting with http://localhost:8181/content/small/ to simulate 'small' system access");
        System.out.println();

        server.start();
        server.join();
    }

    /**
     * Wraps {@code handler} with Jetty's compression, replacing the deprecated
     * {@code org.eclipse.jetty.server.handler.gzip.GzipHandler} (deprecated since Jetty 12.1.1,
     * marked for removal).
     * <p>
     * The gzip implementation is registered explicitly rather than left to the
     * {@code ServiceLoader} discovery in {@link CompressionHandler#doStart()}: the webapp runs in
     * its own classloader, so a discovery based on the thread context classloader may not see
     * {@code org.eclipse.jetty.compression:jetty-compression-gzip} and compression would silently
     * be skipped.
     */
    private static CompressionHandler createCompressionHandler(Handler handler) {
        final GzipCompression gzip = new GzipCompression();
        gzip.setMinCompressSize(256);

        final CompressionHandler compressionHandler = new CompressionHandler(handler);
        compressionHandler.putCompression(gzip);
        compressionHandler.putConfiguration("/", CompressionConfig.builder()
                // Jetty's defaults: compress GET and POST responses, never compress content that is
                // already compressed (zip, gzip, ...) and decompress gzipped POST request bodies.
                .defaults()
                // ... plus the response mime types this demo used to configure on GzipHandler.
                // The include list is exhaustive: anything not listed here is not compressed.
                .compressIncludeMimeType("text/html")
                .compressIncludeMimeType("text/plain")
                .compressIncludeMimeType("text/xml")
                .compressIncludeMimeType("text/css")
                .compressIncludeMimeType("application/javascript")
                .compressIncludeMimeType("application/json")
                // image/svg+xml is not listed although the old GzipHandler config did: defaults()
                // excludes it (and the *.svgz paths) on purpose, and an exclusion always beats an
                // inclusion, so listing it here would be a no-op.
                .build());

        return compressionHandler;
    }

    private static void generateArtificialGcs() {
        Executors.newSingleThreadExecutor().submit((Runnable) () -> {
            while (true) {
                System.gc();
                try {
                    Thread.sleep(10000 + new Random().nextInt(20000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Executors.newSingleThreadExecutor().submit((Runnable) () -> {
            final List<Object> l = new ArrayList<>();

            while (true) {
                l.clear();
                for(int i = 0; i < 10000 + new Random().nextInt(10000); i++) {
                    l.add(new GregorianCalendar());
                }

                try {
                    Thread.sleep(500 + new Random().nextInt(500));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
    }
}


