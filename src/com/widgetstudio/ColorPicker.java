package com.widgetstudio;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.function.IntConsumer;

/** Full colour picker: hue / saturation / brightness / opacity, hex, swatches, live preview. */
final class ColorPicker {
    /** 0 = done, 1 = reset to theme, 2 = cancelled. */
    interface Done { void done(int result); }

    static final int[] EXTRA = {0xFF000000, 0xFF0B0B0C, 0xFF1C1C1E, 0xFF3A3A3C, 0xFF8E8E93, 0xFFC7C7CC, 0xFFF2F2F7,
            0xFFFFFFFF, 0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFF34C759, 0xFF00C7BE, 0xFF30B0C7, 0xFF007AFF,
            0xFF5856D6, 0xFFAF52DE, 0xFFFF2D55, 0xFFA2845E, 0xFFD9B45B};

    static void show(Activity a, String title, int initial, IntConsumer live, Done done) {
        float[] hsv = new float[3];
        Color.colorToHSV(initial, hsv);
        int[] alpha = {Color.alpha(initial)};
        int pad = Ui.dp(a, 18);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(pad, Ui.dp(a, 8), pad, 0);

        View sw = new View(a);
        GradientDrawable g = Ui.round(initial, Ui.dp(a, 14));
        g.setStroke(Ui.dp(a, 1), 0x33FFFFFF);
        sw.setBackground(g);
        col.addView(sw, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(a, 56)));

        EditText hex = new EditText(a);
        hex.setSingleLine(true);
        hex.setTextColor(Ui.TEXT);
        hex.setHint("#AARRGGBB");
        hex.setHintTextColor(Ui.SUB);

        SeekBar[] bars = new SeekBar[4];
        boolean[] internal = {false};
        Runnable apply = () -> {
            int c = Color.HSVToColor(alpha[0], hsv);
            g.setColor(c);
            if (!internal[0]) {
                internal[0] = true;
                hex.setText(String.format(Locale.US, "#%08X", c));
                internal[0] = false;
            }
            live.accept(c);
        };
        String[] names = {"Hue", "Saturation", "Brightness", "Opacity"};
        int[] max = {360, 100, 100, 255};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            TextView l = Ui.label(a, names[i]);
            col.addView(l);
            SeekBar sb = new SeekBar(a);
            sb.setMax(max[i]);
            bars[i] = sb;
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
                    if (!user) return;
                    if (k == 0) hsv[0] = v;
                    else if (k == 1) hsv[1] = v / 100f;
                    else if (k == 2) hsv[2] = v / 100f;
                    else alpha[0] = v;
                    apply.run();
                }
                @Override public void onStartTrackingTouch(SeekBar s) { }
                @Override public void onStopTrackingTouch(SeekBar s) { }
            });
            col.addView(sb);
        }
        Runnable syncBars = () -> {
            bars[0].setProgress(Math.round(hsv[0]));
            bars[1].setProgress(Math.round(hsv[1] * 100));
            bars[2].setProgress(Math.round(hsv[2] * 100));
            bars[3].setProgress(alpha[0]);
        };
        syncBars.run();

        col.addView(Ui.label(a, "Hex"));
        hex.setText(String.format(Locale.US, "#%08X", initial));
        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int x, int y, int z) { }
            @Override public void onTextChanged(CharSequence s, int x, int y, int z) { }
            @Override public void afterTextChanged(Editable s) {
                if (internal[0]) return;
                String t = s.toString().trim();
                if (!t.startsWith("#")) t = "#" + t;
                if (t.length() != 7 && t.length() != 9) return;
                try {
                    int c = Color.parseColor(t);
                    Color.colorToHSV(c, hsv);
                    alpha[0] = Color.alpha(c);
                    syncBars.run();
                    internal[0] = true;
                    apply.run();
                    internal[0] = false;
                } catch (Exception ignored) { }
            }
        });
        col.addView(hex);

        col.addView(Ui.label(a, "Quick colours"));
        HorizontalScrollView hs = new HorizontalScrollView(a);
        LinearLayout row = new LinearLayout(a);
        hs.addView(row);
        int[] all = new int[Theme.SWATCHES.length + EXTRA.length];
        System.arraycopy(EXTRA, 0, all, 0, EXTRA.length);
        System.arraycopy(Theme.SWATCHES, 0, all, EXTRA.length, Theme.SWATCHES.length);
        for (int c : all) {
            View v = new View(a);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(c);
            d.setStroke(Ui.dp(a, 1), 0x55FFFFFF);
            v.setBackground(d);
            LinearLayout.LayoutParams q = Ui.lp(Ui.dp(a, 34), Ui.dp(a, 34));
            q.rightMargin = Ui.dp(a, 8);
            v.setOnClickListener(x -> {
                Color.colorToHSV(c, hsv);
                alpha[0] = Color.alpha(c);
                syncBars.run();
                apply.run();
            });
            row.addView(v, q);
        }
        col.addView(hs);

        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        sv.addView(col);
        new AlertDialog.Builder(a)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton("Done", (d, w) -> done.done(0))
                .setNeutralButton("Reset", (d, w) -> done.done(1))
                .setNegativeButton("Cancel", (d, w) -> done.done(2))
                .setOnCancelListener(d -> done.done(2))
                .show();
    }
}
