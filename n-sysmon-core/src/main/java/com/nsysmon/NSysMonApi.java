package com.nsysmon;

import com.ajjpj.afoundation.collection.immutable.AList;
import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.data.AScalarMeasurementInterceptor;
import com.nsysmon.impl.RobustScalarMeasurerWrapper;
import com.nsysmon.measure.ACollectingMeasurement;
import com.nsysmon.measure.AMeasureCallback;
import com.nsysmon.measure.AMeasureCallbackVoid;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.measure.environment.AEnvironmentData;

import java.util.List;
import java.util.Map;


/**
 * @author arno
 */
public interface NSysMonApi {
    NSysMonConfig getConfig();

    <E extends Exception> void measure(String identifier, AMeasureCallbackVoid<E> callback) throws E;
    <R, E extends Exception> R measure(String identifier, AMeasureCallback<R,E> callback) throws E;

    /**
     * This tells N-SysMon that the currently running measurement starts a new 'flow'. Other measurements (in this
     *  or in another JVM) may 'join' that flow, so that these measurements can be evaluated at a later time. Typical
     *  examples of this is processing done in a spawned thread, batch processing that uses several worker threads,
     *  or asynchronous web service calls.<p>
     *
     * It is an invalid to call this method without a surrounding measurement, and doing so throws an exception.
     */
    void startFlow(ACorrelationId flowId);

    void joinFlow(ACorrelationId flowId);

    ASimpleMeasurement start(String identifier);
    ASimpleMeasurement start(String identifier, boolean serial);

    /**
     * returns true iff a simple measurement is currently running for this thread
     */
    boolean hasRunningMeasurement();

    /**
     * This is for the rare case that measurement data was collected by other means and should be 'injected'
     *  into N-SysMon. If you do not understand this, this method is probably not for you.
     */
    void injectSyntheticMeasurement(AHierarchicalDataRoot d);

    ACollectingMeasurement startCollectingMeasurement(String identifier);
    ACollectingMeasurement startCollectingMeasurement(String identifier, boolean serial);

    Map<String, AScalarDataPoint> getScalarMeasurements();

    Map<String, AScalarDataPoint> getScalarMeasurements(int averagingDelayForScalarsMillis);

    List<AEnvironmentData> getEnvironmentMeasurements() throws Exception;

    AList<RobustScalarMeasurerWrapper> getTimedScalarForDirectAccess();
    Map<String,ARingBuffer<AScalarDataPoint>> getTimedScalarMeasurements();
    Map<String,ARingBuffer<AScalarDataPoint>> getTimedScalarMeasurementsForMonitoring();

    void addTimedScalarMeasurement(AScalarDataPoint... dataPoint);
    void addTimedScalarMeasurementInterceptor(AScalarMeasurementInterceptor interceptor);
}
