package com.nsysmon.server.upload.preprocess.impl;

import com.nsysmon.server.data.InstanceIdentifier;
import com.nsysmon.server.upload.preprocess.SystemClockCorrector;

/**
 * @author arno
 */
public class SystemClockCorrectorNullImpl implements SystemClockCorrector {
    @Override public void updateSystemClockDiff(InstanceIdentifier instance, long senderTimestamp) {
    }

    @Override public long correctedTimestamp(InstanceIdentifier instance, long rawTimestamp) {
        return rawTimestamp;
    }
}
