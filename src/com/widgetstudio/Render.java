package com.widgetstudio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/** Renders any widget type to a Bitmap plus tap zones and live-clock placement. */
final class Render {
    /** When true, data sources return sample content (used to make widget-picker previews). */
    static volatile boolean DEMO = false;
    /** <1 while drawing reduced-size animation frames, so strokes and corners keep their look. */
    static volatile float FRAME_SCALE = 1f;
    /** True while drawing a block inside a Custom Combo (smaller minimum text so blocks don't overflow). */
    static volatile boolean IN_BLOCK = false;

    /** A tappable area on the bitmap -> action sent to ActionReceiver (or a launch). */
    static final class Zone {
        final RectF r;
        final String action;
        Zone(RectF r, String action) { this.r = r; this.action = action; }
    }

    static final class Out {
        Bitmap bmp;
        float listTop, listPad; // app list: where the scrolling rows start (bitmap px)
        final List<Zone> zones = new ArrayList<>();
        float zoneScale = 1f; // bitmap px -> view px (supersampled renders)
        // counter live clock (bottom aligned)
        boolean chrono;
        long chronoBase;
        boolean countDown;
        float chronoSize;
        int chronoColor;
        int chronoPadBottom, chronoPadH;
        int align; // 0 left 1 center 2 right
        // music elapsed clock (absolute position)
        boolean musicChrono;
        float mx, my, msize;
        int mcolor;
        long mbase;
        boolean mrunning;
        // click wheel geometry (bitmap px) so Cover mode can track rotation
        boolean wheel;
        float wcx, wcy, wR, wRc;
        // clock widget: live TextClocks and an AnalogClock placed over the picture
        final List<Clocks.TV> tviews = new ArrayList<>();
        RectF analog;
        Bitmap hHand, mHand, sHand, cap;
        // live status-bar clock (TextClock view)
        boolean clock;
        float cx, cy, csize;
        int ccolor;
    }

    /** Shared drawing context for one render pass. */
    static final class Ctx {
        final Context c;
        final Cfg cfg;
        final int w, h;
        final Theme t;
        final int accent;
        final float u, scale, density, radius;
        final String font;
        final boolean card;
        final int frame;
        final boolean preview;
        final boolean gloss;

        Ctx(Context c, Cfg cfg, int w, int h, boolean preview) {
            this.c = c;
            this.cfg = cfg;
            this.w = w;
            this.h = h;
            this.preview = preview;
            gloss = cfg.s("finish", "matte").equals("gloss");
            t = Theme.get(cfg.s("theme", "slate")).with(cfg);
            accent = cfg.o.has("accent") ? cfg.i("accent", t.accent) : t.accent;
            u = Math.min(w / 2f, h) / 100f;
            scale = cfg.i("scale", 100) / 100f;
            density = c.getResources().getDisplayMetrics().density * FRAME_SCALE;
            radius = cfg.i("radius", 24) * density;
            font = cfg.s("font", "sans");
            card = cfg.b("bg", true);
            frame = Theme.alpha(t.frame, cfg.i("opacity", 100) / 100f);
        }

        /** Text paint, size in units (scaled by the text-size slider). */
        TextPaint p(float sizeU, int color, int weight) {
            return pf(font, sizeU, color, weight);
        }

        TextPaint pf(String fontKey, float sizeU, int color, int weight) {
            TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
            Fonts.apply(p, c, fontKey, weight);
            // never smaller than ~8.5dp so small labels stay readable
            p.setTextSize(Math.max((IN_BLOCK ? 6.5f : 8.5f) * density, sizeU * u * scale));
            p.setColor(color);
            if (card && t.glass) p.setShadowLayer(1.6f * u, 0, 0.4f * u, 0x59000000);
            else if (!card && cfg.b("shadow", false)) p.setShadowLayer(2.2f * u, 0, 0.6f * u, 0x88000000);
            return p;
        }

