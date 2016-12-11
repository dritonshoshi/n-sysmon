package com.nsysmon.datasink.transfer.types;

import java.util.List;

public class TransferMeasurementsRequest {
    private final List<InnerTransferMeasurementsRequest> entries;

    public TransferMeasurementsRequest(List<InnerTransferMeasurementsRequest> entries) {
        this.entries=entries;
    }

    public List<InnerTransferMeasurementsRequest> getEntries() {
        return entries;
    }
}
