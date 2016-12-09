package com.nsysmon.datasink.transfer;

import com.google.gson.Gson;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.datasink.transfer.types.TransferMeasurementRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TransmittingDatasink implements ADataSink {

    private List<AHierarchicalDataRoot> toTransmit;
    private Gson gson = new Gson();
    private final CloseableHttpClient httpClient = HttpClients.createDefault(); //TODO make timeout values configurable
    private final String uri;

    public TransmittingDatasink(String uri) {
        toTransmit = new ArrayList<>();
        this.uri = uri;
    }

    @Override
    public void shutdown() throws Exception {
        //TODO check for current transmitting-job and wait until it is finished
    }

    @Override
    public void onStartedHierarchicalMeasurement(String identifier) {
        //nothing to do
    }

    @Override
    public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        storeDataForTransmittion(data);
        try {
            nodgeTransmitter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void nodgeTransmitter() throws IOException {
        //TODO perform transmition

        Iterator<AHierarchicalDataRoot> iterator = toTransmit.iterator();
        while (iterator.hasNext()) {
            AHierarchicalDataRoot data = iterator.next();
            final HttpPost httpPost = new HttpPost(uri);

            HttpEntity entity = new StringEntity(gson.toJson(new TransferMeasurementRequest(gson.toJson(data))));
            httpPost.setEntity(entity);

            final CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                //TODO response with commands for monitoring this app?!
            } finally {
                response.close();
            }
            iterator.remove();
        }
    }

    private synchronized void storeDataForTransmittion(AHierarchicalDataRoot data) {
        //TODO TKT check for concurrency
        toTransmit.add(data);
    }


}
