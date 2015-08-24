package com.nsysmon.config.log;

import com.ajjpj.afoundation.function.AFunction0NoThrow;
import org.apache.log4j.Logger;


/**
 * @author arno
 */
public class NLog4JLogger extends NSysMonLogger {
    private final Logger log;

    public NLog4JLogger(Class<?> context) {
        log = Logger.getLogger(context);
    }

    @Override public void debug(AFunction0NoThrow<String> msg) {
        if(log.isDebugEnabled()) {
            log.debug(msg.apply());
        }
    }

    @Override public void info(String msg) {
        log.info(msg);
    }

    @Override public void warn(String msg) {
        log.warn(msg);
    }

    @Override public void warn(String msg, Exception exc) {
        log.warn(msg, exc);
    }

    @Override public void error(String msg) {
        log.error(msg);
    }

    @Override public void error(Exception exc) {
        log.error(exc);
    }

    @Override public void error(String msg, Exception exc) {
        log.error(msg, exc);
    }
}
