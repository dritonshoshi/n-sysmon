package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.immutable.AList;
import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonAware;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.ACollectingMeasurement;
import com.nsysmon.measure.AMeasureCallback;
import com.nsysmon.measure.AMeasureCallbackVoid;
import com.nsysmon.measure.AMeasurementHierarchy;
import com.nsysmon.measure.AMeasurementHierarchyImpl;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.util.AShutdownable;
import com.nsysmon.util.DaemonThreadFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * This class is the point of contact for an application to NSysMon. There are basically two ways to use it:
 *
 * <ul>
 *     <li> Use the static get() method to access it as a singleton. That is simple and convenient, and it is
 *          sufficient for many applications. If it is used that way, all configuration must be done through
 *          the static methods of ADefaultSysMonConfig. </li>
 *     <li> Create and manage your own instance (or instances) by calling the constructor, passing in your
 *          configuration. This is for maximum flexibility, but you lose some convenience. </li>
 * </ul>
 *
 * @author arno
 */
public class NSysMonImpl implements AShutdownable, NSysMonApi {
    private static final NSysMonLogger log = NSysMonLogger.get(NSysMonImpl.class);

    private final NSysMonConfig config;
    private volatile AList<RobustDataSinkWrapper> handlers = AList.nil();
    private volatile AList<RobustScalarMeasurerWrapper> scalarMeasurers = AList.nil();
    private volatile AList<RobustScalarMeasurerWrapper> timedScalarMeasurers = AList.nil();
    private volatile AList<RobustEnvironmentMeasurerWrapper> environmentMeasurers = AList.nil();
    private volatile TimedScalarDataWrapper timedScalarMeasureRunnable;

    private final ThreadLocal<AMeasurementHierarchy> hierarchyPerThread = new ThreadLocal<>();

    public NSysMonImpl(NSysMonConfig config) {
        this.config = config;

        config.initialScalarMeasurers.forEach(this::addScalarMeasurer);

        ScheduledExecutorService scheduledPool = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());

        timedScalarMeasureRunnable = new TimedScalarDataWrapper(config.maxNumMeasurementsPerTimedScalar, config.maxNumMeasurementsForMonitoring);
        scheduledPool.scheduleAtFixedRate(timedScalarMeasureRunnable, 0, config.durationOfOneTimedScalar, TimeUnit.SECONDS);
        config.initialTimedScalarMeasurers.forEach(this::addTimedScalarMeasurer);

        config.initialDataSinks.forEach(this::addDataSink);

        config.initialEnvironmentMeasurers.forEach(this::addEnvironmentMeasurer);

