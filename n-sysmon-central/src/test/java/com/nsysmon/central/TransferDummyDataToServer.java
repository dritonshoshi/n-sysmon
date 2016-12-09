package com.nsysmon.central;

import com.google.gson.Gson;
import com.nsysmon.datasink.transfer.types.TransferMeasurementRequest;
import com.nsysmon.datasink.transfer.types.TransferMeasurementResponse;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;


public class TransferDummyDataToServer {

    // http://localhost:4567/jsonEcho
    public static void main(String[] args) {
        Gson gson = new Gson();

        for (int i = 0; i < 5; i++) {
            String dataString = "{name: 'Tester " + i + "'}";
            sendData(gson, dataString);
        }
    }

    private static void sendData(final Gson gson, final String dataString) {
        try {
            Content content = Request.Post("http://localhost:4567/transferMeasurement")
                    .body(new StringEntity(gson.toJson(new TransferMeasurementRequest(dataString))))
                    .execute()
                    .returnContent();
            TransferMeasurementResponse response = gson.fromJson(content.asString(), TransferMeasurementResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}