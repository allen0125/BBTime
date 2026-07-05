package com.bbtime.core;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Date/time formatting helpers built on {@link Calendar} (CLDC has no
 * SimpleDateFormat). Output format is {@code yyyy-MM-dd HH:mm:ss}.
 */
public final class TimeFmt {

    private TimeFmt() {
    }

    public static String format(long millis, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTime(new Date(millis));
        StringBuffer sb = new StringBuffer(19);
        sb.append(c.get(Calendar.YEAR)).append('-');
        two(sb, c.get(Calendar.MONTH) + 1).append('-');
        two(sb, c.get(Calendar.DAY_OF_MONTH)).append(' ');
        two(sb, c.get(Calendar.HOUR_OF_DAY)).append(':');
        two(sb, c.get(Calendar.MINUTE)).append(':');
        two(sb, c.get(Calendar.SECOND));
        return sb.toString();
    }

    /** Formats as UTC with a trailing " UTC". */
    public static String formatUtc(long millis) {
        return format(millis, TimeZone.getTimeZone("GMT")) + " UTC";
    }

    /** Formats in the device's local time zone. */
    public static String formatLocal(long millis) {
        return format(millis, TimeZone.getDefault());
    }

    /**
     * Human-readable signed offset, e.g. "device 1.4s slow" / "device 0.3s fast"
     * / "in sync". {@code offset} is authoritative-minus-local ms.
     */
    public static String describeOffset(long offsetMillis) {
        long abs = offsetMillis < 0 ? -offsetMillis : offsetMillis;
        if (abs < 1000) {
            return "in sync (" + offsetMillis + " ms)";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("device ");
        appendSeconds(sb, abs);
        sb.append(offsetMillis > 0 ? " slow" : " fast");
        return sb.toString();
    }

    private static void appendSeconds(StringBuffer sb, long millis) {
        long whole = millis / 1000L;
        long tenths = (millis % 1000L) / 100L;
        sb.append(whole).append('.').append(tenths).append('s');
    }

    private static StringBuffer two(StringBuffer sb, int v) {
        if (v < 10) {
            sb.append('0');
        }
        return sb.append(v);
    }
}
