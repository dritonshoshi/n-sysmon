package com.nsysmon.measure;

/**
 * This is a builder class representing an ongoing thread specific measurement.
 *
 * @author arno
 */
public interface ASimpleMeasurement extends AWithParameters {
    void finish();
}
