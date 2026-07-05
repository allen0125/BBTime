package com.bbtime.core;

import net.rim.device.api.system.Device;

/**
 * Applies an authoritative time to the device clock via the RIM controlled API
 * {@link Device#setDateTime(long)}.
 *
 * <p>{@code Device.setDateTime(long)} sets the system date/time and is available
 * since BlackBerry API 3.6.0 (so on OS 4.5 and later). It is a <b>controlled
 * API</b>: it runs unsigned on the simulator, but on a real device the build
 * must be <b>code-signed</b> or the call throws {@code ControlledAccessException}.
 * It can also fail (return {@code false}) when the device is set to update time
 * automatically from the network, when an IT policy forbids changes, or when the
 * target is before 2002-01-01.</p>
 *
 * <p>The argument is UTC milliseconds since the Unix epoch — the raw value from
 * NTP/HTTP. Do NOT apply a timezone offset; the zone only affects display.</p>
 */
public final class ClockSetter {

    public static final int OK = 0;
    /** Stored default before any sync has been attempted. */
    public static final int UNSUPPORTED = 1;
    public static final int FAILED = 2;

    private ClockSetter() {
    }

    /**
     * Sets the device clock to {@code utcMillis} (UTC ms since the Unix epoch).
     *
     * @return {@link #OK} if the clock was set, otherwise {@link #FAILED}
     */
    public static int apply(long utcMillis) {
        try {
            return Device.setDateTime(utcMillis) ? OK : FAILED;
        } catch (Throwable t) {
            // ControlledAccessException (unsigned / no permission), IT-policy
            // denial, SecurityException, etc.
            return FAILED;
        }
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
                return "not set (needs signed build / automatic time off / IT policy)";
            case UNSUPPORTED:
            default:
                return "not attempted";
        }
    }
}
