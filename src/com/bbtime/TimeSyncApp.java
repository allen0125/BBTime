package com.bbtime;

import com.bbtime.bg.DailyCheckDaemon;
import com.bbtime.ui.TimeSyncScreen;
import net.rim.device.api.ui.UiApplication;

/**
 * Application entry point.
 *
 * <p>Two run modes, distinguished by the first program argument (configured as
 * an alternate entry point in the project):</p>
 * <ul>
 *   <li>no args &rarr; foreground GUI ({@link TimeSyncScreen}).</li>
 *   <li>{@code "autostart"} &rarr; headless {@link DailyCheckDaemon} started at
 *       device boot; runs the once-a-day check and exits its own event loop
 *       only on shutdown.</li>
 * </ul>
 */
public final class TimeSyncApp extends UiApplication {

    private TimeSyncApp() {
        pushScreen(new TimeSyncScreen());
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0 && "autostart".equals(args[0])) {
            DailyCheckDaemon.startResident();
            return;
        }
        TimeSyncApp app = new TimeSyncApp();
        app.enterEventDispatcher();
    }
}
