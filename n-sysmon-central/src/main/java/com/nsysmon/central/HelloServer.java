package com.nsysmon.central;

import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.SocketSettings;
import com.nsysmon.central.allMeasurements.AllMeasurements;
import com.nsysmon.central.queries.overview.Overview;
import com.nsysmon.datasink.transfer.types.InnerTransferMeasurementsRequest;
import com.nsysmon.datasink.transfer.types.TransferMeasurementResponse;
import com.nsysmon.datasink.transfer.types.TransferMeasurementsRequest;
import com.nsysmon.datasink.transfer.types.db.HierarchicalDataForStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import spark.Request;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

//TODO https://www.mkyong.com/mongodb/java-mongodb-convert-json-data-to-dbobject/
public class HelloServer {
    private static Logger LOG = LogManager.getLogger(HelloServer.class.getName());
    private MongoCollection collectionMeasurements;
    private MongoCollection collectionRoots;

    private void init() throws Exception {

        //FIXME currently no MongoCredential credential = MongoCredential.createCredential("userName", "n-sysmon", "password".toCharArray());
        //FIXME currently no authentification! MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", 27017), Arrays.asList(credential));
        //TODO make server and port configurable
//        MongoClient mongoClient = MongoClients.codecRegistry(com.mongodb.MongoClient.getDefaultCodecRegistry()).create(new ConnectionString("mongodb://localhost:27017"));
        MongoClientSettings mongoClientSettings = MongoClientSettings
                .builder()
                .codecRegistry(com.mongodb.MongoClient.getDefaultCodecRegistry())
                .socketSettings(SocketSettings.builder().build())
                .clusterSettings(ClusterSettings.builder().applyConnectionString(new ConnectionString("mongodb://localhost:27017")).build())
                .build();
        MongoClient mongoClient = MongoClients.create(mongoClientSettings);

        MongoDatabase db = mongoClient.getDatabase("nsysmon");
        collectionMeasurements = db.getCollection("measurements");
        collectionRoots = db.getCollection("roots");
    }

    public static void main(String[] args) throws Exception {
        HelloServer server = new HelloServer();
        server.init();
        server.start();
    }

    private void start() {
        port(4567); // Spark will run on port 4567
        // Static files
        staticFileLocation("/webcontent");

        //Disable security for local development
        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));

        // REST
        Gson gson = new Gson();

        post("/transferMeasurement", (request, response) -> processRequestForJsonCalc(request), gson::toJson);
        get("/getDataOverview", (request, response) -> new Overview(gson, collectionMeasurements, collectionRoots).process(request), gson::toJson);
        get("/getAllMeasurements", (request, response) -> new AllMeasurements(gson, collectionMeasurements, collectionRoots).process(request), gson::toJson);
        post("/getDirectChildrenForMeasurement", (request, response) -> new AllMeasurements(gson, collectionMeasurements, collectionRoots).getDirectChildrenForMeasurement(request), gson::toJson);

    }

    @SuppressWarnings("unchecked")
    private Object processRequestForJsonCalc(Request request) {
        Gson gson = new Gson();
        TransferMeasurementsRequest transferRequests = gson.fromJson(request.body(), TransferMeasurementsRequest.class);

//        System.out.println(transferRequest.getDataString().substring(0, Math.min(50, transferRequest.getDataString().length()-1)));

         System.out.println("Start "+new Date());
        List<Document> children = new ArrayList<>();
        for (InnerTransferMeasurementsRequest transferRequest : transferRequests.getEntries()) {
            final Object idRoot = addRoot(gson, transferRequest);

            //add children
            for (HierarchicalDataForStorage hierarchicalDataForStorage : transferRequest.getChildren()) {
                Document docChild = Document.parse(gson.toJson(hierarchicalDataForStorage));
                docChild.put("idRoot", idRoot);
                //TODO docChild.put("idParent", idParent);
                children.add(docChild);
            }
        }
        collectionMeasurements.insertMany(children, (aVoid, throwable) -> {
            collectionMeasurements.count((aLong, throwable1) -> {
                 System.out.println("\tEntries in db: " + aLong);
                 System.out.println("End "+new Date());
            });
        });

        return new TransferMeasurementResponse();
    }

    @SuppressWarnings("unchecked")
    private Object addRoot(Gson gson, InnerTransferMeasurementsRequest transferRequest) {
        //add root-element
        Document doc = Document.parse(gson.toJson(transferRequest.getRoot()));
        CountDownLatch insertRootLatch = new CountDownLatch(1);
        final Object[] idParent = new Object[1];
        SingleResultCallback<Void> addChildrenCallback = new SingleResultCallback<Void>() {
            @Override
            public void onResult(Void aVoid, Throwable throwable) {
                insertRootLatch.countDown();
            }
        };
        collectionRoots.insertOne(doc, addChildrenCallback);
        idParent[0] = doc.get("_id");
        try {
            insertRootLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //TODO check rc
        return idParent[0];
    }

}