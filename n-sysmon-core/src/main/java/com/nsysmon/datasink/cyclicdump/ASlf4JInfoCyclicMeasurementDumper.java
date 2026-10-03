package com.nsysmon.datasink.cyclicdump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author arno
 */
public class ASlf4JInfoCyclicMeasurementDumper extends ACyclicMeasurementDumper {
    private static final Logger log = LoggerFactory.getLogger(ASlf4JInfoCyclicMeasurementDumper.class);

    public ASlf4JInfoCyclicMeasurementDumper(int initialDelaySeconds, int frequencyInSeconds, int averagingDelayForScalarsMillis) {
        super(initialDelaySeconds, frequencyInSeconds, averagingDelayForScalarsMillis);
    }

    @Override protected void dump(String s) {
        log.info(s);
    }
}
