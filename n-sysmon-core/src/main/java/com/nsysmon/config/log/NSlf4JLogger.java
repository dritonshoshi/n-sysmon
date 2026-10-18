package com.nsysmon.config.log;

import com.ajjpj.afoundation.function.AFunction0NoThrow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SLF4J-backed implementation of {@link NSysMonLogger}.
 *
 * @author arno
 */
public class NSlf4JLogger extends NSysMonLogger {
    private final Logger log;

    public NSlf4JLogger(Class<?> context) {
        log = LoggerFactory.getLogger(context);
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
        // SLF4J has no error(Throwable) overload, so keep the cause in the message as well
        log.error("unexpected exception: " + exc, exc);
    }

    @Override public void error(String msg, Exception exc) {
        log.error(msg, exc);
    }
}
