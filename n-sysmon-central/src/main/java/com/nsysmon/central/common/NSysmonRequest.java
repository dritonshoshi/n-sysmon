package com.nsysmon.central.common;

import com.nsysmon.central.RequestFilterData;

/**
 * Created by torsten on 11.12.2016.
 */
public class NSysmonRequest {
    protected RequestFilterData requestFilterData = new RequestFilterData();

    public RequestFilterData getRequestFilterData() {
        return requestFilterData;
    }

    public void setRequestFilterData(RequestFilterData requestFilterData) {
        this.requestFilterData = requestFilterData;
    }
}