        /** Premium outer card: diagonal gradient, top-left sheen, frost (glass), hairline border. */
        void frame(Canvas cv) {
            if (!card) return;
            float op = cfg.i("opacity", 100) / 100f;
            RectF r = new RectF(0, 0, w, h);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new LinearGradient(0, 0, w * 0.4f, h, Theme.alpha(t.frame, op), Theme.alpha(t.frame2, op),
                    Shader.TileMode.CLAMP));
            cv.drawPath(squircle(r, radius), p);
            Paint sh = new Paint(Paint.ANTI_ALIAS_FLAG);
            sh.setShader(new RadialGradient(w * 0.12f, -h * 0.15f, Math.max(w, h) * 0.85f,
                    Theme.alpha(0xFFFFFFFF, (t.glass ? 0.20f : 0.07f) * op), 0x00FFFFFF, Shader.TileMode.CLAMP));
            if (gloss || t.glass) cv.drawPath(squircle(r, radius), sh);
            if (t.glass && !t.key.equals("liquid")) frost(cv, r, op);
            if (t.key.equals("liquid")) liquid(cv, r, op);
            if (t.glass) rim(cv, r, radius, (gloss ? 0.9f : 0.7f) * op);
            else if (gloss) rim(cv, r, radius, 0.35f * op);
            float bw = Math.max(1f, density * 0.9f);
            RectF b = new RectF(bw / 2, bw / 2, w - bw / 2, h - bw / 2);
            Paint st = new Paint(Paint.ANTI_ALIAS_FLAG);
            st.setStyle(Paint.Style.STROKE);
            st.setStrokeWidth(bw);
            st.setShader(new LinearGradient(0, 0, 0, h, Theme.alpha(t.border, op), Theme.alpha(t.border, op * 0.3f),
                    Shader.TileMode.CLAMP));
            cv.drawPath(squircle(b, Math.max(0, radius - bw / 2)), st);
        }

        /** Specular rim: light catches the top-left and bottom-right edges (iOS-style glass). */
        void rim(Canvas cv, RectF r, float rad, float strength) {
            float sw = Math.max(1.2f, density * 1.4f);
            RectF b = new RectF(r);
            b.inset(sw / 2, sw / 2);
            int hi = Theme.alpha(0xFFFFFFFF, Math.min(1f, strength));
            int lo = Theme.alpha(0xFFFFFFFF, strength * 0.12f);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(sw);
            android.graphics.SweepGradient g = new android.graphics.SweepGradient(r.centerX(), r.centerY(),
                    new int[]{lo, hi, lo, lo, hi, lo, lo}, new float[]{0f, 0.12f, 0.3f, 0.5f, 0.62f, 0.8f, 1f});
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setRotate(180f, r.centerX(), r.centerY());
            g.setLocalMatrix(m);
            p.setShader(g);
            cv.drawPath(squircle(b, Math.max(0, rad - sw / 2)), p);
        }

        /** Liquid glass body: soft inner edge glow + lens highlight across the top. */
        void liquid(Canvas cv, RectF r, float op) {
            cv.save();
            android.graphics.Path clip = new android.graphics.Path();
            clip.addRoundRect(r, radius, radius, android.graphics.Path.Direction.CW);
            cv.clipPath(clip);
            float gw = Math.max(6f, Math.min(w, h) * 0.08f);
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeWidth(gw);
            glow.setColor(Theme.alpha(0xFFFFFFFF, 0.22f * op));
            glow.setMaskFilter(new android.graphics.BlurMaskFilter(gw * 0.9f, android.graphics.BlurMaskFilter.Blur.NORMAL));
            cv.drawPath(squircle(r, radius), glow);
            Paint lens = new Paint(Paint.ANTI_ALIAS_FLAG);
            lens.setShader(new LinearGradient(0, 0, 0, h * 0.42f, Theme.alpha(0xFFFFFFFF, 0.16f * op), 0x00FFFFFF,
                    Shader.TileMode.CLAMP));
            cv.drawRect(0, 0, w, h * 0.42f, lens);
            Paint low = new Paint(Paint.ANTI_ALIAS_FLAG);
            low.setShader(new LinearGradient(0, h * 0.7f, 0, h, 0x00000000, Theme.alpha(0xFF000000, 0.12f * op),
                    Shader.TileMode.CLAMP));
            cv.drawRect(0, h * 0.7f, w, h, low);
            cv.restore();
        }

        /** Fine grain that makes translucent cards read as frosted glass. */
        void frost(Canvas cv, RectF r, float op) {
            cv.save();
            android.graphics.Path clip = new android.graphics.Path();
            clip.addRoundRect(r, radius, radius, android.graphics.Path.Direction.CW);
            cv.clipPath(clip);
            java.util.Random rnd = new java.util.Random(7);
            Paint light = Icons.fill(Theme.alpha(0xFFFFFFFF, 0.10f * op));
            Paint dark = Icons.fill(Theme.alpha(0xFF000000, 0.06f * op));
            int n = (int) (w * h / 700f);
            float s = Math.max(1f, density * 0.6f);
            for (int i = 0; i < n; i++) {
                float x = rnd.nextFloat() * w, y = rnd.nextFloat() * h;
                cv.drawRect(x, y, x + s, y + s, rnd.nextBoolean() ? light : dark);
            }
            cv.restore();
        }

