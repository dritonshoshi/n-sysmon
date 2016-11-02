package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.ajjpj.afoundation.io.AFile;
import com.nsysmon.NSysMon;
import com.nsysmon.data.AScalarDataPoint;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * @author arno
 */
public class AProcNetDevMeasurer implements AScalarMeasurer {
    private static final AFile PROC_NET_DEV = new AFile("/proc/net/dev", Charset.defaultCharset());

    private static final String KEY_PREFIX = "net:";
    private static final String KEY_MEMENTO = KEY_PREFIX;
    private static final String KEY_SUFFIX_RECEIVED_BYTES = ":received-bytes";
    private static final String KEY_SUFFIX_RECEIVED_PACKETS = ":received-pkt";
    private static final String KEY_SUFFIX_SENT_BYTES = ":sent-bytes";
    private static final String KEY_SUFFIX_SENT_PACKETS = ":sent-pkt";
    private static final String KEY_SUFFIX_COLLISIONS = ":collisions";
    private static final Pattern PATTERN_1 = Pattern.compile("\\s+");

    @Override public void prepareMeasurements(Map<String, Object> mementos) throws Exception {
        //this measurement isn't working on windows
        if (NSysMon.isWindows()){
            return;
        }
        mementos.put(KEY_MEMENTO, createSnapshot());
    }

    @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) throws Exception {
        //this measurement isn't working on windows
        if (NSysMon.isWindows()){
            return;
        }
        final Snapshot prev = (Snapshot) mementos.get(KEY_MEMENTO);
        final Snapshot cur = createSnapshot();

        final long diffTime = cur.timestamp - prev.timestamp;

        for(String iface: new TreeSet<>(cur.packetsReceived.keySet())) {
            final long receivedBytes   = cur.bytesReceived  .get(iface) - prev.bytesReceived.  get(iface);
            final long receivedPackets = cur.packetsReceived.get(iface) - prev.packetsReceived.get(iface);
            final long sentBytes       = cur.bytesSent.      get(iface) - prev.bytesSent.      get(iface);
            final long sentPackets     = cur.packetsSent.    get(iface) - prev.packetsSent.    get(iface);
            final long collisions      = cur.collisions.     get(iface) - prev.collisions.     get(iface);

            {
                final String key = getKeyReceivedBytes(iface);
                data.put(key, new AScalarDataPoint(timestamp, key, receivedBytes * 10*1000 / diffTime, 1));
            }
            {
                final String key = getKeyReceivedPackets(iface);
                data.put(key, new AScalarDataPoint(timestamp, key, receivedPackets * 10*1000 / diffTime, 1));
            }
            {
                final String key = getKeySentBytes(iface);
                data.put(key, new AScalarDataPoint(timestamp, key, sentBytes * 10*1000 / diffTime, 1));
            }
            {
                final String key = getKeySentPackets(iface);
                data.put(key, new AScalarDataPoint(timestamp, key, sentPackets * 10*1000 / diffTime, 1));
            }
            {
                final String key = getKeyCollisions(iface);
                data.put(key, new AScalarDataPoint(timestamp, key, collisions * 10*1000 / diffTime, 1));
            }
        }
    }

    private static String getKeyReceivedBytes(String iface) {
        return KEY_PREFIX + iface + KEY_SUFFIX_RECEIVED_BYTES;
    }
    private static String getKeyReceivedPackets(String iface) {
        return KEY_PREFIX + iface + KEY_SUFFIX_RECEIVED_PACKETS;
    }

    private static String getKeySentBytes(String iface) {
        return KEY_PREFIX + iface + KEY_SUFFIX_SENT_BYTES;
    }
    private static String getKeySentPackets(String iface) {
        return KEY_PREFIX + iface + KEY_SUFFIX_SENT_PACKETS;
    }

    private static String getKeyCollisions(String iface) {
        return KEY_PREFIX + iface + KEY_SUFFIX_COLLISIONS;
    }

    private static Snapshot createSnapshot() throws IOException {
        return createSnapshot(PROC_NET_DEV.lines());
    }

    static Snapshot createSnapshot(Iterable<String> source) {
        final Snapshot result = new Snapshot();

        for(String line: source) {
            final String[] ifaceSplit = line.split(":");
            if(ifaceSplit.length != 2) {
                continue;
            }
            final String iface = ifaceSplit[0].trim();

            final String[] split = PATTERN_1.split(ifaceSplit[1].trim());
            if(split.length < 14) {
                continue;
            }

            final long bytesReceived   = Long.valueOf(split[0]);
            final long packetsReceived = Long.valueOf(split[1]);
            final long bytesSent       = Long.valueOf(split[8]);
            final long packetsSent     = Long.valueOf(split[9]);
            final long collisions      = Long.valueOf(split[13]);

            result.add(iface, bytesReceived, packetsReceived, bytesSent, packetsSent, collisions);
        }

        return result;
    }

    @Override public void shutdown() throws Exception {
    }

    static class Snapshot {
        final long timestamp = System.currentTimeMillis();
        final Map<String, Long> bytesReceived = new HashMap<>();
        final Map<String, Long> packetsReceived = new HashMap<>();
        final Map<String, Long> bytesSent = new HashMap<>();
        final Map<String, Long> packetsSent = new HashMap<>();
        final Map<String, Long> collisions = new HashMap<>();

        void add(String iface, long bytesReceived, long packetsReceived, long bytesSent, long packetsSent, long collisons) {
            this.bytesReceived.  put(iface, bytesReceived);
            this.packetsReceived.put(iface, packetsReceived);
            this.bytesSent.      put(iface, bytesSent);
            this.packetsSent.    put(iface, packetsSent);
            this.collisions.     put(iface, collisons);
        }
    }

    @Override public AOption<Long> getTimeoutInMilliSeconds() {
        return AOption.none();
    }
}
