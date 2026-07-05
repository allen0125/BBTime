package com.bbtime.bg;

import java.util.Timer;
import java.util.TimerTask;

import com.bbtime.core.SyncStore;
import com.bbtime.core.TimeSyncManager;
import net.rim.device.api.system.Application;

/**
 * Headless background application started at device boot via the app's
 * "autostart" alternate entry point. It runs a time check shortly after boot
 * and then once every 24 hours: if the last successful sync is older than a day
 * and auto-sync is enabled, it re-syncs using the persisted protocol/server.
 *
 * <p>It runs as a plain {@link Application} (no UI). Failures are swallowed so a
 * transient network error can never kill the daemon.</p>
 */
public final class DailyCheckDaemon extends Application {

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long INITIAL_DELAY_MS = 60L * 1000L;

    private final Timer timer = new Timer();

    private DailyCheckDaemon() {
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                runCheck();
            }
        }, INITIAL_DELAY_MS, DAY_MS);
    }

    /** Constructs the daemon and enters its event loop (call from main()). */
    public static void startResident() {
        DailyCheckDaemon daemon = new DailyCheckDaemon();
        daemon.enterEventDispatcher();
    }

    private void runCheck() {
        try {
            SyncStore store = new SyncStore();
            if (!store.isAutoEnabled()) {
                return;
            }
            long last = store.getLastSyncMillis();
            long now = System.currentTimeMillis();
            if (last > 0 && (now - last) < DAY_MS) {
                return; // already synced within the last day
            }

            int proto = store.getProtocol();
            String host = store.getHost();
            if (host == null || host.length() == 0) {
                host = TimeSyncManager.defaultHost(proto);
            }
            String params = TimeSyncManager.transportParams(store.getTransport());

            TimeSyncManager manager = new TimeSyncManager(store);
            manager.sync(proto, host, params); // result persisted inside sync()
        } catch (Throwable ignore) {
            // Never let the daemon die from a transient failure.
        }
    }
}
