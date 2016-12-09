package com.nsysmon.central.queries;

import com.google.gson.Gson;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import spark.Request;

import java.util.HashSet;
import java.util.Set;

public class Overview {
    private final Gson gson;
    private final DBCollection collection;

    public Overview(Gson gson, DBCollection collection) {
        this.gson = gson;
        this.collection = collection;

    }

    public OverviewResponse process(Request request) {
        OverviewRequest overviewRequest = gson.fromJson(request.body(), OverviewRequest.class);
        OverviewResponse response = new OverviewResponse();
        DBCursor cursor;

        //totals
        long nrObjects = collection.find().count();
        response.setTotalDataEntriesInDb(nrObjects);


        //details
        cursor = collection.find();
        Set<String> identifiers = new HashSet<>();
        cursor.forEach((DBObject dbObject) -> {
            identifiers.add("" + ((DBObject)dbObject.get("root")).get("identifier"));
        });
//        System.out.println(identifiers);

        for (String identifier : identifiers) {
            DBObject whereQuery = QueryBuilder.start("root.identifier").is(identifier).get();
            cursor = collection.find(whereQuery);
//            System.out.println(cursor.count());
            response.getEntriesByType().put(identifier, (long) cursor.count());
        }



        return response;
    }
}
