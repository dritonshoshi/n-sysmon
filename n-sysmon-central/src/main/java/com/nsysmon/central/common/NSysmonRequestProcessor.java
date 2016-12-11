package com.nsysmon.central.common;

import com.google.gson.Gson;
import com.mongodb.async.client.MongoCollection;
import spark.Request;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by torsten on 11.12.2016.
 */
public abstract class NSysmonRequestProcessor<T> {
    protected final Gson gson;
    protected final MongoCollection collectionMeasurements;
    protected final MongoCollection collectionRoots;

    public NSysmonRequestProcessor(Gson gson, MongoCollection collectionMeasurements, MongoCollection collectionRoots) {
        this.gson = gson;
        this.collectionMeasurements = collectionMeasurements;
        this.collectionRoots = collectionRoots;
    }

    public abstract T process(Request request);

    protected void waitForLatch(CountDownLatch marker) {
        try {
            marker.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
