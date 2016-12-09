package com.nsysmon.central;

import com.google.gson.Gson;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.nsysmon.central.queries.Overview;
import com.nsysmon.datasink.transfer.types.TransferMeasurementRequest;
import com.nsysmon.datasink.transfer.types.TransferMeasurementResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

//TODO https://www.mkyong.com/mongodb/java-mongodb-convert-json-data-to-dbobject/
public class HelloServer {
    private static Logger LOG = LogManager.getLogger(HelloServer.class.getName());
    private DBCollection collection;

    private void init() throws Exception {

        //FIXME currently no MongoCredential credential = MongoCredential.createCredential("userName", "n-sysmon", "password".toCharArray());
        //FIXME currently no authentification! MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", 27017), Arrays.asList(credential));
        //TODO make server and port configurable
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        DB db = mongoClient.getDB("nsysmon");
        collection = db.getCollection("measurements");
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
        get("/getDataOverview", (request, response) -> new Overview(gson, collection).process(request), gson::toJson);

    }

    private Object processRequestForJsonCalc(Request request) {
        Gson gson = new Gson();
        TransferMeasurementRequest transferRequest = gson.fromJson(request.body(), TransferMeasurementRequest.class);

        System.out.println(transferRequest.getDataString().substring(0, Math.min(50, transferRequest.getDataString().length()-1)));

        DBObject doc = (DBObject) JSON.parse(transferRequest.getDataString());
        collection.insert(doc);

        System.out.println("Entries in db: " + collection.find().count());

        return new TransferMeasurementResponse();
    }

}