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

import java.util.HashMap;
import java.util.Map;

public class RESTMeasurer implements AScalarMeasurer, NSysMonAware {
    private String url;
    private NSysMonApi sysMon;

//    public static void main(String[] args) throws IOException {
//        RESTMeasurer restMeasurer = new RESTMeasurer("http://localhost:8080/omd-server/rest/nsysmonmeasurements/get/");
//        HashMap map = restMeasurer.callRest(42);
//        System.out.println(map);
//    }

    public RESTMeasurer() {
        this.url = "NOT_CONFIGURED";
    }

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws Exception {
    }

    @Override public void contributeMeasurements(final Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        RESTMeasurer restMeasurer = new RESTMeasurer();
        //System.out.println(url);
        HashMap<String, Double> myMap = (HashMap<String, Double>) restMeasurer.callRest(timestamp);

        for (String key : myMap.keySet()) {
            AScalarDataPoint point = new AScalarDataPoint(timestamp, key, Double.doubleToLongBits(myMap.get(key)), 0);
            data.put(key, point);
        }
    }

    @Override public void shutdown() throws Exception {
    }

    private HashMap callRest(long timestamp) {
        HashMap map = new HashMap();
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpGet getMethod = new HttpGet(url + "/" + timestamp);
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

    @Override public void setNSysMon(NSysMonApi sysMon) {
        this.sysMon = sysMon;
        url = sysMon.getConfig().additionalConfigurationParameters.get(ADefaultConfigFactory.KEY_RESTMEASURER_URL);
    }
}
