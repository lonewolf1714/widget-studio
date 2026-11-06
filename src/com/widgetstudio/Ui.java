package com.widgetstudio;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Tiny view helpers for the programmatic, dark UI. */
final class Ui {
    static final int BG = 0xFF0E1311, CARD = 0xFF18201D, CARD2 = 0xFF222C28, TEXT = 0xFFEEF2EF,
            SUB = 0xFF9DB0A7, ACCENT = 0xFF9CCB5A;

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        l.setBackground(round(CARD, dp(c, 20)));
        LinearLayout.LayoutParams q = matchWrap();
        q.bottomMargin = dp(c, 12);
        l.setLayoutParams(q);
        return l;
    }

    static TextView label(Context c, String s) {
        TextView t = text(c, s, 13, SUB, true);
        t.setPadding(0, dp(c, 12), 0, dp(c, 6));
        return t;
    }

    static TextView chip(Context c, String s, boolean selected) {
        TextView t = text(c, s, 14, selected ? 0xFF10140F : TEXT, true);
        int px = dp(c, 14), py = dp(c, 8);
        t.setPadding(px, py, px, py);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(selected ? ACCENT : CARD2, dp(c, 18)));
        LinearLayout.LayoutParams q = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        q.rightMargin = dp(c, 8);
        t.setLayoutParams(q);
        return t;
    }

    static void select(TextView chip, boolean selected) {
        Context c = chip.getContext();
        chip.setTextColor(selected ? 0xFF10140F : TEXT);
        chip.setBackground(round(selected ? ACCENT : CARD2, dp(c, 18)));
    }

    static Button button(Context c, String s, boolean primary) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(primary ? 0xFF10140F : TEXT);
        b.setBackgroundTintList(ColorStateList.valueOf(primary ? ACCENT : CARD2));
        return b;
    }

    static View space(Context c, int dpH) {
        View v = new View(c);
        v.setLayoutParams(lp(1, dp(c, dpH)));
        return v;
    }
}
