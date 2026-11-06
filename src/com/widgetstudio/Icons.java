package com.widgetstudio;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Small vector icons drawn with Canvas (weather, transport controls, misc). */
final class Icons {
    static Paint fill(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        return p;
    }

    static Paint stroke(int color, float w) {
        Paint p = fill(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(w);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        return p;
    }

    static int kind(int code) {
        if (code == 0) return 0;
        if (code <= 2) return 1;
        if (code == 3) return 2;
        if (code == 45 || code == 48) return 3;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return 4;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return 5;
        if (code >= 95) return 6;
        return 2;
    }

    /** Weather glyph centred at cx,cy inside an s x s box. */
    static void weather(Canvas cv, int code, boolean day, float cx, float cy, float s, int color) {
        Paint f = fill(color);
        Paint st = stroke(color, s * 0.075f);
        switch (kind(code)) {
            case 0:
                if (day) sun(cv, cx, cy, s, f, st); else moon(cv, cx, cy, s, f);
                break;
            case 1:
                if (day) sun(cv, cx + s * 0.2f, cy - s * 0.18f, s * 0.62f, f, st);
                else moon(cv, cx + s * 0.2f, cy - s * 0.18f, s * 0.62f, f);
                cloud(cv, cx - s * 0.06f, cy + s * 0.12f, s * 0.82f, f);
                break;
            case 3:
                for (int i = 0; i < 3; i++) {
                    float y = cy - s * 0.22f + i * s * 0.22f;
                    cv.drawLine(cx - s * 0.4f + (i == 1 ? s * 0.12f : 0), y, cx + s * 0.4f - (i == 2 ? s * 0.14f : 0), y, st);
                }
                break;
            case 4:
                cloud(cv, cx, cy - s * 0.14f, s * 0.9f, f);
                for (int i = 0; i < 3; i++) {
                    float x = cx - s * 0.22f + i * s * 0.22f;
                    cv.drawLine(x, cy + s * 0.2f, x - s * 0.07f, cy + s * 0.4f, st);
                }
                break;
            case 5:
                cloud(cv, cx, cy - s * 0.14f, s * 0.9f, f);
                for (int i = 0; i < 3; i++) cv.drawCircle(cx - s * 0.22f + i * s * 0.22f, cy + s * 0.3f, s * 0.05f, f);
                break;
            case 6: {
                cloud(cv, cx, cy - s * 0.14f, s * 0.9f, f);
                Path b = new Path();
                b.moveTo(cx + s * 0.04f, cy + s * 0.12f);
                b.lineTo(cx - s * 0.1f, cy + s * 0.32f);
                b.lineTo(cx + s * 0.02f, cy + s * 0.32f);
                b.lineTo(cx - s * 0.06f, cy + s * 0.48f);
                cv.drawPath(b, st);
                break;
            }
            default:
                cloud(cv, cx, cy, s, f);
        }
    }

    static void sun(Canvas cv, float cx, float cy, float s, Paint f, Paint st) {
        cv.drawCircle(cx, cy, s * 0.2f, f);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            float r1 = s * 0.3f, r2 = s * 0.42f;
            cv.drawLine(cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1,
                    cx + (float) Math.cos(a) * r2, cy + (float) Math.sin(a) * r2, st);
        }
    }

    static void moon(Canvas cv, float cx, float cy, float s, Paint f) {
        Path a = new Path();
        a.addCircle(cx, cy, s * 0.32f, Path.Direction.CW);
        Path b = new Path();
        b.addCircle(cx + s * 0.17f, cy - s * 0.12f, s * 0.27f, Path.Direction.CW);
        a.op(b, Path.Op.DIFFERENCE);
        cv.drawPath(a, f);
    }

    static void cloud(Canvas cv, float cx, float cy, float s, Paint f) {
        Path p = new Path();
        p.addCircle(cx - s * 0.18f, cy + s * 0.04f, s * 0.19f, Path.Direction.CW);
        p.addCircle(cx + s * 0.04f, cy - s * 0.08f, s * 0.25f, Path.Direction.CW);
        p.addCircle(cx + s * 0.25f, cy + s * 0.06f, s * 0.17f, Path.Direction.CW);
        p.addRoundRect(new RectF(cx - s * 0.37f, cy + s * 0.02f, cx + s * 0.42f, cy + s * 0.23f),
                s * 0.11f, s * 0.11f, Path.Direction.CW);
        cv.drawPath(p, f);
    }

    static void play(Canvas cv, float cx, float cy, float s, Paint f) {
        Path p = new Path();
        p.moveTo(cx - s * 0.32f, cy - s * 0.4f);
        p.lineTo(cx + s * 0.4f, cy);
        p.lineTo(cx - s * 0.32f, cy + s * 0.4f);
        p.close();
        cv.drawPath(p, f);
    }

    /** ▶❚❚ combined glyph (classic iPod bottom button). */
    static void playPause(Canvas cv, float cx, float cy, float s, Paint f) {
        play(cv, cx - s * 0.32f, cy, s * 0.7f, f);
        float bw = s * 0.12f, h = s * 0.28f;
        cv.drawRect(cx + s * 0.12f, cy - h, cx + s * 0.12f + bw, cy + h, f);
        cv.drawRect(cx + s * 0.32f, cy - h, cx + s * 0.32f + bw, cy + h, f);
    }

    /** |◀ (dir = -1) or ▶| (dir = 1). */
    static void skip(Canvas cv, float cx, float cy, float s, Paint f, int dir) {
        Path p = new Path();
        float h = s * 0.3f, w = s * 0.38f;
        float tip = cx + dir * w * 0.55f;
        p.moveTo(tip, cy);
        p.lineTo(tip - dir * w, cy - h);
        p.lineTo(tip - dir * w, cy + h);
        p.close();
        cv.drawPath(p, f);
        float bx = tip;
        float bw = s * 0.1f;
        if (dir > 0) cv.drawRect(bx, cy - h, bx + bw, cy + h, f);
        else cv.drawRect(bx - bw, cy - h, bx, cy + h, f);
    }

    static void arrowRight(Canvas cv, float cx, float cy, float s, int color) {
        Paint st = stroke(color, s * 0.11f);
        cv.drawLine(cx - s * 0.42f, cy, cx + s * 0.4f, cy, st);
        Path p = new Path();
        p.moveTo(cx + s * 0.1f, cy - s * 0.3f);
        p.lineTo(cx + s * 0.42f, cy);
        p.lineTo(cx + s * 0.1f, cy + s * 0.3f);
        cv.drawPath(p, st);
    }

    static void drive(Canvas cv, float cx, float cy, float s, int color) {
        Paint st = stroke(color, s * 0.09f);
        RectF r = new RectF(cx - s * 0.45f, cy - s * 0.28f, cx + s * 0.45f, cy + s * 0.28f);
        cv.drawRoundRect(r, s * 0.12f, s * 0.12f, st);
        cv.drawLine(cx - s * 0.45f, cy + s * 0.02f, cx + s * 0.45f, cy + s * 0.02f, st);
        cv.drawCircle(cx + s * 0.26f, cy + s * 0.15f, s * 0.05f, fill(color));
    }
}
