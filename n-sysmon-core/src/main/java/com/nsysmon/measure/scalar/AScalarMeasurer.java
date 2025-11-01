package com.nsysmon.measure.scalar;


import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.util.AShutdownable;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Scalar measurements are performed in two steps because some data (e.g. CPU load) must be calculated based on raw data
 *  from two distinct points in time. Things like 'load' is inherently an average over some period of time, after all.<p>
 *
 * So first, <code>prepareMeasurements()</code> is called, allowing a measurer to store some data as a memento (e.g.
 *  the first number of jiffies together with the first timestamp).<p>
 *
 * Then the caller waits some time, typically between a tenth of a second and a second, but potentially longer, to
 *  give measurers a solid time base to average data over.<p>
 *
 * Finally, <code>contributeMeasurements()</code> is called, passing in the mementos of the preparation phase. This
 *  is when the actual data is collected. Non-averaging measurers can (and probably should) ignore
 *  <code>prepareMeasurements()</code> and the entire memento mechanism.
 */
public interface AScalarMeasurer extends AShutdownable {

    String KEY_CONFIGURATION_ACTIVE = "active";
    String KEY_CONFIGURATION_MEDIUM = "medium";
    String KEY_CONFIGURATION_HIGH = "high";


    enum EvaluatedValue{
        LOW, MEDIUM, HIGH
    }

    default void prepareMeasurements(Map<String, Object> mementos) throws Exception{
        //nothing to do by default
    }

    void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception;
    AOption<Long> getTimeoutInMilliSeconds();

    default String getGroupnameOfMeasurement(String measurement){
        return null;
    }

    default String getDescriptionOfMeasurement(String measurement){
        return null;
    }

    default List<String> getConfigurationParameters(){
        return Collections.emptyList();
    }

    default boolean isResponsibleForMeasurement(String key) {
        return getDescriptionOfMeasurement(key) != null;
    }


}
