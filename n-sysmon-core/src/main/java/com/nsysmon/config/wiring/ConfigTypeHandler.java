package com.nsysmon.config.wiring;

/**
 * @author arno
 */
interface ConfigTypeHandler {
    boolean canHandle(Class<?> type, Class<?>[] paramTypes, String value);
    Object handle(ConfigValueResolver r, String key, String value, Class<?> type, Class<?>[] paramTypes) throws Exception;
}
