package com.nsysmon.datasink.cyclicdump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arno
 */
public class ASlf4JDebugCyclicMeasurementDumper extends ACyclicMeasurementDumper {
    private static final Logger log = LoggerFactory.getLogger(ASlf4JDebugCyclicMeasurementDumper.class);

    public ASlf4JDebugCyclicMeasurementDumper(int initialDelaySeconds, int frequencyInSeconds, int averagingDelayForScalarsMillis) {
        super(initialDelaySeconds, frequencyInSeconds, averagingDelayForScalarsMillis);
    }

    @Override protected void dump(String s) {
        log.debug(s);
    }
}
