package com.nsysmon.config.log;

/**
 * @author arno
 */
public class NJavaUtilLoggerFactory implements NSysMonLoggerFactory {
    public static final NJavaUtilLoggerFactory INSTANCE = new NJavaUtilLoggerFactory();

    @Override public NSysMonLogger getLogger(Class<?> context) {
        return new NJavaUtilLogger(context);
    }
}
