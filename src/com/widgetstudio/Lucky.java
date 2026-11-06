package com.widgetstudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.TextPaint;

import org.json.JSONObject;

/**
 * Lucky 7: pick Below 7, Lucky 7 or Above 7, then two dice are rolled.
 * Only wins and losses are counted.
 */
final class Lucky {
    static final String[] PICKS = {"low", "seven", "high"};

    static final class S {
        int d1, d2;
        String pick = "";
        int result; // 0 none yet, 1 won, 2 lost
        int wins, losses, sevens;
    }

    static S load(Context c, Cfg cfg) {
        S s = new S();
        if (Render.DEMO) {
            s.d1 = 4; s.d2 = 3; s.pick = "seven"; s.result = 1; s.wins = 6; s.losses = 3;
            return s;
        }
        String raw = Cfg.prefs(c).getString("l" + cfg.id, null);
        if (raw != null) {
            try {
                JSONObject o = new JSONObject(raw);
                s.d1 = o.optInt("d1");
                s.d2 = o.optInt("d2");
                s.pick = o.optString("pick", "");
                s.result = o.optInt("result");
                s.wins = o.optInt("wins");
                s.losses = o.optInt("losses");
                s.sevens = o.optInt("sevens");
            } catch (Exception ignored) { }
        }
        return s;
    }

    static void save(Context c, Cfg cfg, S s) {
        try {
            JSONObject o = new JSONObject();
            o.put("d1", s.d1);
            o.put("d2", s.d2);
            o.put("pick", s.pick);
            o.put("result", s.result);
            o.put("wins", s.wins);
            o.put("losses", s.losses);
            o.put("sevens", s.sevens);
            Cfg.prefs(c).edit().putString("l" + cfg.id, o.toString()).apply();
        } catch (Exception ignored) { }
    }

    static boolean wins(String pick, int total) {
        switch (pick) {
            case "low": return total < 7;
            case "high": return total > 7;
            default: return total == 7;
        }
    }

    /** "bet:low|seven|high" places a bet and rolls; "bet:again" repeats the last pick. Returns true if dice rolled. */
    static boolean act(Context c, Cfg cfg, String cmd) {
        if (!cmd.startsWith("bet:")) return false;
        S s = load(c, cfg);
        String pick = cmd.substring(4);
        if (pick.equals("again")) pick = s.pick.isEmpty() ? "seven" : s.pick;
        if (!pick.equals("low") && !pick.equals("high")) pick = "seven";
        s.pick = pick;
        s.d1 = 1 + Games.RNG.nextInt(6);
        s.d2 = 1 + Games.RNG.nextInt(6);
        int total = s.d1 + s.d2;
        if (total == 7) s.sevens++;
        if (wins(pick, total)) {
            s.result = 1;
            s.wins++;
        } else {
            s.result = 2;
            s.losses++;
        }
        save(c, cfg, s);
        return true;
    }

    static void reset(Context c, Cfg cfg) {
        save(c, cfg, new S());
    }

    static String label(String pick) {
        switch (pick) {
            case "low": return "Below 7";
            case "high": return "Above 7";
            default: return "Lucky 7";
        }
    }

    // ------------------------------------------------------------------------------------ drawing

    /** Small circular-arrow "reset" button centred at (cx, cy); adds its tap zone. */
    static void resetButton(Render.Ctx x, Canvas cv, float cx, float cy, int color, Render.Out out) {
        float u = x.u, r = Math.max(3.4f * u, 4.5f * x.density);
        cv.drawCircle(cx, cy, r * 1.75f, Icons.fill(Theme.alpha(color, 0.10f)));
        float sw = Math.max(1.4f, r * 0.26f);
        android.graphics.Paint p = Icons.stroke(color, sw);
        p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        RectF o = new RectF(cx - r, cy - r, cx + r, cy + r);
        cv.drawArc(o, -60, 290, false, p);
        // arrow head at the arc's start (pointing clockwise)
        double a = Math.toRadians(-60);
        float ex = cx + (float) Math.cos(a) * r, ey = cy + (float) Math.sin(a) * r;
        android.graphics.Path head = new android.graphics.Path();
        float hs = r * 0.62f;
        head.moveTo(ex + hs * 0.95f, ey - hs * 0.15f);
        head.lineTo(ex - hs * 0.15f, ey - hs * 0.75f);
        head.lineTo(ex - hs * 0.05f, ey + hs * 0.55f);
        head.close();
        cv.drawPath(head, Icons.fill(color));
        float hit = r * 3.2f;
        out.zones.add(new Render.Zone(new RectF(cx - hit, cy - hit, cx + hit, cy + hit), "ipod:reset"));
    }

