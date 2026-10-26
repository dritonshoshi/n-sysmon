package com.nsysmon.measure.environment.impl;

import com.nsysmon.measure.environment.AEnvironmentMeasurer;

import java.util.Map;

public class AEnvVarJdkParameterMeasurer implements AEnvironmentMeasurer {
    public static final String KEY_ENV_VAR = "jdkparam";

    @Override public void contributeMeasurements(EnvironmentCollector data) throws Exception {
        final Map<String, String> env = System.getenv();

        for(String envName: env.keySet()) {
            final String value = isAllowedInCleartext(envName) ? env.get(envName) : "********";

            data.add(value, KEY_ENV_VAR, envName);
        }
    }

    @Override
    public void shutdown() throws Exception {
    }
}
