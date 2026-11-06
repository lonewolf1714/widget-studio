package com.widgetstudio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.SparseIntArray;

/**
 * Low-latency sound effects. Sounds are preloaded into one SoundPool that lives for the process,
 * so playback is never cut off by garbage collection.
 */
final class Sfx {
    private static SoundPool pool;
    private static final SparseIntArray ids = new SparseIntArray();
    private static final SparseIntArray loaded = new SparseIntArray();
    private static int pending = 0;

    static synchronized void init(Context c) {
        if (pool != null) return;
        pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((p, sampleId, status) -> {
            synchronized (Sfx.class) {
                if (status == 0) loaded.put(sampleId, 1);
                if (pending == sampleId && status == 0) {
                    p.play(sampleId, 1f, 1f, 1, 0, 1f);
                    pending = 0;
                }
            }
        });
        Context app = c.getApplicationContext();
        for (int res : new int[]{R.raw.coin, R.raw.dice, R.raw.tap}) ids.put(res, pool.load(app, res, 1));
    }

    static synchronized void play(Context c, int res) {
        init(c);
        int id = ids.get(res);
        if (id == 0) return;
        if (loaded.get(id) == 1) pool.play(id, 1f, 1f, 1, 0, 1f);
        else pending = id; // plays as soon as it finishes loading
    }
}
