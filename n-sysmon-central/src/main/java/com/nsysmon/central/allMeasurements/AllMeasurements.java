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
import org.bson.types.ObjectId;
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
                String tmp1 = data.get("idRoot").toString();;
                String tmp2 = data.get("_id").toString();
                data.remove("_id");
                data.remove("idRoot");
                HierarchicalDataForStorage fromJson = gson.fromJson(JSON.serialize(data), HierarchicalDataForStorage.class);
                fromJson.setIdRoot(tmp1);
                fromJson.set_id(tmp2);
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

    public AllMeasurementsDirectChildrenResponse getDirectChildrenForMeasurement(Request request) {
        AllMeasurementsDirectChildrenRequest parsedRequest = gson.fromJson(request.body(), AllMeasurementsDirectChildrenRequest.class);
        AllMeasurementsDirectChildrenResponse response = new AllMeasurementsDirectChildrenResponse();

        CountDownLatch marker = new CountDownLatch(1);

        getChildren(parsedRequest, marker, response);

        waitForLatch(marker);

        return response;
    }

    private void getChildren(AllMeasurementsDirectChildrenRequest parsedRequest, CountDownLatch marker, AllMeasurementsDirectChildrenResponse response) {
        Bson filter = new BasicDBObject("parentIdentifier", parsedRequest.localIdentifier)
                .append("idRoot", new ObjectId(parsedRequest.idRoot));
//        Bson filter = new BasicDBObject("idRoot", new ObjectId(parsedRequest.idRoot))
//                .append("parentIdentifier", parsedRequest.localIdentifier);
        //TODO are we missing a dataentry to the direct parent? .append("id", parsedRequest.idMeasurement);
        List<HierarchicalDataForStorage> entries = new ArrayList<>();

        FindIterable findIterable = collectionMeasurements.find(filter);
        findIterable.forEach(new Block<Document>() {
            @Override
            public void apply(Document data) {
                //TODO add this to response: data.remove("_id");
                String tmp1 = data.get("idRoot").toString();
                String tmp2 = data.get("_id").toString();
                data.remove("_id");
                data.remove("idRoot");
                HierarchicalDataForStorage fromJson = gson.fromJson(JSON.serialize(data), HierarchicalDataForStorage.class);
                fromJson.setIdRoot(tmp1);
                fromJson.set_id(tmp2);
                entries.add(fromJson);
            }
        }, new SingleResultCallback<Void>() {
            @Override
            public void onResult(Void result, Throwable t) {
                response.getChildren().addAll(entries);
                marker.countDown();
                if (t != null){
                    t.printStackTrace(System.err);
                }
            }
        });
    }
}
