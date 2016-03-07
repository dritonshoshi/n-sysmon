package com.nsysmon.measure;

import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.config.appinfo.ADefaultApplicationInfoProvider;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.testutil.CollectingDataSink;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;

public class AMeasurementHierarchyImplTest {

    private NSysMonConfigBuilder configBuilder;

    @Before public void before() throws UnknownHostException {
        configBuilder = new NSysMonConfigBuilder(new ADefaultApplicationInfoProvider("dummy", "version"));
    }

    private NSysMonApi createSysMon(ADataSink dataSink) {
        final NSysMonApi result = new NSysMonImpl(configBuilder.build());
        NSysMonConfigurer.addDataSink(result, dataSink);
        return result;
    }

    @Test public void maxNumMeasurementsPerHierarchy_OK() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        configBuilder.setMaxNumMeasurementsPerHierarchy(11);
        final NSysMonApi sysMon = createSysMon(dataSink);

        ASimpleMeasurement simpleMeasurement = sysMon.start("Start");

        performMeasurement(sysMon);
        simpleMeasurement.finish();

        Assert.assertFalse(sysMon.hasRunningMeasurement());
        Assert.assertEquals(1, dataSink.numStarted);
        Assert.assertEquals(1, dataSink.data.size());
    }

    @Test public void maxNumMeasurementsPerHierarchy_OneTooFew() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        configBuilder.setMaxNumMeasurementsPerHierarchy(11 - 1);
        final NSysMonApi sysMon = createSysMon(dataSink);

        ASimpleMeasurement simpleMeasurement = sysMon.start("Start");

        performMeasurement(sysMon);
        simpleMeasurement.finish();

        Assert.assertFalse(sysMon.hasRunningMeasurement());
        Assert.assertEquals(1, dataSink.numStarted);
        Assert.assertEquals(1, dataSink.data.size());
    }

    @Test public void maxNumMeasurementsPerHierarchy_FourTooFew() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        configBuilder.setMaxNumMeasurementsPerHierarchy(11 - 4);
        final NSysMonApi sysMon = createSysMon(dataSink);

        ASimpleMeasurement simpleMeasurement = sysMon.start("Start");

        performMeasurement(sysMon);
        simpleMeasurement.finish();

        Assert.assertFalse(sysMon.hasRunningMeasurement());
        Assert.assertEquals(1, dataSink.numStarted);
        Assert.assertEquals(1, dataSink.data.size());
    }

    @Test public void maxNumMeasurementsPerHierarchy_FiveTooFew() {
        final CollectingDataSink dataSink = new CollectingDataSink();
        configBuilder.setMaxNumMeasurementsPerHierarchy(11 - 5);
        final NSysMonApi sysMon = createSysMon(dataSink);

        ASimpleMeasurement simpleMeasurement = sysMon.start("Start");

        performMeasurement(sysMon);
        simpleMeasurement.finish();

        Assert.assertFalse(sysMon.hasRunningMeasurement());

        Assert.assertEquals(1, dataSink.numStarted);
        Assert.assertEquals(1, dataSink.data.size());
    }

    private void performMeasurement(final NSysMonApi sysMon) {
        sysMon.measure("A11", m -> {
            sysMon.measure("B11", m1 -> {
                sysMon.measure("C11", m1 -> {
                    //nothing to do
                });
                sysMon.measure("C21", m1 -> {
                    //nothing to do
                });
                sysMon.measure("C31", m1 -> {
                    //nothing to do
                });
                sysMon.measure("C41", m1 -> {
                    //nothing to do
                });
            });
            sysMon.measure("B12", m1 -> {
                sysMon.measure("C12", m1 -> {
                    //nothing to do
                });
                sysMon.measure("C22", m1 -> {
                    //nothing to do
                });
                sysMon.measure("C32", m1 -> {
                    //nothing to do
                });
                sysMon.measure("C42", m1 -> {
                    //nothing to do
                });
            });

        });
    }
}
