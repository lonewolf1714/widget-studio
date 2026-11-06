package com.widgetstudio;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * App List widget: a scrolling list (or grid) of apps you tap to open — all apps or the ones you choose.
 * The card and header are a rendered picture; the rows come from a RemoteViewsService so the list scrolls.
 */
final class AppList {
    static final class App {
        final String label;
        final ComponentName cmp;
        App(String label, ComponentName cmp) { this.label = label; this.cmp = cmp; }
        String key() { return cmp.flattenToShortString(); }
    }

    /** One list entry: an app, or a section letter (app == null). */
    static final class Item {
        final App app;
        final String letter;
        Item(App app, String letter) { this.app = app; this.letter = letter; }
    }

    private static final Map<String, Drawable> ICONS = new HashMap<>();

    // ------------------------------------------------------------------ data

    static List<App> installed(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<App> out = new ArrayList<>();
        for (ResolveInfo r : pm.queryIntentActivities(main, 0)) {
            out.add(new App(r.loadLabel(pm).toString(), new ComponentName(r.activityInfo.packageName, r.activityInfo.name)));
        }
        Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    static List<String> chosen(Cfg cfg) {
        List<String> out = new ArrayList<>();
        JSONArray a = cfg.o.optJSONArray("apps");
        if (a != null) for (int i = 0; i < a.length(); i++) out.add(a.optString(i));
        return out;
    }

    static List<App> apps(Context c, Cfg cfg) {
        List<App> all = installed(c);
        if (!cfg.s("src", "all").equals("pick")) return all;
        Map<String, App> byKey = new HashMap<>();
        for (App a : all) byKey.put(a.key(), a);
        List<App> out = new ArrayList<>();
        for (String k : chosen(cfg)) {
            App a = byKey.get(k);
            if (a != null) out.add(a);
        }
        if (cfg.s("sort", "az").equals("az")) Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    static boolean grid(Cfg cfg) { return cfg.s("layout", "list").equals("grid"); }

    static List<Item> items(Context c, Cfg cfg) {
        List<App> apps = apps(c, cfg);
        boolean sections = !grid(cfg) && cfg.b("sections", false)
                && (!cfg.s("src", "all").equals("pick") || cfg.s("sort", "az").equals("az"));
        List<Item> out = new ArrayList<>();
        String last = null;
        for (App a : apps) {
            if (sections) {
                String l = letter(a.label);
                if (!l.equals(last)) { out.add(new Item(null, l)); last = l; }
            }
            out.add(new Item(a, null));
        }
        return out;
    }

    static String letter(String label) {
        if (label.isEmpty()) return "#";
        char ch = Character.toUpperCase(label.charAt(0));
        return Character.isLetter(ch) ? String.valueOf(ch) : "#";
    }

    static final String LAUNCHER_ICONS = "com.reach.launcher.icons";

    /** True when Reach Launcher is installed and can share its styled icons. */
    static boolean launcherIcons(Context c) {
        try {
            return c.getPackageManager().resolveContentProvider(LAUNCHER_ICONS, 0) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Forgets cached icons so a changed launcher icon style shows up. */
    static synchronized void clearIcons() { ICONS.clear(); }

    static Drawable icon(Context c, ComponentName cmp) { return icon(c, cmp, true); }

    /** The app's icon: as Reach Launcher draws it (icon pack + style) when asked and available, else its own. */
    static synchronized Drawable icon(Context c, ComponentName cmp, boolean launcher) {
        String k = (launcher ? "L:" : "A:") + cmp.flattenToShortString();
        Drawable d = ICONS.get(k);
        if (d != null) return d;
        if (launcher) {
            Uri u = Uri.parse("content://" + LAUNCHER_ICONS + "/icon/" + Uri.encode(cmp.flattenToShortString()) + "?s=192");
            try (java.io.InputStream in = c.getContentResolver().openInputStream(u)) {
                Bitmap b = in != null ? android.graphics.BitmapFactory.decodeStream(in) : null;
                if (b != null) d = new android.graphics.drawable.BitmapDrawable(c.getResources(), b);
            } catch (Exception ignored) { }
        }
        if (d == null) {
            try {
                d = c.getPackageManager().getActivityIcon(cmp);
            } catch (Exception e) {
                d = c.getPackageManager().getDefaultActivityIcon();
            }
        }
        ICONS.put(k, d);
        return d;
    }

    // ------------------------------------------------------------------ drawing

    static float sp(Context c) { return c.getResources().getDisplayMetrics().scaledDensity; }

    static float dp(Context c) { return c.getResources().getDisplayMetrics().density; }

    static TextPaint labelPaint(Render.Ctx x) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Fonts.apply(tp, x.c, x.font, x.cfg.i("weight", 500));
        tp.setTextSize(x.cfg.i("labelSize", 15) * sp(x.c));
        tp.setColor(x.t.frameText);
        if (!x.card && x.cfg.b("shadow", false)) tp.setShadowLayer(3f * dp(x.c), 0, 1f * dp(x.c), 0x88000000);
        return tp;
    }

    /** Height in px of one list row / grid cell. */
    static int itemHeight(Render.Ctx x, Item it) {
        float d = dp(x.c);
        if (it.app == null) return Math.round(x.cfg.i("labelSize", 15) * sp(x.c) * 0.9f + 14 * d);
        float icon = x.cfg.i("iconSize", 38) * d, gap = x.cfg.i("rowGap", 8) * d;
        boolean labels = x.cfg.b("labels", true);
        TextPaint tp = labelPaint(x);
        float text = tp.descent() - tp.ascent();
        if (grid(x.cfg)) return Math.round(gap * 2 + icon + (labels ? 6 * d + text : 0));
        return Math.round(gap * 2 + Math.max(icon, labels ? text : 0));
    }

    static void drawIcon(Render.Ctx x, Canvas cv, Drawable d, float l, float t, float s) {
        int px = Math.max(1, Math.round(s));
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas bc = new Canvas(b);
        String shape = x.cfg.s("iconShape", "original");
        if (!shape.equals("original")) {
            Path p = shape.equals("circle") ? circle(px) : Render.Ctx.squircle(new RectF(0, 0, px, px), px * 0.22f);
            bc.clipPath(p);
            // fill behind so transparent legacy icons still make a full tile
            bc.drawColor(0xFFFFFFFF);
            d.setBounds(Math.round(-px * 0.1f), Math.round(-px * 0.1f), Math.round(px * 1.1f), Math.round(px * 1.1f));
        } else {
            d.setBounds(0, 0, px, px);
        }
        d.draw(bc);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (x.cfg.b("mono", false)) {
            ColorMatrix m = new ColorMatrix();
            m.setSaturation(0);
            p.setColorFilter(new ColorMatrixColorFilter(m));
        }
        cv.drawBitmap(b, l, t, p);
        b.recycle();
    }

    static Path circle(int px) {
        Path p = new Path();
        p.addCircle(px / 2f, px / 2f, px / 2f, Path.Direction.CW);
        return p;
    }

    /** Draws one item into a w x h area at (l, t). */
    static void drawItem(Render.Ctx x, Canvas cv, Item it, float l, float t, float w, float h, boolean last) {
        float d = dp(x.c);
        if (it.app == null) {
            TextPaint lp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            Fonts.apply(lp, x.c, x.font, 700);
            lp.setTextSize(x.cfg.i("labelSize", 15) * sp(x.c) * 0.82f);
            lp.setColor(x.accent);
            lp.setLetterSpacing(0.08f);
            cv.drawText(it.letter, l + 12 * d, x.mid(lp, t + h * 0.58f), lp);
            return;
        }
        float icon = x.cfg.i("iconSize", 38) * d;
        boolean labels = x.cfg.b("labels", true);
        TextPaint tp = labelPaint(x);
        Drawable dr = icon(x.c, it.app.cmp, x.cfg.s("iconSource", "launcher").equals("launcher"));
        if (grid(x.cfg)) {
            float gap = x.cfg.i("rowGap", 8) * d;
            float il = l + (w - icon) / 2f;
            drawIcon(x, cv, dr, il, t + gap, icon);
            if (labels) {
                tp.setTextSize(tp.getTextSize() * 0.86f);
                String s = TextUtils.ellipsize(it.app.label, tp, w - 6 * d, TextUtils.TruncateAt.END).toString();
                cv.drawText(s, l + (w - tp.measureText(s)) / 2f, t + gap + icon + 6 * d - tp.ascent(), tp);
            }
            return;
        }
        float padL = 12 * d;
        boolean center = x.cfg.s("rowAlign", "left").equals("center") && !labels;
        float il = center ? l + (w - icon) / 2f : l + padL;
        drawIcon(x, cv, dr, il, t + (h - icon) / 2f, icon);
        if (labels) {
            float tx = il + icon + 14 * d;
            String s = TextUtils.ellipsize(it.app.label, tp, l + w - padL - tx, TextUtils.TruncateAt.END).toString();
            cv.drawText(s, tx, x.mid(tp, t + h / 2f), tp);
        }
        if (x.cfg.b("dividers", false) && !last) {
            Paint dv = Icons.fill(Theme.alpha(x.t.frameSub, 0.25f));
            float sx = labels ? il + icon + 14 * d : l + padL;
            cv.drawRect(sx, t + h - Math.max(1f, d * 0.6f), l + w - padL, t + h, dv);
        }
    }

    static Bitmap itemBitmap(Render.Ctx x, Item it, int w, boolean last) {
        int h = Math.max(1, itemHeight(x, it));
        Bitmap b = Bitmap.createBitmap(Math.max(1, w), h, Bitmap.Config.ARGB_8888);
        drawItem(x, new Canvas(b), it, 0, 0, w, h, last);
        return b;
    }

    /** Side padding of the list inside the card (px). */
    static float listPad(Render.Ctx x) {
        return (x.card ? 8f : 2f) * dp(x.c);
    }

    /** Card + header; in the editor preview the first rows are drawn too. Sets out.listTop. */
    static void draw(Render.Ctx x, Canvas cv, Render.Out out) {
        x.frame(cv);
        float d = dp(x.c);
        float pad = listPad(x);
        float top = pad + (x.card ? 4 * d : 0);
        List<Item> items = items(x.c, x.cfg);
        if (x.cfg.b("header", true)) {
            TextPaint hp = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            Fonts.apply(hp, x.c, x.font, 700);
            hp.setTextSize(x.cfg.i("titleSize", 19) * sp(x.c));
            hp.setColor(x.t.frameText);
            if (!x.card && x.cfg.b("shadow", false)) hp.setShadowLayer(3f * d, 0, 1f * d, 0x88000000);
            float hy = top + 6 * d;
            String title = x.cfg.s("title", "Apps");
            float hl = pad + 12 * d;
            if (!title.isEmpty()) cv.drawText(title, hl, hy - hp.ascent(), hp);
            if (x.cfg.b("count", true)) {
                TextPaint cp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                Fonts.apply(cp, x.c, x.font, 600);
                cp.setTextSize(12 * sp(x.c));
                cp.setColor(x.t.frameSub);
                cp.setLetterSpacing(0.06f);
                int n = 0;
                for (Item it : items) if (it.app != null) n++;
                String cs = (n + (n == 1 ? " APP" : " APPS"));
                float ch = hp.descent() - hp.ascent();
                cv.drawText(cs, x.w - pad - 12 * d - cp.measureText(cs), x.mid(cp, hy + ch / 2f), cp);
            }
            top = hy + (hp.descent() - hp.ascent()) + 8 * d;
        }
        out.listTop = top;
        out.listPad = pad;
        if (!x.preview) return;
        // preview: lay out rows statically
        float w = x.w - 2 * pad;
        cv.save();
        cv.clipRect(pad, top, x.w - pad, x.h - pad);
        if (items.isEmpty()) {
            TextPaint ep = labelPaint(x);
            ep.setColor(x.t.frameSub);
            cv.drawText("No apps chosen yet", pad + 12 * d, top + 30 * d, ep);
        } else if (grid(x.cfg)) {
            int cols = Math.max(2, Math.min(7, x.cfg.i("cols", 4)));
            float cw = w / cols, y = top;
            for (int i = 0; i < items.size() && y < x.h; i += cols) {
                int ih = itemHeight(x, items.get(i));
                for (int j = 0; j < cols && i + j < items.size(); j++)
                    drawItem(x, cv, items.get(i + j), pad + j * cw, y, cw, ih, false);
                y += ih;
            }
        } else {
            float y = top;
            for (int i = 0; i < items.size() && y < x.h; i++) {
                int ih = itemHeight(x, items.get(i));
                drawItem(x, cv, items.get(i), pad, y, w, ih, i == items.size() - 1);
                y += ih;
            }
        }
        cv.restore();
    }

    // ------------------------------------------------------------------ widget collection

    /** Supplies the rows to the home-screen list / grid. */
    public static class Service extends RemoteViewsService {
        @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
            return new Factory(getApplicationContext(), intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1));
        }
    }

    static final class Factory implements RemoteViewsService.RemoteViewsFactory {
        final Context c;
        final int id;
        List<Item> items = new ArrayList<>();
        Render.Ctx x;
        int itemW;

        Factory(Context c, int id) { this.c = c; this.id = id; }

        @Override public void onCreate() { }

        @Override public void onDataSetChanged() {
            Cfg cfg = Cfg.load(c, id, Cfg.APPLIST);
            int[] sz = Updater.sizePx(c, id, Cfg.APPLIST);
            x = new Render.Ctx(c, cfg, sz[0], sz[1], false);
            int listW = Math.max(1, Math.round(sz[0] - 2 * listPad(x)));
            itemW = grid(cfg) ? Math.max(1, listW / Math.max(2, Math.min(7, cfg.i("cols", 4)))) : listW;
            clearIcons(); // pick up launcher icon-style changes
            try {
                items = items(c, cfg);
            } catch (Exception e) {
                items = new ArrayList<>();
            }
        }

        @Override public void onDestroy() { items.clear(); }

        @Override public int getCount() { return items.size(); }

        @Override public RemoteViews getViewAt(int pos) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.applist_item);
            if (pos < 0 || pos >= items.size() || x == null) return rv;
            Item it = items.get(pos);
            rv.setImageViewBitmap(R.id.cell, itemBitmap(x, it, itemW, pos == items.size() - 1));
            if (it.app != null) {
                rv.setOnClickFillInIntent(R.id.cell, new Intent().putExtra("cmp", it.app.key()));
                rv.setContentDescription(R.id.cell, it.app.label);
            }
            return rv;
        }

        @Override public RemoteViews getLoadingView() { return null; }

        @Override public int getViewTypeCount() { return 1; }

        @Override public long getItemId(int pos) {
            if (pos < 0 || pos >= items.size()) return pos;
            Item it = items.get(pos);
            return it.app != null ? it.app.key().hashCode() : ("#" + it.letter).hashCode();
        }

        @Override public boolean hasStableIds() { return true; }
    }

    /** Invisible trampoline: opens the tapped app, then closes. */
    public static class Launch extends Activity {
        @Override protected void onCreate(Bundle b) {
            super.onCreate(b);
            String k = getIntent().getStringExtra("cmp");
            ComponentName cmp = k != null ? ComponentName.unflattenFromString(k) : null;
            if (cmp != null) {
                Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(cmp)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                try {
                    startActivity(i);
                } catch (Exception e) {
                    Intent p = getPackageManager().getLaunchIntentForPackage(cmp.getPackageName());
                    if (p != null) try { startActivity(p.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception ignored) { }
                }
            }
            finish();
            overridePendingTransition(0, 0);
        }
    }

    static String upper(String s) { return s.toUpperCase(Locale.getDefault()); }
}
