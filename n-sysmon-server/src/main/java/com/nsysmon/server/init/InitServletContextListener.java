package com.nsysmon.server.init;

import com.google.inject.Guice;
import com.google.inject.Injector;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;


/**
 * @author arno
 */
public class InitServletContextListener implements ServletContextListener {
    private static volatile Injector INJECTOR;

    public static Injector getInjector() {
        return INJECTOR;
    }

    @Override public void contextInitialized(ServletContextEvent sce) {
        INJECTOR = Guice.createInjector(new NSysMonModule());
    }

    @Override public void contextDestroyed(ServletContextEvent sce) {
        INJECTOR = null; //TODO trigger shutdown
    }
}
