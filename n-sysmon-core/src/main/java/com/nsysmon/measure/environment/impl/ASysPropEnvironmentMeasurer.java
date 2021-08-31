package com.nsysmon.measure.environment.impl;

import com.nsysmon.measure.environment.AEnvironmentMeasurer;


/**
 * @author arno
 */
public class ASysPropEnvironmentMeasurer implements AEnvironmentMeasurer {
    public static final String KEY_SYSTEM_PROPERTY = "sysprop";

    @Override public void contributeMeasurements(EnvironmentCollector data) throws Exception {
        for(String propName: System.getProperties().stringPropertyNames()) {
            final String value = isAllowedInCleartext(propName) ? System.getProperty(propName) : "********";

            data.add(value, KEY_SYSTEM_PROPERTY, propName);
        }
    }

    @Override
    public void shutdown() throws Exception {
    }

    private boolean isAllowedInCleartext(String envName) {
        return !envName.toLowerCase().startsWith("secure");
    }
}
