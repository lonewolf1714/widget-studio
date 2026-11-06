package com.widgetstudio;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextPaint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Clock widget styles. Live time comes from TextClock / AnalogClock views placed over the picture
 * (they tick by themselves); the picture provides the face, cards, labels and background.
 */
final class Clocks {
    static final String[] STYLES = {"niagara", "day", "stack", "stack2", "split", "lock", "bold", "analog", "flip", "world", "word"};
    static final String[] LABELS = {"Launcher (any font)", "Day name", "Stacked", "Stacked two-tone", "Split",
            "Lock screen", "Bold", "Analog", "Flip", "World", "Word clock"};

    /** Styles that use the per-font live-clock layout. */
    static boolean perFont(String style) {
        return style.equals("niagara") || style.equals("day") || style.equals("stack") || style.equals("stack2") || style.equals("split");
    }
    static final String[][] ZONES = {
            {"Asia/Kolkata", "New Delhi"}, {"America/New_York", "New York"}, {"Europe/London", "London"},
            {"Asia/Dubai", "Dubai"}, {"Asia/Tokyo", "Tokyo"}, {"Australia/Sydney", "Sydney"},
            {"America/Los_Angeles", "Los Angeles"}, {"Europe/Paris", "Paris"}, {"Asia/Singapore", "Singapore"},
            {"Europe/Berlin", "Berlin"}};

    /** A live TextClock placement: which view, where, how big, colour, optional time zone. */
    static final class TV {
        final String view;
        final RectF r;
        final float size;
        final int color;
        final String tz;
        String fmt; // optional TextClock pattern (used for both 12 and 24 hour)
        TV(String view, RectF r, float size, int color, String tz) {
            this.view = view; this.r = r; this.size = size; this.color = color; this.tz = tz;
        }
    }

    static String city(String tz) {
        for (String[] z : ZONES) if (z[0].equals(tz)) return z[1];
        return tz;
    }

    static boolean h24(android.content.Context c) {
        return android.text.format.DateFormat.is24HourFormat(c);
    }

    static void draw(Render.Ctx x, Canvas cv, Render.Out out) {
        x.frame(cv);
        String style = x.cfg.s("style", "lock");
        switch (style) {
            case "bold": bold(x, cv, out); break;
            case "analog": analog(x, cv, out); break;
            case "flip": flip(x, cv, out); break;
            case "world": world(x, cv, out); break;
            case "word": word(x, cv, out); break;
            case "niagara": niagara(x, cv, out); break;
            case "day": day(x, cv, out); break;
            case "stack": stack(x, cv, out, false); break;
            case "stack2": stack(x, cv, out, true); break;
            case "split": split(x, cv, out); break;
            default: lock(x, cv, out);
        }
    }

    static String time(android.content.Context c, String tz) {
        SimpleDateFormat f = new SimpleDateFormat(h24(c) ? "HH:mm" : "h:mm", Locale.getDefault());
        if (tz != null) f.setTimeZone(TimeZone.getTimeZone(tz));
        return f.format(new Date());
    }

    /** Places a live clock, or (in previews) draws the same text into the picture. */
    static void live(Render.Ctx x, Canvas cv, Render.Out out, String view, RectF r, float size, int color, String tz,
                     String fontKey, int weight, String previewText) {
        if (x.preview) {
            TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            Fonts.apply(p, x.c, fontKey, weight);
            p.setTextSize(size);
            p.setColor(color);
            float tw = p.measureText(previewText);
            float tx = view.endsWith("_l") ? r.left : r.centerX() - tw / 2f;
            cv.drawText(previewText, tx, x.mid(p, r.centerY()), p);
        } else {
            out.tviews.add(new TV(view, r, size, color, tz));
        }
    }

    static int battery(android.content.Context c) {
        try {
            android.os.BatteryManager bm = c.getSystemService(android.os.BatteryManager.class);
            if (bm != null) return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception ignored) { }
        return -1;
    }

    /** "5:00 · 30 Aug · 100%" as a TextClock pattern (time and date live, battery refreshed with the widget). */
    static String infoPattern(android.content.Context c, String sep, boolean withDay) {
        int b = battery(c);
        String p = (h24(c) ? "HH:mm" : "h:mm") + sep + (withDay ? "EEE " : "") + "d MMM";
        if (b >= 0) p += sep + "'" + b + "%'";
        return p;
    }

