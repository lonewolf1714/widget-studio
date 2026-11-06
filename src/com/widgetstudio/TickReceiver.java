package com.widgetstudio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Periodic refresh (alarm), boot, time and timezone changes. */
public class TickReceiver extends BroadcastReceiver {
    @Override public void onReceive(final Context c, Intent intent) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                Weather.refreshIfStale(c);
                Updater.updateAll(c);
            } finally {
                pr.finish();
            }
        }).start();
    }
}
