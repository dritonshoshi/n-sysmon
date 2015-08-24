package com.nsysmon.config.log;


/**
 * @author arno
 */
public interface NSysMonLoggerFactory {
    NSysMonLogger getLogger(Class<?> context);
}
