package com.nsysmon.measure.scalar;


public class ScalarMeasurerStatus  {

    public enum Status {RUNNING, STOPPED, UNKNOWN};

    private Status status;

    public ScalarMeasurerStatus(Status status){
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
