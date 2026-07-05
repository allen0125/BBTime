package com.bbtime.ui;

import com.bbtime.core.ClockSetter;
import com.bbtime.core.SyncOutcome;
import com.bbtime.core.SyncStore;
import com.bbtime.core.TimeFmt;
import com.bbtime.core.TimeSyncManager;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.CheckboxField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.MainScreen;

/**
 * Main application screen.
 *
 * <ul>
 *   <li>Shows a live device clock (local + UTC).</li>
 *   <li>Protocol / server / transport / auto-sync settings.</li>
 *   <li>Press <b>S</b> (or the menu) to sync now.</li>
 * </ul>
 *
 * Network I/O runs on a worker thread; UI mutation is marshalled back with
 * {@code invokeLater}.
 */
public final class TimeSyncScreen extends MainScreen implements FieldChangeListener {

    private final SyncStore store = new SyncStore();
    private final TimeSyncManager manager = new TimeSyncManager(store);

    private final LabelField clockLabel =
        new LabelField("", Field.NON_FOCUSABLE);
    private final ObjectChoiceField protocolChoice =
        new ObjectChoiceField("Protocol: ", TimeSyncManager.protocolNames());
    private final BasicEditField hostField =
        new BasicEditField("Server: ", "", 200, BasicEditField.NO_NEWLINE);
    private final ObjectChoiceField transportChoice =
        new ObjectChoiceField("Transport: ", TimeSyncManager.transportNames());
    private final CheckboxField autoCheck =
        new CheckboxField("Daily auto-sync in background", true);
    private final RichTextField statusField =
        new RichTextField("", Field.NON_FOCUSABLE);

    private boolean syncing = false;
    private int tickerId = -1;

    private final MenuItem syncMenu = new MenuItem("Sync now", 100, 10) {
        public void run() {
            triggerSync();
        }
    };
    private final MenuItem saveMenu = new MenuItem("Save settings", 110, 20) {
        public void run() {
            saveSettings();
            setStatus("Settings saved.");
        }
    };
    private final MenuItem aboutMenu = new MenuItem("About", 120, 30) {
        public void run() {
            Dialog.inform("bbtime 1.0.0\nNetwork time sync for BlackBerry OS 4.5+\n"
                    + "NTP / HTTP / HTTPS / UDP-TIME\nPress 'S' to sync.");
        }
    };

    public TimeSyncScreen() {
        setTitle("bbtime — Network Time Sync");

        int proto = clamp(store.getProtocol(), 0, TimeSyncManager.protocolNames().length - 1);
        protocolChoice.setSelectedIndex(proto);

        String host = store.getHost();
        if (host == null || host.length() == 0) {
            host = TimeSyncManager.defaultHost(proto);
        }
        hostField.setText(host);

        transportChoice.setSelectedIndex(
            clamp(store.getTransport(), 0, TimeSyncManager.transportNames().length - 1));
        autoCheck.setChecked(store.isAutoEnabled());

        protocolChoice.setChangeListener(this);
        transportChoice.setChangeListener(this);
        autoCheck.setChangeListener(this);

        add(clockLabel);
        add(new SeparatorField());
        add(protocolChoice);
        add(hostField);
        add(transportChoice);
        add(autoCheck);
        add(new SeparatorField());
        add(new LabelField("Press 'S' to sync now", Field.NON_FOCUSABLE));
        add(new SeparatorField());
        add(statusField);

        addMenuItem(syncMenu);
        addMenuItem(saveMenu);
        addMenuItem(aboutMenu);

        renderLastSync();
    }

    // --- lifecycle: run the live-clock ticker only while attached ---------

