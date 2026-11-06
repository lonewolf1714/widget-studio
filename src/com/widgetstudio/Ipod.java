package com.widgetstudio;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.util.Locale;

/** Click-wheel iPod body shared by the music player and the games widget. */
final class Ipod {
    static final class Geo {
        RectF body, bezel, screen;
        float cx, cy, R, rc, m;
        boolean vertical;
    }

    static Geo geo(Render.Ctx x) {
        float w = x.w, h = x.h;
        String o = x.cfg.s("orient", "auto");
        boolean fill = x.cfg.b("fill", false);
        boolean v = fill ? h >= w * 0.75f : o.equals("vertical") || (o.equals("auto") && h > w * 1.05f);
        Geo g = new Geo();
        g.vertical = v;
        float bw, bh;
        if (fill) {
            bw = w;
            bh = h;
        } else if (!v) {
            float a = 2.15f;
            if (w / h > a) { bh = h; bw = h * a; } else { bw = w; bh = w / a; }
        } else {
            float a = 0.6f;
            if (w / h > a) { bh = h; bw = h * a; } else { bw = w; bh = w / a; }
        }
        float l = (w - bw) / 2f, t = (h - bh) / 2f;
        g.body = new RectF(l, t, l + bw, t + bh);
        g.m = Math.min(bw, bh);
        float p = g.m * 0.075f;
        if (!v) {
            g.bezel = new RectF(l + p, t + p, l + bw * 0.535f, t + bh - p);
            float area = (l + bw - p) - g.bezel.right;
            g.R = Math.min(area - p * 0.5f, bh - 2 * p) / 2f;
            g.cx = g.bezel.right + area / 2f;
            g.cy = t + bh / 2f;
        } else {
            g.bezel = new RectF(l + p, t + p, l + bw - p, t + bh * 0.47f);
            float area = (t + bh - p) - g.bezel.bottom;
            g.R = Math.min(bw - 2 * p, area - p * 0.5f) / 2f;
            g.cx = l + bw / 2f;
            g.cy = g.bezel.bottom + area / 2f;
        }
        float bz = g.m * 0.02f;
        g.screen = new RectF(g.bezel.left + bz, g.bezel.top + bz, g.bezel.right - bz, g.bezel.bottom - bz);
        g.rc = g.R * 0.36f;
        return g;
    }

