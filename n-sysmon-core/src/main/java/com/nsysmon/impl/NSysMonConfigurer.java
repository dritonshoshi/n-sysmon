package com.nsysmon.impl;


import com.nsysmon.NSysMonApi;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import com.nsysmon.measure.scalar.AScalarMeasurer;

/**
 * This class can change the configuration of an existing ASysMon instance. This is done to avoid race conditions
 *  during application startup, e.g. if ASysMon is used during Spring startup, and servlets need to contribute
 *  configuration later.<p>
 *
 * These methods are not part of NSysMon itself to keep that API lean and clean: These methods are for use during system
 *  initialization.
 *
 * @author arno
 */
public class NSysMonConfigurer {
    public static void addScalarMeasurer(NSysMonApi sysMon, AScalarMeasurer m) {
        ((NSysMonImpl) sysMon).addScalarMeasurer(m);
    }

    public static void addEnvironmentMeasurer(NSysMonApi sysMon, AEnvironmentMeasurer m) {
        ((NSysMonImpl) sysMon).addEnvironmentMeasurer(m);
    }

    public static void addDataSink(NSysMonApi sysMon, ADataSink handler) {
        ((NSysMonImpl) sysMon).addDataSink(handler);
    }
}
