package com.nsysmon.testutil;

import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.config.appinfo.ADefaultApplicationInfoProvider;
import com.nsysmon.impl.NSysMonImpl;

/**
 * Creates real {@link NSysMonApi} instances for the page definitions that register themselves as data sinks via
 *  {@link com.nsysmon.impl.NSysMonConfigurer}, which requires a real implementation rather than a mock.
 */
public final class TestSysMons {
    private TestSysMons() {
    }

    public static NSysMonApi create() {
        return new NSysMonImpl(configBuilder().build());
    }

    public static NSysMonConfigBuilder configBuilder() {
        return new NSysMonConfigBuilder(
                new ADefaultApplicationInfoProvider("test-app", "1.0", "test-deployment", "test-node"));
    }

    /**
     * A real config with an explicitly chosen {@code correlationFlowDisabled} flag. The flag is a final field, so a
     *  mocked {@link NSysMonConfig} cannot have it set to anything but the default.
     */
    public static NSysMonConfig config(boolean correlationFlowDisabled) {
        final NSysMonConfig defaults = configBuilder().build();
        return new NSysMonConfig(defaults.appInfo, defaults.averagingDelayForScalarsMillis,
                defaults.durationOfOneTimedScalar, defaults.maxNestedMeasurements,
                defaults.maxNumMeasurementsPerHierarchy, defaults.maxNumMeasurementsPerTimedScalar,
                defaults.maxNumMeasurementsForMonitoring, defaults.measurementTimeoutNanos,
                defaults.maxNumMeasurementTimeouts, defaults.dataSinkTimeoutNanos, defaults.maxNumDataSinkTimeouts,
                defaults.timer, defaults.httpRequestAnalyzer, defaults.initialEnvironmentMeasurers,
                defaults.initialScalarMeasurers, defaults.initialTimedScalarMeasurers, defaults.initialDataSinks,
                defaults.defaultPage, defaults.presentationMenuEntries, defaults.additionalConfigurationParameters,
                defaults.configuredCollectSqlParameters(), defaults.configuredCollectTooltips(),
                defaults.pathDatafiles, defaults.getTimedScalarMonitoringParameters(), correlationFlowDisabled);
    }
}
