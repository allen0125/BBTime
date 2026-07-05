package com.bbtime.net;

/**
 * A source of authoritative network time.
 */
public interface TimeSource {

    /**
     * Queries the remote server and returns the authoritative UTC time together
     * with the estimated offset from the local clock.
     *
     * @return a populated {@link TimeResult}
     * @throws Exception on any network or protocol error
     */
    TimeResult query() throws Exception;

    /** Short human-readable protocol name, e.g. "NTP". */
    String name();
}
