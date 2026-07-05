package com.bbtime.core;

import com.bbtime.net.TimeResult;

/**
 * Outcome of a single sync attempt. Never carries an exception out to callers;
 * failures are reported via {@link #ok} plus {@link #error}.
 */
public final class SyncOutcome {

    public final boolean ok;
    /** Populated only when {@link #ok}. */
    public final TimeResult result;
    /** One of the {@code ClockSetter.*} constants. */
    public final int applied;
    /** Populated only when not {@link #ok}. */
    public final String error;

    public SyncOutcome(boolean ok, TimeResult result, int applied, String error) {
        this.ok = ok;
        this.result = result;
        this.applied = applied;
        this.error = error;
    }
}
