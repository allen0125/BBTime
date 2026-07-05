package com.bbtime.net;

/**
 * Immutable result of a single time query. All times are UTC milliseconds since
 * the Unix epoch.
 */
public final class TimeResult {

    /** Authoritative UTC time, already compensated for round-trip where possible. */
    public final long utcMillis;

    /** Local clock reading captured as close as possible to {@link #utcMillis}. */
    public final long localMillis;

    /** Estimated round-trip delay in ms, or -1 if unknown. */
    public final long roundTripMillis;

    /** Name of the protocol that produced this result. */
    public final String source;

    public TimeResult(long utcMillis, long localMillis, long roundTripMillis, String source) {
        this.utcMillis = utcMillis;
        this.localMillis = localMillis;
        this.roundTripMillis = roundTripMillis;
        this.source = source;
    }

    /**
     * Offset = authoritative - local. A positive value means the device clock is
     * running slow (behind real time); negative means it is fast.
     */
    public long offsetMillis() {
        return utcMillis - localMillis;
    }
}
