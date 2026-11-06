package com.widgetstudio;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Builds RemoteViews for widgets and schedules the next refresh. */
final class Updater {
    static final Class<?>[] PROVIDERS = {Providers.Cal.class, Providers.Events.class, Providers.Storage.class,
            Providers.BigDate.class, Providers.Quote.class, Providers.Counter.class, Providers.Music.class,
            Providers.Game.class, Providers.Dots.class,
            Providers.Combo.class, Providers.Weather.class, Providers.Photo.class, Providers.Clock.class,
            Providers.Lucky.class, Providers.AppListW.class};

    private static int[] overlays;

    /** The 32 transparent tap-area views in widget_ipod.xml. */
    static synchronized int[] overlays(Context c) {
        if (overlays == null) {
            overlays = new int[32];
            for (int i = 0; i < 32; i++) overlays[i] = c.getResources().getIdentifier("o" + i, "id", c.getPackageName());
        }
        return overlays;
    }

    static Class<?> providerFor(String type) {
        for (int i = 0; i < Cfg.TYPES.length; i++) if (Cfg.TYPES[i].equals(type)) return PROVIDERS[i];
        return PROVIDERS[0];
    }

    static String typeOf(Context c, int id) {
        AppWidgetProviderInfo info = AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
        if (info == null) return null;
        String n = info.provider.getClassName();
        for (int i = 0; i < PROVIDERS.length; i++) if (PROVIDERS[i].getName().equals(n)) return Cfg.TYPES[i];
        if (n.equals(Providers.CoverMusic.class.getName())) return Cfg.MUSIC;
        return null;
    }

