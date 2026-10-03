package com.nsysmon.impl;

import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.testutil.CollectingDataSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

import static com.nsysmon.impl.NSysMonImplTest.APP_INFO;
import static com.nsysmon.impl.NSysMonImplTest.contributing;
import static com.nsysmon.impl.NSysMonImplTest.environmentMeasurer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NSysMonConfigurerTest {

    private NSysMonImpl sysMon;

    @BeforeEach
    void before() {
        final NSysMonConfigBuilder builder = new NSysMonConfigBuilder(APP_INFO);
        builder.setAveragingDelayForScalarsMillis(0);
        sysMon = new NSysMonImpl(builder.build());
    }

    @Test
    void everyConfigurerMethodIsStatic() {
        assertNotNull(new NSysMonConfigurer());
        assertTrue(Arrays.stream(NSysMonConfigurer.class.getDeclaredMethods())
                .allMatch(m -> Modifier.isStatic(m.getModifiers())));
    }

    @Test
    void aDataSinkIsRegisteredOnTheInstance() {
        final CollectingDataSink sink = new CollectingDataSink();

        NSysMonConfigurer.addDataSink(sysMon, sink);
        sysMon.start("a").finish();

        assertEquals(1, sink.data.size());
    }

    @Test
    void aScalarMeasurerIsRegisteredOnTheInstance() throws Exception {
        NSysMonConfigurer.addScalarMeasurer(sysMon, contributing("cpu", 5));

        assertEquals(5L, sysMon.getScalarMeasurements().get("cpu").getValueRaw());
    }

    @Test
    void anEnvironmentMeasurerIsRegisteredOnTheInstance() throws Exception {
        NSysMonConfigurer.addEnvironmentMeasurer(sysMon, environmentMeasurer("value", "key"));

        assertEquals(1, sysMon.getEnvironmentMeasurements().size());
    }

    @Test
    void severalDataSinksAllReceiveTheMeasurement() {
        final CollectingDataSink first = new CollectingDataSink();
        final CollectingDataSink second = new CollectingDataSink();

        NSysMonConfigurer.addDataSink(sysMon, first);
        NSysMonConfigurer.addDataSink(sysMon, second);
        sysMon.start("a").finish();

        assertEquals(1, first.data.size());
        assertEquals(1, second.data.size());
    }

    @Test
    void theApiTypeIsAcceptedAsParameter() {
        final NSysMonApi asApi = sysMon;
        final CollectingDataSink sink = new CollectingDataSink();

        NSysMonConfigurer.addDataSink(asApi, sink);
        sysMon.start("a").finish();

        assertEquals(1, sink.data.size());
    }

    @Test
    void registeringOnAnUnrelatedApiImplementationIsRejected() {
        final NSysMonApi foreign = mock(NSysMonApi.class);

        assertThrows(ClassCastException.class, () -> NSysMonConfigurer.addDataSink(foreign, mock(ADataSink.class)));
    }

    @Test
    void environmentMeasurersContributeToOneSharedList() throws Exception {
        NSysMonConfigurer.addEnvironmentMeasurer(sysMon, environmentMeasurer("first", "k"));
        NSysMonConfigurer.addEnvironmentMeasurer(sysMon, environmentMeasurer("second", "k"));

        assertEquals(2, sysMon.getEnvironmentMeasurements().size());
    }

    @Test
    void scalarMeasurersSeeTheSameMementoAcrossPrepareAndContribute() throws Exception {
        final MementoRecordingMeasurer measurer = new MementoRecordingMeasurer();

        NSysMonConfigurer.addScalarMeasurer(sysMon, measurer);
        sysMon.getScalarMeasurements();

        assertTrue(measurer.sawMemento);
    }

    static final class MementoRecordingMeasurer implements AScalarMeasurer {
        private boolean sawMemento;

        @Override public void prepareMeasurements(Map<String, Object> mementos) {
            mementos.put("prepared", Boolean.TRUE);
        }
        @Override public void contributeMeasurements(Map<String, com.nsysmon.data.AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
            sawMemento = mementos.containsKey("prepared");
        }
        @Override public com.ajjpj.afoundation.collection.immutable.AOption<Long> getTimeoutInMilliSeconds() {
            return com.ajjpj.afoundation.collection.immutable.AOption.none();
        }
        @Override public void shutdown() {
        }
    }
}