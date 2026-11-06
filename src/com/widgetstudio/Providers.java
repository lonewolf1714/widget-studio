package com.widgetstudio;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

/** One provider per widget type; all share the same logic. */
public final class Providers {
    public static class Base extends AppWidgetProvider {
        @Override public void onUpdate(final Context c, AppWidgetManager m, final int[] ids) {
            final PendingResult pr = goAsync();
            new Thread(() -> {
                try {
                    com.widgetstudio.Weather.refreshIfStale(c);
                    for (int id : ids) Updater.update(c, id);
                    Updater.schedule(c);
                } finally {
                    pr.finish();
                }
            }).start();
        }

        @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) {
            Updater.update(c, id);
        }

        @Override public void onDeleted(Context c, int[] ids) {
            for (int id : ids) Cfg.delete(c, id);
        }

        @Override public void onDisabled(Context c) {
            Updater.schedule(c);
        }
    }

    public static class Cal extends Base { }
    public static class Events extends Base { }
    public static class Storage extends Base { }
    public static class BigDate extends Base { }
    public static class Quote extends Base { }
    public static class Counter extends Base { }
    public static class Music extends Base { }
    public static class Game extends Base { }
    public static class Dots extends Base { }
    public static class Combo extends Base { }
    public static class Weather extends Base { }
    public static class Photo extends Base { }
    public static class Clock extends Base { }
    public static class Lucky extends Base { }
    public static class AppListW extends Base { }
    /** iPod Music for the Galaxy Flip cover screen (Samsung sub-display widget category). */
    public static class CoverMusic extends Base { }
}