    static void draw(Render.Ctx x, Canvas cv, Render.Out out) {
        Theme t = x.t;
        float w = x.w, h = x.h, u = x.u;
        x.frame(cv);
        S st = load(x.c, x.cfg);
        int fg = t.frameText, sub = t.frameSub;
        int gold = x.accent;
        float ph = Games.phase(x.cfg.id);
        boolean anim = ph >= 0;
        float pad = 6f * u;

        // header: title left, wins / losses right
        TextPaint title = x.p(6.4f, fg, 700);
        title.setLetterSpacing(0.01f);
        float headY = pad + 4.5f * u;
        String tt = "Lucky 7";
        cv.drawText(tt, pad, x.mid(title, headY), title);
        float tw = title.measureText(tt);
        cv.drawCircle(pad + tw + 3.2f * u, headY, 1.1f * u, Icons.fill(gold));
        float rbR = Math.max(3.4f * u, 4.5f * x.density) * 1.75f;
        resetButton(x, cv, w - pad - rbR, headY, sub, out);
        float right = w - pad - rbR * 2 - 3f * u;
        drawScore(x, cv, st, pad + tw + 6 * u, right, headY, fg, sub);
        body(x, cv, new RectF(pad, headY + 6f * u, w - pad, h - pad), st, false, out);
    }

    /** "WON 14   LOST 25" right-aligned to {@code right}, if it fits after {@code minLeft}. */
    static void drawScore(Render.Ctx x, Canvas cv, S st, float minLeft, float right, float headY, int fg, int sub) {
        float u = x.u;
        // "Won 14   Lost 25": numbers bright, words muted
        TextPaint num = x.p(5.2f, fg, 650);
        TextPaint sm = x.p(3.4f, sub, 600);
        sm.setLetterSpacing(0.12f);
        String[][] parts = {{"WON ", String.valueOf(st.wins)}, {"LOST ", String.valueOf(st.losses)}};
        float gapW = 4.5f * u;
        float need = 0;
        for (String[] pt : parts) need += sm.measureText(pt[0]) + num.measureText(pt[1]);
        need += gapW;
        if (right - need > minLeft) {
            float xx = right - need;
            for (int i = 0; i < parts.length; i++) {
                cv.drawText(parts[i][0], xx, x.mid(sm, headY), sm);
                xx += sm.measureText(parts[i][0]);
                cv.drawText(parts[i][1], xx, x.mid(num, headY), num);
                xx += num.measureText(parts[i][1]) + gapW;
            }
        }
    }

