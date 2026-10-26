package com.nsysmon.measure.environment;

import com.ajjpj.afoundation.collection.immutable.AList;
import com.nsysmon.util.AShutdownable;

import java.util.List;


/**
 * @author arno
 */
public interface AEnvironmentMeasurer extends AShutdownable {
    /**
     * @param data is a 'collectiong parameter', i.e. the method adds its own results to the existing collection.
     */
    void contributeMeasurements(EnvironmentCollector data) throws Exception;

    class EnvironmentCollector {
        public final List<AEnvironmentData> data;

        public EnvironmentCollector(List<AEnvironmentData> data) {
            this.data = data;
        }

        public void add(String value, String... key) {
            final AList<String> fullKey = AList.create(key);
            data.add(new AEnvironmentData(fullKey, value));
        }
    }

    default boolean isAllowedInCleartext(String envName) {
        return envName != null
                && !envName.toLowerCase().contains("secure")
                && !envName.toLowerCase().contains("password")
                && !envName.toLowerCase().contains("truststore")
                && !envName.toLowerCase().contains("keystore")
                && !envName.toLowerCase().contains("token")
                && !envName.toLowerCase().contains("private");
    }
}