    protected void onUiEngineAttached(boolean attached) {
        super.onUiEngineAttached(attached);
        if (attached) {
            updateClock();
            tickerId = UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    updateClock();
                }
            }, 1000, true);
        } else if (tickerId != -1) {
            UiApplication.getUiApplication().cancelInvokeLater(tickerId);
            tickerId = -1;
        }
    }

    // --- input ------------------------------------------------------------

    protected boolean keyChar(char c, int status, int time) {
        // Let the server field receive real text (including the letter 's').
        if (getLeafFieldWithFocus() != hostField && (c == 's' || c == 'S')) {
            triggerSync();
            return true;
        }
        return super.keyChar(c, status, time);
    }

    public void fieldChanged(Field field, int context) {
        if (context == Field.PROGRAMMATIC) {
            return;
        }
        if (field == protocolChoice) {
            // Switching protocol implies a different server; reset to its default.
            int proto = protocolChoice.getSelectedIndex();
            hostField.setText(TimeSyncManager.defaultHost(proto));
        }
        saveSettings();
    }

    // --- sync -------------------------------------------------------------

    private void triggerSync() {
        if (syncing) {
            return;
        }
        syncing = true;
        saveSettings();

        final int proto = protocolChoice.getSelectedIndex();
        final String host = hostField.getText().trim();
        final String params = TimeSyncManager.transportParams(transportChoice.getSelectedIndex());

        setStatus("Syncing via " + TimeSyncManager.protocolName(proto) + " …");

        new Thread() {
            public void run() {
                final SyncOutcome outcome = manager.sync(proto, host, params);
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        syncing = false;
                        showOutcome(outcome);
                    }
                });
            }
        }.start();
    }

    // --- rendering --------------------------------------------------------

    private void updateClock() {
        long now = System.currentTimeMillis();
        clockLabel.setText("Device: " + TimeFmt.formatLocal(now)
                + "\n        " + TimeFmt.formatUtc(now));
    }

    private void showOutcome(SyncOutcome outcome) {
        if (!outcome.ok) {
            setStatus("Sync failed: " + outcome.error);
            return;
        }
        long offset = outcome.result.offsetMillis();
        StringBuffer sb = new StringBuffer();
        sb.append("Synced via ").append(outcome.result.source);
        sb.append("\nCorrect time: ").append(TimeFmt.formatLocal(outcome.result.utcMillis));
        sb.append("\n").append(TimeFmt.formatUtc(outcome.result.utcMillis));
        sb.append("\nClock: ").append(TimeFmt.describeOffset(offset));
        if (outcome.result.roundTripMillis >= 0) {
            sb.append("\nRound-trip: ").append(outcome.result.roundTripMillis).append(" ms");
        }
        sb.append("\nApply: ").append(ClockSetter.describe(outcome.applied));
        setStatus(sb.toString());

        // The clock is set via Device.setDateTime (signed/controlled API). If it
        // could not be applied, explain the usual causes and give the exact time
        // so the user can set it by hand as a fallback.
        if (!ClockSetter.applied(outcome.applied)) {
            Dialog.inform("Could not set the clock automatically.\n\n"
                    + "Correct time:\n" + TimeFmt.formatLocal(outcome.result.utcMillis)
                    + "\n\nOn a real device this needs:\n"
                    + "1) a code-signed build,\n"
                    + "2) Options > Date/Time set to Manual (automatic time off),\n"
                    + "3) no IT policy blocking time changes.\n\n"
                    + "Otherwise set the time above manually.");
        }
    }

    private void renderLastSync() {
        long last = store.getLastSyncMillis();
        if (last <= 0) {
            setStatus("No sync yet. Press 'S' to sync now.");
            return;
        }
        StringBuffer sb = new StringBuffer();
        sb.append("Last sync: ").append(TimeFmt.formatLocal(last));
        sb.append("\nVia ").append(store.getLastSource());
        sb.append("\nClock: ").append(TimeFmt.describeOffset(store.getLastOffsetMillis()));
        sb.append("\nApply: ").append(ClockSetter.describe(store.getLastApplied()));
        setStatus(sb.toString());
    }

    private void setStatus(String text) {
        statusField.setText(text);
    }

    private void saveSettings() {
        store.setProtocol(protocolChoice.getSelectedIndex());
        store.setHost(hostField.getText().trim());
        store.setTransport(transportChoice.getSelectedIndex());
        store.setAutoEnabled(autoCheck.getChecked());
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }
}
