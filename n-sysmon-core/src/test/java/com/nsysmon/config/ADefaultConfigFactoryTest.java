package com.nsysmon.config;

import com.nsysmon.config.appinfo.ADefaultApplicationInfoProvider;
import com.nsysmon.config.log.NSlf4JLoggerFactory;
import com.nsysmon.config.log.NStdOutLoggerFactory;
import com.nsysmon.measure.scalar.ACpuUtilizationMeasurer;
import com.nsysmon.servlet.auth.AAllowAllWebAuthorizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ADefaultConfigFactoryTest {

    private static final String LOGGER_SYSPROP = ADefaultConfigFactory.SYSPROP_PREFIX + ADefaultConfigFactory.KEY_LOGGER;
    private static final String WEB_AUTHORIZER_SYSPROP = ADefaultConfigFactory.SYSPROP_PREFIX + ADefaultConfigFactory.KEY_WEB_AUTHORIZER;

    @BeforeEach
    @AfterEach
    void resetGlobalState() {
        System.clearProperty(LOGGER_SYSPROP);
        System.clearProperty(WEB_AUTHORIZER_SYSPROP);
        System.clearProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED);
        ADefaultConfigFactory.setConfiguredLogger(null);
    }

    @Test
    void getConfigFactoryReturnsTheFactoryNamedInTheProperties() {
        final AConfigFactory factory = ADefaultConfigFactory.getConfigFactory();

        assertInstanceOf(ADefaultConfigFactory.class, factory);
    }

    @Test
    void getConfigFactoryReturnsANewInstanceOnEachCall() {
        assertFalse(ADefaultConfigFactory.getConfigFactory() == ADefaultConfigFactory.getConfigFactory());
    }

    @Test
    void configuredLoggerDefaultsToSlf4j() {
        assertSame(NSlf4JLoggerFactory.INSTANCE, ADefaultConfigFactory.getConfiguredLogger());
    }

    @Test
    void configuredLoggerIsReadFromTheLoggerProperty() {
        System.setProperty(LOGGER_SYSPROP, NStdOutLoggerFactory.class.getName());

        assertInstanceOf(NStdOutLoggerFactory.class, ADefaultConfigFactory.getConfiguredLogger());
    }

    @Test
    void configuredLoggerPropertyIsTrimmed() {
        System.setProperty(LOGGER_SYSPROP, "   " + NStdOutLoggerFactory.class.getName() + "  ");

        assertInstanceOf(NStdOutLoggerFactory.class, ADefaultConfigFactory.getConfiguredLogger());
    }

    @Test
    void unknownLoggerClassFallsBackToTheDefaultLogger() {
        System.setProperty(LOGGER_SYSPROP, "com.does.not.ExistLoggerFactory");

        assertSame(NSlf4JLoggerFactory.INSTANCE, ADefaultConfigFactory.getConfiguredLogger());
    }

    @Test
    void configuredLoggerCanBeOverriddenProgrammatically() {
        final NStdOutLoggerFactory override = new NStdOutLoggerFactory();
        ADefaultConfigFactory.setConfiguredLogger(override);

        assertSame(override, ADefaultConfigFactory.getConfiguredLogger());
    }

    @Test
    void programmaticOverrideWinsOverTheLoggerProperty() {
        System.setProperty(LOGGER_SYSPROP, NStdOutLoggerFactory.class.getName());
        final NStdOutLoggerFactory override = new NStdOutLoggerFactory();
        ADefaultConfigFactory.setConfiguredLogger(override);

        assertSame(override, ADefaultConfigFactory.getConfiguredLogger());
    }

    @Test
    void getConfigReadsTheApplicationInfoFromTheDefaultProperties() {
        final NSysMonConfig config = new ADefaultConfigFactory().getConfig();

        assertInstanceOf(ADefaultApplicationInfoProvider.class, config.appInfo);
    }

    @Test
    void getConfigReadsTheNumericLimitsFromTheDefaultProperties() {
        final NSysMonConfig config = new ADefaultConfigFactory().getConfig();

        assertEquals(1000, config.averagingDelayForScalarsMillis);
        assertEquals(100, config.maxNestedMeasurements);
        assertEquals(100_000, config.maxNumMeasurementsPerHierarchy);
        assertEquals(2880, config.maxNumMeasurementsPerTimedScalar);
        assertEquals(50, config.maxNumMeasurementsForMonitoring);
        assertEquals(2, config.durationOfOneTimedScalar);
        assertEquals(5_000_000_000L, config.measurementTimeoutNanos);
        assertEquals(10, config.maxNumMeasurementTimeouts);
        assertEquals(1_000_000_000L, config.dataSinkTimeoutNanos);
        assertEquals(10, config.maxNumDataSinkTimeouts);
    }

    @Test
    void getConfigReadsTheFlagsFromTheDefaultProperties() {
        final NSysMonConfig config = new ADefaultConfigFactory().getConfig();

        assertFalse(config.configuredCollectSqlParameters());
        assertFalse(config.configuredCollectTooltips());
        assertTrue(config.correlationFlowDisabled);
    }

    @Test
    void getConfigReadsTheInfrastructureFromTheDefaultProperties() {
        final NSysMonConfig config = new ADefaultConfigFactory().getConfig();

        assertNotNull(config.timer);
        assertNotNull(config.httpRequestAnalyzer);
        assertEquals("/tmp", config.pathDatafiles);
        assertEquals("scalars", config.defaultPage);
    }

    @Test
    void getConfigUsesAllowAllWebAuthorizerByDefault() {
        assertSame(AAllowAllWebAuthorizer.INSTANCE, new ADefaultConfigFactory().getConfig().webAuthorizer);
    }

    @Test
    void getConfigReadsTheWebAuthorizerFromTheSystemProperties() {
        System.setProperty(WEB_AUTHORIZER_SYSPROP, AAllowAllWebAuthorizer.class.getName());

        final NSysMonConfig config = new ADefaultConfigFactory().getConfig();

        assertInstanceOf(AAllowAllWebAuthorizer.class, config.webAuthorizer);
        assertNotSame(AAllowAllWebAuthorizer.INSTANCE, config.webAuthorizer);
    }

    @Test
    void getConfigReadsTheTomcatParametersAsAdditionalConfiguration() {
        final Map<String, String> additional = new ADefaultConfigFactory().getConfig().additionalConfigurationParameters;

        assertEquals("Tomcat:type=GlobalRequestProcessor,name=\"http-bio-8080\"",
                additional.get(ADefaultConfigFactory.KEY_TOMCAT_GLOBAL_REQUEST_PROCESSOR));
        assertEquals("Tomcat:type=ThreadPool,name=\"http-bio-8080\"",
                additional.get(ADefaultConfigFactory.KEY_TOMCAT_THREAD_POOL));
    }

    @Test
    void getConfigReadsTheMeasurerAndSinkLists() {
        final NSysMonConfig config = new ADefaultConfigFactory().getConfig();

        assertEquals(7, config.initialEnvironmentMeasurers.size());
        assertEquals(9, config.initialScalarMeasurers.size());
        assertEquals(1, config.initialTimedScalarMeasurers.size());
        assertInstanceOf(ACpuUtilizationMeasurer.class, config.initialTimedScalarMeasurers.get(0));
        assertTrue(config.initialDataSinks.isEmpty());
    }

    @Test
    void getConfigReadsThePresentationMenus() {
        final NSysMonConfig config = new ADefaultConfigFactory().getConfig();

        assertEquals(6, config.presentationMenuEntries.size());
        assertEquals("Context", config.presentationMenuEntries.get(0).label);
        assertEquals(4, config.presentationMenuEntries.get(0).pageDefinitions.size());
    }

    @Test
    void getConfigReadsTheTimedScalarMonitoringThresholds() {
        final Map<String, Object> params = new ADefaultConfigFactory().getConfig().getTimedScalarMonitoringParameters();

        assertEquals(50L, params.get("timedscalar.ACpuUtilizationMeasurer.cpu:all-used.medium"));
        assertEquals(80L, params.get("timedscalar.ACpuUtilizationMeasurer.cpu:all-used.high"));
        assertEquals(Boolean.TRUE, params.get("timedscalar.ACpuUtilizationMeasurer.cpu:all-used.active"));
        assertEquals(50L, params.get("timedscalar.ACpuUtilizationMeasurer.cpu:self-kernel.medium"));
    }

    @Test
    void timedScalarMonitoringThresholdsAreSkippedWhenGloballyDisabled() {
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        final Map<String, Object> params = new ADefaultConfigFactory().getConfig().getTimedScalarMonitoringParameters();

        assertFalse(params.containsKey("timedscalar.ACpuUtilizationMeasurer.cpu:all-used.medium"));
        assertFalse(params.containsKey("timedscalar.ACpuUtilizationMeasurer.cpu:all-used.high"));
    }

    @Test
    void defaultConfigFileNameIsUsedWhenNoOverrideIsPresent() {
        assertEquals("nsysmon-default.properties", ADefaultConfigFactory.DEFAULT_CONFIG_FILE);
        assertEquals("nsysmon.properties", ADefaultConfigFactory.CONFIG_FILE);
        assertEquals("nsysmon.", ADefaultConfigFactory.SYSPROP_PREFIX);
    }

    @Test
    void factoryImplementsTheConfigFactoryContract() {
        assertInstanceOf(AConfigFactory.class, new ADefaultConfigFactory());
    }
}
