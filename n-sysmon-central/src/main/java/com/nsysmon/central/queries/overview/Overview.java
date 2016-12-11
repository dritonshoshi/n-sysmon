package com.nsysmon.central.queries.overview;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.nsysmon.central.common.NSysmonRequestProcessor;
import org.bson.conversions.Bson;
import spark.Request;

import java.util.concurrent.CountDownLatch;

public class Overview extends NSysmonRequestProcessor<OverviewResponse> {

    public Overview(Gson gson, MongoCollection collectionMeasurements, MongoCollection collectionRoots) {
        super(gson, collectionMeasurements, collectionRoots);
    }

    public OverviewResponse process(Request request) {
        OverviewRequest overviewRequest = gson.fromJson(request.body(), OverviewRequest.class);
        OverviewResponse response = new OverviewResponse();

        //adjust this latch to the number of queries done
        CountDownLatch marker = new CountDownLatch(2);

        countTotalNumbers(response, marker);
        countParentEntries(response, marker);

        waitForLatch(marker);

//        //details
//        DBCursor cursor;
//        cursor = collection.find();
//        Set<String> identifiers = new HashSet<>();
//        cursor.forEach((DBObject dbObject) -> {
//            identifiers.add("" + ((DBObject)dbObject.get("root")).get("identifier"));
//        });
////        System.out.println(identifiers);
//
//        for (String identifier : identifiers) {
//            DBObject whereQuery = QueryBuilder.start("root.identifier").is(identifier).get();
//            cursor = collection.find(whereQuery);
////            System.out.println(cursor.count());
//            response.getEntriesByType().put(identifier, (long) cursor.count());
//        }

        return response;
    }

    private void countTotalNumbers(OverviewResponse response, CountDownLatch marker) {
        //TODO move to serverstart/db init
//        collection.createIndex(new BasicDBObject("level", 1), new IndexOptions(), new SingleResultCallback<String>() {
//            @Override
//            public void onResult(String result, Throwable t) {
//                System.out.println(result);
//                System.out.println(t);
//            }
//        });
        //totals
        collectionMeasurements.count(new SingleResultCallback<Long>() {
            @Override
            public void onResult(Long result, Throwable t) {
                response.setTotalDataEntriesInDb(result);
                marker.countDown();
            }
        });
    }

    private void countParentEntries(OverviewResponse response, CountDownLatch marker) {
        //totals
        Bson filter = new BasicDBObject("level", 0);

        collectionMeasurements.count(filter, new SingleResultCallback<Long>() {
            @Override
            public void onResult(Long result, Throwable t) {
                response.setParentEntriesInDb(result);
                marker.countDown();
            }
        });
    }
}
