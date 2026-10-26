package com.nsysmon.demo;


import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;

import java.io.File;

/**
 * Runs the n-sysmon demo webapp on embedded Jetty.
 * <p>
 * The webapp itself lives in {@code n-sysmon-demo-webapp} and is shared with the Open Liberty runner
 * {@code n-sysmon-demo-liberty}; this class only starts a container around it. The artificial load
 * the demo is about (a deadlock, a GC churn, an allocation churn) is started by the webapp's own
 * {@code ArtificialLoadServletContextListener}, so it does not depend on this launcher.
 *
 * @author arno
 */
public final class NsysmonDemoMain {
    static final int PORT = 8181;

    private NsysmonDemoMain() {
        // entry point holder
    }

    public static void main(String[] args) throws Exception {
//        System.setProperty("com.nsysmon.globallydisabled", "true");

//        NSysMonConfigurer.addDataSink(NSysMon.get(), new AHttpJsonOffloadingDataSink(NSysMon.get(), "http://localhost:8899/upload", "demo", "the-instance", 100, 1000, 1, 10*1000));

        final Server server = new Server(PORT);

        final WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(WebappResourceBase.resolve(new File(System.getProperty("user.dir"))).getAbsolutePath());

        server.setHandler(DemoCompressionHandler.createFor(webapp));

        printAccessInstructions();

        server.start();
        server.join();
    }

    private static void printAccessInstructions() {
        System.out.println("access http://localhost:" + PORT + "/nsysmon for n-sysmon (user 'admin', password 'admin')");
        System.out.println("access any url starting with http://localhost:" + PORT + "/content/ to simulate 'regular' system access");
        System.out.println("access any url starting with http://localhost:" + PORT + "/content/small/ to simulate 'small' system access");
        System.out.println();
    }
}
