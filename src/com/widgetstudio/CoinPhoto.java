package com.widgetstudio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stores the user's own coin pictures (square-cropped) inside the app and loads them for drawing. */
final class CoinPhoto {
    private static final Map<String, Bitmap> cache = new LinkedHashMap<String, Bitmap>(4, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Bitmap> e) { return size() > 4; }
    };

    /** Copies the picked image, centre-cropped to a 600px square. Returns the saved path or null. */
    static String save(Context c, Uri uri, int id, String side) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, o); }
            int sample = 1;
            while (Math.min(o.outWidth, o.outHeight) / (sample * 2) >= 600) sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            Bitmap b;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) { b = BitmapFactory.decodeStream(in, null, d); }
            if (b == null) return null;
            int s = Math.min(b.getWidth(), b.getHeight());
            Bitmap sq = Bitmap.createBitmap(b, (b.getWidth() - s) / 2, (b.getHeight() - s) / 2, s, s);
            Bitmap out = Bitmap.createScaledBitmap(sq, 600, 600, true);
            File f = new File(c.getFilesDir(), "coin_" + id + "_" + side + "_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fo = new FileOutputStream(f)) { out.compress(Bitmap.CompressFormat.JPEG, 92, fo); }
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** Copies a picked image keeping its aspect ratio (longest side 1400px). */
    static String saveFull(Context c, Uri uri, int id) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, o); }
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= 1400) sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            Bitmap b;
            try (InputStream in = c.getContentResolver().openInputStream(uri)) { b = BitmapFactory.decodeStream(in, null, d); }
            if (b == null) return null;
            File f = new File(c.getFilesDir(), "photo_" + id + "_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fo = new FileOutputStream(f)) { b.compress(Bitmap.CompressFormat.JPEG, 92, fo); }
            return f.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    static synchronized Bitmap load(String path) {
        if (path == null || path.isEmpty()) return null;
        Bitmap b = cache.get(path);
        if (b != null) return b;
        if (!new File(path).exists()) return null;
        b = BitmapFactory.decodeFile(path);
        if (b != null) cache.put(path, b);
        return b;
    }
}
