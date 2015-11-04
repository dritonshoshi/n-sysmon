package com.nsysmon.config;

import com.nsysmon.config.appinfo.AApplicationInfoProvider;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import com.nsysmon.measure.http.AHttpRequestAnalyzer;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.util.timer.ATimer;

import java.util.Collections;
import java.util.List;


/**
 * @author arno
 */
public class NSysMonConfig {
    public static final String PROPNAME_GLOBALLY_DISABLED = "com.nsysmon.globallydisabled";

    public final AApplicationInfoProvider appInfo;

    public final int averagingDelayForScalarsMillis;

    public final int maxNestedMeasurements;
    public final int maxNumMeasurementsPerHierarchy;
    public final int maxNumMeasurementsPerTimedScalar;
    public final int durationOfOneTimedScalar;

    public final long measurementTimeoutNanos;
    public final int maxNumMeasurementTimeouts;

    public final long dataSinkTimeoutNanos;
    public final int maxNumDataSinkTimeouts;

    public final ATimer timer;
    public final AHttpRequestAnalyzer httpRequestAnalyzer;

    public final List<AEnvironmentMeasurer> initialEnvironmentMeasurers;
    public final List<AScalarMeasurer> initialScalarMeasurers;
    public final List<AScalarMeasurer> initialTimedScalarMeasurers;
    public final List<ADataSink> initialDataSinks;

    public final String defaultPage;
    public final List<APresentationMenuEntry> presentationMenuEntries;

    public NSysMonConfig(AApplicationInfoProvider appInfo,
                         int averagingDelayForScalarsMillis, int durationOfOneTimedScalar,
                         int maxNestedMeasurements, int maxNumMeasurementsPerHierarchy, int maxNumMeasurementsPerTimedScalar,
                         long measurementTimeoutNanos, int maxNumMeasurementTimeouts, long dataSinkTimeoutNanos, int maxNumDataSinkTimeouts,
                         ATimer timer, AHttpRequestAnalyzer httpRequestAnalyzer,
                         List<AEnvironmentMeasurer> environmentMeasurers, List<AScalarMeasurer> initialScalarMeasurers, List<AScalarMeasurer> initialTimedScalarMeasurers,
                         List<ADataSink> initialDataSinks,
                         String defaultPage, List<APresentationMenuEntry> presentationMenuEntries) {
        this.appInfo = appInfo;
        this.averagingDelayForScalarsMillis = averagingDelayForScalarsMillis;
        this.durationOfOneTimedScalar = durationOfOneTimedScalar;
        this.maxNestedMeasurements = maxNestedMeasurements;
        this.maxNumMeasurementsPerHierarchy = maxNumMeasurementsPerHierarchy;
        this.maxNumMeasurementsPerTimedScalar = maxNumMeasurementsPerTimedScalar;
        this.measurementTimeoutNanos = measurementTimeoutNanos;
        this.maxNumMeasurementTimeouts = maxNumMeasurementTimeouts;
        this.dataSinkTimeoutNanos = dataSinkTimeoutNanos;
        this.maxNumDataSinkTimeouts = maxNumDataSinkTimeouts;
        this.timer = timer;
        this.httpRequestAnalyzer = httpRequestAnalyzer;
        this.initialEnvironmentMeasurers = Collections.unmodifiableList(environmentMeasurers);
        this.initialScalarMeasurers = Collections.unmodifiableList(initialScalarMeasurers);
        this.initialTimedScalarMeasurers = Collections.unmodifiableList(initialTimedScalarMeasurers);
        this.initialDataSinks = Collections.unmodifiableList(initialDataSinks);
        this.defaultPage = defaultPage;
        this.presentationMenuEntries = Collections.unmodifiableList(presentationMenuEntries);
    }

    /**
     * This flag switches off all 'risky' (or potentially expensive) functionality. It serves as a safeguard in case
     *  N-SysMon has a bug that impacts an application.
     */
    public static boolean isGloballyDisabled() {
        final String s = System.getProperty(PROPNAME_GLOBALLY_DISABLED);
        return "true".equals(s);
    }
}
