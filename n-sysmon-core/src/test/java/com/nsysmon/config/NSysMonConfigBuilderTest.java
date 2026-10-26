package com.nsysmon.config;

import com.nsysmon.config.appinfo.AApplicationInfoProvider;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import com.nsysmon.measure.http.AHttpRequestAnalyzer;
import com.nsysmon.measure.http.ASimpleHttpRequestAnalyzer;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.servlet.auth.AAllowAllWebAuthorizer;
import com.nsysmon.servlet.auth.AWebAuthorizer;
import com.nsysmon.testutil.CountingDataSink;
import com.nsysmon.testutil.ExplicitTimer;
import com.nsysmon.util.timer.ASystemNanoTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NSysMonConfigBuilderTest {

    private static final AApplicationInfoProvider APP_INFO = new AApplicationInfoProvider() {
        @Override public String getApplicationName() {
            return "app";
        }
        @Override public String getVersion() {
            return "1";
        }
        @Override public String getDeployment() {
            return "dev";
        }
        @Override public String getNodeId() {
            return "n";
        }
        @Override public String getHtmlColorCode() {
            return "#abcdef";
        }
    };

    private NSysMonConfigBuilder builder;

    @BeforeEach
    void before() {
        builder = new NSysMonConfigBuilder(APP_INFO);
    }

    @Test
    void builtConfigCarriesTheApplicationInfoProvider() {
        assertSame(APP_INFO, builder.build().appInfo);
    }

    @Test
    void applicationInfoCanBeReplaced() {
        final AApplicationInfoProvider other = APP_INFO;
        builder.setApplicationInfo(other);

        assertSame(other, builder.build().appInfo);
    }

    @Test
    void numericLimitsHaveSaneDefaults() {
        final NSysMonConfig config = builder.build();

        assertEquals(1000, config.averagingDelayForScalarsMillis);
        assertEquals(300, config.durationOfOneTimedScalar);
        assertEquals(100, config.maxNestedMeasurements);
        assertEquals(100_000, config.maxNumMeasurementsPerHierarchy);
        assertEquals(500, config.maxNumMeasurementsPerTimedScalar);
        assertEquals(50, config.maxNumMeasurementsForMonitoring);
        assertEquals(20_000_000L, config.measurementTimeoutNanos);
        assertEquals(3, config.maxNumMeasurementTimeouts);
        assertEquals(100_000L, config.dataSinkTimeoutNanos);
        assertEquals(3, config.maxNumDataSinkTimeouts);
    }

    @Test
    void booleanFlagsDefaultToFalse() {
        final NSysMonConfig config = builder.build();

        assertFalse(config.configuredCollectSqlParameters());
        assertFalse(config.configuredCollectTooltips());
        assertFalse(config.correlationFlowDisabled);
        assertFalse(config.collectSqlParameters());
        assertFalse(config.collectTooltips());
    }

    @Test
    void infrastructureDefaultsAreUsableImplementations() {
        final NSysMonConfig config = builder.build();

        assertInstanceOf(ASystemNanoTimer.class, config.timer);
        assertInstanceOf(ASimpleHttpRequestAnalyzer.class, config.httpRequestAnalyzer);
    }

    @Test
    void datafilePathDefaultsToTmp() {
        assertEquals("/tmp", builder.getPathDatafiles());
        assertEquals("/tmp", builder.build().pathDatafiles);
    }

    @Test
    void datafilePathCanBeChanged() {
        builder.setPathDatafiles("/var/data");

        assertEquals("/var/data", builder.build().pathDatafiles);
    }

    @Test
    void everyNumericSetterIsPropagated() {
        builder.setAveragingDelayForScalarsMillis(11)
                .setDurationOfOneTimedScalar(22)
                .setMaxNestedMeasurements(33)
                .setMaxNumMeasurementsPerHierarchy(44)
                .setMaxNumMeasurementsPerTimedScalar(55)
                .setMaxNumMeasurementsForCockpit(66)
                .setMeasurementTimeoutNanos(77L)
                .setMaxNumMeasurementTimeouts(88)
                .setDataSinkTimeoutNanos(99L)
                .setMaxNumDataSinkTimeouts(111);

        final NSysMonConfig config = builder.build();

        assertEquals(11, config.averagingDelayForScalarsMillis);
        assertEquals(22, config.durationOfOneTimedScalar);
        assertEquals(33, config.maxNestedMeasurements);
        assertEquals(44, config.maxNumMeasurementsPerHierarchy);
        assertEquals(55, config.maxNumMeasurementsPerTimedScalar);
        assertEquals(66, config.maxNumMeasurementsForMonitoring);
        assertEquals(77L, config.measurementTimeoutNanos);
        assertEquals(88, config.maxNumMeasurementTimeouts);
        assertEquals(99L, config.dataSinkTimeoutNanos);
        assertEquals(111, config.maxNumDataSinkTimeouts);
    }

    @Test
    void setterReturnTheBuilderForChaining() {
        final NSysMonConfigBuilder returned = builder.setMaxNestedMeasurements(1);

        assertSame(builder, returned);
    }

    @Test
    void timerCanBeReplaced() {
        final ExplicitTimer timer = new ExplicitTimer();
        builder.setTimer(timer);

        assertSame(timer, builder.build().timer);
    }

    @Test
    void httpRequestAnalyzerCanBeReplaced() {
        final AHttpRequestAnalyzer analyzer = new ASimpleHttpRequestAnalyzer();
        builder.setHttpRequestAnalyzer(analyzer);

        assertSame(analyzer, builder.build().httpRequestAnalyzer);
    }

    @Test
    void webAuthorizerDefaultsToAllowAll() {
        assertSame(AAllowAllWebAuthorizer.INSTANCE, builder.build().webAuthorizer);
    }

    @Test
    void webAuthorizerCanBeReplaced() {
        final AWebAuthorizer authorizer = mock(AWebAuthorizer.class);
        builder.setWebAuthorizer(authorizer);

        assertSame(authorizer, builder.build().webAuthorizer);
    }

    @Test
    void defaultPageCanBeSet() {
        builder.setDefaultPage("overview");

        assertEquals("overview", builder.build().defaultPage);
    }

    @Test
    void defaultPageIsNullByDefault() {
        assertNull(builder.build().defaultPage);
    }

    @Test
    void measurersAndSinksAreCollectedInRegistrationOrder() {
        final AEnvironmentMeasurer env = mock(AEnvironmentMeasurer.class);
        final AScalarMeasurer scalar = mock(AScalarMeasurer.class);
        final AScalarMeasurer timed = mock(AScalarMeasurer.class);
        final ADataSink sink = new CountingDataSink();

        builder.addEnvironmentMeasurer(env).addScalarMeasurer(scalar)
                .addScalarTimedMeasurer(timed).addDataSink(sink);

        final NSysMonConfig config = builder.build();

        assertEquals(List.of(env), config.initialEnvironmentMeasurers);
        assertEquals(List.of(scalar), config.initialScalarMeasurers);
        assertEquals(List.of(timed), config.initialTimedScalarMeasurers);
        assertEquals(List.of(sink), config.initialDataSinks);
    }

    @Test
    void initialCollectionsAreUnmodifiable() {
        builder.addDataSink(new CountingDataSink());
        final NSysMonConfig config = builder.build();

        assertThrows(UnsupportedOperationException.class, () -> config.initialDataSinks.add(new CountingDataSink()));
    }

    @Test
    void presentationMenuEntriesAreWrappedInALabel() {
        builder.addPresentationMenuEntry("Menu", List.of());

        assertEquals(1, builder.build().presentationMenuEntries.size());
        assertEquals("Menu", builder.build().presentationMenuEntries.get(0).label);
    }

    @Test
    void presentationMenuEntriesAcceptVarargs() {
        builder.addPresentationMenuEntry("Menu");

        assertEquals("Menu", builder.build().presentationMenuEntries.get(0).label);
        assertTrue(builder.build().presentationMenuEntries.get(0).pageDefinitions.isEmpty());
    }

    @Test
    void presentationMenuEntriesAreCollected() {
        builder.addPresentationMenuEntry("A", List.of()).addPresentationMenuEntry("B", List.of());

        final NSysMonConfig config = builder.build();

        assertEquals(List.of("A", "B"), config.presentationMenuEntries.stream().map(e -> e.label).toList());
    }

    @Test
    void additionalConfigurationParametersArePassedThrough() {
        builder.setAdditionalConfigurationParameters(Map.of("a", "1"));

        assertEquals(Map.of("a", "1"), builder.build().additionalConfigurationParameters);
    }

    @Test
    void timedScalarMonitoringParametersAcceptLongValues() {
        builder.addTimedScalarMonitoringParameter("k", 5L);

        assertEquals(5L, builder.build().getTimedScalarMonitoringParameters().get("k"));
    }

    @Test
    void timedScalarMonitoringParametersAcceptBooleanValues() {
        builder.addTimedScalarMonitoringParameter("b", Boolean.TRUE);

        assertEquals(Boolean.TRUE, builder.build().getTimedScalarMonitoringParameters().get("b"));
    }

    @Test
    void collectFlagsAreVisibleOnTheBuilder() {
        builder.setCollectSqlParameters(true);
        builder.setCollectTooltips(true);

        assertTrue(builder.isCollectSqlParameters());
        assertTrue(builder.isCollectTooltips());
        assertTrue(builder.build().configuredCollectSqlParameters());
        assertTrue(builder.build().configuredCollectTooltips());
    }
}
