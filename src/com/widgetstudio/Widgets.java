package com.widgetstudio;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Drawing code for the card-style widgets. */
final class Widgets {
    static final long DAY = 86400000L;

    // ---------------------------------------------------------------- Calendar + Weather (Norfolk)
    static void cal(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, u = x.u;
        x.frame(cv);
        float pad = 3.4f * u, gap = 2.6f * u;
        float pr = Math.max(3 * u, x.radius - pad * 0.8f);
        boolean showW = x.cfg.b("showWeather", true);
        float leftW = showW ? w * 0.34f : 0;
        float headerH = h * 0.26f;
        float rx = pad + (showW ? leftW + gap : 0);
        if (showW) weatherTile(x, cv, pad, pad, pad + leftW, h - pad, pr);

        Calendar now = Calendar.getInstance();
        float hcy = (pad + headerH) / 2f + pad * 0.15f;
        float bx = rx + 1.5f * u;
        int[] bars = {0xFFA463F2, 0xFF35C2B0, x.accent};
        for (int i = 0; i < 3; i++) {
            Path p = new Path();
            float bw = 1.3f * u, bh = 5f * u, sk = 1.1f * u;
            float l = bx + i * 2.3f * u;
            p.moveTo(l + sk, hcy - bh / 2);
            p.lineTo(l + sk + bw, hcy - bh / 2);
            p.lineTo(l + bw, hcy + bh / 2);
            p.lineTo(l, hcy + bh / 2);
            p.close();
            cv.drawPath(p, Icons.fill(bars[i]));
        }
        TextPaint month = x.p(7.4f, t.frameText, 800);
        String mName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(now.getTime()).toUpperCase(Locale.getDefault());
        float mx = bx + 8.4f * u;
        TextPaint year = x.p(9.5f, t.frameSub, 800);
        String yr = String.valueOf(now.get(Calendar.YEAR));
        float yw = year.measureText(yr);
        cv.drawText(x.ell(mName, month, w - pad - 3 * u - yw - mx), mx, x.mid(month, hcy), month);
        cv.drawText(yr, w - pad - 1.5f * u - yw, x.mid(year, hcy), year);

        float pl = rx, pt = headerH + pad * 0.3f, pR = w - pad, pb = h - pad;
        x.panel(cv, pl, pt, pR, pb, pr);
        float ip = 4f * u, ph = pb - pt;
        int weekStart = x.cfg.i("weekStart", 1);
        Calendar k = (Calendar) now.clone();
        k.set(Calendar.HOUR_OF_DAY, 0);
        k.set(Calendar.MINUTE, 0);
        k.set(Calendar.SECOND, 0);
        k.set(Calendar.MILLISECOND, 0);
        int dow = k.get(Calendar.DAY_OF_WEEK);
        int back = weekStart == 1 ? (dow + 5) % 7 : dow - 1;
        k.add(Calendar.DAY_OF_MONTH, -back);
        boolean[] has = Events.week(x.c, k.getTimeInMillis());
        float colW = (pR - pl - 2 * ip) / 7f;
        TextPaint lab = x.p(3.8f, t.sub, 600);
        TextPaint num = x.p(6.4f, t.text, 500);
        float labY = pt + ph * 0.15f, numY = pt + ph * 0.38f;
        SimpleDateFormat df = new SimpleDateFormat("EEE", Locale.getDefault());
        int today = now.get(Calendar.DAY_OF_YEAR), ty = now.get(Calendar.YEAR);
        for (int i = 0; i < 7; i++) {
            float cx = pl + ip + colW * (i + 0.5f);
            String l = df.format(k.getTime()).toUpperCase(Locale.getDefault());
            if (l.length() > 3) l = l.substring(0, 3);
            cv.drawText(l, cx - lab.measureText(l) / 2, x.mid(lab, labY), lab);
            String n = String.valueOf(k.get(Calendar.DAY_OF_MONTH));
            boolean isToday = k.get(Calendar.DAY_OF_YEAR) == today && k.get(Calendar.YEAR) == ty;
            if (isToday) {
                cv.drawCircle(cx, numY, num.getTextSize() * 0.78f, Icons.fill(t.text));
                num.setColor(Theme.on(t.text));
            }
            cv.drawText(n, cx - num.measureText(n) / 2, x.mid(num, numY), num);
            num.setColor(t.text);
            if (has[i]) cv.drawCircle(cx + num.getTextSize() * 0.62f, numY + num.getTextSize() * 0.72f, 0.75f * u,
                    Icons.fill(t.sub));
            k.add(Calendar.DAY_OF_MONTH, 1);
        }
        float divY = pt + ph * 0.6f;
        cv.drawLine(pl + ip, divY, pR - ip, divY, Icons.stroke(Theme.alpha(t.sub, 0.5f), Math.max(1, 0.25f * u)));
        if (x.cfg.b("showEvent", true)) {
            TextPaint wl = x.p(3.8f, t.sub, 500);
            TextPaint tl = x.p(5.0f, t.text, 800);
            String whenS, title;
            if (!Events.allowed(x.c)) {
                whenS = "Calendar";
                title = "ALLOW ACCESS IN THE APP";
            } else {
                List<Events.Ev> ev = Events.upcoming(x.c, 30, 1);
                if (ev.isEmpty()) {
                    whenS = "Next 30 days";
                    title = "NO UPCOMING EVENTS";
                } else {
                    whenS = Events.when(ev.get(0));
                    title = ev.get(0).title.toUpperCase(Locale.getDefault());
                }
            }
            float ey = pt + ph * 0.72f;
            cv.drawCircle(pl + ip + 0.9f * u, ey, 0.9f * u, Icons.fill(t.sub));
            cv.drawText(x.ell(whenS, wl, pR - pl - 2 * ip - 3 * u), pl + ip + 3f * u, x.mid(wl, ey), wl);
            cv.drawText(x.ell(title, tl, pR - pl - 2 * ip), pl + ip, x.mid(tl, pt + ph * 0.86f), tl);
        }
    }

