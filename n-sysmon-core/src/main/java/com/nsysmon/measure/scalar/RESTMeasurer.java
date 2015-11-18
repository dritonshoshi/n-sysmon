package com.nsysmon.measure.scalar;

import com.nsysmon.NSysMonApi;
import com.nsysmon.config.ADefaultConfigFactory;
import com.nsysmon.config.NSysMonAware;
import com.nsysmon.data.AScalarDataPoint;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;

import java.util.HashMap;
import java.util.Map;

public class RESTMeasurer implements AScalarMeasurer, NSysMonAware {
    private String url;

//    public static void main(String[] args) throws IOException {
//        RESTMeasurer restMeasurer = new RESTMeasurer();
//       restMeasurer.url = "http://localhost:8080/omd-server/rest/nsysmonmeasurements/get/";
//        Map map = restMeasurer.callRest(42);
//        System.out.println(map);
//    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws Exception {
    }

    @Override public void contributeMeasurements(final Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        Map<String, Double> myMap = (HashMap<String, Double>) callRest(timestamp);

        for (String key : myMap.keySet()) {
            AScalarDataPoint point = new AScalarDataPoint(timestamp, key, Double.doubleToLongBits(myMap.get(key)), 0);
            data.put(key, point);
        }
    }

    @Override public void shutdown() throws Exception {
    }

    private Map callRest(long timestamp) {
        Map map = new HashMap();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpGet getMethod = new HttpGet(url + timestamp);
            HttpResponse response = httpClient.execute(getMethod);

            // CONVERT RESPONSE TO STRING
            String result = EntityUtils.toString(response.getEntity());

            ObjectReader reader = new ObjectMapper().reader(HashMap.class);
            map = reader.readValue(result);
        }
        catch (Exception e) {

        }
        return map;
    }

    @Override public void setNSysMon(NSysMonApi sysMon) {
        url = sysMon.getConfig().additionalConfigurationParameters.get(ADefaultConfigFactory.KEY_RESTMEASURER_URL);
    }
}
