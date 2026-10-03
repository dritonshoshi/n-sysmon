package com.nsysmon.server.init;

import com.google.inject.Guice;
import com.google.inject.Injector;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;


/**
 * @author arno
 */
public class InitServletContextListener implements ServletContextListener {
    private static volatile Injector injector;

    public static Injector getInjector() {
        return injector;
    }

    @Override public void contextInitialized(ServletContextEvent sce) {
        injector = Guice.createInjector(new NSysMonModule());
    }

    @Override public void contextDestroyed(ServletContextEvent sce) {
        injector = null; //TODO trigger shutdown
    }
}
