package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.ADefaultConfigFactory;
import com.nsysmon.config.NSysMonAware;
import com.nsysmon.data.AScalarDataPoint;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RESTMeasurer implements AScalarMeasurer, NSysMonAware {
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    private String restMeasurerUrl;
    private int timeoutInSeconds = 10;
    private static final Logger LOG = Logger.getLogger(RESTMeasurer.class);

//    public static void main(String[] args) throws IOException {
//        RESTMeasurer restMeasurer = new RESTMeasurer();
//        restMeasurer.url = "http://localhost:8080/omd-server/rest/nsysmonmeasurements/get/";
//        Map map = restMeasurer.callRest(42);
//        System.out.println(map);
//    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws Exception {
    }

    @Override public void contributeMeasurements(final Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        final Map<String, Double> myMap = callRest(timestamp);

        for (Map.Entry<String, Double> stringDoubleEntry : myMap.entrySet()) {
            final String key = stringDoubleEntry.getKey().intern();
            final String valueString = Double.toString(stringDoubleEntry.getValue());
            final int numFracDigits = valueString.substring(valueString.indexOf('.') + 1).length();
            final long value = Long.parseLong(DOT_PATTERN.matcher(valueString).replaceAll(""));
            final AScalarDataPoint point = new AScalarDataPoint(timestamp, key, value, numFracDigits);
            data.put(key, point);
        }
    }

    @Override public void shutdown() throws Exception {
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.some((long) ((timeoutInSeconds  + 2) * 1000));
    }

    private Map<String, Double> callRest(long timestamp) throws IOException {

        final URL url = new URL(restMeasurerUrl + timestamp);
        final URLConnection conn = url.openConnection();
        conn.setConnectTimeout(timeoutInSeconds * 1000);
        conn.setReadTimeout(timeoutInSeconds * 1000);
        try (InputStreamReader isr = new InputStreamReader(conn.getInputStream()); BufferedReader br = new BufferedReader(isr)) {
            return parseResponse(br.readLine());
        }
        catch (SocketTimeoutException e) {
            return Collections.emptyMap();
        }
        catch (Exception e) {
            LOG.error(e);
            return Collections.emptyMap();
        }
    }

    private Map<String, Double> parseResponse(final String response) {
        if (response == null || response.length() < 3) {
            return Collections.emptyMap();
        }

        final String responseWithoutBraces = response.substring(1, response.length()-1);
        final String[] measurements = responseWithoutBraces.split(",");
        final Map<String, Double> result = new HashMap<>(measurements.length);
        for (String measurement : measurements) {
            final String[] values = measurement.split(":");
            final String measurer = values[0].substring(1, values[0].length() - 1);
            final Double value = Double.parseDouble(values[1]);
            result.put(measurer, value);
        }
        return result;
    }

    @Override public void setNSysMon(NSysMonApi sysMon) {
        restMeasurerUrl = sysMon.getConfig().additionalConfigurationParameters.get(ADefaultConfigFactory.KEY_RESTMEASURER_URL);
        timeoutInSeconds = Integer.parseInt(sysMon.getConfig().additionalConfigurationParameters.get(ADefaultConfigFactory.KEY_RESTMEASURER_URL_TIMEOUT_SECONDS));
    }
}
