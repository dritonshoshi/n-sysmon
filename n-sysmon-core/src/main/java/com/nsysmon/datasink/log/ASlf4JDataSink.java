package com.nsysmon.datasink.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arno
 */
public class ASlf4JDataSink extends ALoggingDataSink {
    private static final Logger log = LoggerFactory.getLogger(ASlf4JDataSink.class);

    @Override protected void log(String s) {
        log.debug(s);
    }

    @Override protected boolean isLoggingEnabled() {
        return log.isDebugEnabled();
    }
}