package com.nsysmon.config.log;


import org.apache.log4j.Logger;

/**
 * @author arno
 */
public class NLog4JLoggerFactory implements NSysMonLoggerFactory {
    public static final NLog4JLoggerFactory INSTANCE = new NLog4JLoggerFactory();

    static {
        Logger.getLogger(NLog4JLogger.class); // ensure that this does not load if Log4J is not on the classpath
    }

    @Override public NSysMonLogger getLogger(Class<?> context) {
        return new NLog4JLogger(context);
    }
}
