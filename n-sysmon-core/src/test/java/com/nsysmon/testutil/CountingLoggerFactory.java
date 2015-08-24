package com.nsysmon.testutil;

import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.log.NSysMonLoggerFactory;

/**
 * @author arno
 */
public class CountingLoggerFactory implements NSysMonLoggerFactory {
    public static final CountingLogger logger = new CountingLogger();

    @Override public NSysMonLogger getLogger(Class<?> context) {
        return logger;
    }
}