    static void weatherTile(Render.Ctx x, Canvas cv, float l, float tp, float r, float b, float rad) {
        Theme t = x.t;
        float u = x.u;
        x.panel(cv, l, tp, r, b, rad);
        float ip = 4.2f * u, tw = r - l - 2 * ip;
        Weather.Data d = Weather.get(x.c);
        if (d == null) {
            TextPaint p = x.p(4.4f, t.sub, 500);
            StaticLayout sl = x.layout(Weather.hasCity(x.c) ? "Loading weather…" : "Set your city in Widget Studio",
                    p, (int) tw, 1.1f, 5);
            cv.save();
            cv.translate(l + ip, tp + ip);
            sl.draw(cv);
            cv.restore();
            return;
        }
        float ic = 9f * u;
        Icons.weather(cv, d.code, d.isDay, l + ip + ic * 0.45f, tp + ip + ic * 0.45f, ic, t.text);
        TextPaint cond = x.p(5.4f, t.text, 400);
        cv.drawText(x.ell(Weather.text(d.code), cond, tw), l + ip, x.top(cond, tp + ip + ic + 1.2f * u), cond);
        TextPaint small = x.p(4.0f, t.text, 500);
        float sb = b - ip;
        cv.drawText(x.ell("L:" + Math.round(d.tmin) + "°  H:" + Math.round(d.tmax) + "°", small, tw), l + ip, sb, small);
        TextPaint big = x.p(25f, t.text, 300);
        String temp = Weather.deg(d);
        while (big.measureText(temp) > tw && big.getTextSize() > 10) big.setTextSize(big.getTextSize() * 0.9f);
        cv.drawText(temp, l + ip - 0.4f * u, sb - small.getTextSize() * 1.35f, big);
    }

