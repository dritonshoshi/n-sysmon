package com.nsysmon;

import com.ajjpj.afoundation.function.AStatement0;
import com.nsysmon.util.AShutdownable;
import com.ajjpj.afoundation.util.AUnchecker;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;


/**
 * This listener takes care of shutting down the default N-SysMon instance on web application shutdown.
 *
 * @author arno
 */
public class AShutdownServletContextListener implements ServletContextListener {
    @Override public void contextDestroyed(ServletContextEvent sce) {
        AUnchecker.executeUnchecked((AStatement0<Exception>) () -> ((AShutdownable) NSysMon.get()).shutdown());
    }

    @Override public void contextInitialized(ServletContextEvent sce) {
    }
}
