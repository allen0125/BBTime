package com.bbtime.core;

import java.util.Hashtable;
import net.rim.device.api.system.PersistentObject;
import net.rim.device.api.system.PersistentStore;

/**
 * Persistent settings and last-sync state, backed by {@link PersistentStore}.
 * Values are held in a {@link Hashtable} of persistable primitives (String,
 * Long) so no custom {@code Persistable} class is required. All accessors are
 * synchronized because the UI and the background daemon may touch the store
 * concurrently.
 */
public final class SyncStore {

    // Unique 64-bit key for this app's persistent object.
    private static final long KEY = 0x8f3ac1e2b7d40591L;

    private static final String K_LAST_SYNC = "lastSync";
    private static final String K_LAST_OFFSET = "lastOffset";
    private static final String K_LAST_SOURCE = "lastSource";
    private static final String K_LAST_APPLIED = "lastApplied";
    private static final String K_PROTOCOL = "protocol";
    private static final String K_HOST = "host";
    private static final String K_TRANSPORT = "transport";
    private static final String K_AUTO = "auto";

    private final PersistentObject store;
    private final Hashtable data;

    public SyncStore() {
        store = PersistentStore.getPersistentObject(KEY);
        Object contents = store.getContents();
        if (contents instanceof Hashtable) {
            data = (Hashtable) contents;
        } else {
            data = new Hashtable();
            store.setContents(data);
            store.commit();
        }
    }

    public synchronized long getLastSyncMillis() {
        return getLong(K_LAST_SYNC, 0L);
    }

    public synchronized void setLastSyncMillis(long v) {
        put(K_LAST_SYNC, new Long(v));
    }

    public synchronized long getLastOffsetMillis() {
        return getLong(K_LAST_OFFSET, 0L);
    }

    public synchronized void setLastOffsetMillis(long v) {
        put(K_LAST_OFFSET, new Long(v));
    }

    public synchronized String getLastSource() {
        return getString(K_LAST_SOURCE, "-");
    }

    public synchronized void setLastSource(String v) {
        put(K_LAST_SOURCE, v);
    }

    public synchronized int getLastApplied() {
        return (int) getLong(K_LAST_APPLIED, (long) ClockSetter.UNSUPPORTED);
    }

    public synchronized void setLastApplied(int v) {
        put(K_LAST_APPLIED, new Long(v));
    }

    public synchronized int getProtocol() {
        return (int) getLong(K_PROTOCOL, 0L);
    }

    public synchronized void setProtocol(int v) {
        put(K_PROTOCOL, new Long(v));
    }

    public synchronized String getHost() {
        return getString(K_HOST, "");
    }

    public synchronized void setHost(String v) {
        put(K_HOST, v == null ? "" : v);
    }

    public synchronized int getTransport() {
        return (int) getLong(K_TRANSPORT, 0L);
    }

    public synchronized void setTransport(int v) {
        put(K_TRANSPORT, new Long(v));
    }

    public synchronized boolean isAutoEnabled() {
        return getLong(K_AUTO, 1L) != 0L;
    }

    public synchronized void setAutoEnabled(boolean v) {
        put(K_AUTO, new Long(v ? 1L : 0L));
    }

    private long getLong(String key, long def) {
        Object o = data.get(key);
        return (o instanceof Long) ? ((Long) o).longValue() : def;
    }

    private String getString(String key, String def) {
        Object o = data.get(key);
        return (o instanceof String) ? (String) o : def;
    }

    private void put(String key, Object value) {
        data.put(key, value);
        store.setContents(data);
        store.commit();
    }
}
