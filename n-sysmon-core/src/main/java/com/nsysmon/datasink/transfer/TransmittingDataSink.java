package com.nsysmon.datasink.transfer;

import com.google.gson.Gson;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TransmittingDataSink implements ADataSink {

    private final List<AHierarchicalDataRoot> toTransmit = new ArrayList<>();;
    private final Gson gson = new Gson();
    private final CloseableHttpClient httpClient = HttpClients.createDefault(); //TODO make timeout values configurable
    private final String uri;
    private final ScheduledExecutorService sendingExecutorService;
    private final long maxWaittimeInMs = 1000;
    private final TransmittingService transmittingService;

    public TransmittingDataSink(String uri) {
        this.uri = uri;
        transmittingService = new TransmittingService();

        sendingExecutorService = Executors.newSingleThreadScheduledExecutor();
        sendingExecutorService.scheduleAtFixedRate((Runnable) () -> {
            try {
                transmittingService.transmit(toTransmit, uri, gson, httpClient);
            } catch (IOException e) {
                //TODO TKT
                e.printStackTrace();
            }
        }, 0, maxWaittimeInMs, TimeUnit.MILLISECONDS);

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
        //TODO perform transmittion
        //TODO remove?
    }

    private synchronized void storeDataForTransmittion(AHierarchicalDataRoot data) {
        //TODO TKT check for concurrency
        toTransmit.add(data);
    }


}
