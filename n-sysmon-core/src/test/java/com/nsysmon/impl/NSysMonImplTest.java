package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonAware;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.config.appinfo.AApplicationInfoProvider;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.data.AScalarMeasurementInterceptor;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.ACollectingMeasurement;
import com.nsysmon.measure.AMeasureCallback;
import com.nsysmon.measure.AMeasureCallbackVoid;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.testutil.CollectingDataSink;
import com.nsysmon.testutil.ExplicitTimer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NSysMonImplTest {

    static final AApplicationInfoProvider APP_INFO = new AApplicationInfoProvider() {
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

    private NSysMonConfigBuilder configBuilder;
    private CollectingDataSink dataSink;
    private NSysMonImpl sysMon;

    @BeforeEach
    void before() {
        System.clearProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED);
        configBuilder = new NSysMonConfigBuilder(APP_INFO);
        configBuilder.setAveragingDelayForScalarsMillis(0);
        dataSink = new CollectingDataSink();
        sysMon = new NSysMonImpl(configBuilder.build());
        NSysMonConfigurer.addDataSink(sysMon, dataSink);
    }

    @AfterEach
    void after() {
        System.clearProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED);
        Thread.interrupted(); // make sure a test cannot leak an interrupt into the next one
    }

    private NSysMonImpl createSysMon(ADataSink sink) {
        final NSysMonImpl result = new NSysMonImpl(configBuilder.build());
        NSysMonConfigurer.addDataSink(result, sink);
        return result;
    }

    private AHierarchicalDataRoot onlyCollected() {
        assertEquals(1, dataSink.data.size());
        return dataSink.data.get(0);
    }

    // --- measurements -------------------------------------------------------------------------------

    @Test
    void getConfigReturnsTheConfigTheInstanceWasBuiltWith() {
        final NSysMonConfig config = configBuilder.build();

        assertSame(config, new NSysMonImpl(config).getConfig());
    }

    @Test
    void theApiTypeIsImplemented() {
        assertInstanceOf(NSysMonApi.class, sysMon);
    }

    @Test
    void aSimpleMeasurementReachesTheDataSink() {
        sysMon.start("a").finish();

        assertEquals("a", onlyCollected().getRootNode().getIdentifier());
    }

    @Test
    void aParallelMeasurementBecomesAChildOfItsEnclosingMeasurement() {
        final ASimpleMeasurement root = sysMon.start("a");

        sysMon.start("p", false).finish();
        root.finish();

        assertEquals("p", onlyCollected().getRootNode().getChildren().get(0).getIdentifier());
    }

    @Test
    void thereIsNoRunningMeasurementOnAFreshInstance() {
        assertFalse(sysMon.hasRunningMeasurement());
    }

    @Test
    void thereIsARunningMeasurementWhileOneIsOpen() {
        final ASimpleMeasurement measurement = sysMon.start("a");

        assertTrue(sysMon.hasRunningMeasurement());
        measurement.finish();
    }

    @Test
    void thereIsNoRunningMeasurementAfterTheRootWasFinished() {
        sysMon.start("a").finish();

        assertFalse(sysMon.hasRunningMeasurement());
    }

    @Test
    void measureReturnsTheCallbackResult() {
        assertEquals("result", sysMon.measure("a", m -> "result"));

        assertEquals("a", onlyCollected().getRootNode().getIdentifier());
    }

    @Test
    void measureWithAVoidCallbackIsSupported() {
        final List<String> seen = new ArrayList<>();

        sysMon.measure("a", (AMeasureCallbackVoid<RuntimeException>) m -> seen.add(m.getClass().getSimpleName()));

        assertEquals(1, seen.size());
        assertFalse(sysMon.hasRunningMeasurement());
    }

    @Test
    void measureFinishesTheMeasurementWhenTheCallbackThrows() {
        assertThrows(IllegalStateException.class, () -> sysMon.measure("a", (AMeasureCallback<String, RuntimeException>) m -> {
            throw new IllegalStateException("boom");
        }));

        assertFalse(sysMon.hasRunningMeasurement());
        assertEquals("a", onlyCollected().getRootNode().getIdentifier());
    }

    @Test
    void theTimerFromTheConfigIsUsedForDurations() {
        final ExplicitTimer timer = new ExplicitTimer();
        configBuilder.setTimer(timer);
        final NSysMonImpl timed = createSysMon(dataSink);

        final ASimpleMeasurement measurement = timed.start("a");
        timer.curNanos += 500;
        measurement.finish();

        assertEquals(500L, onlyCollected().getRootNode().getDurationNanos());
    }

    @Test
    void nestedMeasurementsBecomeChildren() {
        final ASimpleMeasurement outer = sysMon.start("a");
        sysMon.start("b").finish();
        outer.finish();

        assertEquals("b", onlyCollected().getRootNode().getChildren().get(0).getIdentifier());
    }

    @Test
    void aSimpleMeasurementParameterIsPassedOn() {
        final ASimpleMeasurement measurement = sysMon.start("a");
        measurement.addParameter("k", "v");
        measurement.finish();

        assertEquals(Map.of("k", "v"), onlyCollected().getRootNode().getParameters());
    }

    @Test
    void collectingMeasurementDetailsAreAggregated() {
        final ASimpleMeasurement outer = sysMon.start("a");
        final ACollectingMeasurement collecting = sysMon.startCollectingMeasurement("b");

        collecting.addDetailMeasurement("c", 10);
        collecting.addDetailMeasurement("c", 20);
        collecting.finish();
        outer.finish();

        final AHierarchicalData b = onlyCollected().getRootNode().getChildren().get(0);

        assertEquals(1, b.getChildren().size());
        assertEquals(30L, b.getChildren().get(0).getDurationNanos());
    }

    @Test
    void collectingMeasurementParametersArePassedOn() {
        final ASimpleMeasurement outer = sysMon.start("a");
        final ACollectingMeasurement collecting = sysMon.startCollectingMeasurement("b");
        collecting.addParameter("k", "v");
        collecting.finish();
        outer.finish();

        assertEquals(Map.of("k", "v"), onlyCollected().getRootNode().getChildren().get(0).getParameters());
    }

    // --- limits ------------------------------------------------------------------------------------

    @Test
    void exceedingMaxNumMeasurementsPerHierarchyKillsTheHierarchy() {
        configBuilder.setMaxNumMeasurementsPerHierarchy(2);
        final NSysMonImpl limited = createSysMon(dataSink);

        final ASimpleMeasurement root = limited.start("a");
        limited.start("b");
        limited.start("c");
        root.finish();

        assertTrue(onlyCollected().getRootNode().isWasKilled());
    }

    @Test
    void stayingWithinMaxNumMeasurementsPerHierarchyKeepsTheData() {
        configBuilder.setMaxNumMeasurementsPerHierarchy(3);
        final NSysMonImpl limited = createSysMon(dataSink);

        limited.start("a").finish();

        assertFalse(onlyCollected().getRootNode().isWasKilled());
    }

    @Test
    void exceedingMaxNestedMeasurementsKillsTheHierarchy() {
        configBuilder.setMaxNestedMeasurements(1);
        final NSysMonImpl limited = createSysMon(dataSink);

        final ASimpleMeasurement root = limited.start("a");
        limited.start("b");
        root.finish();

        assertTrue(onlyCollected().getRootNode().isWasKilled());
    }

    @Test
    void unfinishedNestedMeasurementsOnlyReachTheSinkWhenTheRootFinishes() {
        final ASimpleMeasurement root = sysMon.start("a");
        sysMon.start("b");

        assertTrue(dataSink.data.isEmpty());

        root.finish();

        assertEquals(1, dataSink.data.size());
    }

    // --- flows -------------------------------------------------------------------------------------

    @Test
    void startFlowWithoutAMeasurementIsIgnored() {
        assertDoesNotThrow(() -> sysMon.startFlow(new ACorrelationId("q", "1", "0")));

        assertFalse(sysMon.hasRunningMeasurement());
        assertTrue(dataSink.data.isEmpty());
    }

    @Test
    void joinFlowWithoutAMeasurementIsIgnored() {
        assertDoesNotThrow(() -> sysMon.joinFlow(new ACorrelationId("q", "1", "0")));

        assertFalse(sysMon.hasRunningMeasurement());
        assertTrue(dataSink.data.isEmpty());
    }

    @Test
    void startedFlowsAreAttachedToTheMeasurement() {
        final ASimpleMeasurement measurement = sysMon.start("m");
        sysMon.startFlow(new ACorrelationId("q", "1", "0"));
        measurement.finish();

        assertEquals(1, onlyCollected().getStartedFlows().size());
    }

    @Test
    void joinedFlowsAreAttachedToTheMeasurement() {
        final ASimpleMeasurement measurement = sysMon.start("m");
        sysMon.joinFlow(new ACorrelationId("q", "1", "0"));
        measurement.finish();

        assertEquals(1, onlyCollected().getJoinedFlows().size());
    }

    @Test
    void duplicateFlowsAreRecordedOnlyOnce() {
        final ASimpleMeasurement measurement = sysMon.start("m");
        final ACorrelationId flow = new ACorrelationId("q", "1", "0");
        sysMon.startFlow(flow);
        sysMon.startFlow(flow);
        measurement.finish();

        assertEquals(1, onlyCollected().getStartedFlows().size());
    }

    @Test
    void flowsAreNotCarriedOverToTheNextMeasurement() {
        final ASimpleMeasurement first = sysMon.start("first");
        sysMon.startFlow(new ACorrelationId("q", "1", "0"));
        first.finish();
        sysMon.start("second").finish();

        assertEquals(2, dataSink.data.size());
        assertTrue(dataSink.data.get(1).getStartedFlows().isEmpty());
    }

    // --- synthetic measurements --------------------------------------------------------------------

    @Test
    void aSyntheticMeasurementIsForwardedToTheDataSink() {
        sysMon.injectSyntheticMeasurement(newDataRoot("synthetic"));

        assertEquals(1, dataSink.numStarted);
        assertEquals("synthetic", onlyCollected().getRootNode().getIdentifier());
    }

    @Test
    void aSyntheticMeasurementIsForwardedEvenWhileAnotherOneRuns() {
        final ASimpleMeasurement measurement = sysMon.start("a");

        sysMon.injectSyntheticMeasurement(newDataRoot("synthetic"));

        assertEquals(2, dataSink.numStarted);
        measurement.finish();
    }

    private static AHierarchicalDataRoot newDataRoot(String identifier) {
        final AHierarchicalData root = new AHierarchicalData(true, 0, 0, identifier, Map.of(), List.of(), false);
        return new AHierarchicalDataRoot(root, List.of(), List.of());
    }

    // --- scalar measurements -----------------------------------------------------------------------

    @Test
    void scalarMeasurementsAreCollectedFromTheRegisteredMeasurers() throws Exception {
        sysMon.addScalarMeasurer(contributing("cpu", 5));

        assertEquals(5L, sysMon.getScalarMeasurements().get("cpu").getValueRaw());
    }

    @Test
    void scalarMeasurementsAreEmptyWithoutMeasurers() {
        assertTrue(sysMon.getScalarMeasurements().isEmpty());
    }

    @Test
    void scalarMeasurementsAreEmptyWhenGloballyDisabled() throws Exception {
        sysMon.addScalarMeasurer(contributing("cpu", 5));
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        assertTrue(sysMon.getScalarMeasurements().isEmpty());
    }

    @Test
    void theAveragingDelayCanBeOverriddenPerCall() throws Exception {
        sysMon.addScalarMeasurer(contributing("cpu", 5));

        assertEquals(5L, sysMon.getScalarMeasurements(0).get("cpu").getValueRaw());
    }

    @Test
    void aFailingScalarMeasurerIsTolerated() throws Exception {
        sysMon.addScalarMeasurer(failing());

        assertTrue(sysMon.getScalarMeasurements().isEmpty());
    }

    @Test
    void scalarMeasurementsAreStillCollectedIfTheAveragingDelayIsInterrupted() throws Exception {
        sysMon.addScalarMeasurer(contributing("cpu", 5));
        Thread.currentThread().interrupt();

        // a pending interrupt makes Thread.sleep throw right away, so this returns immediately
        assertEquals(5L, sysMon.getScalarMeasurements(60_000).get("cpu").getValueRaw());
    }

    // --- timed scalar measurements -----------------------------------------------------------------

    @Test
    void timedScalarMeasurementsAreStored() {
        sysMon.addTimedScalarMeasurement(new AScalarDataPoint(1L, "cpu", 5, 0));

        assertEquals(1, count(sysMon.getTimedScalarMeasurements().get("cpu")));
    }

    @Test
    void timedScalarMeasurementsAreEmptyByDefault() {
        assertTrue(sysMon.getTimedScalarMeasurements().isEmpty());
    }

    @Test
    void timedScalarMeasurementsAreEmptyWhenGloballyDisabled() {
        sysMon.addTimedScalarMeasurement(new AScalarDataPoint(1L, "cpu", 5, 0));
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        assertTrue(sysMon.getTimedScalarMeasurements().isEmpty());
    }

    @Test
    void monitoringMeasurementsAreEmptyByDefault() {
        assertTrue(sysMon.getTimedScalarMeasurementsForMonitoring().isEmpty());
    }

    @Test
    void monitoringMeasurementsAreEmptyWhenGloballyDisabled() {
        sysMon.addTimedScalarMeasurement(new AScalarDataPoint(1L, "cpu", 5, 0));
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        assertTrue(sysMon.getTimedScalarMeasurementsForMonitoring().isEmpty());
    }

    @Test
    void aRegisteredInterceptorSeesAddedTimedScalarMeasurements() {
        final AScalarMeasurementInterceptor interceptor = mock(AScalarMeasurementInterceptor.class);
        sysMon.addTimedScalarMeasurementInterceptor(interceptor);
        final AScalarDataPoint point = new AScalarDataPoint(1L, "cpu", 5, 0);

        sysMon.addTimedScalarMeasurement(point);

        verify(interceptor).addMeasurement(point);
    }

    @Test
    void timedScalarMeasurersAreEmptyByDefault() {
        assertTrue(sysMon.getTimedScalarForDirectAccess().isEmpty());
    }

    @Test
    void aRegisteredTimedScalarMeasurerIsExposedForDirectAccess() throws Exception {
        sysMon.addTimedScalarMeasurer(contributing("cpu", 5));

        assertEquals(1, sysMon.getTimedScalarForDirectAccess().size());
    }

    private static int count(Iterable<AScalarDataPoint> buffer) {
        int result = 0;
        for (final AScalarDataPoint ignored : buffer) {
            result += 1;
        }
        return result;
    }

    // --- environment measurements ------------------------------------------------------------------

    @Test
    void environmentMeasurementsAreCollectedFromTheRegisteredMeasurers() throws Exception {
        sysMon.addEnvironmentMeasurer(environmentMeasurer("value", "key"));

        final List<AEnvironmentData> data = sysMon.getEnvironmentMeasurements();

        assertEquals(1, data.size());
        assertEquals("value", data.get(0).getValue());
    }

    @Test
    void environmentMeasurementsAreEmptyWithoutMeasurers() {
        assertTrue(sysMon.getEnvironmentMeasurements().isEmpty());
    }

    @Test
    void environmentMeasurementsAreEmptyWhenGloballyDisabled() throws Exception {
        sysMon.addEnvironmentMeasurer(environmentMeasurer("value", "key"));
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        assertTrue(sysMon.getEnvironmentMeasurements().isEmpty());
    }

    @Test
    void aFailingEnvironmentMeasurerIsTolerated() throws Exception {
        sysMon.addEnvironmentMeasurer(new AEnvironmentMeasurer() {
            @Override public void contributeMeasurements(AEnvironmentMeasurer.EnvironmentCollector data) {
                throw new IllegalStateException("boom");
            }
            @Override public void shutdown() {
            }
        });

        assertTrue(sysMon.getEnvironmentMeasurements().isEmpty());
    }

    // --- wiring ------------------------------------------------------------------------------------

    @Test
    void sysMonAwareDataSinksGetTheApiInjected() {
        final AwareDataSink sink = new AwareDataSink();
        final NSysMonImpl target = new NSysMonImpl(configBuilder.build());

        NSysMonConfigurer.addDataSink(target, sink);

        assertSame(target, sink.injected);
    }

    @Test
    void sysMonAwareScalarMeasurersGetTheApiInjected() throws Exception {
        final AwareScalarMeasurer measurer = new AwareScalarMeasurer();
        final NSysMonImpl target = new NSysMonImpl(configBuilder.build());

        NSysMonConfigurer.addScalarMeasurer(target, measurer);

        assertSame(target, measurer.injected);
    }

    @Test
    void sysMonAwareEnvironmentMeasurersGetTheApiInjected() throws Exception {
        final AwareEnvironmentMeasurer measurer = new AwareEnvironmentMeasurer();
        final NSysMonImpl target = new NSysMonImpl(configBuilder.build());

        NSysMonConfigurer.addEnvironmentMeasurer(target, measurer);

        assertSame(target, measurer.injected);
    }

    @Test
    void presentationPageDefinitionsAreInitialised() {
        final StubPageDefinition page = new StubPageDefinition();
        configBuilder.addPresentationMenuEntry("Menu", List.of(page));

        new NSysMonImpl(configBuilder.build());

        assertTrue(page.initialised);
        assertNotNull(sysMon);
    }

    // --- shutdown ----------------------------------------------------------------------------------

    @Test
    void shutdownShutsDownTheDataSinks() throws Exception {
        final ADataSink sink = mock(ADataSink.class);
        final NSysMonImpl target = createSysMon(sink);

        target.shutdown();

        verify(sink).shutdown();
    }

    @Test
    void shutdownShutsDownTheMeasurers() throws Exception {
        final AScalarMeasurer scalar = mock(AScalarMeasurer.class);
        final AScalarMeasurer timed = mock(AScalarMeasurer.class);
        final AEnvironmentMeasurer env = mock(AEnvironmentMeasurer.class);
        final NSysMonImpl target = createSysMon(dataSink);
        target.addScalarMeasurer(scalar);
        target.addTimedScalarMeasurer(timed);
        target.addEnvironmentMeasurer(env);

        target.shutdown();

        verify(scalar).shutdown();
        verify(timed).shutdown();
        verify(env).shutdown();
    }

    @Test
    void aFailingDataSinkDoesNotAbortTheShutdown() throws Exception {
        final ADataSink sink = mock(ADataSink.class);
        doThrow(new IllegalStateException("boom")).when(sink).shutdown();
        final NSysMonImpl target = createSysMon(sink);

        assertDoesNotThrow(target::shutdown);
    }

    // --- globally disabled -------------------------------------------------------------------------

    @Test
    void noDataIsProducedWhenGloballyDisabled() {
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        sysMon.start("a").finish();

        assertTrue(dataSink.data.isEmpty());
    }

    // --- helpers -----------------------------------------------------------------------------------

    static AScalarMeasurer contributing(String name, long value) throws Exception {
        final AScalarMeasurer measurer = mock(AScalarMeasurer.class);
        lenient().when(measurer.getTimeoutInMilliSeconds()).thenReturn(AOption.none());
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Map.class).put(name, new AScalarDataPoint(1L, name, value, 0));
            return null;
        }).when(measurer).contributeMeasurements(ArgumentMatchers.any(), ArgumentMatchers.anyLong(), ArgumentMatchers.any());
        return measurer;
    }

    static AScalarMeasurer failing() throws Exception {
        final AScalarMeasurer measurer = mock(AScalarMeasurer.class);
        lenient().when(measurer.getTimeoutInMilliSeconds()).thenReturn(AOption.none());
        lenient().doThrow(new IllegalStateException("boom")).when(measurer)
                .contributeMeasurements(ArgumentMatchers.any(), ArgumentMatchers.anyLong(), ArgumentMatchers.any());
        return measurer;
    }

    static AEnvironmentMeasurer environmentMeasurer(String value, String key) {
        return new AEnvironmentMeasurer() {
            @Override public void contributeMeasurements(AEnvironmentMeasurer.EnvironmentCollector data) {
                data.add(value, key);
            }
            @Override public void shutdown() {
            }
        };
    }

    static final class AwareDataSink implements ADataSink, NSysMonAware {
        private NSysMonApi injected;

        @Override public void setNSysMon(NSysMonApi sysMon) {
            this.injected = sysMon;
        }
        @Override public void onStartedHierarchicalMeasurement(String identifier) {
        }
        @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        }
        @Override public void shutdown() {
        }
    }

    static final class AwareScalarMeasurer implements AScalarMeasurer, NSysMonAware {
        private NSysMonApi injected;

        @Override public void setNSysMon(NSysMonApi sysMon) {
            this.injected = sysMon;
        }
        @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
        }
        @Override public AOption<Long> getTimeoutInMilliSeconds() {
            return AOption.none();
        }
        @Override public void shutdown() {
        }
    }

    static final class AwareEnvironmentMeasurer implements AEnvironmentMeasurer, NSysMonAware {
        private NSysMonApi injected;

        @Override public void setNSysMon(NSysMonApi sysMon) {
            this.injected = sysMon;
        }
        @Override public void contributeMeasurements(AEnvironmentMeasurer.EnvironmentCollector data) {
        }
        @Override public void shutdown() {
        }
    }

    static final class StubPageDefinition implements APresentationPageDefinition {
        private boolean initialised;

        @Override public String getId() {
            return "stub";
        }
        @Override public String getShortLabel() {
            return "stub";
        }
        @Override public String getFullLabel() {
            return "stub";
        }
        @Override public String getHtmlFileName() {
            return "stub.html";
        }
        @Override public String getControllerName() {
            return "stub";
        }
        @Override public boolean handleRestCall(String service, List<String> params, com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon json) {
            return false;
        }
        @Override public void init(NSysMonApi sysMon) {
            initialised = true;
        }
    }
}