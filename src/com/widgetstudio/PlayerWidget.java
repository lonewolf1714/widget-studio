package com.widgetstudio;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.widget.RemoteViews;

/**
 * "iPod Player" launcher widget (Widget Studio): a clearly labelled tile that opens the full-screen
 * click-wheel player (Cover mode). Shows the Widget Studio name on the tile itself.
 */
public class PlayerWidget extends AppWidgetProvider {

    static PendingIntent openPlayer(Context c) {
        Intent i = new Intent(c, CoverActivity.class)
                .putExtra("pod_only", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 9001, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) update(c, m, id);
    }

    @Override public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle o) {
        update(c, m, id);
    }

    static void update(Context c, AppWidgetManager m, int id) {
        int[] sz = sizePx(c, m, id);
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_image);
        rv.setImageViewBitmap(R.id.img, draw(c, sz[0], sz[1]));
        rv.setOnClickPendingIntent(R.id.root, openPlayer(c));
        rv.setContentDescription(R.id.root, "Widget Studio: open iPod Player");
        try { m.updateAppWidget(id, rv); } catch (Exception ignored) { }
    }

    static int[] sizePx(Context c, AppWidgetManager m, int id) {
        float d = c.getResources().getDisplayMetrics().density;
        Bundle o = m.getAppWidgetOptions(id);
        int w = o != null ? o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) : 0;
        int h = o != null ? o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) : 0;
        if (w <= 0) w = 160;
        if (h <= 0) h = 72;
        return new int[]{Math.round(w * d), Math.round(h * d)};
    }

    /** Graphite tile: mini click wheel, "iPod Player" title, "Widget Studio" caption. */
    static Bitmap draw(Context c, int w, int h) {
        w = Math.max(120, Math.min(w, 1400));
        h = Math.max(60, Math.min(h, 1000));
        float d = c.getResources().getDisplayMetrics().density;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        RectF r = new RectF(0, 0, w, h);
        float rad = Math.min(24 * d, h * 0.45f);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new LinearGradient(0, 0, 0, h, 0xFF30343D, 0xFF24272E, Shader.TileMode.CLAMP));
        cv.drawRoundRect(r, rad, rad, bg);
        RectF e = new RectF(r);
        e.inset(d * 0.5f, d * 0.5f);
        cv.drawRoundRect(e, rad, rad, Icons.stroke(0x14FFFFFF, Math.max(1f, d * 0.8f)));

        float pad = Math.min(h * 0.18f, 16 * d);
        float wr = (h - 2 * pad) / 2f;
        float cx = pad + wr, cy = h / 2f;
        cv.drawCircle(cx, cy, wr, Icons.fill(0xFF1C1F25));
        cv.drawCircle(cx, cy, wr * 0.36f, Icons.fill(0xFF2A2D35));
        Paint ic = Icons.fill(0xE6F2F2F4);
        float s = wr * 0.16f;
        Ipod.dbl(cv, cx - wr * 0.68f, cy, s, ic, -1);
        Ipod.dbl(cv, cx + wr * 0.68f, cy, s, ic, 1);
        Icons.playPause(cv, cx, cy + wr * 0.68f, s * 1.5f, ic);

        float tx = cx + wr + pad * 0.9f, tw = w - tx - pad;
        if (tw > 30) {
            TextPaint t = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            Fonts.apply(t, c, "inter", 650);
            t.setColor(0xFFFFFFFF);
            t.setTextSize(Math.min(h * 0.24f, 18 * d));
            TextPaint cap = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            Fonts.apply(cap, c, "inter", 500);
            cap.setColor(0x99FFFFFF);
            cap.setTextSize(Math.min(h * 0.16f, 12.5f * d));
            String title = TextUtils.ellipsize("iPod Player", t, tw, TextUtils.TruncateAt.END).toString();
            String by = TextUtils.ellipsize("Widget Studio", cap, tw, TextUtils.TruncateAt.END).toString();
            float block = t.getTextSize() * 0.9f + cap.getTextSize() * 1.5f;
            float base = cy - block / 2f + t.getTextSize() * 0.85f;
            cv.drawText(title, tx, base, t);
            cv.drawText(by, tx, base + cap.getTextSize() * 1.5f, cap);
        }
        return b;
    }
}