    static TextPaint tpx(Render.Ctx x, String font, float px, int color, int weight) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
        Fonts.apply(p, x.c, font, weight);
        p.setTextSize(Math.max(1, px));
        p.setColor(color);
        return p;
    }

    static boolean light(Render.Ctx x) {
        return x.cfg.s("screen", "dark").equals("light");
    }

    static void body(Render.Ctx x, Canvas cv, Geo g, Theme.Pod pod) {
        float m = g.m;
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setShader(new LinearGradient(0, g.body.top, 0, g.body.bottom, pod.body, pod.bodyDark, Shader.TileMode.CLAMP));
        float br = m * 0.15f;
        cv.drawRoundRect(g.body, br, br, bp);
        RectF in = new RectF(g.body);
        float e = m * 0.006f;
        in.inset(e, e);
        cv.drawRoundRect(in, br, br, Icons.stroke(Theme.alpha(pod.edge, 0.55f), e * 1.6f));
        // glossy specular sheen (stronger on dark finishes), plus a crisp reflection edge on jet black
        boolean dark = Theme.luminance(pod.body) < 90;
        boolean glossy = x.gloss;
        Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
        gloss.setShader(new LinearGradient(g.body.left, g.body.top, g.body.left + g.body.width() * 0.55f,
                g.body.top + g.body.height() * 0.9f, dark ? 0x30FFFFFF : 0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        if (glossy) cv.drawRoundRect(g.body, br, br, gloss);
        if (glossy && pod.key.equals("black")) {
            cv.save();
            Path clipB = new Path();
            clipB.addRoundRect(g.body, br, br, Path.Direction.CW);
            cv.clipPath(clipB);
            Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
            float bx = g.body.left + g.body.width() * 0.62f;
            band.setShader(new LinearGradient(bx, g.body.top, bx + g.body.width() * 0.12f, g.body.bottom,
                    new int[]{0x00FFFFFF, 0x14FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            Path p = new Path();
            p.moveTo(bx, g.body.top);
            p.lineTo(bx + g.body.width() * 0.16f, g.body.top);
            p.lineTo(bx - g.body.width() * 0.05f, g.body.bottom);
            p.lineTo(bx - g.body.width() * 0.21f, g.body.bottom);
            p.close();
            cv.drawPath(p, band);
            cv.restore();
        }
        // soft top highlight
        Paint hl = Icons.stroke(dark ? 0x40FFFFFF : 0x55FFFFFF, e * 1.2f);
        if (glossy) cv.drawLine(g.body.left + br, g.body.top + e * 2.5f, g.body.right - br, g.body.top + e * 2.5f, hl);

        boolean lt0 = light(x);
        x.rr(cv, g.bezel.left, g.bezel.top, g.bezel.right, g.bezel.bottom, m * 0.035f, Theme.ov(x.cfg, "p_bezel", 0xFF070707));
        int s1 = Theme.ov(x.cfg, "p_screen1", Theme.podPart(pod, lt0, "p_screen1"));
        int s2 = Theme.ov(x.cfg, "p_screen2", Theme.podPart(pod, lt0, "p_screen2"));
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (light(x)) {
            sp.setShader(new LinearGradient(0, g.screen.top, 0, g.screen.bottom, s1, s2, Shader.TileMode.CLAMP));
        } else {
            sp.setShader(new LinearGradient(g.screen.left, 0, g.screen.right, 0, s1, s2, Shader.TileMode.CLAMP));
        }
        cv.drawRoundRect(g.screen, m * 0.022f, m * 0.022f, sp);

        // wheel with a slight drop shadow and radial sheen
        cv.drawCircle(g.cx, g.cy + m * 0.006f, g.R + m * 0.004f, Icons.fill(Theme.alpha(0xFF000000, 0.12f)));
        Paint wp = new Paint(Paint.ANTI_ALIAS_FLAG);
        wp.setShader(new RadialGradient(g.cx, g.cy - g.R * 0.4f, g.R * 1.4f, pod.wheel,
                Theme.alpha(pod.wheel, 1f) & 0xFFF0F0F0 | (pod.wheel & 0xFF000000), Shader.TileMode.CLAMP));
        cv.drawCircle(g.cx, g.cy, g.R, wp);
        cv.drawCircle(g.cx, g.cy, g.R, Icons.stroke(Theme.alpha(pod.edge, 0.35f), m * 0.004f));
        Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
        cp.setShader(new LinearGradient(0, g.cy - g.rc, 0, g.cy + g.rc, pod.center, pod.wheel, Shader.TileMode.CLAMP));
        cv.drawCircle(g.cx, g.cy, g.rc, cp);
        cv.drawCircle(g.cx, g.cy, g.rc, Icons.stroke(Theme.alpha(pod.edge, 0.45f), m * 0.004f));

        TextPaint mp = tpx(x, "sans_medium", g.R * 0.17f, pod.label2, 500);
        mp.setLetterSpacing(0.04f);
        String menu = "MENU";
        cv.drawText(menu, g.cx - mp.measureText(menu) / 2, x.mid(mp, g.cy - g.R * 0.68f), mp);
        Paint ic = Icons.fill(pod.label2);
        float s = g.R * 0.26f;
        Icons.skip(cv, g.cx - g.R * 0.68f, g.cy, s, ic, -1);
        Icons.skip(cv, g.cx + g.R * 0.68f, g.cy, s, ic, 1);
        Icons.playPause(cv, g.cx, g.cy + g.R * 0.68f, s, ic);
        // (volume spots on the upper ring stay tappable but are not drawn)
    }

    // ------------------------------------------------------------------ modern (matte, minimal) style
    static boolean modern(Render.Ctx x) {
        return x.cfg.s("podStyle", "modern").equals("modern");
    }

    /** Flat matte body, deep bezel, flat recessed wheel with small glyphs. */
    static void bodyModern(Render.Ctx x, Canvas cv, Geo g, Theme.Pod pod) {
        float m = g.m, br = m * 0.12f;
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setShader(new LinearGradient(0, g.body.top, 0, g.body.bottom, Render.Ctx.lighten(pod.body, 0.02f),
                Render.Ctx.darken(pod.body, 0.04f), Shader.TileMode.CLAMP));
        cv.drawRoundRect(g.body, br, br, bp);
        RectF e = new RectF(g.body);
        float ew = Math.max(1f, m * 0.003f);
        e.inset(ew / 2, ew / 2);
        cv.drawRoundRect(e, br, br, Icons.stroke(Theme.alpha(0xFFFFFFFF, 0.05f), ew));
        if (x.gloss) {
            Paint gl = new Paint(Paint.ANTI_ALIAS_FLAG);
            gl.setShader(new LinearGradient(g.body.left, g.body.top, g.body.left + g.body.width() * 0.6f, g.body.bottom,
                    0x24FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            cv.drawRoundRect(g.body, br, br, gl);
        }

        float bzr = m * 0.06f;
        x.rr(cv, g.bezel.left, g.bezel.top, g.bezel.right, g.bezel.bottom, bzr, Theme.ov(x.cfg, "p_bezel", 0xFF050506));

        float R = g.R, rc = g.rc;
        cv.drawCircle(g.cx, g.cy, R, Icons.fill(pod.wheel));
        cv.drawCircle(g.cx, g.cy, R, Icons.stroke(Theme.alpha(0xFF000000, 0.18f), Math.max(1f, m * 0.003f)));
        cv.drawCircle(g.cx, g.cy, rc, Icons.fill(pod.center));
        if (x.gloss) {
            Paint sat = new Paint(Paint.ANTI_ALIAS_FLAG);
            sat.setShader(new RadialGradient(g.cx - R * 0.3f, g.cy - R * 0.5f, R * 1.1f, 0x1FFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            cv.drawCircle(g.cx, g.cy, R, sat);
        }
        cv.drawCircle(g.cx, g.cy, rc, Icons.stroke(Theme.alpha(0xFF000000, 0.22f), Math.max(1f, m * 0.003f)));

        Paint ic = Icons.fill(Theme.alpha(pod.label2, 0.92f));
        float s = R * 0.13f;
        // top: small back/menu triangle, sides: double arrows, bottom: play/pause
        Path tri = new Path();
        float tx = g.cx, ty = g.cy - R * 0.7f;
        tri.moveTo(tx - s * 0.45f, ty);
        tri.lineTo(tx + s * 0.35f, ty - s * 0.42f);
        tri.lineTo(tx + s * 0.35f, ty + s * 0.42f);
        tri.close();
        cv.drawPath(tri, ic);
        dbl(cv, g.cx - R * 0.7f, g.cy, s, ic, -1);
        dbl(cv, g.cx + R * 0.7f, g.cy, s, ic, 1);
        Icons.playPause(cv, g.cx, g.cy + R * 0.7f, s * 1.5f, ic);
    }

    /** ◀◀ or ▶▶ */
    static void dbl(Canvas cv, float cx, float cy, float s, Paint p, int dir) {
        for (int k = 0; k < 2; k++) {
            float ox = cx + dir * (k - 0.5f) * s * 0.75f;
            Path t = new Path();
            t.moveTo(ox + dir * s * 0.4f, cy);
            t.lineTo(ox - dir * s * 0.35f, cy - s * 0.42f);
            t.lineTo(ox - dir * s * 0.35f, cy + s * 0.42f);
            t.close();
            cv.drawPath(t, p);
        }
    }

    private static Bitmap blurSrc, blurOut;

    /** Smooth multi-step blur of the artwork, cached per artwork bitmap. */
    static synchronized Bitmap blurred(Bitmap art) {
        if (art == blurSrc && blurOut != null) return blurOut;
        Bitmap soft = Bitmap.createScaledBitmap(art, 8, 8, true);
        for (int sz = 16; sz <= 256; sz *= 2) soft = Bitmap.createScaledBitmap(soft, sz, sz, true);
        blurSrc = art;
        blurOut = soft;
        return soft;
    }

    static void musicModern(Render.Ctx x, Canvas cv, Render.Out out, Geo g, Theme.Pod pod0, Theme.Pod pod) {
        bodyModern(x, cv, g, pod);
        RectF s = g.screen;
        float sr = g.m * 0.05f;
        MusicService.Now n = MusicService.now();
        boolean access = MusicService.hasAccess(x.c);
        int fg = Theme.ov(x.cfg, "p_title", 0xFFFFFFFF);
        int sub = Theme.ov(x.cfg, "p_artist", 0xB3FFFFFF);

        cv.save();
        Path clip = new Path();
        clip.addRoundRect(s, sr, sr, Path.Direction.CW);
        cv.clipPath(clip);
        // background: soft blur of the artwork, dimmed (or a deep gradient)
        int s1 = Theme.ov(x.cfg, "p_screen1", 0xFF1B2430), s2 = Theme.ov(x.cfg, "p_screen2", 0xFF0B0F14);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new LinearGradient(s.left, s.top, s.right, s.bottom, s1, s2, Shader.TileMode.CLAMP));
        cv.drawRect(s, bg);
        if (n != null && n.art != null) {
            Bitmap soft = blurred(n.art);
            cv.drawBitmap(soft, null, s, new Paint(Paint.FILTER_BITMAP_FLAG));
            cv.drawRect(s, Icons.fill(0x8C000000));
        }

        float pad = s.width() * 0.055f;
        // status bar: live clock (TextClock view) on the left, signal / wifi / battery on the right
        float barH = s.height() * 0.15f;
        float barY = s.top + pad * 0.55f + barH / 2f;
        TextPaint clk = tpx(x, "inter", barH * 0.62f, fg, 600);
        if (x.preview) {
            String t = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date());
            cv.drawText(t, s.left + pad * 1.1f, x.mid(clk, barY), clk);
        } else {
            out.clock = true;
            out.cx = s.left + pad * 1.1f;
            out.cy = barY - (clk.descent() - clk.ascent()) / 2f;
            out.csize = clk.getTextSize();
            out.ccolor = fg;
        }

        // artwork + title / artist
        float contentTop = s.top + pad * 0.55f + barH + pad * 0.35f;
        float bottomRow = s.height() * 0.17f;
        float lineY = s.bottom - pad * 0.5f - bottomRow - pad * 0.45f;
        float art = Math.min(lineY - pad * 0.7f - contentTop, s.width() * 0.4f);
        RectF ar = new RectF(s.left + pad, contentTop, s.left + pad + art, contentTop + art);
        float arR = art * 0.08f;
        if (n != null && n.art != null) {
            cv.save();
            Path ac = new Path();
            ac.addRoundRect(ar, arR, arR, Path.Direction.CW);
            cv.clipPath(ac);
            cv.drawBitmap(n.art, null, ar, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
            cv.restore();
        } else {
            x.rr(cv, ar.left, ar.top, ar.right, ar.bottom, arR, 0x33FFFFFF);
            note(cv, ar.centerX(), ar.centerY(), art * 0.42f, 0x80FFFFFF);
        }
        String title, artist;
        if (n != null) { title = n.title; artist = n.artist; }
        else if (access) { title = "Not playing"; artist = "Tap ▶❚❚ to play"; }
        else { title = "Allow music access"; artist = "Tap the centre button"; }
        float tx = ar.right + pad * 0.8f, tw = s.right - pad - tx;
        TextPaint tp = tpx(x, x.font, s.height() * 0.11f * x.scale, fg, 600);
        fitWords(tp, title, tw);
        StaticLayout tl = lines(title, tp, tw, 2);
        TextPaint ap = tpx(x, x.font, s.height() * 0.095f * x.scale, sub, 400);
        fitWords(ap, artist == null ? "" : artist, tw);
        StaticLayout al = lines(artist == null ? "" : artist, ap, tw, 1);
        float block = tl.getHeight() + al.getHeight();
        cv.save();
        cv.translate(tx, ar.centerY() - block / 2f);
        tl.draw(cv);
        cv.translate(0, tl.getHeight());
        al.draw(cv);
        cv.restore();

        // hairline progress
        long dur = n != null ? n.duration : 0, pos = n != null ? n.positionNow() : 0;
        float f = dur > 0 ? Math.min(1f, pos / (float) dur) : 0;
        float lw = Math.max(1.5f, s.height() * 0.012f);
        RectF track = new RectF(s.left + pad, lineY - lw / 2, s.right - pad, lineY + lw / 2);
        x.rr(cv, track.left, track.top, track.right, track.bottom, lw / 2, Theme.ov(x.cfg, "p_track", 0x40FFFFFF));
        if (f > 0) x.rr(cv, track.left, track.top, track.left + track.width() * f, track.bottom, lw / 2,
                Theme.ov(x.cfg, "p_bar", 0xF2FFFFFF));

        // bottom row: play / pause state on the left, dots in the middle
        float rowY = s.bottom - pad * 0.5f - bottomRow / 2f;
        Paint gp = Icons.fill(fg);
        float gs = bottomRow * 0.45f;
        if (n != null && n.playing) {
            float bw = gs * 0.18f;
            cv.drawRect(s.left + pad, rowY - gs * 0.4f, s.left + pad + bw, rowY + gs * 0.4f, gp);
            cv.drawRect(s.left + pad + bw * 2.2f, rowY - gs * 0.4f, s.left + pad + bw * 3.2f, rowY + gs * 0.4f, gp);
        } else {
            Icons.play(cv, s.left + pad + gs * 0.35f, rowY, gs * 0.9f, gp);
        }
        float dr = Math.max(1.5f, gs * 0.1f);
        for (int k = -1; k <= 1; k++) cv.drawCircle(s.centerX() + k * dr * 3.4f, rowY, dr, gp);
        volumeOverlay(x, cv, s);
        cv.restore();
        wheelZones(g, out);
    }

    /** Signal bars, Wi-Fi arcs and a battery (real level) for the iPod status bar. */
    static void statusIcons(android.content.Context c, Canvas cv, float right, float cy, float h, int color) {
        Paint p = Icons.fill(color);
        int level = 80;
        try {
            android.os.BatteryManager bm = c.getSystemService(android.os.BatteryManager.class);
            if (bm != null) level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } catch (Exception ignored) { }
        float bw = h * 2.1f, bh = h * 1.0f;
        RectF b = new RectF(right - bw, cy - bh / 2, right, cy + bh / 2);
        cv.drawRoundRect(b, bh * 0.28f, bh * 0.28f, Icons.stroke(Theme.alpha(color, 0.55f), Math.max(1f, h * 0.1f)));
        RectF fill = new RectF(b);
        fill.inset(h * 0.16f, h * 0.16f);
        fill.right = fill.left + fill.width() * Math.max(0.08f, Math.min(1f, level / 100f));
        cv.drawRoundRect(fill, bh * 0.16f, bh * 0.16f, p);
        cv.drawRect(b.right + h * 0.06f, cy - bh * 0.18f, b.right + h * 0.16f, cy + bh * 0.18f, Icons.fill(Theme.alpha(color, 0.55f)));
        // wifi
        float wx = b.left - h * 1.75f;
        Paint arc = Icons.stroke(color, Math.max(1f, h * 0.16f));
        for (int i = 1; i <= 3; i++) {
            float r = h * 0.32f * i;
            cv.drawArc(new RectF(wx - r, cy + h * 0.42f - r, wx + r, cy + h * 0.42f + r), 225, 90, false, arc);
        }
        // signal
        float sx = wx - h * 2.6f;
        for (int i = 0; i < 4; i++) {
            float bh2 = h * (0.3f + 0.22f * i);
            cv.drawRect(sx + i * h * 0.3f, cy + h * 0.45f - bh2, sx + i * h * 0.3f + h * 0.2f, cy + h * 0.45f, p);
        }
    }

    static void wheelZones(Geo g, Render.Out out) {
        float R = g.R, cx = g.cx, cy = g.cy, k = 0.36f;
        out.zones.add(new Render.Zone(new RectF(cx - R * k, cy - R, cx + R * k, cy - R * 0.42f), "ipod:menu"));
        out.zones.add(new Render.Zone(new RectF(cx - R, cy - R * k, cx - R * 0.42f, cy + R * k), "ipod:prev"));
        out.zones.add(new Render.Zone(new RectF(cx + R * 0.42f, cy - R * k, cx + R, cy + R * k), "ipod:next"));
        out.zones.add(new Render.Zone(new RectF(cx - R * k, cy + R * 0.42f, cx + R * k, cy + R), "ipod:play"));
        out.zones.add(new Render.Zone(new RectF(cx - g.rc, cy - g.rc, cx + g.rc, cy + g.rc), "ipod:center"));
        float d = R * 0.68f * 0.7071f, z = R * 0.24f;
        out.zones.add(new Render.Zone(new RectF(cx - d - z, cy - d - z, cx - d + z, cy - d + z), "ipod:voldown"));
        out.zones.add(new Render.Zone(new RectF(cx + d - z, cy - d - z, cx + d + z, cy - d + z), "ipod:volup"));
    }

    static StaticLayout lines(CharSequence s, TextPaint p, float width, int max) {
        return StaticLayout.Builder.obtain(s, 0, s.length(), p, Math.max(1, (int) width))
                .setMaxLines(max).setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false)
                .setLineSpacing(0, 0.95f).setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
    }

    /** Shrinks the paint until the longest single word fits the width (avoids mid-word breaks). */
    static void fitWords(TextPaint p, String text, float width) {
        float longest = 0;
        for (String wd : text.split(" ")) longest = Math.max(longest, p.measureText(wd));
        if (longest > width && longest > 0) p.setTextSize(Math.max(8f, p.getTextSize() * width / longest * 0.98f));
    }

    static String mmss(long ms) {
        long s = Math.max(0, ms) / 1000;
        if (s >= 3600) return String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    // ------------------------------------------------------------------ music
    /** Volume overlay shown while the wheel is being turned in Cover mode. */
    static volatile long VOL_UNTIL = 0;
    static volatile float VOL_LEVEL = 0;

    static void volumeOverlay(Render.Ctx x, Canvas cv, RectF s) {
        if (System.currentTimeMillis() > VOL_UNTIL) return;
        float w = s.width() * 0.62f, hh = s.height() * 0.2f;
        RectF p = new RectF(s.centerX() - w / 2, s.centerY() - hh / 2, s.centerX() + w / 2, s.centerY() + hh / 2);
        x.rr(cv, p.left, p.top, p.right, p.bottom, hh / 2, 0xCC111214);
        float ic = hh * 0.42f, ix = p.left + hh * 0.55f, cy = p.centerY();
        Paint wpt = Icons.fill(0xFFFFFFFF);
        cv.drawRect(ix - ic * 0.5f, cy - ic * 0.22f, ix - ic * 0.15f, cy + ic * 0.22f, wpt);
        Path sp = new Path();
        sp.moveTo(ix - ic * 0.15f, cy - ic * 0.22f);
        sp.lineTo(ix + ic * 0.25f, cy - ic * 0.5f);
        sp.lineTo(ix + ic * 0.25f, cy + ic * 0.5f);
        sp.lineTo(ix - ic * 0.15f, cy + ic * 0.22f);
        sp.close();
        cv.drawPath(sp, wpt);
        float bl = ix + ic * 0.75f, br = p.right - hh * 0.5f, bh = Math.max(2f, hh * 0.12f);
        x.rr(cv, bl, cy - bh / 2, br, cy + bh / 2, bh / 2, 0x40FFFFFF);
        x.rr(cv, bl, cy - bh / 2, bl + (br - bl) * Math.max(0f, Math.min(1f, VOL_LEVEL)), cy + bh / 2, bh / 2, 0xFFFFFFFF);
    }

    static void music(Render.Ctx x, Canvas cv, Render.Out out) {
        Geo g = geo(x);
        out.wheel = true;
        out.wcx = g.cx;
        out.wcy = g.cy;
        out.wR = g.R;
        out.wRc = g.rc;
        Theme.Pod pod0 = Theme.pod(x.cfg.s("ipod", "silver"));
        Theme.Pod pod = Theme.podWith(pod0, x.cfg);
        if (modern(x)) {
            musicModern(x, cv, out, g, pod0, pod);
            return;
        }
        body(x, cv, g, pod);
        boolean lt = light(x);
        int fg = Theme.ov(x.cfg, "p_title", Theme.podPart(pod0, lt, "p_title"));
        int sub = Theme.ov(x.cfg, "p_artist", Theme.podPart(pod0, lt, "p_artist"));
        RectF s = g.screen;
        float sp = s.height() * 0.075f;
        MusicService.Now n = MusicService.now();
        boolean access = MusicService.hasAccess(x.c);

        float rowH = s.height() * 0.15f;
        float rowY = s.bottom - sp - rowH / 2;
        float artTop = s.top + sp, artBottom = rowY - rowH / 2 - sp * 0.7f;
        float art = Math.min(artBottom - artTop, s.width() * 0.46f);
        RectF ar = new RectF(s.left + sp, artTop, s.left + sp + art, artTop + art);
        float arR = art * 0.07f;
        if (n != null && n.art != null) {
            cv.save();
            Path clip = new Path();
            clip.addRoundRect(ar, arR, arR, Path.Direction.CW);
            cv.clipPath(clip);
            Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            cv.drawBitmap(n.art, null, ar, bp);
            cv.restore();
        } else {
            x.rr(cv, ar.left, ar.top, ar.right, ar.bottom, arR, lt ? 0xFFB4C2CD : 0xFF2A2A2A);
            note(cv, ar.centerX(), ar.centerY(), art * 0.42f, lt ? 0xFF7F909D : 0xFF6A6A6A);
        }

        float tx = ar.right + sp, tw = s.right - sp - tx;
        String title, artist;
        if (n != null) {
            title = n.title;
            artist = n.artist;
        } else if (access) {
            title = "Not playing";
            artist = "Press ▶❚❚ to start music";
        } else {
            title = "Allow music access";
            artist = "Tap the centre button";
        }
        TextPaint tp = tpx(x, x.font, s.height() * 0.135f * x.scale, fg, 800);
        fitWords(tp, title, tw);
        StaticLayout tl = lines(title, tp, tw, 2);
        TextPaint ap = tpx(x, x.font, s.height() * 0.105f * x.scale, sub, 500);
        fitWords(ap, artist == null ? "" : artist, tw);
        StaticLayout al = lines(artist == null ? "" : artist, ap, tw, 2);
        float gap = s.height() * 0.03f;
        float blockH = tl.getHeight() + gap + al.getHeight();
        float ty = artTop + Math.max(0, (art - blockH) / 2f);
        cv.save();
        cv.translate(tx, ty);
        tl.draw(cv);
        cv.translate(0, tl.getHeight() + gap);
        al.draw(cv);
        cv.restore();

        TextPaint mono = tpx(x, "mono", rowH * 0.66f, fg, 500);
        long dur = n != null ? n.duration : 0;
        long pos = n != null ? n.positionNow() : 0;
        String durS = dur > 0 ? mmss(dur) : "0:00";
        float durW = mono.measureText(durS);
        float elW = mono.measureText(dur >= 3600000 ? "0:00:00" : "00:00");
        cv.drawText(durS, s.right - sp - durW, x.mid(mono, rowY), mono);
        float bl = s.left + sp + elW + sp * 0.7f, brr = s.right - sp - durW - sp * 0.7f, bh = rowH * 0.36f;
        x.rr(cv, bl, rowY - bh / 2, brr, rowY + bh / 2, bh * 0.3f, Theme.ov(x.cfg, "p_track", Theme.podPart(pod0, lt, "p_track")));
        float f = dur > 0 ? Math.min(1f, pos / (float) dur) : 0;
        if (f > 0) x.rr(cv, bl, rowY - bh / 2, bl + (brr - bl) * f, rowY + bh / 2, bh * 0.3f, Theme.ov(x.cfg, "p_bar", Theme.podPart(pod0, lt, "p_bar")));

        if (x.preview || n == null) {
            cv.drawText(mmss(pos), s.left + sp, x.mid(mono, rowY), mono);
        } else {
            out.musicChrono = true;
            out.mx = s.left + sp;
            out.my = rowY - (mono.descent() - mono.ascent()) / 2f;
            out.msize = mono.getTextSize();
            out.mcolor = fg;
            out.mbase = SystemClock.elapsedRealtime() - pos;
            out.mrunning = n.playing;
        }
        volumeOverlay(x, cv, g.screen);
        wheelZones(g, out);
    }

    static void note(Canvas cv, float cx, float cy, float s, int color) {
        Paint f = Icons.fill(color);
        cv.drawOval(new RectF(cx - s * 0.42f, cy + s * 0.12f, cx - s * 0.02f, cy + s * 0.42f), f);
        cv.drawRect(cx - s * 0.08f, cy - s * 0.42f, cx - s * 0.0f, cy + s * 0.28f, f);
        Path p = new Path();
        p.moveTo(cx - s * 0.04f, cy - s * 0.42f);
        p.lineTo(cx + s * 0.36f, cy - s * 0.26f);
        p.lineTo(cx + s * 0.36f, cy - s * 0.1f);
        p.lineTo(cx - s * 0.04f, cy - s * 0.24f);
        p.close();
        cv.drawPath(p, f);
    }

    // ------------------------------------------------------------------ games (themed card, no wheel)
    static void game(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, u = x.u;
        x.frame(cv);
        Games.S st = Games.load(x.c, x.cfg);
        int fg = t.frameText, sub = t.frameSub;
        float pad = 5.5f * u;
        // minimal text tabs: active = bright + accent dot, others = muted
        String[] names = {"Coin", "Dice", "Tic-Tac-Toe", "Lucky 7"};
        float tx = pad, tabY = pad + 5f * u, tabGap = 7f * u;
        float rbR = Math.max(3.4f * u, 4.5f * x.density) * 1.75f;
        // shrink the tab text until all four tabs fit beside the reset button
        float fs = 6f;
        while (fs > 3.6f) {
            TextPaint mp = x.p(fs, fg, 700);
            mp.setLetterSpacing(0.02f);
            float need = 0;
            for (String n : names) need += mp.measureText(n);
            need += tabGap * (fs / 6f) * (names.length - 1);
            if (pad + need <= w - pad - rbR * 2 - 2 * u) break;
            fs -= 0.2f;
        }
        tabGap *= fs / 6f;
        for (int i = 0; i < names.length; i++) {
            boolean sel = Games.ORDER[i].equals(st.game);
            TextPaint tp = x.p(fs, sel ? fg : Theme.alpha(sub, 0.85f), sel ? 700 : 500);
            tp.setLetterSpacing(0.02f);
            float tw = tp.measureText(names[i]);
            cv.drawText(names[i], tx, x.mid(tp, tabY), tp);
            if (sel) cv.drawCircle(tx + tw / 2, tabY + tp.getTextSize() * 0.9f, 0.95f * u, Icons.fill(x.accent));
            out.zones.add(new Render.Zone(new RectF(tx - 3 * u, 0, tx + tw + 3 * u, tabY + 9 * u), "ipod:tab:" + Games.ORDER[i]));
            tx += tw + tabGap;
        }
        if (w - pad - rbR * 2 > tx - tabGap) Lucky.resetButton(x, cv, w - pad - rbR, tabY, Theme.alpha(sub, 0.9f), out);
        RectF a = new RectF(pad, tabY + 9f * u, w - pad, h - pad);
        switch (st.game) {
            case "coin": coin(x, cv, a, st, fg, sub, out); break;
            case "dice": dice(x, cv, a, st, fg, sub, out); break;
            case "lucky": Lucky.body(x, cv, a, Lucky.load(x.c, x.cfg), true, out); break;
            default: ttt(x, cv, a, st, fg, sub, out);
        }
    }

    /** Info column: light headline, spaced small-caps stats, hairline ghost button. */
    static void info(Render.Ctx x, Canvas cv, RectF col, String head, String l1, String l2, String btn,
                     int fg, int sub, Render.Out out) {
        float u = x.u;
        TextPaint big = x.p(15f, fg, 300);
        big.setLetterSpacing(-0.01f);
        while (big.measureText(head) > col.width() && big.getTextSize() > 8) big.setTextSize(big.getTextSize() * 0.93f);
        TextPaint sm = x.p(4.2f, sub, 600);
        sm.setLetterSpacing(0.12f);
        float y = col.top;
        cv.drawText(x.ell(head, big, col.width()), col.left, x.top(big, y), big);
        y += big.getTextSize() * 1.12f;
        if (l1 != null) {
            String s = l1.toUpperCase(java.util.Locale.getDefault());
            cv.drawText(x.ell(s, sm, col.width()), col.left, x.top(sm, y), sm);
            y += sm.getTextSize() * 1.7f;
        }
        if (l2 != null) {
            String s = l2.toUpperCase(java.util.Locale.getDefault());
            cv.drawText(x.ell(s, sm, col.width()), col.left, x.top(sm, y), sm);
        }
        TextPaint bp = x.p(4f, x.accent, 700);
        bp.setLetterSpacing(0.16f);
        String bs = btn.toUpperCase(java.util.Locale.getDefault());
        float bh = Math.min(10f * u * x.scale, col.height() * 0.34f);
        float bw = Math.min(col.width(), bp.measureText(bs) + 12f * u);
        RectF b = new RectF(col.left, col.bottom - bh, col.left + bw, col.bottom);
        cv.drawRoundRect(b, bh / 2, bh / 2, Icons.fill(Theme.alpha(x.accent, 0.10f)));
        RectF bb = new RectF(b);
        float sw = Math.max(1f, x.density * 0.9f);
        bb.inset(sw / 2, sw / 2);
        cv.drawRoundRect(bb, bh / 2, bh / 2, Icons.stroke(Theme.alpha(x.accent, 0.85f), sw));
        cv.drawText(x.ell(bs, bp, bw - 4 * u), b.centerX() - Math.min(bp.measureText(bs), bw - 4 * u) / 2, x.mid(bp, b.centerY()), bp);
        RectF hit = new RectF(b);
        hit.inset(-2 * u, -2 * u);
        out.zones.add(new Render.Zone(hit, "ipod:action"));
    }

    /** Coin metals: {key, label}. */
    static final String[][] COINS = {
            {"gold", "Gold"}, {"silver", "Silver"}, {"rose", "Rose gold"}, {"bronze", "Bronze"},
            {"obsidian", "Obsidian"}, {"emerald", "Emerald"}, {"glass", "Glass"}, {"duo", "Gold / silver"}};

    /** {light, dark, ink} for a metal; "duo" = gold heads, silver tails. */
    static int[] metal(String style, boolean heads) {
        switch (style) {
            case "silver": return new int[]{0xFFF4F5F7, 0xFF8E949B, 0xCC33373C};
            case "rose": return new int[]{0xFFF9D9CE, 0xFFB46A57, 0xCC5C2A1E};
            case "bronze": return new int[]{0xFFEDB98A, 0xFF84491C, 0xCC3F2108};
            case "obsidian": return new int[]{0xFF55555B, 0xFF0E0E10, 0xE6E4E4EA};
            case "emerald": return new int[]{0xFF8BE6BC, 0xFF17704A, 0xCC0A3322};
            case "glass": return new int[]{0x80FFFFFF, 0x1AFFFFFF, 0xF2FFFFFF};
            case "duo": return heads ? metal("gold", true) : metal("silver", false);
            default: return new int[]{0xFFFBE39A, 0xFFB9861C, 0xCC5A3D05};
        }
    }

    static void coin(Render.Ctx x, Canvas cv, RectF a, Games.S st, int fg, int sub, Render.Out out) {
        float r = Math.min(a.height() * 0.47f, a.width() * 0.17f);
        float cx = a.centerX(), cy = a.centerY();
        boolean heads = st.coin != 1;
        float ph = Games.phase(x.cfg.id);
        boolean anim = ph >= 0;
        if (anim) heads = Games.animFrame % 2 == 0;
        String faces = x.cfg.s("coinFace", "letters");
        int[] k = metal(x.cfg.s("coinStyle", "gold"), heads);
        // ground shadow + body
        Paint sh = new Paint(Paint.ANTI_ALIAS_FLAG);
        sh.setShader(new RadialGradient(cx, cy + r * 0.95f, r * 1.05f, 0x55000000, 0x00000000, Shader.TileMode.CLAMP));
        cv.drawOval(new RectF(cx - r * 1.05f, cy + r * 0.72f, cx + r * 1.05f, cy + r * 1.18f), sh);
        cv.save();
        if (anim) {
            float lift = (float) Math.sin(ph * Math.PI) * r * 0.45f;
            float squash = Math.max(0.06f, Math.abs((float) Math.cos(ph * Math.PI * 5)));
            cv.translate(0, -lift);
            cv.scale(1f, squash, cx, cy);
        }
        cv.drawCircle(cx, cy + r * 0.07f, r, Icons.fill(Theme.alpha(k[1], 1f) & 0xFFFFFFFF));
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r, k[0], k[1], Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r, p);
        // milled edge ticks
        Paint tick = Icons.stroke(Theme.alpha(k[1], 0.55f), Math.max(1f, r * 0.012f));
        for (int i = 0; i < 72; i++) {
            double ang = i * Math.PI * 2 / 72;
            float c1 = (float) Math.cos(ang), s1 = (float) Math.sin(ang);
            cv.drawLine(cx + c1 * r * 0.9f, cy + s1 * r * 0.9f, cx + c1 * r * 0.97f, cy + s1 * r * 0.97f, tick);
        }
        float ir = r * 0.82f;
        Paint in = new Paint(Paint.ANTI_ALIAS_FLAG);
        in.setShader(new LinearGradient(cx + r, cy + r, cx - r, cy - r, k[0], k[1], Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, ir, in);

        Bitmap photo = null;
        if (faces.equals("photo")) {
            String path = heads ? x.cfg.s("photoH", "") : x.cfg.s("photoT", "");
            if (path.isEmpty() && !heads) path = null; // tails falls back to a letter
            if (path != null) photo = CoinPhoto.load(path);
        }
        if (photo != null) {
            cv.save();
            Path clip = new Path();
            clip.addCircle(cx, cy, ir * 0.96f, Path.Direction.CW);
            cv.clipPath(clip);
            cv.drawBitmap(photo, null, new RectF(cx - ir, cy - ir, cx + ir, cy + ir),
                    new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
            cv.restore();
        } else {
            String face;
            float fs = 0.95f;
            switch (faces) {
                case "rupee": face = st.coin < 0 ? "₹" : heads ? "₹" : "1"; break;
                case "yes_no": face = st.coin < 0 ? "?" : heads ? "YES" : "NO"; fs = 0.42f; break;
                default: face = st.coin < 0 ? "?" : heads ? "H" : "T";
            }
            TextPaint fp = tpx(x, faces.equals("yes_no") ? "cinzel" : "dm_serif", r * fs, k[2], 700);
            if (faces.equals("yes_no")) fp.setLetterSpacing(0.06f);
            cv.drawText(face, cx - fp.measureText(face) / 2, x.mid(fp, cy), fp);
        }
        cv.drawCircle(cx, cy, ir, Icons.stroke(Theme.alpha(k[1], 0.8f), r * 0.025f));
        Paint gl = new Paint(Paint.ANTI_ALIAS_FLAG);
        gl.setShader(new RadialGradient(cx - r * 0.42f, cy - r * 0.52f, r * 0.95f, 0x70FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, r, gl);
        cv.restore();
        out.zones.add(new Render.Zone(new RectF(cx - r, cy - r, cx + r, cy + r), "ipod:action"));

        String res;
        if (anim) res = "Flipping…";
        else if (st.coin < 0) res = "Ready";
        else if (faces.equals("yes_no")) res = heads ? "Yes" : "No";
        else res = heads ? "Heads" : "Tails";
        String stats = faces.equals("yes_no") ? "Yes " + st.heads + "  ·  No " + st.tails
                : "Heads " + st.heads + "  ·  Tails " + st.tails;
        sides(x, cv, a, cx - r, cx + r, res, stats, "Flip", fg, sub, out);
    }

    static void dice(Render.Ctx x, Canvas cv, RectF a, Games.S st, int fg, int sub, Render.Out out) {
        int count = Math.max(1, Math.min(2, x.cfg.i("dice", 2)));
        float size = Math.min(a.height() * 0.62f, a.width() * (count == 2 ? 0.14f : 0.2f));
        float depth = size * 0.3f, gap = size * 0.28f;
        float total = count * (size + depth) + (count - 1) * gap;
        float l0 = a.centerX() - total / 2f;
        float top = a.centerY() - (size + depth) / 2f + depth * 0.15f;
        int[] v = {st.d1, st.d2};
        float ph = Games.phase(x.cfg.id);
        boolean anim = ph >= 0;
        java.util.Random jr = new java.util.Random(Games.animFrame * 31L + 7);
        float l = l0;
        for (int i = 0; i < count; i++) {
            cv.save();
            if (anim) {
                v[i] = 1 + jr.nextInt(6);
                float cxd = l + (size + depth) / 2f, cyd = top + (size + depth) / 2f;
                float amp = 1f - ph;
                cv.translate((jr.nextFloat() - 0.5f) * size * 0.25f * amp, -(float) Math.abs(Math.sin(ph * Math.PI * 3)) * size * 0.35f * amp);
                cv.rotate((jr.nextFloat() - 0.5f) * 50f * amp, cxd, cyd);
            }
            die3d(x, cv, l, top, size, depth, v[i], i);
            cv.restore();
            l += size + depth + gap;
        }
        out.zones.add(new Render.Zone(new RectF(l0, top, l0 + total, top + size + depth), "ipod:action"));
        String res = anim ? "Rolling…" : st.d1 == 0 ? "Ready" : count == 2 ? "Total " + (st.d1 + st.d2) : "Rolled " + st.d1;
        sides(x, cv, a, l0, l0 + total, res, "Rolls  " + st.rolls, "Roll", fg, sub, out);
    }

    /** Pseudo-3D cube: lit top face, front face, shaded right face, all with pips. */
    static void die3d(Render.Ctx x, Canvas cv, float l, float t, float s, float d, int v, int seed) {
        // ground shadow
        Paint sh = new Paint(Paint.ANTI_ALIAS_FLAG);
        sh.setShader(new RadialGradient(l + (s + d) / 2f, t + d + s, (s + d) * 0.62f, 0x66000000, 0x00000000, Shader.TileMode.CLAMP));
        cv.drawOval(new RectF(l - d * 0.2f, t + d + s - s * 0.14f, l + s + d * 1.4f, t + d + s + s * 0.16f), sh);
        int front = v < 1 ? 1 + (seed * 3) % 6 : v;
        int topV = 0, rightV = 0;
        for (int c = 1; c <= 6 && topV == 0; c++) if (c != front && c != 7 - front) topV = c;
        for (int c = 6; c >= 1 && rightV == 0; c--) if (c != front && c != 7 - front && c != topV && c != 7 - topV) rightV = c;
        float[] fq = {l, t + d, l + s, t + d, l + s, t + d + s, l, t + d + s};
        float[] tq = {l + d, t, l + s + d, t, l + s, t + d, l, t + d};
        float[] rq = {l + s, t + d, l + s + d, t, l + s + d, t + s, l + s, t + d + s};
        face(cv, tq, topV, 0xFFFFFFFF, 0xFFE9ECEF, false);
        face(cv, rq, rightV, 0xFFC9CED4, 0xFFA8AEB6, false);
        face(cv, fq, v, 0xFFFFFFFF, 0xFFDCE0E5, v < 1);
        // edge highlights
        Paint e = Icons.stroke(0x99FFFFFF, Math.max(1f, s * 0.012f));
        cv.drawLine(l, t + d, l + s, t + d, e);
        cv.drawLine(l + s, t + d, l + s + d, t, e);
    }

    /** Draws one die face (unit square mapped onto quad q) with pips for value v. */
    static void face(Canvas cv, float[] q, int v, int c1, int c2, boolean question) {
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.setPolyToPoly(new float[]{0, 0, 1, 0, 1, 1, 0, 1}, 0, q, 0, 4);
        cv.save();
        cv.concat(m);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 1, 1, c1, c2, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(0, 0, 1, 1), 0.1f, 0.1f, p);
        Paint edge = Icons.stroke(0x22000000, 0.012f);
        cv.drawRoundRect(new RectF(0.006f, 0.006f, 0.994f, 0.994f), 0.1f, 0.1f, edge);
        if (question) {
            Paint qp = new Paint(Paint.ANTI_ALIAS_FLAG);
            qp.setColor(0xFF6B6F75);
            qp.setTextSize(0.6f);
            qp.setFakeBoldText(true);
            cv.drawText("?", 0.5f - qp.measureText("?") / 2, 0.71f, qp);
            cv.restore();
            return;
        }
        float lo = 0.27f, hi = 0.73f, mid = 0.5f, pr = 0.085f;
        float[][] pos;
        switch (v) {
            case 1: pos = new float[][]{{mid, mid}}; break;
            case 2: pos = new float[][]{{lo, lo}, {hi, hi}}; break;
            case 3: pos = new float[][]{{lo, lo}, {mid, mid}, {hi, hi}}; break;
            case 4: pos = new float[][]{{lo, lo}, {hi, lo}, {lo, hi}, {hi, hi}}; break;
            case 5: pos = new float[][]{{lo, lo}, {hi, lo}, {mid, mid}, {lo, hi}, {hi, hi}}; break;
            default: pos = new float[][]{{lo, lo}, {hi, lo}, {lo, mid}, {hi, mid}, {lo, hi}, {hi, hi}};
        }
        for (float[] o : pos) {
            float r = v == 1 ? pr * 1.55f : pr;
            Paint pip = new Paint(Paint.ANTI_ALIAS_FLAG);
            int col = v == 1 ? 0xFFC8102E : 0xFF1C1D20;
            pip.setShader(new RadialGradient(o[0] - r * 0.3f, o[1] - r * 0.3f, r * 1.3f, Render.Ctx.lighten(col, 0.25f), col, Shader.TileMode.CLAMP));
            cv.drawCircle(o[0], o[1], r, pip);
        }
        cv.restore();
    }

    /** Balanced layout around a centred object: result on the left, ghost button on the right. */
    static void sides(Render.Ctx x, Canvas cv, RectF a, float objL, float objR, String head, String l1, String btn,
                      int fg, int sub, Render.Out out) {
        float u = x.u, gap = 5 * u;
        float lw = objL - gap - a.left;
        TextPaint big = x.p(11f, fg, 300);
        big.setLetterSpacing(-0.01f);
        while (big.measureText(head) > lw && big.getTextSize() > 8) big.setTextSize(big.getTextSize() * 0.92f);
        TextPaint sm = x.p(3.4f, sub, 600);
        sm.setLetterSpacing(0.12f);
        String stats = l1.toUpperCase(java.util.Locale.getDefault());
        float bigH = big.getTextSize() * 0.78f, smH = sm.getTextSize() * 0.8f;
        float block = bigH + 2.6f * u + smH;
        float base = a.centerY() - block / 2f + bigH;
        cv.drawText(x.ell(head, big, lw), a.left, base, big);
        cv.drawText(x.ell(stats, sm, lw), a.left, base + 2.6f * u + smH, sm);

        TextPaint bp = x.p(4f, x.accent, 700);
        bp.setLetterSpacing(0.16f);
        String bs = btn.toUpperCase(java.util.Locale.getDefault());
        float rw = a.right - (objR + gap);
        float bh = Math.min(10f * u * x.scale, a.height() * 0.32f);
        float bw = Math.min(rw, bp.measureText(bs) + 12f * u);
        RectF b = new RectF(a.right - bw, a.centerY() - bh / 2, a.right, a.centerY() + bh / 2);
        ghost(x, cv, b, bs, bp);
        RectF hit = new RectF(b);
        hit.inset(-2 * u, -3 * u);
        out.zones.add(new Render.Zone(hit, "ipod:action"));
    }

    static void ghost(Render.Ctx x, Canvas cv, RectF b, String bs, TextPaint bp) {
        float bh = b.height();
        cv.drawRoundRect(b, bh / 2, bh / 2, Icons.fill(Theme.alpha(x.accent, 0.10f)));
        RectF bb = new RectF(b);
        float sw = Math.max(1f, x.density * 0.9f);
        bb.inset(sw / 2, sw / 2);
        cv.drawRoundRect(bb, bh / 2, bh / 2, Icons.stroke(Theme.alpha(x.accent, 0.85f), sw));
        String s = x.ell(bs, bp, b.width() - 4 * x.u);
        cv.drawText(s, b.centerX() - bp.measureText(s) / 2, x.mid(bp, b.centerY()), bp);
    }

    static void ttt(Render.Ctx x, Canvas cv, RectF a, Games.S st, int fg, int sub, Render.Out out) {
        // bigger board: nearly the full widget height, on the right; status column on the left
        float size = Math.min(x.h - 7f * x.u, a.width() * 0.6f);
        float l = a.right - size, t = (x.h - size) / 2f;
        float c = size / 3f;
        Paint line = Icons.stroke(Theme.alpha(x.t.frameSub, 0.35f), Math.max(1f, size * 0.007f));
        for (int i = 1; i < 3; i++) {
            cv.drawLine(l + c * i, t + c * 0.18f, l + c * i, t + size - c * 0.18f, line);
            cv.drawLine(l + c * 0.18f, t + c * i, l + size - c * 0.18f, t + c * i, line);
        }
        int xs = x.accent, os = x.t.frameText;
        int[] win = Games.winLine(st.board);
        for (int k = 0; k < 9; k++) {
            float cx = l + c * (k % 3 + 0.5f), cy = t + c * (k / 3 + 0.5f);
            char ch = st.board.charAt(k);
            float r = c * 0.24f;
            boolean hl = win != null && (win[0] == k || win[1] == k || win[2] == k);
            if (hl) x.rr(cv, cx - c * 0.4f, cy - c * 0.4f, cx + c * 0.4f, cy + c * 0.4f, c * 0.14f, Theme.alpha(x.accent, 0.12f));
            if (ch == 'X') {
                Paint p = Icons.stroke(xs, c * 0.055f);
                cv.drawLine(cx - r, cy - r, cx + r, cy + r, p);
                cv.drawLine(cx + r, cy - r, cx - r, cy + r, p);
            } else if (ch == 'O') {
                cv.drawCircle(cx, cy, r, Icons.stroke(os, c * 0.05f));
            }
            out.zones.add(new Render.Zone(new RectF(l + c * (k % 3), t + c * (k / 3), l + c * (k % 3 + 1), t + c * (k / 3 + 1)),
                    "ipod:cell:" + k));
        }
        String status = st.win == 1 ? "You win!" : st.win == 2 ? "Phone wins" : st.win == 3 ? "Draw" : "Your turn";
        RectF col = new RectF(a.left, a.top, l - 5 * x.u, a.bottom);
        info(x, cv, col, status, "You " + st.you + "  ·  Phone " + st.cpu, "Draws " + st.draws, "New game", fg, sub, out);
    }
}
