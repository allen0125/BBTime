package com.bbtime.net;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Minimal RFC 1123 HTTP {@code Date} header parser, e.g.
 * {@code "Sun, 05 Jul 2026 12:00:00 GMT"}. CLDC has no SimpleDateFormat, so the
 * parse is done by hand. Used only as a fallback when
 * {@code HttpConnection.getDate()} returns 0.
 */
final class HttpDateParser {

    private static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private HttpDateParser() {
    }

    /** Returns Unix ms, or 0 if the header cannot be parsed. */
    static long parse(String header) {
        if (header == null) {
            return 0L;
        }
        try {
            String s = header.trim();
            int comma = s.indexOf(',');
            if (comma >= 0) {
                s = s.substring(comma + 1).trim();
            }
            // Expect: "dd MMM yyyy HH:mm:ss GMT"
            String[] p = splitChar(s, ' ');
            if (p.length < 5) {
                return 0L;
            }
            int day = Integer.parseInt(p[0]);
            int month = monthIndex(p[1]);
            int year = Integer.parseInt(p[2]);
            if (month < 0) {
                return 0L;
            }

            String[] hms = splitChar(p[3], ':');
            if (hms.length < 3) {
                return 0L;
            }
            int hh = Integer.parseInt(hms[0]);
            int mm = Integer.parseInt(hms[1]);
            int ss = Integer.parseInt(hms[2]);

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DAY_OF_MONTH, day);
            c.set(Calendar.HOUR_OF_DAY, hh);
            c.set(Calendar.MINUTE, mm);
            c.set(Calendar.SECOND, ss);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTime().getTime();
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static int monthIndex(String mon) {
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].equalsIgnoreCase(mon)) {
                return i;
            }
        }
        return -1;
    }

    /** CLDC String has no split(); collapse runs of the separator. */
    private static String[] splitChar(String s, char sep) {
        java.util.Vector parts = new java.util.Vector();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == sep) {
                if (i > start) {
                    parts.addElement(s.substring(start, i));
                }
                start = i + 1;
            }
        }
        if (start < s.length()) {
            parts.addElement(s.substring(start));
        }
        String[] out = new String[parts.size()];
        parts.copyInto(out);
        return out;
    }
}
