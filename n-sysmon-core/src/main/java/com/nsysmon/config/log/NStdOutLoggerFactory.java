package com.nsysmon.config.log;

/**
 * @author arno
 */
public class NStdOutLoggerFactory implements NSysMonLoggerFactory {
    public static final NStdOutLoggerFactory INSTANCE = new NStdOutLoggerFactory();

    @Override public NSysMonLogger getLogger(Class<?> context) {
        return new NStdOutLogger(context);
    }
}
