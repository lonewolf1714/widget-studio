package com.widgetstudio;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;

import java.util.HashMap;

/** Font catalogue. Keep in sync with FONTS in build.py (counter layouts are generated from it). */
final class Fonts {
    /** key, label, family ("@name" = bundled res/font), weightable (1 = apply requested weight). */
    static final String[][] BASE = {
            {"sans", "Sans (One UI)", "sans-serif", "1"},
            {"sans_light", "Sans Light", "sans-serif-light", "0"},
            {"sans_medium", "Sans Medium", "sans-serif-medium", "0"},
            {"sans_black", "Sans Black", "sans-serif-black", "0"},
            {"condensed", "Condensed", "sans-serif-condensed", "1"},
            {"serif", "Serif", "serif", "1"},
            {"mono", "Monospace", "monospace", "1"},
            {"serif_mono", "Typewriter", "serif-monospace", "1"},
            {"cursive", "Cursive", "cursive", "1"},
            {"casual", "Casual", "casual", "1"},
            {"inter", "Inter", "@inter", "1"},
            {"space_grotesk", "Space Grotesk", "@space_grotesk", "1"},
            {"bebas_neue", "Bebas Neue", "@bebas_neue", "0"},
            {"archivo_black", "Archivo Black", "@archivo_black", "0"},
            {"playfair", "Playfair Display", "@playfair", "1"},
            {"dm_serif", "DM Serif Display", "@dm_serif", "0"},
            {"caveat", "Caveat (handwriting)", "@caveat", "1"},
            {"pacifico", "Pacifico", "@pacifico", "0"},
    };

    /** Built-in families first, then the bundled premium fonts. */
    static final String[][] LIST = merge(BASE, FontsExtra.LIST);

    static String[][] merge(String[][] a, String[][] b) {
        String[][] out = new String[a.length + b.length][];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static final HashMap<String, Typeface> cache = new HashMap<>();

    static String[] row(String key) {
        for (String[] r : LIST) if (r[0].equals(key)) return r;
        return LIST[0];
    }

    static String label(String key) { return row(key)[1]; }

    static synchronized Typeface base(Context c, String key) {
        Typeface tf = cache.get(key);
        if (tf != null) return tf;
        String fam = row(key)[2];
        if (fam.startsWith("@")) {
            int id = c.getResources().getIdentifier(fam.substring(1), "font", c.getPackageName());
            try { tf = c.getResources().getFont(id); } catch (Exception e) { tf = Typeface.DEFAULT; }
        } else {
            tf = Typeface.create(fam, Typeface.NORMAL);
        }
        cache.put(key, tf);
        return tf;
    }

    /** Sets typeface + weight on a paint. weight 100..900. */
    static void apply(Paint p, Context c, String key, int weight) {
        String[] r = row(key);
        Typeface tf = base(c, key);
        boolean bundled = r[2].startsWith("@");
        boolean weightable = "1".equals(r[3]);
        p.setFakeBoldText(false);
        if (bundled) {
            p.setTypeface(tf);
            if (weightable) {
                boolean ok = p.setFontVariationSettings("'wght' " + weight);
                if (!ok && weight >= 650) p.setFakeBoldText(true);
            }
        } else if (weightable && Build.VERSION.SDK_INT >= 28) {
            p.setTypeface(Typeface.create(tf, weight, false));
        } else {
            p.setTypeface(weightable && weight >= 650 ? Typeface.create(tf, Typeface.BOLD) : tf);
        }
    }
}
