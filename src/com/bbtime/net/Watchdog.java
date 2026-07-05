package com.bbtime.net;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.io.Connection;

/**
 * Closes a connection after a timeout so a blocked datagram {@code receive()}
 * fails instead of hanging forever. BlackBerry's {@code DatagramConnection} has
 * no portable socket-timeout setter, so we force the issue by closing it.
 */
final class Watchdog {

    private final Timer timer = new Timer();
    private volatile boolean fired = false;

    void arm(final Connection conn, long timeoutMs) {
        timer.schedule(new TimerTask() {
            public void run() {
                fired = true;
                try {
                    conn.close();
                } catch (IOException ignore) {
                } catch (Throwable ignore) {
                }
            }
        }, timeoutMs);
    }

    boolean fired() {
        return fired;
    }

    void disarm() {
        timer.cancel();
    }
}
