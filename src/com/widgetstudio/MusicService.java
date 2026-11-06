package com.widgetstudio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;

import java.util.List;

/**
 * Needs "Notification access" (enabled by the user in Settings). Gives the iPod widget the
 * current song, artwork, progress, and lets the wheel control playback.
 */
public class MusicService extends NotificationListenerService
        implements MediaSessionManager.OnActiveSessionsChangedListener {

    static final class Now {
        String title, artist, pkg;
        Bitmap art;
        long duration, position, updateTime;
        float speed = 1f;
        boolean playing;

        long positionNow() {
            if (!playing || updateTime <= 0) return position;
            long p = position + (long) ((SystemClock.elapsedRealtime() - updateTime) * speed);
            return duration > 0 ? Math.min(p, duration) : p;
        }
    }

    static volatile Now now;
    static volatile MusicService inst;

    private MediaSessionManager msm;
    private MediaController ctrl;
    private final Handler h = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            PowerManager pm = getSystemService(PowerManager.class);
            if (pm == null || pm.isInteractive()) {
                Updater.updateType(MusicService.this, Cfg.MUSIC);
                Updater.updateType(MusicService.this, Cfg.COMBO);
            }
            if (now != null && now.playing) h.postDelayed(this, 10000);
        }
    };

    private final MediaController.Callback cb = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { snap(); }
        @Override public void onMetadataChanged(MediaMetadata metadata) { snap(); }
        @Override public void onSessionDestroyed() { pick(); }
    };

    static Now now() {
        if (Render.DEMO) {
            Now n = new Now();
            n.title = "Midnight Drive";
            n.artist = "Lo-fi Collective";
            n.duration = 206000;
            n.position = 70000;
            return n;
        }
        return now;
    }

    static boolean hasAccess(Context c) {
        if (Render.DEMO) return true;
        String s = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(c.getPackageName());
    }

    @Override public void onListenerConnected() {
        inst = this;
        msm = getSystemService(MediaSessionManager.class);
        try {
            msm.addOnActiveSessionsChangedListener(this, new ComponentName(this, MusicService.class));
        } catch (Exception ignored) { }
        pick();
    }

    @Override public void onListenerDisconnected() {
        try { if (msm != null) msm.removeOnActiveSessionsChangedListener(this); } catch (Exception ignored) { }
        setCtrl(null);
        inst = null;
    }

    private void pick() {
        try {
            onActiveSessionsChanged(msm.getActiveSessions(new ComponentName(this, MusicService.class)));
        } catch (Exception e) {
            setCtrl(null);
        }
    }

    @Override public void onActiveSessionsChanged(List<MediaController> list) {
        MediaController best = null;
        if (list != null) {
            for (MediaController m : list) {
                PlaybackState s = m.getPlaybackState();
                if (s != null && s.getState() == PlaybackState.STATE_PLAYING) { best = m; break; }
                if (best == null) best = m;
            }
        }
        setCtrl(best);
    }

    private void setCtrl(MediaController m) {
        if (ctrl != null) {
            try { ctrl.unregisterCallback(cb); } catch (Exception ignored) { }
        }
        ctrl = m;
        if (m != null) m.registerCallback(cb, h);
        snap();
    }

    private void snap() {
        MediaController m = ctrl;
        if (m == null) {
            now = null;
            push();
            return;
        }
        Now n = new Now();
        MediaMetadata md = m.getMetadata();
        if (md != null) {
            n.title = md.getString(MediaMetadata.METADATA_KEY_TITLE);
            if (n.title == null) n.title = md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
            n.artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST);
            if (n.artist == null) n.artist = md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
            if (n.artist == null) n.artist = md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
            n.duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION);
            Bitmap b = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (b == null) b = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            if (b != null) n.art = scale(b, 320);
        }
        PlaybackState s = m.getPlaybackState();
        if (s != null) {
            n.position = s.getPosition();
            n.updateTime = s.getLastPositionUpdateTime();
            n.speed = s.getPlaybackSpeed() == 0 ? 1f : s.getPlaybackSpeed();
            n.playing = s.getState() == PlaybackState.STATE_PLAYING;
        }
        n.pkg = m.getPackageName();
        if (n.title == null || n.title.isEmpty()) {
            try {
                n.title = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(n.pkg, 0)).toString();
            } catch (Exception e) {
                n.title = "Music";
            }
        }
        if (n.artist == null) n.artist = "";
        now = n;
        push();
    }

    static Bitmap scale(Bitmap b, int max) {
        try {
            int w = b.getWidth(), hh = b.getHeight();
            float f = Math.min(1f, max / (float) Math.max(w, hh));
            if (f >= 1f && b.getConfig() != Bitmap.Config.HARDWARE) return b.copy(Bitmap.Config.ARGB_8888, false);
            Bitmap soft = b.getConfig() == Bitmap.Config.HARDWARE ? b.copy(Bitmap.Config.ARGB_8888, false) : b;
            return Bitmap.createScaledBitmap(soft, Math.max(1, (int) (w * f)), Math.max(1, (int) (hh * f)), true);
        } catch (Exception e) {
            return null;
        }
    }

    private void push() {
        Updater.updateType(this, Cfg.MUSIC);
        Updater.updateType(this, Cfg.COMBO);
        h.removeCallbacks(tick);
        if (now != null && now.playing) h.postDelayed(tick, 10000);
    }

    /** Seeks the current track by deltaMs (negative = rewind). Returns false if nothing is playing. */
    static boolean seek(long deltaMs) {
        MusicService s = inst;
        MediaController m = s != null ? s.ctrl : null;
        Now n = now;
        if (m == null || n == null) return false;
        long target = n.positionNow() + deltaMs;
        if (n.duration > 0) target = Math.min(target, n.duration - 500);
        m.getTransportControls().seekTo(Math.max(0, target));
        return true;
    }

    /** Wheel buttons. */
    static void command(Context c, String cmd) {
        if (cmd.equals("volup") || cmd.equals("voldown")) {
            android.media.AudioManager am = c.getSystemService(android.media.AudioManager.class);
            if (am != null) am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                    cmd.equals("volup") ? android.media.AudioManager.ADJUST_RAISE : android.media.AudioManager.ADJUST_LOWER,
                    android.media.AudioManager.FLAG_SHOW_UI);
            return;
        }
        MusicService s = inst;
        MediaController m = s != null ? s.ctrl : null;
        if (m == null) return;
        MediaController.TransportControls tc = m.getTransportControls();
        switch (cmd) {
            case "prev": tc.skipToPrevious(); break;
            case "next": tc.skipToNext(); break;
            case "play": {
                PlaybackState st = m.getPlaybackState();
                if (st != null && st.getState() == PlaybackState.STATE_PLAYING) tc.pause(); else tc.play();
                break;
            }
        }
    }

    /** MENU button: always the app chosen for this widget (default YT Music), else the current player. */
    static Intent menuIntent(Context c, Cfg cfg) {
        String chosen = cfg.s("musicApp", "app:com.google.android.apps.youtube.music");
        if (chosen.startsWith("app:")) chosen = chosen.substring(4);
        Intent i = chosen.isEmpty() ? null : c.getPackageManager().getLaunchIntentForPackage(chosen);
        if (i == null && now != null && now.pkg != null) i = c.getPackageManager().getLaunchIntentForPackage(now.pkg);
        if (i == null) i = c.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.youtube.music");
        return i;
    }

    /** Intent that opens the current (or chosen) music app, or notification-access settings. */
    static Intent playerIntent(Context c, Cfg cfg) {
        if (!hasAccess(c)) return new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        Now n = now;
        String pkg = n != null ? n.pkg : null;
        String chosen = cfg.s("musicApp", "");
        if (chosen.startsWith("app:")) chosen = chosen.substring(4);
        if ((pkg == null || pkg.isEmpty()) && !chosen.isEmpty()) pkg = chosen;
        if (pkg == null || pkg.isEmpty()) return null;
        return c.getPackageManager().getLaunchIntentForPackage(pkg);
    }
}