        for(APresentationMenuEntry menuEntry: config.presentationMenuEntries) {
            for(APresentationPageDefinition pageDef: menuEntry.pageDefinitions) {
                pageDef.init(this);
            }
        }
    }

    @Override public NSysMonConfig getConfig() {
        return config;
    }

    private void injectSysMon(Object o) {
        if(o instanceof NSysMonAware) {
            ((NSysMonAware) o).setNSysMon(this);
        }
    }

    void addScalarMeasurer(AScalarMeasurer m) {
        injectSysMon(m);
        scalarMeasurers = scalarMeasurers.cons(new RobustScalarMeasurerWrapper(m, config.measurementTimeoutNanos, config.maxNumMeasurementTimeouts));
    }

    void addTimedScalarMeasurer(AScalarMeasurer m) {
        injectSysMon(m);
        timedScalarMeasurers = timedScalarMeasurers.cons(new RobustScalarMeasurerWrapper(m, config.measurementTimeoutNanos, config.maxNumMeasurementTimeouts));
        timedScalarMeasureRunnable.refreshMeasurers(timedScalarMeasurers);
    }

    void addEnvironmentMeasurer(AEnvironmentMeasurer m) {
        injectSysMon(m);
        environmentMeasurers = environmentMeasurers.cons(new RobustEnvironmentMeasurerWrapper(m, config.measurementTimeoutNanos, config.maxNumMeasurementTimeouts));
    }

    void addDataSink(ADataSink handler) {
        injectSysMon(handler);
        handlers = handlers.cons(new RobustDataSinkWrapper(handler, config.dataSinkTimeoutNanos, config.maxNumDataSinkTimeouts));
    }

    private ADataSink getCompositeDataSink() {
        return new ADataSink() {
            @Override public void onStartedHierarchicalMeasurement(String identifier) {
                for(RobustDataSinkWrapper handler: handlers) {
                    handler.onStartedHierarchicalMeasurement(identifier);
                }
            }

            @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
                hierarchyPerThread.remove();

                for(RobustDataSinkWrapper handler: handlers) {
                    handler.onFinishedHierarchicalMeasurement(data);
                }
            }

            @Override
            public void onWorkingStep(AHierarchicalDataRoot trace) {
                for(RobustDataSinkWrapper handler: handlers) {
                    handler.onWorkingStep(trace);
                }
            }

            @Override public void shutdown() {
            }
        };
    }

    private AMeasurementHierarchy getMeasurementHierarchy(boolean create) {
        final AMeasurementHierarchy candidate = hierarchyPerThread.get();
        if(candidate != null || ! create) {
            return candidate;
        }

        final AMeasurementHierarchy result = new AMeasurementHierarchyImpl(config, getCompositeDataSink());
        hierarchyPerThread.set(result);
        return result;
    }

    @Override public <E extends Exception> void measure(String identifier, AMeasureCallbackVoid<E> callback) throws E {
        final ASimpleMeasurement m = start(identifier);
        try {
            callback.call(m);
        } finally {
            m.finish();
        }
    }

    @Override public <R, E extends Exception> R measure(String identifier, AMeasureCallback<R,E> callback) throws E {
        final ASimpleMeasurement m = start(identifier);
        try {
            return callback.call(m);
        } finally {
            m.finish();
        }
    }

    @Override public ASimpleMeasurement start(String identifier) {
        return start(identifier, true);
    }
    @Override public ASimpleMeasurement start(String identifier, boolean serial) {
        return getMeasurementHierarchy(true).start(identifier, serial);
    }

    @Override public boolean hasRunningMeasurement() {
        return getMeasurementHierarchy(false) != null;
    }

    @Override
    public void startFlow(ACorrelationId flowId) {
        final AMeasurementHierarchy h = getMeasurementHierarchy(false);
        if(h == null) {
            log.error (new IllegalStateException("flow handling only while a measurement is running"));
            return;
        }
        h.onStartFlow(flowId);
    }

    @Override
    public void joinFlow(ACorrelationId flowId) {
        final AMeasurementHierarchy h = getMeasurementHierarchy(false);
        if(h == null) {
            log.error (new IllegalStateException("flow handling only while a measurement is running"));
            return;
        }
        h.onJoinFlow(flowId);
    }

    /**
     * This is for the rare case that measurement data was collected by other means and should be 'injected'
     *  into N-SysMon. If you do not understand this, this method is probably not for you.
     */
    @Override public void injectSyntheticMeasurement(AHierarchicalDataRoot d) {
        getCompositeDataSink().onStartedHierarchicalMeasurement(d.getRootNode().getIdentifier());
        getCompositeDataSink().onFinishedHierarchicalMeasurement(d);
    }

    @Override public ACollectingMeasurement startCollectingMeasurement(String identifier) {
        return startCollectingMeasurement(identifier, true);
    }
    @Override public ACollectingMeasurement startCollectingMeasurement(String identifier, boolean serial) {
        return getMeasurementHierarchy(true).startCollectingMeasurement(identifier, serial);
    }

    @Override public Map<String, AScalarDataPoint> getScalarMeasurements() {
        return getScalarMeasurements(config.averagingDelayForScalarsMillis);
    }

    @Override public Map<String, AScalarDataPoint> getScalarMeasurements(int averagingDelayForScalarsMillis) {
        final Map<String, AScalarDataPoint> result = new TreeMap<>();
        if(NSysMonConfig.isGloballyDisabled()) {
            return result;
        }

        final Map<String, Object> mementos = new TreeMap<>();
        for(RobustScalarMeasurerWrapper measurer: scalarMeasurers) {
            measurer.prepareMeasurements(mementos);
        }

        try {
            Thread.sleep(averagingDelayForScalarsMillis);
        } catch (InterruptedException e) {
        }

        final long now = System.currentTimeMillis();
        for(RobustScalarMeasurerWrapper measurer: scalarMeasurers) {
            measurer.contributeMeasurements(result, now, mementos);
        }
        return result;
    }

    @Override
    public AList<RobustScalarMeasurerWrapper> getTimedScalarForDirectAccess() {
        AList<RobustScalarMeasurerWrapper> timedScalarMeasurers = timedScalarMeasureRunnable.getTimedScalarMeasurers();
        return timedScalarMeasurers;
    }

    @Override public Map<String, ARingBuffer<AScalarDataPoint>> getTimedScalarMeasurements() {
        Map<String, ARingBuffer<AScalarDataPoint>> result = new TreeMap<>();
        if(NSysMonConfig.isGloballyDisabled()) {
            return result;
        }

        result = timedScalarMeasureRunnable.getMeasurements();

        return result;
    }

    @Override public Map<String, ARingBuffer<AScalarDataPoint>> getTimedScalarMeasurementsForMonitoring() {
        /*
            Note: The measurements for monitoring could also be extracted from the total data.
            It was done by using a second list, so the processing-time overall is less.
            If you use the filtering, every monitor-call must parse every datapoint of every timedscalar.
         */
        Map<String, ARingBuffer<AScalarDataPoint>> result = new TreeMap<>();
        if(NSysMonConfig.isGloballyDisabled()) {
            return result;
        }

        result = timedScalarMeasureRunnable.getMeasurementsForMonitoring();

        return result;
    }

    @Override public void addTimedScalarMeasurement(AScalarDataPoint... dataPoint) {
        timedScalarMeasureRunnable.addMeasurement(dataPoint);
    }

    @Override public List<AEnvironmentData> getEnvironmentMeasurements() {
        final List<AEnvironmentData> result = new ArrayList<>();
        if(NSysMonConfig.isGloballyDisabled()) {
            return result;
        }

        for(RobustEnvironmentMeasurerWrapper m: environmentMeasurers) {
            m.contributeMeasurements(new AEnvironmentMeasurer.EnvironmentCollector(result));
        }
        return result;
    }

    @Override public void shutdown() {
        log.info("shutting down N-SysMon");

        for(RobustDataSinkWrapper handler: handlers) {
            handler.shutdown();
        }

        for (RobustScalarMeasurerWrapper m: scalarMeasurers) {
            m.shutdown();
        }

        for (RobustScalarMeasurerWrapper m: timedScalarMeasurers) {
            m.shutdown();
        }

        for(RobustEnvironmentMeasurerWrapper m: environmentMeasurers) {
            m.shutdown();
        }

        log.info("finished shutting down N-SysMon");
    }
}