    // ---------------------------------------------------------------- Next events
    static void events(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, u = x.u;
        x.frame(cv);
        float pad = 5f * u;
        TextPaint hb = x.p(4.5f, t.frameSub, 800), hr = x.p(4.5f, t.frameSub, 400);
        float hy = pad + 2.5f * u;
        float hx = x.alignX(hb, "NEXT EVENTS:", pad, w - pad);
        cv.drawText("NEXT", hx, x.mid(hb, hy), hb);
        cv.drawText(" EVENTS:", hx + hb.measureText("NEXT"), x.mid(hr, hy), hr);
        int max = Math.max(1, Math.min(6, x.cfg.i("max", 3)));
        List<Events.Ev> ev = Events.allowed(x.c) ? Events.upcoming(x.c, x.cfg.i("days", 14), max) : null;
        float top = hy + 4.5f * u, bottom = h - pad;
        if (ev == null || ev.isEmpty()) {
            TextPaint p = x.p(4.5f, t.frameText, 500);
            String m = ev == null ? "Open Widget Studio and allow calendar access" : "Nothing planned. Enjoy the free time.";
            cv.drawText(x.ell(m, p, w - 2 * pad), pad, x.mid(p, (top + bottom) / 2), p);
            return;
        }
        int n = ev.size();
        float rowH = Math.min((bottom - top) / n, 16f * u);
        float leftCol = w * 0.36f;
        boolean colors = x.cfg.b("eventColors", false);
        for (int i = 0; i < n; i++) {
            Events.Ev e = ev.get(i);
            float cy = top + rowH * (i + 0.5f);
            int dot = i == 0 ? x.accent : (colors ? e.color : t.frameSub);
            cv.drawCircle(pad + 1f * u, cy, 1f * u, Icons.fill(dot));
            TextPaint wp = x.p(4.2f, Theme.alpha(t.frameText, 0.85f), 500);
            cv.drawText(x.ell(Events.shortWhen(e), wp, leftCol - pad - 5 * u), pad + 3.6f * u, x.mid(wp, cy), wp);
            float pl = leftCol, prr = w - pad, ph = Math.min(rowH * 0.72f, 10f * u);
            int fill = i == 0 ? x.accent : (colors ? Theme.alpha(e.color, 0.85f) : t.panel);
            if (i == 0 || colors) x.pill(cv, pl, cy - ph / 2, prr, cy + ph / 2, 1.8f * u, fill); else x.panel(cv, pl, cy - ph / 2, prr, cy + ph / 2, 1.8f * u);
            TextPaint tp = x.p(4.1f, (i == 0 || colors) ? Theme.on(fill) : t.text, 800);
            cv.drawText(x.ell(e.title.toUpperCase(Locale.getDefault()), tp, prr - pl - 5 * u), pl + 2.5f * u, x.mid(tp, cy), tp);
        }
    }

    // ---------------------------------------------------------------- Storage
    static void storage(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h;
        x.frame(cv);
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        long total = fs.getTotalBytes(), free = fs.getAvailableBytes(), used = total - free;
        float s = Math.min(h / 100f, w / 300f);
        boolean bar = x.cfg.b("bar", true);
        float padX = Math.max(18 * s, Math.min(x.radius * 0.8f, h * 0.3f));
        float cy = bar ? h * 0.42f : h / 2f;
        float ic = 22 * s * x.scale;
        Icons.drive(cv, padX + ic / 2, cy, ic, t.frameSub);
        float tx = padX + ic + 9 * s;
        float sz = 17 * s * x.scale;
        TextPaint lab = x.pf(x.font, 1, t.frameSub, 400);
        lab.setTextSize(sz);
        TextPaint val = x.pf(x.font, 1, t.frameText, 800);
        val.setTextSize(sz);
        String[] parts = {"FREE ", gb(free), "    USED ", gb(used)};
        for (int i = 0; i < parts.length; i++) {
            TextPaint p = i % 2 == 0 ? lab : val;
            cv.drawText(parts[i], tx, x.mid(p, cy), p);
            tx += p.measureText(parts[i]);
        }
        if (bar) {
            float by = h * 0.72f, bh = Math.max(3, 6 * s);
            float l = padX, r = w - padX;
            x.rr(cv, l, by - bh / 2, r, by + bh / 2, bh / 2, Theme.alpha(t.frameSub, 0.3f));
            float f = total > 0 ? used / (float) total : 0;
            x.pill(cv, l, by - bh / 2, l + (r - l) * f, by + bh / 2, bh / 2, x.accent);
        }
    }

    static String gb(long bytes) {
        double g = bytes / 1e9;
        return g >= 100 ? Math.round(g) + " GB" : String.format(Locale.getDefault(), "%.1f GB", g);
    }

