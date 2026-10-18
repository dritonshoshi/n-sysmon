package com.nsysmon.config.log;


import org.slf4j.LoggerFactory;

/**
 * @author arno
 */
public class NSlf4JLoggerFactory implements NSysMonLoggerFactory {
    public static final NSlf4JLoggerFactory INSTANCE = new NSlf4JLoggerFactory();

    static {
        // force eager initialisation of the SLF4J provider, so that a missing or misconfigured
        // binding is reported here instead of on the first actual log statement
        LoggerFactory.getLogger(NSlf4JLoggerFactory.class);
    }

    @Override public NSysMonLogger getLogger(Class<?> context) {
        return new NSlf4JLogger(context);
    }
}