        /** Premium inner panel: vertical gradient + soft highlight edge. */
        void panel(Canvas cv, float l, float tp, float r, float b, float rad) {
            RectF rc = new RectF(l, tp, r, b);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new LinearGradient(l, tp, l, b, t.panel, t.panel2, Shader.TileMode.CLAMP));
            cv.drawPath(squircle(rc, rad), p);
            float bw = Math.max(1f, density * 0.7f);
            rc.inset(bw / 2, bw / 2);
            Paint st = new Paint(Paint.ANTI_ALIAS_FLAG);
            st.setStyle(Paint.Style.STROKE);
            st.setStrokeWidth(bw);
            int edge = Theme.luminance(t.panel) > 150 ? 0x66FFFFFF : (t.glass ? 0x40FFFFFF : 0x24FFFFFF);
            st.setShader(new LinearGradient(l, tp, l, b, edge, edge & 0x00FFFFFF | 0x08000000, Shader.TileMode.CLAMP));
            cv.drawPath(squircle(rc, rad), st);
            if (t.glass) rim(cv, new RectF(l, tp, r, b), rad, 0.55f);
        }

        /** Accent pill with a subtle vertical gloss. */
        void pill(Canvas cv, float l, float tp, float r, float b, float rad, int color) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new LinearGradient(l, tp, l, b, gloss ? lighten(color, 0.12f) : color, color, Shader.TileMode.CLAMP));
            cv.drawPath(squircle(new RectF(l, tp, r, b), rad), p);
        }

        /** Continuous-curvature ("squircle") rounded rectangle, like Apple's corners. */
        static android.graphics.Path squircle(RectF r, float rad) {
            float half = Math.min(r.width(), r.height()) / 2f;
            float k = Math.min(rad * 1.28f, half);
            float hd = k * 0.64f;
            float l = r.left, t = r.top, ri = r.right, b = r.bottom;
            android.graphics.Path p = new android.graphics.Path();
            p.moveTo(l + k, t);
            p.lineTo(ri - k, t);
            p.cubicTo(ri - k + hd, t, ri, t + k - hd, ri, t + k);
            p.lineTo(ri, b - k);
            p.cubicTo(ri, b - k + hd, ri - k + hd, b, ri - k, b);
            p.lineTo(l + k, b);
            p.cubicTo(l + k - hd, b, l, b - k + hd, l, b - k);
            p.lineTo(l, t + k);
            p.cubicTo(l, t + k - hd, l + k - hd, t, l + k, t);
            p.close();
            return p;
        }

        static int darken(int c, float f) {
            int a = c >>> 24, rr = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
            rr = (int) (rr * (1 - f));
            g = (int) (g * (1 - f));
            b = (int) (b * (1 - f));
            return (a << 24) | (rr << 16) | (g << 8) | b;
        }

        static int lighten(int c, float f) {
            int a = c >>> 24, r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
            r += (int) ((255 - r) * f);
            g += (int) ((255 - g) * f);
            b += (int) ((255 - b) * f);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        void rr(Canvas cv, float l, float tp, float r, float b, float rad, int color) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color);
            cv.drawPath(squircle(new RectF(l, tp, r, b), rad), p);
        }

        String ell(String s, TextPaint p, float width) {
            if (width <= 0) return "";
            return TextUtils.ellipsize(s == null ? "" : s, p, width, TextUtils.TruncateAt.END).toString();
        }

        /** Baseline that vertically centres p's text on y. */
        float mid(TextPaint p, float y) {
            return y - (p.ascent() + p.descent()) / 2f;
        }

        /** Baseline for text whose top sits at y. */
        float top(TextPaint p, float y) {
            return y - p.ascent() * 0.86f;
        }

        float alignX(TextPaint p, String s, float left, float right) {
            int a = cfg.i("align", 0);
            float tw = p.measureText(s);
            if (a == 1) return (left + right - tw) / 2f;
            if (a == 2) return right - tw;
            return left;
        }

        Layout.Alignment alignment() {
            int a = cfg.i("align", 0);
            return a == 1 ? Layout.Alignment.ALIGN_CENTER : a == 2 ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;
        }

        StaticLayout layout(CharSequence s, TextPaint p, int width, float spacing, int maxLines) {
            StaticLayout.Builder b = StaticLayout.Builder.obtain(s, 0, s.length(), p, Math.max(1, width))
                    .setAlignment(alignment()).setLineSpacing(0, spacing).setIncludePad(false);
            if (maxLines > 0) b.setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END);
            return b.build();
        }
    }

    /** Renders with an Apple-style soft drop shadow when the widget has a card (or is an iPod). */
    static Out render(Context c, Cfg cfg, int w, int h, boolean preview) {
        float density = c.getResources().getDisplayMetrics().density * FRAME_SCALE;
        boolean shape = cfg.b("bg", true) || cfg.type.equals(Cfg.MUSIC);
        boolean depth = !IN_BLOCK && shape && cfg.b("depth", true) && !cfg.type.equals(Cfg.COMBO)
                && !cfg.b("fill", false) && w > 120 && h > 90;
        if (!depth) return renderRaw(c, cfg, w, h, preview);
        int m = Math.max(2, Math.round(6 * density));
        Out o = renderRaw(c, cfg, w - 2 * m, h - 2 * m, preview);
        Bitmap full = Bitmap.createBitmap(Math.max(64, w), Math.max(48, h), Bitmap.Config.ARGB_8888);
        Canvas fc = new Canvas(full);
        Bitmap alpha = o.bmp.extractAlpha();
        Paint amb = new Paint(Paint.ANTI_ALIAS_FLAG);
        amb.setColor(0x4D000000);
        amb.setMaskFilter(new android.graphics.BlurMaskFilter(5.5f * density, android.graphics.BlurMaskFilter.Blur.NORMAL));
        fc.drawBitmap(alpha, m, m + 2.6f * density, amb);
        Paint contact = new Paint(Paint.ANTI_ALIAS_FLAG);
        contact.setColor(0x2E000000);
        contact.setMaskFilter(new android.graphics.BlurMaskFilter(1.2f * density, android.graphics.BlurMaskFilter.Blur.NORMAL));
        fc.drawBitmap(alpha, m, m + 0.7f * density, contact);
        fc.drawBitmap(o.bmp, m, m, null);
        o.bmp = full;
        // everything positioned on the picture moves with it
        for (Zone z : o.zones) z.r.offset(m, m);
        for (Clocks.TV t : o.tviews) t.r.offset(m, m);
        if (o.analog != null) o.analog.offset(m, m);
        o.mx += m; o.my += m; o.cx += m; o.cy += m; o.wcx += m; o.wcy += m;
        o.chronoPadBottom += m;
        o.chronoPadH += m;
        return o;
    }

    static Out renderRaw(Context c, Cfg cfg, int w, int h, boolean preview) {
        if (w > 1600 || h > 1600) {
            float f = 1600f / Math.max(w, h);
            w = Math.round(w * f);
            h = Math.round(h * f);
        }
        w = Math.max(64, w);
        h = Math.max(48, h);
        Out out = new Out();
        out.bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out.bmp);
        Ctx x = new Ctx(c, cfg, w, h, preview);
        try {
            switch (cfg.type) {
                case Cfg.CAL: Widgets.cal(x, cv, out); break;
                case Cfg.EVENTS: Widgets.events(x, cv, out); break;
                case Cfg.STORAGE: Widgets.storage(x, cv, out); break;
                case Cfg.BIGDATE: Widgets.bigDate(x, cv, out); break;
                case Cfg.QUOTE: Widgets.quote(x, cv, out); break;
                case Cfg.COUNTER: Widgets.counter(x, cv, out); break;
                case Cfg.MUSIC: Ipod.music(x, cv, out); break;
                case Cfg.GAME: Ipod.game(x, cv, out); break;
                case Cfg.LUCKY: Lucky.draw(x, cv, out); break;
                case Cfg.APPLIST: AppList.draw(x, cv, out); break;
                case Cfg.DOTS: Widgets.dots(x, cv, out); break;
                case Cfg.COMBO: Combo.draw(x, cv, out); break;
                case Cfg.WEATHER: Widgets.weather(x, cv, out); break;
                case Cfg.PHOTO: Widgets.photo(x, cv, out); break;
                case Cfg.CLOCK: Clocks.draw(x, cv, out); break;
            }
        } catch (Throwable e) {
            TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0xFFFF6666);
            p.setTextSize(28);
            cv.drawText("Render error: " + e.getClass().getSimpleName(), 20, 40, p);
        }
        return out;
    }
}