    // ---------------------------------------------------------------- Big date ("8 september")
    static void bigDate(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, u = x.u;
        x.frame(cv);
        float pad = 6f * u;
        int fg = t.frameText;
        int align = x.cfg.i("align", 0);
        Calendar now = Calendar.getInstance();
        TextPaint day = x.p(30f, fg, 900);
        TextPaint mon = x.p(10f, fg, 800);
        TextPaint sen = x.p(3.8f, Theme.alpha(fg, 0.82f), 400);
        String dS = String.valueOf(now.get(Calendar.DAY_OF_MONTH));
        String mS = new SimpleDateFormat("MMMM", Locale.getDefault()).format(now.getTime()).toLowerCase(Locale.getDefault());
        boolean wl = x.cfg.b("weatherLine", true), btn = x.cfg.b("button", true);
        Weather.Data d = Weather.get(x.c);
        String s = d != null ? Weather.sentence(d)
                : Weather.hasCity(x.c) ? "Fetching the weather…" : "Pick your city in Widget Studio to see the weather here.";
        int tw = (int) Math.min(w - 2 * pad, Math.max(w * 0.62f, 60 * u));
        StaticLayout sl = wl ? x.layout(s, sen, tw, 1.15f, 4) : null;
        float dayH = day.getTextSize() * 0.72f;
        float monH = mon.getTextSize() * 0.95f;
        float btnH = 8.5f * u * x.scale, btnW = 23f * u * x.scale;
        float total = dayH + 2f * u + monH + (sl != null ? 3.5f * u + sl.getHeight() : 0) + (btn ? 4.5f * u + btnH : 0);
        float y = Math.max(pad, (h - total) / 2f);
        float base = y + dayH;
        cv.drawText(dS, x.alignX(day, dS, pad, w - pad), base, day);
        y = base + 2f * u;
        cv.drawText(mS, x.alignX(mon, mS, pad, w - pad), y + mon.getTextSize() * 0.76f, mon);
        y += monH;
        if (sl != null) {
            y += 3.5f * u;
            float lx = align == 1 ? (w - tw) / 2f : align == 2 ? w - pad - tw : pad;
            cv.save();
            cv.translate(lx, y);
            sl.draw(cv);
            cv.restore();
            y += sl.getHeight();
        }
        if (btn) {
            y += 4.5f * u;
            float bl = align == 1 ? (w - btnW) / 2 : align == 2 ? w - pad - btnW : pad;
            RectF r = new RectF(bl, y, bl + btnW, y + btnH);
            x.pill(cv, r.left, r.top, r.right, r.bottom, 1.2f * u, x.accent);
            Icons.arrowRight(cv, r.centerX(), r.centerY(), btnH * 0.55f, Theme.on(x.accent));
            out.zones.add(new Render.Zone(r, "btn"));
        }
    }

    // ---------------------------------------------------------------- Quote
    static void quote(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, u = x.u;
        x.frame(cv);
        int fg = t.frameText;
        float pad = (x.card ? 7f : 4f) * u;
        String raw = x.cfg.s("text", "");
        if (raw.trim().isEmpty()) raw = "Tap to write your quote";
        String author = x.cfg.s("author", "").trim();
        if (Quotes.on(x.cfg)) {
            String[] q = Quotes.current(x.c, x.cfg);
            raw = q[0];
            author = x.cfg.b("showAuthor", true) ? q[1] : "";
        }
        float spacing = x.cfg.i("spacing", 110) / 100f;
        int weight = x.cfg.i("weight", 500);
        int tw = (int) (w - 2 * pad);
        float availH = h - 2 * pad;
        float base = 9f;
        StaticLayout sl = null, al = null;
        float gap = 2.5f * u;
        for (int iter = 0; iter < 30; iter++) {
            TextPaint p = x.p(base, fg, weight);
            sl = x.layout(parse(raw, x.accent), p, tw, spacing, 0);
            al = null;
            float ah = 0;
            if (!author.isEmpty()) {
                TextPaint ap = x.p(Math.max(3.2f, base * 0.42f), Theme.alpha(fg, 0.75f), 500);
                al = x.layout("— " + author, ap, tw, 1f, 2);
                ah = al.getHeight() + gap;
            }
            if (sl.getHeight() + ah <= availH || base < 2.5f) break;
            base *= 0.92f;
        }
        float total = sl.getHeight() + (al != null ? al.getHeight() + gap : 0);
        float y = (h - total) / 2f;
        cv.save();
        cv.translate(pad, y);
        sl.draw(cv);
        cv.restore();
        if (al != null) {
            cv.save();
            cv.translate(pad, y + sl.getHeight() + gap);
            al.draw(cv);
            cv.restore();
        }
    }

    /** Bold/weight span that works with variable fonts (falls back to fake bold). */
    static final class WeightSpan extends MetricAffectingSpan {
        final int weight;
        WeightSpan(int weight) { this.weight = weight; }
        void apply(TextPaint p) {
            if (!p.setFontVariationSettings("'wght' " + weight)) p.setFakeBoldText(true);
        }
        @Override public void updateMeasureState(TextPaint p) { apply(p); }
        @Override public void updateDrawState(TextPaint p) { apply(p); }
    }

