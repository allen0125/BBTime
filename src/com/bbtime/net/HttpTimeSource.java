package com.bbtime.net;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

/**
 * Reads authoritative time from an HTTP(S) {@code Date} response header. A HEAD
 * request keeps it cheap. Resolution is one second, corrected by half the
 * round-trip. The same class serves plain HTTP and HTTPS; on BlackBerry the
 * {@code https://} scheme yields an {@code HttpsConnection}, which is an
 * {@code HttpConnection}.
 */
public final class HttpTimeSource implements TimeSource {

    private final String baseUrl;
    private final String params;
    private final String label;

    /**
     * @param baseUrl full URL including scheme, e.g. {@code http://www.google.com/}
     * @param connectionParams BlackBerry transport suffix (may be empty)
     * @param secure whether this instance represents HTTPS (affects the label only)
     */
    public HttpTimeSource(String baseUrl, String connectionParams, boolean secure) {
        this.baseUrl = baseUrl;
        this.params = (connectionParams == null) ? "" : connectionParams;
        this.label = secure ? "HTTPS" : "HTTP";
    }

    public String name() {
        return label;
    }

    public TimeResult query() throws IOException {
        HttpConnection conn = null;
        try {
            long t1 = System.currentTimeMillis();
            conn = (HttpConnection) Connector.open(baseUrl + params, Connector.READ, true);
            conn.setRequestMethod(HttpConnection.HEAD);
            conn.setRequestProperty("Connection", "close");

            int rc = conn.getResponseCode(); // sends the request
            long t4 = System.currentTimeMillis();

            long serverDate = conn.getDate(); // parses Date header, 0 if absent
            if (serverDate <= 0) {
                serverDate = HttpDateParser.parse(conn.getHeaderField("Date"));
            }
            if (serverDate <= 0) {
                throw new IOException("No usable Date header (HTTP " + rc + ")");
            }

            long roundTrip = t4 - t1;
            // Header is whole-second; centre it, then add half the round-trip.
            long utc = serverDate + 500L + roundTrip / 2L;
            long localNow = System.currentTimeMillis();
            return new TimeResult(utc, localNow, roundTrip, label);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
