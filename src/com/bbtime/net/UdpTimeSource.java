package com.bbtime.net;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

/**
 * RFC 868 TIME client over UDP/37. The server replies with a single 32-bit
 * big-endian value: seconds since 1900-01-01 00:00:00 UTC. Resolution is one
 * second, corrected here by half the measured round-trip.
 */
public final class UdpTimeSource implements TimeSource {

    /** Seconds between 1900-01-01 and 1970-01-01. */
    private static final long RFC868_UNIX_DELTA_SECONDS = 2208988800L;
    private static final int TIME_PORT = 37;
    private static final int TIMEOUT_MS = 8000;

    private final String host;
    private final String params;

    public UdpTimeSource(String host, String connectionParams) {
        this.host = host;
        this.params = (connectionParams == null) ? "" : connectionParams;
    }

    public String name() {
        return "UDP-TIME";
    }

    public TimeResult query() throws IOException {
        DatagramConnection conn = null;
        Watchdog dog = new Watchdog();
        try {
            String url = "datagram://" + host + ":" + TIME_PORT + params;
            conn = (DatagramConnection) Connector.open(url, Connector.READ_WRITE, true);
            dog.arm(conn, TIMEOUT_MS);

            // Any datagram sent to the port triggers the reply.
            byte[] probe = new byte[1];
            Datagram request = conn.newDatagram(probe, probe.length);
            long t1 = System.currentTimeMillis();
            conn.send(request);

            byte[] rbuf = new byte[4];
            Datagram response = conn.newDatagram(rbuf, rbuf.length);
            conn.receive(response);
            long t4 = System.currentTimeMillis();

            if (dog.fired()) {
                throw new IOException("UDP TIME request timed out");
            }
            if (response.getLength() < 4) {
                throw new IOException("Short TIME reply (" + response.getLength() + " bytes)");
            }

            byte[] data = response.getData();
            long seconds = readUInt32(data, 0);
            long serverUtc = (seconds - RFC868_UNIX_DELTA_SECONDS) * 1000L;

            long roundTrip = t4 - t1;
            // Header carries whole seconds; centre it and add half the RTT.
            long utc = serverUtc + 500L + roundTrip / 2L;
            long localNow = System.currentTimeMillis();
            return new TimeResult(utc, localNow, roundTrip, name());
        } finally {
            dog.disarm();
            if (conn != null) {
                try {
                    conn.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static long readUInt32(byte[] data, int index) {
        return ((long) (data[index] & 0xFF) << 24)
                | ((long) (data[index + 1] & 0xFF) << 16)
                | ((long) (data[index + 2] & 0xFF) << 8)
                | ((long) (data[index + 3] & 0xFF));
    }
}