    /** Dice, total, outcome and the three sections, inside {@code a}. Inside Games the score shows under the total. */
    static void body(Render.Ctx x, Canvas cv, RectF a, S st, boolean embedded, Render.Out out) {
        float u = x.u;
        int fg = x.t.frameText, sub = x.t.frameSub;
        int gold = x.accent;
        float ph = Games.phase(x.cfg.id);
        boolean anim = ph >= 0;

        // bottom: three bet sections
        float btnH = Math.min(26f * u, a.height() * 0.34f);
        float btnBottom = a.bottom, btnTop = btnBottom - btnH;
        float g = 3.2f * u;
        float sideW = (a.width() - 2 * g) * 0.3f;
        float midW = (a.width() - 2 * g) - 2 * sideW;
        RectF[] rs = {
                new RectF(a.left, btnTop, a.left + sideW, btnBottom),
                new RectF(a.left + sideW + g, btnTop, a.left + sideW + g + midW, btnBottom),
                new RectF(a.right - sideW, btnTop, a.right, btnBottom)};
        int total = st.d1 + st.d2;
        boolean haveRoll = st.d1 > 0 && !anim;
        for (int i = 0; i < 3; i++) {
            String key = PICKS[i];
            RectF r = rs[i];
            boolean center = i == 1;
            boolean picked = key.equals(st.pick) && (anim || st.d1 > 0);
            boolean winner = haveRoll && wins(key, total);
            float rad = Math.min(r.height() * 0.38f, 9f * u);
            if (center) {
                // gold "Lucky 7" pill
                x.pill(cv, r.left, r.top, r.right, r.bottom, rad, winner || !haveRoll ? gold : Theme.alpha(gold, 0.55f));
                x.rim(cv, r, rad, 0.5f);
            } else {
                x.panel(cv, r.left, r.top, r.right, r.bottom, rad);
                if (winner) x.rr(cv, r.left, r.top, r.right, r.bottom, rad, Theme.alpha(gold, 0.16f));
            }
            if (picked) {
                float sw = Math.max(1.5f, 1.1f * u);
                RectF o = new RectF(r);
                o.inset(-sw * 1.4f, -sw * 1.4f);
                int col = anim ? fg : st.result == 1 ? 0xFF34C759 : 0xFFFF453A;
                cv.drawPath(Render.Ctx.squircle(o, rad + sw * 1.4f), Icons.stroke(col, sw));
            }
            int ink = center ? 0xFF1E1600 : fg;
            int inkSub = center ? 0xB31E1600 : sub;
            TextPaint lp = x.p(center ? 6.6f : 5.4f, ink, center ? 800 : 650);
            lp.setLetterSpacing(center ? 0.04f : 0.02f);
            String lab = center ? "LUCKY 7" : label(key);
            while (lp.measureText(lab) > r.width() - 4 * u && lp.getTextSize() > 6) lp.setTextSize(lp.getTextSize() * 0.92f);
            TextPaint op = x.p(3.3f, inkSub, 700);
            op.setLetterSpacing(0.14f);
            String odds = center ? "EXACTLY 7" : key.equals("low") ? "2 – 6" : "8 – 12";
            odds = x.ell(odds, op, r.width() - 4 * u);
            float block = lp.getTextSize() * 0.75f + 2f * u + op.getTextSize() * 0.75f;
            float base = r.centerY() - block / 2f + lp.getTextSize() * 0.75f;
            cv.drawText(lab, r.centerX() - lp.measureText(lab) / 2f, base, lp);
            cv.drawText(odds, r.centerX() - op.measureText(odds) / 2f, base + 2f * u + op.getTextSize() * 0.75f, op);
            RectF hit = new RectF(r);
            hit.inset(-g / 2f, -2 * u);
            out.zones.add(new Render.Zone(hit, "ipod:bet:" + key));
        }

        // middle: two 3D dice, total on the left, outcome on the right
        float areaTop = a.top, areaBottom = btnTop - 4f * u;
        float areaH = areaBottom - areaTop;
        float size = Math.min(areaH / 1.32f, a.width() * 0.14f);
        float depth = size * 0.3f, dg = size * 0.3f;
        float totalW = 2 * (size + depth) + dg;
        float l0 = a.centerX() - totalW / 2f;
        float top = areaTop + (areaH - (size + depth)) / 2f;
        int[] v = {st.d1, st.d2};
        java.util.Random jr = new java.util.Random(Games.animFrame * 31L + 11);
        float l = l0;
        for (int i = 0; i < 2; i++) {
            cv.save();
            if (anim) {
                v[i] = 1 + jr.nextInt(6);
                float cxd = l + (size + depth) / 2f, cyd = top + (size + depth) / 2f;
                float amp = 1f - ph;
                cv.translate((jr.nextFloat() - 0.5f) * size * 0.25f * amp,
                        -(float) Math.abs(Math.sin(ph * Math.PI * 3)) * size * 0.3f * amp);
                cv.rotate((jr.nextFloat() - 0.5f) * 50f * amp, cxd, cyd);
            }
            Ipod.die3d(x, cv, l, top, size, depth, v[i], i);
            cv.restore();
            l += size + depth + dg;
        }
        RectF diceHit = new RectF(l0, top, l0 + totalW, top + size + depth);
        out.zones.add(new Render.Zone(diceHit, "ipod:bet:again"));

        float leftW = l0 - a.left - 4 * u, rightL = l0 + totalW + 4 * u, rightW = a.right - rightL;
        float cy = top + (size + depth) / 2f;
        // total
        String big = anim ? "…" : st.d1 == 0 ? "?" : String.valueOf(total);
        TextPaint bp = x.p(17f, total == 7 && haveRoll ? gold : fg, 300);
        while (bp.measureText(big) > leftW && bp.getTextSize() > 8) bp.setTextSize(bp.getTextSize() * 0.92f);
        TextPaint cap = x.p(3.3f, sub, 700);
        cap.setLetterSpacing(0.14f);
        float bH = bp.getTextSize() * 0.72f, cH = cap.getTextSize() * 0.75f;
        float base = cy - (bH + 2.4f * u + cH) / 2f + bH;
        if (leftW > 6 * u) {
            cv.drawText(big, a.left, base, bp);
            String capS = embedded ? "WON " + st.wins + " · LOST " + st.losses : "TOTAL";
            cv.drawText(x.ell(capS, cap, leftW), a.left, base + 2.4f * u + cH, cap);
        }
        // outcome
        if (rightW > 6 * u) {
            String head, line;
            int hc = fg;
            if (anim) { head = "Rolling"; line = label(st.pick).toUpperCase(java.util.Locale.getDefault()); }
            else if (st.d1 == 0) { head = "Pick one"; line = "TAP A SECTION"; }
            else if (st.result == 1) {
                head = st.pick.equals("seven") ? "Lucky!" : "You win";
                line = "YOU PICKED " + label(st.pick).toUpperCase(java.util.Locale.getDefault());
                hc = st.pick.equals("seven") ? gold : 0xFF34C759;
            } else { head = "No luck"; line = "YOU PICKED " + label(st.pick).toUpperCase(java.util.Locale.getDefault()); hc = fg; }
            TextPaint hp = x.p(8.5f, hc, 600);
            while (hp.measureText(head) > rightW && hp.getTextSize() > 7) hp.setTextSize(hp.getTextSize() * 0.92f);
            float hH = hp.getTextSize() * 0.75f;
            float b2 = cy - (hH + 2.4f * u + cH) / 2f + hH;
            float hx = a.right - hp.measureText(head);
            cv.drawText(head, Math.max(rightL, hx), b2, hp);
            String ln = x.ell(line, cap, rightW);
            cv.drawText(ln, Math.max(rightL, a.right - cap.measureText(ln)), b2 + 2.4f * u + cH, cap);
        }
    }
}