    /**
     * Quote markup: a line starting with "+" is bigger (each + = 30%), "~" is smaller.
     * Inline: **bold**, *italic*, ==accent colour==.
     */
    static CharSequence parse(String raw, int accent) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String[] lines = raw.split("\n", -1);
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li];
            int plus = 0, tilde = 0, k = 0;
            while (k < line.length() && (line.charAt(k) == '+' || line.charAt(k) == '~')) {
                if (line.charAt(k) == '+') plus++; else tilde++;
                k++;
            }
            if (k > 0 && k < line.length() && line.charAt(k) == ' ') k++;
            int start = sb.length();
            inline(sb, line.substring(k), accent);
            float f = (float) (Math.pow(1.3, plus) * Math.pow(0.78, tilde));
            if (f != 1f && sb.length() > start)
                sb.setSpan(new RelativeSizeSpan(f), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (li < lines.length - 1) sb.append('\n');
        }
        return sb;
    }

    static void inline(SpannableStringBuilder sb, String s, int accent) {
        int bold = -1, ital = -1, acc = -1;
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("**", i)) {
                if (bold < 0) bold = sb.length();
                else { span(sb, new WeightSpan(800), bold); bold = -1; }
                i += 2;
                continue;
            }
            if (s.startsWith("==", i)) {
                if (acc < 0) acc = sb.length();
                else { span(sb, new ForegroundColorSpan(accent), acc); acc = -1; }
                i += 2;
                continue;
            }
            if (s.charAt(i) == '*') {
                if (ital < 0) ital = sb.length();
                else { span(sb, new StyleSpan(Typeface.ITALIC), ital); ital = -1; }
                i++;
                continue;
            }
            sb.append(s.charAt(i));
            i++;
        }
    }

    static void span(SpannableStringBuilder sb, Object what, int start) {
        if (sb.length() > start) sb.setSpan(what, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // ---------------------------------------------------------------- Counter (age / love / project / countdown)
    static void counter(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, u = x.u;
        x.frame(cv);
        long now = System.currentTimeMillis();
        Counter.R r = Counter.calc(x.cfg, now);
        float pad = 6f * u;
        int fg = t.frameText, sub = t.frameSub;
        String mode = x.cfg.s("mode", "up");
        boolean live = x.cfg.b("live", true) && !(r.done && !mode.equals("up"));
        String title = x.cfg.s("title", "").toUpperCase(Locale.getDefault());

        List<String[]> seg = new ArrayList<>();
        String style = x.cfg.s("style", "breakdown");
        if (style.equals("breakdown")) {
            if (x.cfg.b("uY", true)) seg.add(new String[]{String.valueOf(r.years), r.years == 1 ? "year" : "years"});
            if (x.cfg.b("uM", true)) seg.add(new String[]{String.valueOf(r.months), r.months == 1 ? "month" : "months"});
            if (x.cfg.b("uW", false)) seg.add(new String[]{String.valueOf(r.weeks), r.weeks == 1 ? "week" : "weeks"});
            if (x.cfg.b("uD", true) || seg.isEmpty()) seg.add(new String[]{String.valueOf(r.days), r.days == 1 ? "day" : "days"});
        } else {
            long v;
            String lab;
            switch (style) {
                case "weeks": v = r.totalMs / (7 * DAY); lab = "weeks"; break;
                case "months": v = r.totalMonths; lab = "months"; break;
                case "hours": v = r.totalMs / 3600000L; lab = "hours"; break;
                case "minutes": v = r.totalMs / 60000L; lab = "minutes"; break;
                default: v = r.totalMs / DAY; lab = "days";
            }
            seg.add(new String[]{NumberFormat.getIntegerInstance().format(v), lab});
        }

        TextPaint tp = x.p(4.3f, x.accent, 800);
        tp.setLetterSpacing(0.12f);
        TextPaint np = x.p(15f, fg, 700), lp = x.p(4.4f, sub, 500);
        float avail = w - 2 * pad;
        float total = measure(seg, np, lp, u);
        if (total > avail) {
            float f = avail / total;
            np.setTextSize(np.getTextSize() * f);
            lp.setTextSize(lp.getTextSize() * Math.max(f, 0.7f));
            total = measure(seg, np, lp, u);
        }

        SimpleDateFormat df = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        long start = x.cfg.l("start", now), end = x.cfg.l("end", now);
        String subtitle = x.cfg.s("subtitle", "").trim();
        String auto;
        switch (mode) {
            case "down": auto = r.done ? "ended " + df.format(new Date(end)) : "until " + df.format(new Date(end)); break;
            case "timeline": auto = r.done ? "completed " + df.format(new Date(end)) : df.format(new Date(start)) + "  →  " + df.format(new Date(end)); break;
            default: auto = r.counting ? "starts " + df.format(new Date(start)) : "since " + df.format(new Date(start));
        }
        subtitle = subtitle.isEmpty() ? auto : subtitle + " · " + auto;
        TextPaint sp = x.p(3.8f, sub, 500);
        boolean bar = mode.equals("timeline") && r.progress >= 0;

        float chronoH = live ? 8f * u * x.scale : 0;
        float titleH = title.isEmpty() ? 0 : tp.getTextSize() * 1.15f + 2.2f * u;
        float numH = np.getTextSize() * 0.78f;
        float subH = sp.getTextSize() * 1.35f;
        float barH = bar ? 8f * u : 0;
        float stack = titleH + numH + 3f * u + subH + barH;
        float areaBottom = h - pad - (live ? chronoH + 2f * u : 0);
        float y = pad + Math.max(0, (areaBottom - pad - stack) / 2f);

        if (!title.isEmpty()) {
            String tt = x.ell(title, tp, avail);
            cv.drawText(tt, x.alignX(tp, tt, pad, w - pad), x.top(tp, y), tp);
            y += titleH;
        }
        int align = x.cfg.i("align", 0);
        float sx = align == 1 ? (w - total) / 2f : align == 2 ? w - pad - total : pad;
        float baseline = y + numH;
        for (String[] sg : seg) {
            cv.drawText(sg[0], sx, baseline, np);
            sx += np.measureText(sg[0]) + 0.9f * u;
            cv.drawText(sg[1], sx, baseline, lp);
            sx += lp.measureText(sg[1]) + 3.4f * u;
        }
        y = baseline + 3f * u;
        String st = x.ell(subtitle, sp, avail);
        cv.drawText(st, x.alignX(sp, st, pad, w - pad), x.top(sp, y), sp);
        y += subH;
        if (bar) {
            float by = y + 1.5f * u, bh = 1.8f * u;
            x.rr(cv, pad, by, w - pad, by + bh, bh / 2, Theme.alpha(sub, 0.35f));
            x.pill(cv, pad, by, pad + (w - 2 * pad) * r.progress, by + bh, bh / 2, x.accent);
            TextPaint bp = x.p(3.5f, sub, 600);
            String pct = Math.round(r.progress * 100) + "%";
            long left = Math.max(0, (end - now) / DAY);
            String lft = r.done ? "done" : left + (left == 1 ? " day left" : " days left");
            float ly = x.top(bp, by + bh + 1.4f * u);
            cv.drawText(lft, pad, ly, bp);
            cv.drawText(pct, w - pad - bp.measureText(pct), ly, bp);
        }
        if (live) {
            out.chrono = true;
            out.chronoSize = 7f * u * x.scale;
            out.chronoColor = fg;
            out.align = align;
            out.chronoPadBottom = (int) pad;
            out.chronoPadH = (int) pad;
            out.countDown = r.counting;
            long el = SystemClock.elapsedRealtime();
            out.chronoBase = r.counting ? el + r.remMs : el - r.remMs;
            if (x.preview) {
                TextPaint cp = x.p(7f, fg, 500);
                String s = hms(r.remMs);
                cv.drawText(s, x.alignX(cp, s, pad, w - pad), h - pad - cp.descent(), cp);
            }
        }
    }

    static float measure(List<String[]> seg, TextPaint np, TextPaint lp, float u) {
        float t = 0;
        for (String[] s : seg) t += np.measureText(s[0]) + 0.9f * u + lp.measureText(s[1]) + 3.4f * u;
        return t - 3.4f * u;
    }

    static String hms(long ms) {
        long s = ms / 1000;
        return String.format(Locale.getDefault(), "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    // ---------------------------------------------------------------- Year in dots
    static void dots(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, u = x.u;
        x.frame(cv);
        long now = System.currentTimeMillis();
        String period = x.cfg.s("period", "year");
        Calendar k = Calendar.getInstance();
        int total, done; // done = whole days before today
        if (period.equals("month")) {
            total = k.getActualMaximum(Calendar.DAY_OF_MONTH);
            done = k.get(Calendar.DAY_OF_MONTH) - 1;
        } else if (period.equals("custom")) {
            long s0 = Events.startOfDay(x.cfg.l("start", now)), e0 = Events.startOfDay(x.cfg.l("end", now));
            if (e0 < s0) { long tmp = s0; s0 = e0; e0 = tmp; }
            total = (int) Math.max(1, Math.round((e0 - s0) / (double) DAY) + 1);
            done = (int) Math.round((Events.startOfDay(now) - s0) / (double) DAY);
        } else {
            total = k.getActualMaximum(Calendar.DAY_OF_YEAR);
            done = k.get(Calendar.DAY_OF_YEAR) - 1;
        }
        total = Math.min(total, 3000);
        boolean before = done < 0, after = done >= total;
        int doneC = Math.max(0, Math.min(total, done));
        int left = before ? total : after ? 0 : total - doneC - 1;

        String capMode = x.cfg.s("caption", "left_pct");
        boolean cap = !capMode.equals("none");
        float pad = 9f * u;
        float capH = cap ? 12f * u * x.scale : 0;
        RectF area = new RectF(pad, pad, w - pad, h - pad - capH);
        int cols = 1;
        float cell = 0;
        for (int c = 1; c <= total; c++) {
            int r = (total + c - 1) / c;
            float s = Math.min(area.width() / c, area.height() / r);
            if (s > cell) { cell = s; cols = c; }
        }
        int rows = (total + cols - 1) / cols;
        float gx = area.left + (area.width() - cell * cols) / 2f, gy = area.top + (area.height() - cell * rows) / 2f;
        float rad = cell * 0.31f;
        String shape = x.cfg.s("shape", "circle");
        Paint past = Icons.fill(t.frameText), today = Icons.fill(x.accent), rest = Icons.fill(Theme.alpha(t.frameText, 0.17f));
        for (int i = 0; i < total; i++) {
            float cx = gx + cell * (i % cols + 0.5f), cy = gy + cell * (i / cols + 0.5f);
            Paint p = after || i < doneC ? past : (i == doneC && !before ? today : rest);
            if (shape.equals("square")) cv.drawRect(cx - rad, cy - rad, cx + rad, cy + rad, p);
            else if (shape.equals("rounded")) cv.drawRoundRect(new RectF(cx - rad, cy - rad, cx + rad, cy + rad), rad * 0.45f, rad * 0.45f, p);
            else cv.drawCircle(cx, cy, rad, p);
        }
        if (cap) {
            int pct = after ? 100 : before ? 0 : Math.round(100f * (doneC + 1) / total);
            String a, b;
            if (before) { a = "Starts in " + (-done) + "d"; b = total + " days"; }
            else if (after) { a = "Complete"; b = "100%"; }
            else if (capMode.equals("day_of")) { a = "Day " + (doneC + 1); b = "of " + total; }
            else if (capMode.equals("passed")) { a = (doneC + 1) + "d passed"; b = left + "d left"; }
            else { a = left + "d left"; b = pct + "%"; }
            TextPaint ap = x.p(5.4f, x.accent, 500), sp = x.p(5.4f, t.frameSub, 400);
            String dot = "  ·  ";
            float tw = ap.measureText(a) + sp.measureText(dot) + sp.measureText(b);
            float tx = (w - tw) / 2f, ty = x.mid(ap, h - pad * 0.8f - capH / 2f);
            cv.drawText(a, tx, ty, ap);
            tx += ap.measureText(a);
            cv.drawText(dot, tx, ty, sp);
            cv.drawText(b, tx + sp.measureText(dot), ty, sp);
        }
    }

    // ---------------------------------------------------------------- Weather
    static void weather(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h;
        x.frame(cv);
        float s = Math.min(w, h) / 100f;           // square-friendly unit
        float pad = 9f * s;
        Weather.Data d = Weather.get(x.c);
        if (d == null) {
            TextPaint p = x.pf(x.font, 1, t.frameSub, 500);
            p.setTextSize(Math.max(8.5f * x.density, 7f * s * x.scale));
            StaticLayout sl = x.layout(Weather.hasCity(x.c) ? "Loading weather…" : "Set your city in Widget Studio",
                    p, (int) (w - 2 * pad), 1.1f, 4);
            cv.save();
            cv.translate(pad, (h - sl.getHeight()) / 2f);
            sl.draw(cv);
            cv.restore();
            return;
        }
        float ic = 22f * s;
        Icons.weather(cv, d.code, d.isDay, pad + ic * 0.5f, pad + ic * 0.5f, ic, t.frameText);
        TextPaint city = x.pf(x.font, 1, t.frameSub, 600);
        city.setTextSize(Math.max(8.5f * x.density, 7f * s * x.scale));
        String cn = x.ell(d.city == null ? "" : d.city, city, w - 2 * pad - ic - 4 * s);
        cv.drawText(cn, w - pad - city.measureText(cn), x.mid(city, pad + ic * 0.45f), city);
        TextPaint hl = x.pf(x.font, 1, t.frameSub, 500);
        hl.setTextSize(Math.max(8.5f * x.density, 7f * s * x.scale));
        TextPaint cond = x.pf(x.font, 1, t.frameText, 650);
        cond.setTextSize(Math.max(9f * x.density, 8.5f * s * x.scale));
        TextPaint big = x.pf(x.font, 1, t.frameText, 250);
        big.setTextSize(42f * s * x.scale);
        float yb = h - pad;
        cv.drawText(x.ell("H:" + Math.round(d.tmax) + "°   L:" + Math.round(d.tmin) + "°", hl, w - 2 * pad), pad, yb, hl);
        yb -= hl.getTextSize() * 1.45f;
        cv.drawText(x.ell(Weather.text(d.code), cond, w - 2 * pad), pad, yb, cond);
        yb -= cond.getTextSize() * 1.25f;
        String temp = Weather.deg(d);
        while (big.measureText(temp) > w - 2 * pad && big.getTextSize() > 12) big.setTextSize(big.getTextSize() * 0.9f);
        cv.drawText(temp, pad - 1.2f * s, yb, big);
    }

    // ---------------------------------------------------------------- Photo
    static void photo(Render.Ctx x, Canvas cv, Render.Out out) {
        float w = x.w, h = x.h;
        RectF r = new RectF(0, 0, w, h);
        android.graphics.Bitmap b = CoinPhoto.load(x.cfg.s("photo", ""));
        if (b == null) {
            boolean card = x.card;
            if (!card) x.rr(cv, 0, 0, w, h, x.radius, Theme.alpha(x.t.frame, 0.9f)); else x.frame(cv);
            TextPaint p = x.p(6f, x.t.frameSub, 600);
            String m = "Choose a photo";
            cv.drawText(x.ell(m, p, w - 20), (w - Math.min(p.measureText(m), w - 20)) / 2f, x.mid(p, h / 2f), p);
            return;
        }
        cv.save();
        android.graphics.Path clip = new android.graphics.Path();
        clip.addRoundRect(r, x.radius, x.radius, android.graphics.Path.Direction.CW);
        cv.clipPath(clip);
        float sa = b.getWidth() / (float) b.getHeight(), da = w / h;
        android.graphics.Rect src;
        if (sa > da) {
            int sw = Math.round(b.getHeight() * da);
            src = new android.graphics.Rect((b.getWidth() - sw) / 2, 0, (b.getWidth() + sw) / 2, b.getHeight());
        } else {
            int sh = Math.round(b.getWidth() / da);
            src = new android.graphics.Rect(0, (b.getHeight() - sh) / 2, b.getWidth(), (b.getHeight() + sh) / 2);
        }
        cv.drawBitmap(b, src, r, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        String cap = x.cfg.s("caption", "").trim();
        if (!cap.isEmpty()) {
            Paint scrim = new Paint();
            scrim.setShader(new android.graphics.LinearGradient(0, h * 0.55f, 0, h, 0x00000000, 0x99000000,
                    android.graphics.Shader.TileMode.CLAMP));
            cv.drawRect(0, h * 0.55f, w, h, scrim);
            float s = Math.min(w, h) / 100f, pad = 8f * s;
            TextPaint p = x.pf(x.font, 1, 0xFFFFFFFF, 700);
            p.setTextSize(Math.max(9f * x.density, 9f * s * x.scale));
            p.setShadowLayer(4, 0, 1, 0x80000000);
            cv.drawText(x.ell(cap, p, w - 2 * pad), pad, h - pad, p);
        }
        cv.restore();
        x.rim(cv, r, x.radius, 0.35f);
    }

    static Paint unused() { return new Paint(); }
}
