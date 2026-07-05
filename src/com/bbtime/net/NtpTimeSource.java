package com.bbtime.net;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

/**
 * NTP client (RFC 5905, UDP/123). Uses the standard four-timestamp offset
 * calculation so the result is corrected for network delay.
 */
public final class NtpTimeSource implements TimeSource {

    /** Seconds between 1900-01-01 (NTP epoch) and 1970-01-01 (Unix epoch). */
    private static final long NTP_UNIX_DELTA_SECONDS = 2208988800L;
    private static final int NTP_PORT = 123;
    private static final int PACKET_SIZE = 48;
    private static final int TIMEOUT_MS = 8000;

    private final String host;
    private final String params;

    public NtpTimeSource(String host, String connectionParams) {
        this.host = host;
        this.params = (connectionParams == null) ? "" : connectionParams;
    }

    public String name() {
        return "NTP";
    }

    public TimeResult query() throws IOException {
        DatagramConnection conn = null;
        Watchdog dog = new Watchdog();
        try {
            String url = "datagram://" + host + ":" + NTP_PORT + params;
            conn = (DatagramConnection) Connector.open(url, Connector.READ_WRITE, true);
            dog.arm(conn, TIMEOUT_MS);

            byte[] buf = new byte[PACKET_SIZE];
            // LI = 0, VN = 3, Mode = 3 (client).
            buf[0] = (byte) 0x1B;
            Datagram request = conn.newDatagram(buf, buf.length);

            long t1 = System.currentTimeMillis();
            conn.send(request);

            byte[] rbuf = new byte[PACKET_SIZE];
            Datagram response = conn.newDatagram(rbuf, rbuf.length);
            conn.receive(response);
            long t4 = System.currentTimeMillis();

            if (dog.fired()) {
                throw new IOException("NTP request timed out");
            }
            if (response.getLength() < PACKET_SIZE) {
                throw new IOException("Short NTP reply (" + response.getLength() + " bytes)");
            }

            byte[] data = response.getData();
            long receiveTs = readTimestamp(data, 32);  // T2: server received request
            long transmitTs = readTimestamp(data, 40);  // T3: server sent reply

            // Clock offset per RFC 5905: ((T2 - T1) + (T3 - T4)) / 2.
            long offset = ((receiveTs - t1) + (transmitTs - t4)) / 2;
            long delay = (t4 - t1) - (transmitTs - receiveTs);
            if (delay < 0) {
                delay = t4 - t1;
            }

            long localNow = System.currentTimeMillis();
            long utc = localNow + offset;
            return new TimeResult(utc, localNow, delay, name());
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

    /** Reads a 64-bit NTP timestamp at {@code index} and returns Unix ms. */
    private static long readTimestamp(byte[] data, int index) {
        long seconds = readUInt32(data, index);
        long fraction = readUInt32(data, index + 4);
        return (seconds - NTP_UNIX_DELTA_SECONDS) * 1000L
                + ((fraction * 1000L) >>> 32);
    }

    /** Reads 4 bytes big-endian as an unsigned 32-bit value into a long. */
    private static long readUInt32(byte[] data, int index) {
        return ((long) (data[index] & 0xFF) << 24)
                | ((long) (data[index + 1] & 0xFF) << 16)
                | ((long) (data[index + 2] & 0xFF) << 8)
                | ((long) (data[index + 3] & 0xFF));
    }
}
