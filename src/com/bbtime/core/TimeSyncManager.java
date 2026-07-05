package com.bbtime.core;

import com.bbtime.net.HttpTimeSource;
import com.bbtime.net.NtpTimeSource;
import com.bbtime.net.TimeResult;
import com.bbtime.net.TimeSource;
import com.bbtime.net.UdpTimeSource;

/**
 * Coordinates a sync: build the selected protocol's source, query it, attempt
 * to apply the result to the device clock, and persist the outcome. Shared by
 * the UI and the background daemon.
 */
public final class TimeSyncManager {

    public static final int PROTO_NTP = 0;
    public static final int PROTO_HTTP = 1;
    public static final int PROTO_HTTPS = 2;
    public static final int PROTO_UDP = 3;

    /** BlackBerry connection-string transport suffixes, indexed by transport id. */
    private static final String[] TRANSPORT_PARAMS = {
        "",                  // 0 Auto (let the OS choose)
        ";deviceside=true",  // 1 Direct TCP/UDP
        ";interface=wifi",   // 2 Wi-Fi
        ";deviceside=false"  // 3 BIS/MDS
    };

    private static final String[] TRANSPORT_NAMES = {
        "Auto", "Direct TCP", "Wi-Fi", "BIS/MDS"
    };

    private static final String[] PROTOCOL_NAMES = {
        "NTP", "HTTP", "HTTPS", "UDP-TIME"
    };

    private final SyncStore store;

    public TimeSyncManager(SyncStore store) {
        this.store = store;
    }

    public static String[] protocolNames() {
        return PROTOCOL_NAMES;
    }

    public static String[] transportNames() {
        return TRANSPORT_NAMES;
    }

    public static String protocolName(int proto) {
        if (proto < 0 || proto >= PROTOCOL_NAMES.length) {
            return "?";
        }
        return PROTOCOL_NAMES[proto];
    }

    public static String transportParams(int transport) {
        if (transport < 0 || transport >= TRANSPORT_PARAMS.length) {
            return "";
        }
        return TRANSPORT_PARAMS[transport];
    }

    public static String defaultHost(int proto) {
        switch (proto) {
            case PROTO_NTP:
                return "pool.ntp.org";
            case PROTO_HTTP:
                return "http://www.google.com/";
            case PROTO_HTTPS:
                return "https://www.google.com/";
            case PROTO_UDP:
                return "time.nist.gov";
            default:
                return "";
        }
    }

    private static TimeSource buildSource(int proto, String host, String params) {
        switch (proto) {
            case PROTO_NTP:
                return new NtpTimeSource(host, params);
            case PROTO_HTTP:
                return new HttpTimeSource(host, params, false);
            case PROTO_HTTPS:
                return new HttpTimeSource(host, params, true);
            case PROTO_UDP:
                return new UdpTimeSource(host, params);
            default:
                throw new IllegalArgumentException("unknown protocol " + proto);
        }
    }

    /**
     * Runs a full sync. Never throws; all failures come back inside the
     * returned {@link SyncOutcome}.
     */
    public SyncOutcome sync(int proto, String host, String connectionParams) {
        try {
            TimeSource src = buildSource(proto, host, connectionParams);
            TimeResult r = src.query();
            int applied = ClockSetter.apply(r.utcMillis);

            store.setLastSyncMillis(System.currentTimeMillis());
            store.setLastOffsetMillis(r.offsetMillis());
            store.setLastSource(r.source);
            store.setLastApplied(applied);

            return new SyncOutcome(true, r, applied, null);
        } catch (Throwable t) {
            String msg = t.getMessage();
            if (msg == null || msg.length() == 0) {
                msg = t.toString();
            }
            return new SyncOutcome(false, null, ClockSetter.FAILED, msg);
        }
    }
}
