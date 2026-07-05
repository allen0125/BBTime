package com.bbtime.core;

/**
 * Applies an authoritative time to the device clock.
 *
 * <p><b>Platform reality:</b> stock BlackBerry OS (4.5 through 7.x) provides no
 * public API for a third-party application to set the system clock. The device
 * normally sources time from the carrier network (NITZ). Consequently
 * {@link #apply(long)} returns {@link #UNSUPPORTED} on a stock build, and the
 * caller is expected to surface the exact correct time and offset so the user
 * can adjust the clock from Options &gt; Date/Time.</p>
 *
 * <p>This class is the single integration seam: a signed or otherwise
 * privileged build that has a native time-set capability can implement it here
 * and return {@link #OK}. CLDC has no reflection, so any such call must be a
 * compile-time reference resolved against that build's libraries.</p>
 */
public final class ClockSetter {

    public static final int OK = 0;
    public static final int UNSUPPORTED = 1;
    public static final int FAILED = 2;

    private ClockSetter() {
    }

    /**
     * Attempts to set the device clock to {@code utcMillis} (Unix ms).
     *
     * @return {@link #OK}, {@link #UNSUPPORTED}, or {@link #FAILED}
     */
    public static int apply(long utcMillis) {
        // No public setter on stock BlackBerry OS. Do not pretend otherwise.
        return UNSUPPORTED;
    }

    /** Whether {@code result} represents the clock actually being changed. */
    public static boolean applied(int result) {
        return result == OK;
    }

    public static String describe(int result) {
        switch (result) {
            case OK:
                return "device clock updated";
            case FAILED:
                return "clock update failed";
            case UNSUPPORTED:
            default:
                return "set manually (OS blocks auto-set)";
        }
    }
}
