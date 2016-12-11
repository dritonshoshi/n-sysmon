package com.nsysmon.central.allMeasurements;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.FindIterable;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.util.JSON;
import com.nsysmon.central.RequestFilterData;
import com.nsysmon.central.common.NSysmonRequestProcessor;
import com.nsysmon.datasink.transfer.types.db.HierarchicalDataForStorage;
import org.bson.Document;
import org.bson.conversions.Bson;
import spark.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by torsten on 11.12.2016.
 */
public class AllMeasurements extends NSysmonRequestProcessor<AllMeasurementsResponse> {

    public AllMeasurements(Gson gson, MongoCollection collectionMeasurements, MongoCollection collectionRoots) {
        super(gson, collectionMeasurements, collectionRoots);
    }

    @Override
    public AllMeasurementsResponse process(Request request) {
        AllMeasurementsRequest parsedRequest = gson.fromJson(request.body(), AllMeasurementsRequest.class);
        AllMeasurementsResponse response = new AllMeasurementsResponse();

        CountDownLatch marker = new CountDownLatch(1);

        getAllMatchingMeasurements(parsedRequest == null ? null : parsedRequest.getRequestFilterData(), marker, response);

        waitForLatch(marker);

        return response;
    }

    private Bson buildFilterForRequest(RequestFilterData requestFilterData) {
        Bson filter = new BasicDBObject("level", 0);
        return filter;
    }

    private void getAllMatchingMeasurements(RequestFilterData requestFilterData, CountDownLatch marker, AllMeasurementsResponse response) {
        //totals
        List<HierarchicalDataForStorage> entries = new ArrayList<>();
        Bson filter = buildFilterForRequest(requestFilterData);

        FindIterable findIterable = collectionMeasurements.find(filter);
        findIterable.forEach(new Block<Document>() {
            @Override
            public void apply(Document data) {
                //TODO add this to response data.remove("_id");
                HierarchicalDataForStorage fromJson = gson.fromJson(JSON.serialize(data), HierarchicalDataForStorage.class);
                entries.add(fromJson);
//                System.out.println();
            }
        }, new SingleResultCallback<Void>() {
            @Override
            public void onResult(Void result, Throwable t) {
                response.setEntries(entries);
                marker.countDown();
                if (t != null){
                    t.printStackTrace(System.err);
                }
            }
        });
    }
}
