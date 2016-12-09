package com.nsysmon.datasink.transfer.types;

public class TransferMeasurementRequest {
    private String dataString;
    public TransferMeasurementRequest(String dataString) {
        this.dataString = dataString;
    }

    public String getDataString() {
        return dataString;
    }
}
