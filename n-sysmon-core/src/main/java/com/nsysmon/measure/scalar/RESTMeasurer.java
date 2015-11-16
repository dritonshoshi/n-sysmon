package com.nsysmon.measure.scalar;

import com.nsysmon.data.AScalarDataPoint;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RESTMeasurer implements AScalarMeasurer {

    public static void main(String[] args) throws IOException {
        RESTMeasurer restMeasurer = new RESTMeasurer();
        HashMap map = restMeasurer.callRest();
        System.out.println(map);

    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws Exception {
    }

    @Override public void contributeMeasurements(final Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        RESTMeasurer restMeasurer = new RESTMeasurer();
        HashMap<String, Double> myMap = (HashMap<String, Double>) restMeasurer.callRest();

        for (String key : myMap.keySet()) {
            AScalarDataPoint point = new AScalarDataPoint(timestamp, key, Double.doubleToLongBits(myMap.get(key)), 0);
            data.put(key, point);
        }
    }

    @Override public void shutdown() throws Exception {

    }

    private HashMap callRest() {
        HashMap map = null;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();

            String getUrl = "http://localhost:8080/omd-server/rest/nsysmonmeasurements/get/1"; //TODO move to config

            HttpGet getMethod = new HttpGet(getUrl);
            HttpResponse response = httpClient.execute(getMethod);

            // CONVERT RESPONSE TO STRING
            String result = EntityUtils.toString(response.getEntity());
            ObjectMapper om = new ObjectMapper();
            map = om.readValue(result, HashMap.class);
        }
        catch (Exception e) {

        }
        return map;
    }
}