    /** Places the small live info line (or draws it in previews). */
    static void info(Render.Ctx x, Canvas cv, Render.Out out, String view, RectF r, float size, int color, String pattern,
                     boolean centered, float letterSpacing) {
        if (x.preview) {
            TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            Fonts.apply(p, x.c, centered ? "sans" : "sans_medium", centered ? 400 : 500);
            p.setTextSize(size);
            p.setColor(color);
            p.setLetterSpacing(letterSpacing);
            String s = new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
            if (centered) s = s.toUpperCase(Locale.getDefault());
            float tx = centered ? r.centerX() - p.measureText(s) / 2f : r.left;
            cv.drawText(s, tx, x.mid(p, r.centerY()), p);
        } else {
            TV t = new TV(view, r, size, color, null);
            t.fmt = pattern;
            out.tviews.add(t);
        }
    }

    // ---------------------------------------------------------------- Day name ("Thu.")
    static void day(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, pad = (x.card ? 7f : 3f) * x.u;
        String dn = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());
        dn = dn.substring(0, 1).toUpperCase(Locale.getDefault()) + dn.substring(1).toLowerCase(Locale.getDefault()) + ".";
        TextPaint big = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Fonts.apply(big, x.c, x.font, 800);
        big.setColor(t.frameText);
        float size = h * 0.5f * x.scale;
        big.setTextSize(size);
        if (big.measureText(dn) > w - 2 * pad) big.setTextSize(size * (w - 2 * pad) / big.measureText(dn));
        float infoSize = Math.max(8.5f * x.density, h * 0.07f) * x.scale;
        float block = big.getTextSize() * 0.8f + infoSize * 2.2f;
        float base = (h - block) / 2f + big.getTextSize() * 0.8f;
        cv.drawText(dn, pad, base, big);
        RectF ir = new RectF(pad, base + infoSize * 0.6f, w - pad, base + infoSize * 2.2f);
        info(x, cv, out, "tc_info", ir, infoSize, Theme.alpha(t.frameText, 0.85f), infoPattern(x.c, " · ", false), false, 0f);
    }

    // ---------------------------------------------------------------- Stacked "05 / 00"
    static void stack(Render.Ctx x, Canvas cv, Render.Out out, boolean twoTone) {
        Theme t = x.t;
        float w = x.w, h = x.h, pad = (x.card ? 7f : 3f) * x.u;
        float infoSize = Math.max(8.5f * x.density, h * 0.065f) * x.scale;
        float rowH = (h - 2 * pad - infoSize * 2f) / 2f;
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Fonts.apply(tp, x.c, x.font, 600);
        float size = rowH * 1.08f * x.scale;
        tp.setTextSize(size);
        if (tp.measureText("00") > w - 2 * pad) size *= (w - 2 * pad) / tp.measureText("00");
        tp.setTextSize(size);
        float lineH = (tp.descent() - tp.ascent()) * 0.78f;
        float top = (h - lineH * 2 - infoSize * 2f) / 2f;
        RectF hr = new RectF(pad, top, w - pad, top + lineH);
        RectF mr = new RectF(pad, top + lineH, w - pad, top + lineH * 2);
        int hc = twoTone ? Theme.alpha(t.frameText, 0.42f) : t.frameText;
        Calendar k = Calendar.getInstance();
        String hh = String.format(Locale.US, "%02d", h24(x.c) ? k.get(Calendar.HOUR_OF_DAY) : (k.get(Calendar.HOUR) == 0 ? 12 : k.get(Calendar.HOUR)));
        String mm = String.format(Locale.US, "%02d", k.get(Calendar.MINUTE));
        if (x.preview) {
            tp.setColor(hc);
            cv.drawText(hh, pad, x.mid(tp, hr.centerY()), tp);
            tp.setColor(t.frameText);
            cv.drawText(mm, pad, x.mid(tp, mr.centerY()), tp);
        } else {
            out.tviews.add(new TV("tc_sh", hr, size, hc, null));
            out.tviews.add(new TV("tc_sm", mr, size, t.frameText, null));
        }
        RectF ir = new RectF(pad + size * 0.04f, mr.bottom + infoSize * 0.3f, w - pad, mr.bottom + infoSize * 1.9f);
        info(x, cv, out, "tc_info", ir, infoSize, Theme.alpha(t.frameText, 0.85f), "EEE, d MMM" + (battery(x.c) >= 0 ? "  '" + battery(x.c) + "%'" : ""), false, 0f);
    }

    // ---------------------------------------------------------------- Split "THU" with a live band through the middle
    static void split(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, pad = (x.card ? 7f : 3f) * x.u;
        String dn = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault());
        TextPaint big = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Fonts.apply(big, x.c, x.font, 300);
        big.setColor(t.frameText);
        big.setLetterSpacing(0.25f);
        float size = h * 0.55f * x.scale;
        big.setTextSize(size);
        if (big.measureText(dn) > w - 2 * pad) big.setTextSize(size * (w - 2 * pad) / big.measureText(dn));
        float cy = h / 2f;
        float infoSize = Math.max(8.5f * x.density, h * 0.075f) * x.scale;
        float band = infoSize * 2.1f;
        // letters with a clear band cut through the middle
        cv.save();
        android.graphics.Path keep = new android.graphics.Path();
        keep.addRect(0, 0, w, cy - band / 2f, android.graphics.Path.Direction.CW);
        keep.addRect(0, cy + band / 2f, w, h, android.graphics.Path.Direction.CW);
        cv.clipPath(keep);
        float tw = big.measureText(dn);
        cv.drawText(dn, (w - tw) / 2f + big.getLetterSpacing() * big.getTextSize() / 2f, x.mid(big, cy), big);
        cv.restore();
        RectF ir = new RectF(pad, cy - band / 2f, w - pad, cy + band / 2f);
        info(x, cv, out, "tc_infoc", ir, infoSize, t.frameText, infoPattern(x.c, "   ", false), true, 0.18f);
    }

    // ---------------------------------------------------------------- launcher style (big time, any font)
    static void niagara(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, pad = (x.card ? 7f : 3f) * x.u;
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        Fonts.apply(tp, x.c, x.font, 600);
        float size = h * 0.5f * x.scale;
        tp.setTextSize(size);
        String sample = h24(x.c) ? "00:00" : "12:00";
        float maxW = w - 2 * pad;
        if (tp.measureText(sample) > maxW) size *= maxW / tp.measureText(sample);
        tp.setTextSize(size);
        float timeH = (tp.descent() - tp.ascent()) * 0.92f;
        TextPaint ip = x.p(5f, t.frameText, 550);
        float infoH = ip.getTextSize() * 1.6f;
        float top = (h - timeH - infoH) / 2f;
        RectF tr = new RectF(pad, top, w - pad, top + timeH);
        tp.setColor(t.frameText);
        if (x.preview) {
            cv.drawText(time(x.c, null), pad, x.mid(tp, tr.centerY()), tp);
        } else {
            out.tviews.add(new TV("tc_ni", tr, size, t.frameText, null));
        }
        // info line: date · battery · weather
        float y = x.mid(ip, top + timeH + infoH * 0.55f);
        float ix = pad + size * 0.04f;
        String date = new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(new Date());
        cv.drawText(date, ix, y, ip);
        ix += ip.measureText(date) + ip.getTextSize() * 1.1f;
        int level = -1;
        try {
            android.os.BatteryManager bm = x.c.getSystemService(android.os.BatteryManager.class);
            if (bm != null) level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception ignored) { }
        if (level >= 0) {
            String b = level + "%";
            cv.drawText(b, ix, y, ip);
            ix += ip.measureText(b) + ip.getTextSize() * 1.1f;
        }
        Weather.Data wd = Weather.get(x.c);
        if (wd != null && ix < w - pad - ip.getTextSize() * 3) {
            float ic = ip.getTextSize() * 1.15f;
            Icons.weather(cv, wd.code, wd.isDay, ix + ic / 2f, y - ip.getTextSize() * 0.35f, ic, x.accent);
            cv.drawText(Weather.deg(wd), ix + ic * 1.15f, y, ip);
        }
    }

    // ---------------------------------------------------------------- iOS lock-screen style
    static void lock(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h;
        float dateH = h * 0.16f, timeH = h * 0.5f;
        float top = (h - dateH - timeH) / 2f;
        TextPaint dp = x.p(5.2f, Theme.alpha(t.frameText, 0.85f), 600);
        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
        RectF dr = new RectF(0, top, w, top + dateH);
        if (x.preview) {
            cv.drawText(date, w / 2f - dp.measureText(date) / 2f, x.mid(dp, dr.centerY()), dp);
        } else {
            out.tviews.add(new TV("tc_date", dr, dp.getTextSize(), dp.getColor(), null));
        }
        RectF tr = new RectF(0, top + dateH, w, top + dateH + timeH);
        live(x, cv, out, "tc_thin", tr, Math.min(timeH * 0.92f, w * 0.3f) * x.scale, t.frameText, null, "sans_light", 300,
                time(x.c, null));
    }

    // ---------------------------------------------------------------- bold
    static void bold(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, pad = 7 * x.u;
        RectF tr = new RectF(pad, pad, w - pad, h * 0.68f);
        live(x, cv, out, "tc_bold", tr, Math.min(tr.height() * 0.95f, w * 0.27f) * x.scale, t.frameText, null,
                "sans_black", 900, time(x.c, null));
        TextPaint dp = x.p(5f, x.accent, 700);
        dp.setLetterSpacing(0.08f);
        String date = new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault());
        cv.drawText(date, w / 2f - dp.measureText(date) / 2f, x.mid(dp, h * 0.82f), dp);
    }

    // ---------------------------------------------------------------- analog
    static void analog(Render.Ctx x, Canvas cv, Render.Out out) {
        float w = x.w, h = x.h;
        float d = Math.min(w, h) * 0.86f;
        RectF r = new RectF((w - d) / 2f, (h - d) / 2f, (w + d) / 2f, (h + d) / 2f);
        int size = Math.max(64, Math.round(d));
        Bitmap dial = dial(x, size);
        cv.drawBitmap(dial, null, r, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        boolean secs = x.cfg.b("seconds", true);
        if (x.preview) {
            Calendar k = Calendar.getInstance();
            float hr = (k.get(Calendar.HOUR) + k.get(Calendar.MINUTE) / 60f) * 30f;
            float mn = (k.get(Calendar.MINUTE) + k.get(Calendar.SECOND) / 60f) * 6f;
            drawRot(cv, hand(x, size, "h"), r, hr);
            drawRot(cv, hand(x, size, "m"), r, mn);
            if (secs) drawRot(cv, hand(x, size, "s"), r, k.get(Calendar.SECOND) * 6f);
            drawRot(cv, hand(x, size, "c"), r, 0);
        } else {
            out.analog = r;
            out.hHand = hand(x, size, "h");
            out.mHand = hand(x, size, "m");
            out.sHand = secs ? hand(x, size, "s") : null;
            out.cap = hand(x, size, "c");
        }
    }

    static void drawRot(Canvas cv, Bitmap b, RectF r, float deg) {
        cv.save();
        cv.rotate(deg, r.centerX(), r.centerY());
        cv.drawBitmap(b, null, r, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        cv.restore();
    }

    /** Clock face: soft concave dial, minute ticks, bold hour markers, numerals at 12/3/6/9. */
    static Bitmap dial(Render.Ctx x, int s) {
        Theme t = x.t;
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        float c = s / 2f, r = s / 2f;
        boolean lightFace = Theme.luminance(t.panel) > 140;
        int face1 = lightFace ? 0xFFFFFFFF : Render.Ctx.lighten(t.panel, 0.06f);
        int face2 = lightFace ? 0xFFE9E9EC : Render.Ctx.darken(t.panel, 0.1f);
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setShader(new RadialGradient(c, c * 0.8f, r * 1.1f, face1, face2, Shader.TileMode.CLAMP));
        cv.drawCircle(c, c, r * 0.98f, fp);
        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(r * 0.025f);
        rim.setShader(new LinearGradient(0, 0, 0, s, 0x33FFFFFF, 0x22000000, Shader.TileMode.CLAMP));
        cv.drawCircle(c, c, r * 0.97f, rim);
        int ink = lightFace ? 0xFF1C1C1E : t.text;
        int soft = Theme.alpha(ink, 0.35f);
        for (int i = 0; i < 60; i++) {
            boolean hour = i % 5 == 0;
            double a = Math.toRadians(i * 6 - 90);
            float r1 = r * (hour ? 0.78f : 0.84f), r2 = r * 0.89f;
            Paint p = Icons.stroke(hour ? ink : soft, r * (hour ? 0.03f : 0.012f));
            cv.drawLine(c + (float) Math.cos(a) * r1, c + (float) Math.sin(a) * r1,
                    c + (float) Math.cos(a) * r2, c + (float) Math.sin(a) * r2, p);
        }
        TextPaint np = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Fonts.apply(np, x.c, "inter", 600);
        np.setTextSize(r * 0.16f);
        np.setColor(ink);
        String[] n = {"12", "3", "6", "9"};
        float[][] pos = {{0, -0.6f}, {0.6f, 0}, {0, 0.6f}, {-0.6f, 0}};
        for (int i = 0; i < 4; i++) {
            float px = c + pos[i][0] * r, py = c + pos[i][1] * r;
            cv.drawText(n[i], px - np.measureText(n[i]) / 2f, py - (np.ascent() + np.descent()) / 2f, np);
        }
        TextPaint brand = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Fonts.apply(brand, x.c, "inter", 500);
        brand.setTextSize(r * 0.075f);
        brand.setColor(soft);
        String date = new SimpleDateFormat("EEE d", Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault());
        cv.drawText(date, c - brand.measureText(date) / 2f, c + r * 0.32f, brand);
        return b;
    }

    /** Hands drawn pointing to 12 in a full-size square (AnalogClock rotates them about the centre). */
    static Bitmap hand(Render.Ctx x, int s, String kind) {
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        float c = s / 2f, r = s / 2f;
        boolean lightFace = Theme.luminance(x.t.panel) > 140;
        int ink = lightFace ? 0xFF1C1C1E : x.t.text;
        Paint sh = new Paint(Paint.ANTI_ALIAS_FLAG);
        sh.setColor(0x40000000);
        sh.setMaskFilter(new android.graphics.BlurMaskFilter(r * 0.02f, android.graphics.BlurMaskFilter.Blur.NORMAL));
        switch (kind) {
            case "h": {
                Paint p = Icons.stroke(ink, r * 0.07f);
                Paint ps = Icons.stroke(0x40000000, r * 0.07f);
                cv.drawLine(c + r * 0.012f, c + r * 0.03f, c + r * 0.012f, c - r * 0.47f, ps);
                cv.drawLine(c, c, c, c - r * 0.5f, p);
                break;
            }
            case "m": {
                Paint p = Icons.stroke(ink, r * 0.045f);
                Paint ps = Icons.stroke(0x33000000, r * 0.045f);
                cv.drawLine(c + r * 0.012f, c + r * 0.03f, c + r * 0.012f, c - r * 0.73f, ps);
                cv.drawLine(c, c, c, c - r * 0.76f, p);
                break;
            }
            case "s": {
                Paint p = Icons.stroke(x.accent, r * 0.018f);
                cv.drawLine(c, c + r * 0.16f, c, c - r * 0.82f, p);
                break;
            }
            default: { // centre cap
                cv.drawCircle(c, c, r * 0.05f, Icons.fill(x.accent));
                cv.drawCircle(c, c, r * 0.022f, Icons.fill(lightFace ? 0xFFFFFFFF : 0xFF1C1C1E));
            }
        }
        return b;
    }

    // ---------------------------------------------------------------- flip
    static void flip(Render.Ctx x, Canvas cv, Render.Out out) {
        float w = x.w, h = x.h, pad = 6 * x.u, gap = 3.5f * x.u;
        float cw = (w - 2 * pad - gap) / 2f, ch = Math.min(h - 2 * pad, cw * 1.05f);
        float top = (h - ch) / 2f;
        int card1 = Render.Ctx.lighten(x.t.panel, 0.05f), card2 = Render.Ctx.darken(x.t.panel, 0.08f);
        String[] views = {"tc_hh", "tc_mm"};
        Calendar k = Calendar.getInstance();
        String[] prev = {String.format(Locale.US, "%02d", h24(x.c) ? k.get(Calendar.HOUR_OF_DAY)
                : (k.get(Calendar.HOUR) == 0 ? 12 : k.get(Calendar.HOUR))), String.format(Locale.US, "%02d", k.get(Calendar.MINUTE))};
        for (int i = 0; i < 2; i++) {
            float l = pad + i * (cw + gap);
            RectF r = new RectF(l, top, l + cw, top + ch);
            float rad = cw * 0.12f;
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new LinearGradient(0, r.top, 0, r.bottom, card1, card2, Shader.TileMode.CLAMP));
            cv.drawPath(Render.Ctx.squircle(r, rad), p);
            live(x, cv, out, views[i], r, ch * 0.62f * x.scale, x.t.text, null, "sans_medium", 500, prev[i]);
            // the split line drawn over the digits by the picture can't overlay live text, so mark it at the edges
            Paint split = Icons.stroke(Theme.alpha(0xFF000000, 0.45f), Math.max(1.5f, ch * 0.012f));
            cv.drawLine(r.left, r.centerY(), r.left + cw * 0.08f, r.centerY(), split);
            cv.drawLine(r.right - cw * 0.08f, r.centerY(), r.right, r.centerY(), split);
            cv.drawCircle(r.left + cw * 0.04f, r.centerY(), ch * 0.02f, Icons.fill(0x66000000));
            cv.drawCircle(r.right - cw * 0.04f, r.centerY(), ch * 0.02f, Icons.fill(0x66000000));
        }
    }

    // ---------------------------------------------------------------- world clock
    static void world(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, pad = 7 * x.u;
        String[] tz = {x.cfg.s("tz1", "Asia/Kolkata"), x.cfg.s("tz2", "America/New_York")};
        String[] views = {"tc_w1", "tc_w2"};
        float rowH = (h - 2 * pad) / 2f;
        for (int i = 0; i < 2; i++) {
            float top = pad + i * rowH, cy = top + rowH / 2f;
            TextPaint cp = x.p(6f, t.frameText, 650);
            TextPaint sp = x.p(4.2f, t.frameSub, 500);
            String name = city(tz[i]);
            int off = TimeZone.getTimeZone(tz[i]).getOffset(System.currentTimeMillis()) - TimeZone.getDefault().getOffset(System.currentTimeMillis());
            float hrs = off / 3600000f;
            String rel = Math.abs(hrs) < 0.01f ? "Local time" : (hrs > 0 ? "+" : "") + (hrs == Math.round(hrs) ? String.valueOf(Math.round(hrs)) : String.valueOf(hrs)) + " hrs";
            cv.drawText(x.ell(name, cp, w * 0.5f), pad, cy - cp.getTextSize() * 0.15f, cp);
            cv.drawText(rel, pad, cy + sp.getTextSize() * 1.2f, sp);
            RectF r = new RectF(w * 0.45f, top, w - pad, top + rowH);
            live(x, cv, out, views[i], r, rowH * 0.55f * x.scale, t.frameText, tz[i], "sans_light", 300, time(x.c, tz[i]));
            if (i == 0) cv.drawLine(pad, top + rowH, w - pad, top + rowH, Icons.stroke(Theme.alpha(t.frameSub, 0.3f), Math.max(1f, x.density * 0.7f)));
        }
    }

    // ---------------------------------------------------------------- word clock
    static final String[] NUM = {"twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "eleven", "twelve"};

    static String words(Calendar k) {
        int m = (int) Math.round(k.get(Calendar.MINUTE) / 5.0) * 5;
        int hr = k.get(Calendar.HOUR);
        if (m == 60) { m = 0; hr = (hr + 1) % 12; }
        String next = NUM[(hr + 1) % 12], cur = NUM[hr % 12];
        switch (m) {
            case 0: return "It's\n" + cur + "\no'clock";
            case 5: return "It's five\npast\n" + cur;
            case 10: return "It's ten\npast\n" + cur;
            case 15: return "It's quarter\npast\n" + cur;
            case 20: return "It's twenty\npast\n" + cur;
            case 25: return "It's twenty-five\npast\n" + cur;
            case 30: return "It's half\npast\n" + cur;
            case 35: return "It's twenty-five\nto\n" + next;
            case 40: return "It's twenty\nto\n" + next;
            case 45: return "It's quarter\nto\n" + next;
            case 50: return "It's ten\nto\n" + next;
            default: return "It's five\nto\n" + next;
        }
    }

    static void word(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, pad = 8 * x.u;
        String[] lines = words(Calendar.getInstance()).split("\n");
        TextPaint dim = x.p(9f, Theme.alpha(t.frameText, 0.45f), 400);
        TextPaint hi = x.p(9f, t.frameText, 800);
        float lh = Math.min((h - 2 * pad) / 3f, w * 0.13f);
        dim.setTextSize(lh * 0.72f * x.scale);
        hi.setTextSize(lh * 0.72f * x.scale);
        float y = (h - lh * 3) / 2f;
        for (int i = 0; i < 3 && i < lines.length; i++) {
            TextPaint p = i == 2 || (i == 0 && lines[0].length() > 4) ? hi : dim;
            if (i == 1) p = dim;
            String s = x.ell(lines[i], p, w - 2 * pad);
            cv.drawText(s, x.alignX(p, s, pad, w - pad), x.mid(p, y + lh * (i + 0.5f)), p);
        }
    }

    /** Next refresh for the word clock (every 5 minutes). */
    static long nextWordBoundary(long now) {
        long period = 300000, off = 150000; // words change at mm:02:30, mm:07:30, ...
        return ((now - off) / period + 1) * period + off + 2000;
    }

    static List<TV> none() { return new ArrayList<>(); }
}