    static int[] ids(Context c, String type) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] a = m.getAppWidgetIds(new ComponentName(c, providerFor(type)));
        if (!type.equals(Cfg.MUSIC)) return a;
        int[] b = m.getAppWidgetIds(new ComponentName(c, Providers.CoverMusic.class));
        int[] out = new int[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static List<int[]> all(Context c) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < Cfg.TYPES.length; i++) {
            for (int id : ids(c, Cfg.TYPES[i])) out.add(new int[]{id, i});
        }
        return out;
    }

    static void updateAll(Context c) {
        for (int[] e : all(c)) update(c, e[0]);
        schedule(c);
    }

    static void updateType(Context c, String type) {
        for (int id : ids(c, type)) update(c, id);
    }

    static void update(Context c, int id) {
        String type = typeOf(c, id);
        if (type == null) return;
        Cfg cfg = Cfg.load(c, id, type);
        if (isCover(c, id)) cfg.put("fill", true); // full-page iPod on the cover screen
        int[] sz = sizePx(c, id, type);
        Render.Out out;
        float ss = Math.min(SS, 1500f / Math.max(1, Math.max(sz[0], sz[1])));
        if (supersample(type) && ss > 1.02f) {
            // launchers scale widgets (Samsung ~0.83x); render larger so text stays crisp after scaling
            Render.FRAME_SCALE = ss;
            try {
                out = Render.render(c, cfg, Math.round(sz[0] * ss), Math.round(sz[1] * ss), false);
            } finally {
                Render.FRAME_SCALE = 1f;
            }
            out.zoneScale = out.bmp.getWidth() > 0 ? sz[0] / (float) out.bmp.getWidth() : 1f / ss;
        } else {
            out = Render.render(c, cfg, sz[0], sz[1], false);
        }
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            m.updateAppWidget(id, build(c, cfg, out));
            if (type.equals(Cfg.APPLIST)) m.notifyAppWidgetViewDataChanged(id, AppList.grid(cfg) ? R.id.grid : R.id.list);
        } catch (Exception ignored) { }
    }

    /** One animation frame: only the picture is sent (partial update), rendered smaller for speed. */
    static void frame(Context c, int id, float scale) {
        String type = typeOf(c, id);
        if (type == null) return;
        Cfg cfg = Cfg.load(c, id, type);
        int[] sz = sizePx(c, id, type);
        Render.FRAME_SCALE = scale;
        Render.Out out;
        try {
            out = Render.render(c, cfg, Math.round(sz[0] * scale), Math.round(sz[1] * scale), false);
        } finally {
            Render.FRAME_SCALE = 1f;
        }
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_ipod);
        rv.setImageViewBitmap(R.id.img, out.bmp);
        try {
            AppWidgetManager.getInstance(c).partiallyUpdateAppWidget(id, rv);
        } catch (Exception ignored) { }
    }

    static boolean isCover(Context c, int id) {
        AppWidgetProviderInfo info = AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
        return info != null && info.provider.getClassName().equals(Providers.CoverMusic.class.getName());
    }

    static final float SS = 1.25f;

    /** Picture-only widgets (no tap zones / live clocks in pixel space) are rendered supersampled. */
    static boolean supersample(String type) {
        switch (type) {
            case Cfg.CAL: case Cfg.EVENTS: case Cfg.STORAGE: case Cfg.QUOTE: case Cfg.DOTS:
            case Cfg.COMBO: case Cfg.WEATHER: case Cfg.PHOTO:
                return true;
        }
        return false;
    }

    static int[] defaultDp(String type) {
        switch (type) {
            case Cfg.STORAGE: return new int[]{330, 72};
            case Cfg.BIGDATE: return new int[]{330, 250};
            case Cfg.DOTS: return new int[]{300, 330};
            case Cfg.COMBO: return new int[]{330, 330};
            case Cfg.WEATHER: return new int[]{165, 165};
            case Cfg.PHOTO: return new int[]{165, 165};
            case Cfg.CLOCK: return new int[]{330, 165};
            case Cfg.APPLIST: return new int[]{300, 360};
            default: return new int[]{330, 165};
        }
    }

    @SuppressWarnings("deprecation")
    static int[] sizePx(Context c, int id, String type) {
        float d = c.getResources().getDisplayMetrics().density;
        float wdp = 0, hdp = 0;
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Bundle o = AppWidgetManager.getInstance(c).getAppWidgetOptions(id);
            if (o != null) {
                if (Build.VERSION.SDK_INT >= 31) {
                    ArrayList<SizeF> sizes = o.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
                    if (sizes != null && !sizes.isEmpty()) {
                        wdp = sizes.get(0).getWidth();
                        hdp = sizes.get(0).getHeight();
                    }
                }
                if (wdp <= 0 || hdp <= 0) {
                    wdp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                    hdp = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
                }
            }
        }
        if (wdp <= 0 || hdp <= 0) {
            int[] def = defaultDp(type);
            wdp = def[0];
            hdp = def[1];
        }
        return new int[]{Math.round(wdp * d), Math.round(hdp * d)};
    }

    static RemoteViews build(Context c, Cfg cfg, Render.Out out) {
        String pkg = c.getPackageName();
        boolean ipod = cfg.type.equals(Cfg.MUSIC) || cfg.type.equals(Cfg.GAME) || cfg.type.equals(Cfg.COMBO)
                || cfg.type.equals(Cfg.LUCKY);
        int layout;
        if (cfg.type.equals(Cfg.CLOCK) && Clocks.perFont(cfg.s("style", "lock"))) {
            layout = c.getResources().getIdentifier("clockn_" + cfg.s("font", "inter"), "layout", pkg);
            if (layout == 0) layout = c.getResources().getIdentifier("clockn_inter", "layout", pkg);
        } else if (cfg.type.equals(Cfg.APPLIST)) {
            layout = R.layout.widget_applist;
        } else if (cfg.type.equals(Cfg.CLOCK)) {
            layout = R.layout.widget_clock;
        } else if (cfg.type.equals(Cfg.COUNTER)) {
            layout = c.getResources().getIdentifier("counter_" + cfg.s("font", "sans"), "layout", pkg);
            if (layout == 0) layout = R.layout.counter_sans;
        } else {
            layout = ipod ? R.layout.widget_ipod : R.layout.widget_image;
        }
        RemoteViews rv = new RemoteViews(pkg, layout);
        rv.setImageViewBitmap(R.id.img, out.bmp);
        PendingIntent tap = tapIntent(c, cfg);
        if (tap != null) rv.setOnClickPendingIntent(R.id.root, tap);
        if (cfg.type.equals(Cfg.APPLIST)) {
            appList(c, cfg, rv, out);
            return rv;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            int[] slots = ipod ? overlays(c) : (cfg.type.equals(Cfg.BIGDATE) ? new int[]{R.id.o0} : new int[0]);
            for (int i = 0; i < slots.length; i++) {
                int v = slots[i];
                Render.Zone z = i < out.zones.size() ? out.zones.get(i) : null;
                PendingIntent pi = z != null ? zoneIntent(c, cfg, z.action, i) : null;
                if (pi == null) {
                    // Launchers may re-apply onto the previous view tree: clear leftover tap areas.
                    rv.setViewLayoutWidth(v, 0, TypedValue.COMPLEX_UNIT_PX);
                    rv.setViewLayoutHeight(v, 0, TypedValue.COMPLEX_UNIT_PX);
                    rv.setViewVisibility(v, View.GONE);
                    rv.setOnClickPendingIntent(v, null);
                    continue;
                }
                rv.setViewVisibility(v, View.VISIBLE);
                float k = out.zoneScale;
                rv.setViewLayoutWidth(v, z.r.width() * k, TypedValue.COMPLEX_UNIT_PX);
                rv.setViewLayoutHeight(v, z.r.height() * k, TypedValue.COMPLEX_UNIT_PX);
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_LEFT, z.r.left * k, TypedValue.COMPLEX_UNIT_PX);
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_TOP, z.r.top * k, TypedValue.COMPLEX_UNIT_PX);
                rv.setOnClickPendingIntent(v, pi);
            }
        }

        if (out.chrono) {
            int[] ids = {R.id.chrono_l, R.id.chrono_c, R.id.chrono_r};
            int v = ids[Math.max(0, Math.min(2, out.align))];
            rv.setViewVisibility(v, View.VISIBLE);
            rv.setChronometer(v, out.chronoBase, "%s", true);
            rv.setChronometerCountDown(v, out.countDown);
            rv.setTextColor(v, out.chronoColor);
            rv.setTextViewTextSize(v, TypedValue.COMPLEX_UNIT_PX, out.chronoSize);
            rv.setViewPadding(v, out.chronoPadH, 0, out.chronoPadH, out.chronoPadBottom);
        }
        if (cfg.type.equals(Cfg.CLOCK)) placeClocks(c, rv, out);
        if (out.clock) {
            int v = R.id.clock_m;
            rv.setViewVisibility(v, View.VISIBLE);
            rv.setTextColor(v, out.ccolor);
            rv.setTextViewTextSize(v, TypedValue.COMPLEX_UNIT_PX, out.csize * out.zoneScale);
            if (Build.VERSION.SDK_INT >= 31) {
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_LEFT, out.cx * out.zoneScale, TypedValue.COMPLEX_UNIT_PX);
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_TOP, out.cy * out.zoneScale, TypedValue.COMPLEX_UNIT_PX);
            }
        } else if (cfg.type.equals(Cfg.MUSIC) || cfg.type.equals(Cfg.GAME) || cfg.type.equals(Cfg.COMBO) || cfg.type.equals(Cfg.LUCKY)) {
            rv.setViewVisibility(R.id.clock_m, View.GONE);
        }
        if (out.musicChrono) {
            int v = R.id.chrono_m;
            rv.setViewVisibility(v, View.VISIBLE);
            rv.setChronometer(v, out.mbase, "%s", out.mrunning);
            rv.setTextColor(v, out.mcolor);
            rv.setTextViewTextSize(v, TypedValue.COMPLEX_UNIT_PX, out.msize);
            if (Build.VERSION.SDK_INT >= 31) {
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_LEFT, out.mx, TypedValue.COMPLEX_UNIT_PX);
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_TOP, out.my, TypedValue.COMPLEX_UNIT_PX);
            }
        }
        return rv;
    }

    /** Positions the live clock views (API 31+ can move views; older phones keep the picture only). */
    static void placeClocks(Context c, RemoteViews rv, Render.Out out) {
        if (Build.VERSION.SDK_INT < 31) return;
        String pkg = c.getPackageName();
        for (Clocks.TV t : out.tviews) {
            int v = c.getResources().getIdentifier(t.view, "id", pkg);
            if (v == 0) continue;
            rv.setViewVisibility(v, View.VISIBLE);
            rv.setViewLayoutWidth(v, t.r.width(), TypedValue.COMPLEX_UNIT_PX);
            rv.setViewLayoutHeight(v, t.r.height(), TypedValue.COMPLEX_UNIT_PX);
            rv.setViewLayoutMargin(v, RemoteViews.MARGIN_LEFT, t.r.left, TypedValue.COMPLEX_UNIT_PX);
            rv.setViewLayoutMargin(v, RemoteViews.MARGIN_TOP, t.r.top, TypedValue.COMPLEX_UNIT_PX);
            rv.setTextViewTextSize(v, TypedValue.COMPLEX_UNIT_PX, t.size);
            rv.setTextColor(v, t.color);
            if (t.tz != null) rv.setString(v, "setTimeZone", t.tz);
            if (t.fmt != null) {
                rv.setCharSequence(v, "setFormat12Hour", t.fmt);
                rv.setCharSequence(v, "setFormat24Hour", t.fmt);
            }
        }
        if (out.analog != null) {
            int v = R.id.analog;
            rv.setViewVisibility(v, View.VISIBLE);
            rv.setViewLayoutWidth(v, out.analog.width(), TypedValue.COMPLEX_UNIT_PX);
            rv.setViewLayoutHeight(v, out.analog.height(), TypedValue.COMPLEX_UNIT_PX);
            rv.setViewLayoutMargin(v, RemoteViews.MARGIN_LEFT, out.analog.left, TypedValue.COMPLEX_UNIT_PX);
            rv.setViewLayoutMargin(v, RemoteViews.MARGIN_TOP, out.analog.top, TypedValue.COMPLEX_UNIT_PX);
            // transparent dial: the face is drawn in the picture underneath
            rv.setIcon(v, "setDial", android.graphics.drawable.Icon.createWithBitmap(
                    android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)));
            rv.setIcon(v, "setHourHand", android.graphics.drawable.Icon.createWithBitmap(out.hHand));
            rv.setIcon(v, "setMinuteHand", android.graphics.drawable.Icon.createWithBitmap(out.mHand));
            if (out.sHand != null) rv.setIcon(v, "setSecondHand", android.graphics.drawable.Icon.createWithBitmap(out.sHand));
        }
    }

    static int flags() {
        return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
    }

    static Intent configIntent(Context c, int id) {
        Intent i = new Intent(c, ConfigActivity.class);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        i.putExtra("edit", true);
        i.setData(Uri.parse("ws://config/" + id));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return i;
    }

    static Intent launch(Context c, String what, int id) {
        if (what == null) what = "";
        Intent i = null;
        if (what.equals("config")) return configIntent(c, id);
        if (what.equals("calendar")) {
            i = new Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                    .appendPath(String.valueOf(System.currentTimeMillis())).build());
        } else if (what.equals("storage")) {
            i = new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        } else if (what.equals("clock")) {
            i = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
        } else if (what.startsWith("app:")) {
            i = c.getPackageManager().getLaunchIntentForPackage(what.substring(4));
        }
        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    /** Hooks the scrolling list / grid of the App List widget to its row service. */
    static void appList(Context c, Cfg cfg, RemoteViews rv, Render.Out out) {
        boolean grid = AppList.grid(cfg);
        int on = grid ? R.id.grid : R.id.list, off = grid ? R.id.list : R.id.grid;
        rv.setViewVisibility(off, View.GONE);
        rv.setViewVisibility(on, View.VISIBLE);
        Intent si = new Intent(c, AppList.Service.class);
        si.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, cfg.id);
        si.setData(Uri.parse("ws://applist/" + cfg.id + "/" + (grid ? "g" : "l")));
        rv.setRemoteAdapter(on, si);
        rv.setEmptyView(on, R.id.empty);
        rv.setTextColor(R.id.empty, Theme.get(cfg.s("theme", "ios_dark")).with(cfg).frameSub);
        if (grid) rv.setInt(R.id.grid, "setNumColumns", Math.max(2, Math.min(7, cfg.i("cols", 4))));
        Intent li = new Intent(c, AppList.Launch.class);
        rv.setPendingIntentTemplate(on, PendingIntent.getActivity(c, cfg.id * 64 + 60, li,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));
        if (Build.VERSION.SDK_INT >= 31) {
            for (int v : new int[]{R.id.list, R.id.grid, R.id.empty}) {
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_TOP, out.listTop * out.zoneScale, TypedValue.COMPLEX_UNIT_PX);
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_LEFT, out.listPad * out.zoneScale, TypedValue.COMPLEX_UNIT_PX);
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_RIGHT, out.listPad * out.zoneScale, TypedValue.COMPLEX_UNIT_PX);
                rv.setViewLayoutMargin(v, RemoteViews.MARGIN_BOTTOM, out.listPad * out.zoneScale, TypedValue.COMPLEX_UNIT_PX);
            }
        }
    }

    static PendingIntent tapIntent(Context c, Cfg cfg) {
        boolean ipod = cfg.type.equals(Cfg.MUSIC) || cfg.type.equals(Cfg.GAME) || cfg.type.equals(Cfg.LUCKY);
        if (cfg.type.equals(Cfg.QUOTE) && Quotes.on(cfg) && cfg.b("tapNext", true)) {
            // tap the quote widget: show the next quote
            Intent i = new Intent(c, ActionReceiver.class).setAction("com.widgetstudio.ACT")
                    .setData(Uri.parse("ws://act/" + cfg.id + "/-1/qnext"))
                    .putExtra("id", cfg.id).putExtra("slot", -1).putExtra("cmd", "qnext");
            return PendingIntent.getBroadcast(c, cfg.id * 64, i, flags());
        }
        String tap = cfg.s("tap", ipod ? "none" : "config");
        if (tap.equals("none")) return null;
        Intent i = launch(c, tap, cfg.id);
        if (i == null) i = configIntent(c, cfg.id);
        return PendingIntent.getActivity(c, cfg.id * 64, i, flags());
    }

    static PendingIntent zoneIntent(Context c, Cfg cfg, String action, int slot) {
        int req = cfg.id * 64 + 1 + slot;
        int block = -1;
        int widgetId = cfg.id;
        if (action.startsWith("slot:")) {
            // a tap inside a Custom Combo block: act with that block's settings
            int colon = action.indexOf(':', 5);
            block = Integer.parseInt(action.substring(5, colon));
            action = action.substring(colon + 1);
            cfg = Combo.blockCfg(cfg, block);
        }
        if (action.equals("btn")) {
            String app = cfg.s("btnApp", "");
            Intent i = launch(c, app.isEmpty() ? "calendar" : app, widgetId);
            if (i == null) i = configIntent(c, widgetId);
            return PendingIntent.getActivity(c, req, i, flags());
        }
        if (!action.startsWith("ipod:")) return null;
        String cmd = action.substring(5);
        if (cfg.type.equals(Cfg.MUSIC)) {
            if (cmd.equals("menu")) {
                Intent i = MusicService.menuIntent(c, cfg);
                if (i == null) i = configIntent(c, widgetId);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return PendingIntent.getActivity(c, req, i, flags());
            }
            if (cmd.equals("center") || (cmd.equals("play") && MusicService.now() == null)) {
                Intent i = MusicService.playerIntent(c, cfg);
                if (i == null) i = configIntent(c, widgetId);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return PendingIntent.getActivity(c, req, i, flags());
            }
        }
        Intent i = new Intent(c, ActionReceiver.class);
        i.setAction("com.widgetstudio.ACT");
        i.setData(Uri.parse("ws://act/" + widgetId + "/" + block + "/" + cmd));
        i.putExtra("id", widgetId);
        i.putExtra("slot", block);
        i.putExtra("cmd", cmd);
        return PendingIntent.getBroadcast(c, req, i, flags());
    }

    static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        Intent ti = new Intent(c, TickReceiver.class).setAction("com.widgetstudio.TICK");
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, ti, flags());
        List<int[]> all = all(c);
        if (all.isEmpty()) {
            am.cancel(pi);
            return;
        }
        long now = System.currentTimeMillis();
        long next = now + 30 * 60000L;
        Calendar mid = Calendar.getInstance();
        mid.add(Calendar.DAY_OF_MONTH, 1);
        mid.set(Calendar.HOUR_OF_DAY, 0);
        mid.set(Calendar.MINUTE, 0);
        mid.set(Calendar.SECOND, 5);
        next = Math.min(next, mid.getTimeInMillis());
        for (int[] e : all) {
            if (Cfg.TYPES[e[1]].equals(Cfg.CLOCK)) {
                Cfg ck = Cfg.load(c, e[0], Cfg.CLOCK);
                if (ck.s("style", "lock").equals("word")) next = Math.min(next, Clocks.nextWordBoundary(now));
                continue;
            }
            if (Cfg.TYPES[e[1]].equals(Cfg.QUOTE)) {
                Cfg q = Cfg.load(c, e[0], Cfg.QUOTE);
                if (Quotes.on(q)) next = Math.min(next, Quotes.nextChange(q, now));
                continue;
            }
            if (!Cfg.TYPES[e[1]].equals(Cfg.COUNTER)) continue;
            Cfg cfg = Cfg.load(c, e[0], Cfg.COUNTER);
            next = Math.min(next, Counter.nextBoundary(cfg, now));
        }
        next = Math.max(next, now + 15000);
        am.setAndAllowWhileIdle(AlarmManager.RTC, next, pi);
    }
}
