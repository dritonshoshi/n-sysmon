package com.nsysmon.datasink.transfer;

import com.google.gson.Gson;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.transfer.types.InnerTransferMeasurementsRequest;
import com.nsysmon.datasink.transfer.types.TransferMeasurementsRequest;
import com.nsysmon.datasink.transfer.types.db.HierarchicalDataForStorage;
import com.nsysmon.datasink.transfer.types.db.HierarchicalDataForStorageConverter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by torsten on 11.12.2016.
 */
public class TransmittingService {

    public void transmit(List<AHierarchicalDataRoot> toTransmit, String uri, Gson gson, CloseableHttpClient httpClient) throws IOException {
        if (toTransmit == null || toTransmit.size() == 0) {
            return;
        }
        Iterator<AHierarchicalDataRoot> iterator = toTransmit.iterator();
        List<InnerTransferMeasurementsRequest> dataToTransmitInThisTurn = new ArrayList<>(toTransmit.size());

        while (iterator.hasNext()) {
            AHierarchicalDataRoot data = iterator.next();
            iterator.remove();

            List<HierarchicalDataForStorage> rc = new ArrayList<>();
            HierarchicalDataForStorageConverter.fromChilds(data.getRootNode().getChildren(), rc, 1, "1", null);
            //TODO merge root into rootstorage
            rc.add(HierarchicalDataForStorageConverter.fromChild(data.getRootNode(), 0));
            dataToTransmitInThisTurn.add(new InnerTransferMeasurementsRequest(HierarchicalDataForStorageConverter.fromRoot(data), rc));

        }

        final HttpPost httpPost = new HttpPost(uri);

        HttpEntity entity = new StringEntity(gson.toJson(new TransferMeasurementsRequest(dataToTransmitInThisTurn)));
        httpPost.setEntity(entity);

        final CloseableHttpResponse response = httpClient.execute(httpPost);
        try {
            //nothing to do
        } finally {
            response.close();
        }


    }
}
