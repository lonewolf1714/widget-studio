package com.widgetstudio;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cover mode: shows your Widget Studio widgets full-size on the Galaxy Flip cover screen (or anywhere),
 * live and tappable. Swipe left / right to switch.
 */
public class CoverActivity extends Activity {
    final Handler h = new Handler(Looper.getMainLooper());
    final List<Integer> ids = new ArrayList<>();
    int index;
    ImageView image;
    LinearLayout dots;
    TextView empty;
    Render.Out last;
    Cfg lastCfg;
    long fastUntil;
    boolean podOnly;   // full-screen iPod player only
    Cfg podCfg;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("global", Context.MODE_PRIVATE);
    }

    /** Widgets hidden from Cover mode (by widget id). */
    static Set<String> hidden(Context c) {
        return new HashSet<>(prefs(c).getStringSet("cover_hide", new HashSet<>()));
    }

    static void setHidden(Context c, int id, boolean hide) {
        Set<String> s = hidden(c);
        if (hide) s.add(String.valueOf(id)); else s.remove(String.valueOf(id));
        prefs(c).edit().putStringSet("cover_hide", s).apply();
    }

    /** True when this context is on the small cover display. */
    static boolean onCover(Activity a) {
        android.content.res.Configuration cf = a.getResources().getConfiguration();
        return cf.screenHeightDp < 520 && cf.screenWidthDp < 520;
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            redraw();
            boolean fast = System.currentTimeMillis() < fastUntil || Games.animPhase >= 0;
            h.postDelayed(this, fast ? 33 : 1000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setShowWhenLocked(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 18), Gravity.BOTTOM);
        dl.bottomMargin = Ui.dp(this, 4);
        root.addView(dots, dl);
        empty = Ui.text(this, "Add a Widget Studio widget to your home screen, then it shows up here.", 15, Ui.SUB, false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        root.addView(empty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        image.setOnTouchListener(new Touch());
        Sfx.init(this);
    }

    @Override protected void onResume() {
        super.onResume();
        podOnly = getIntent().getBooleanExtra("pod_only", false) || prefs(this).getBoolean("cover_ipod_only", true);
        if (podOnly) {
            // use the look of your iPod Music widget if you have one, otherwise the defaults
            int[] mus = Updater.ids(this, Cfg.MUSIC);
            podCfg = mus.length > 0 ? Cfg.load(this, mus[0], Cfg.MUSIC).copy() : new Cfg(0, Cfg.MUSIC, Cfg.defaults(Cfg.MUSIC));
            podCfg.put("fill", true);
            image.setPadding(0, 0, 0, 0);
            dots.removeAllViews();
            empty.setVisibility(View.GONE);
            h.removeCallbacks(tick);
            h.post(tick);
            return;
        }
        int pad = Ui.dp(this, 10);
        image.setPadding(pad, pad, pad, Ui.dp(this, 22));
        ids.clear();
        Set<String> hide = hidden(this);
        for (int[] e : Updater.all(this)) if (!hide.contains(String.valueOf(e[0]))) ids.add(e[0]);
        if (index >= ids.size()) index = 0;
        empty.setVisibility(ids.isEmpty() ? View.VISIBLE : View.GONE);
        buildDots();
        h.removeCallbacks(tick);
        h.post(tick);
    }

    @Override protected void onPause() {
        super.onPause();
        h.removeCallbacks(tick);
    }

    void buildDots() {
        dots.removeAllViews();
        if (ids.size() < 2) return;
        for (int i = 0; i < ids.size(); i++) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(i == index ? 0xFFFFFFFF : 0x55FFFFFF);
            d.setBackground(g);
            LinearLayout.LayoutParams q = Ui.lp(Ui.dp(this, 6), Ui.dp(this, 6));
            q.leftMargin = q.rightMargin = Ui.dp(this, 4);
            dots.addView(d, q);
        }
    }

    void redraw() {
        if (image.getWidth() == 0) return;
        if (podOnly) {
            last = Render.render(this, podCfg, image.getWidth(), image.getHeight(), true);
            lastCfg = podCfg;
            image.setImageBitmap(last.bmp);
            return;
        }
        if (ids.isEmpty()) return;
        int id = ids.get(index);
        String type = Updater.typeOf(this, id);
        if (type == null) return;
        Cfg cfg = Cfg.load(this, id, type);
        int vw = image.getWidth() - image.getPaddingLeft() - image.getPaddingRight();
        int vh = image.getHeight() - image.getPaddingTop() - image.getPaddingBottom();
        int[] dp = Updater.sizePx(this, id, type);
        float aspect = dp[1] / (float) Math.max(1, dp[0]);
        int w = vw, hh = Math.round(vw * aspect);
        if (hh > vh) { hh = vh; w = Math.round(vh / aspect); }
        // preview = true draws clocks / elapsed time into the picture; we refresh every second
        last = Render.render(this, cfg, Math.max(64, w), Math.max(48, hh), true);
        lastCfg = cfg;
        image.setImageBitmap(last.bmp);
    }

    /** Converts a touch on the ImageView to bitmap coordinates (FIT_CENTER). */
    float[] toBitmap(float x, float y) {
        Bitmap b = last.bmp;
        float vw = image.getWidth() - image.getPaddingLeft() - image.getPaddingRight();
        float vh = image.getHeight() - image.getPaddingTop() - image.getPaddingBottom();
        float s = Math.min(vw / b.getWidth(), vh / b.getHeight());
        float ox = image.getPaddingLeft() + (vw - b.getWidth() * s) / 2f;
        float oy = image.getPaddingTop() + (vh - b.getHeight() * s) / 2f;
        return new float[]{(x - ox) / s, (y - oy) / s};
    }

    void tap(float x, float y) {
        if (last == null || lastCfg == null) return;
        float[] p = toBitmap(x, y);
        if (p[0] < 0 || p[1] < 0 || p[0] > last.bmp.getWidth() || p[1] > last.bmp.getHeight()) return;
        if (podOnly) {
            for (int i = last.zones.size() - 1; i >= 0; i--) {
                Render.Zone z = last.zones.get(i);
                if (!z.r.contains(p[0], p[1]) || !z.action.startsWith("ipod:")) continue;
                String cmd = z.action.substring(5);
                android.content.Intent in = null;
                if (cmd.equals("menu")) in = MusicService.menuIntent(this, podCfg);
                else if (cmd.equals("center") || (cmd.equals("play") && MusicService.now() == null)) in = MusicService.playerIntent(this, podCfg);
                if (in != null) {
                    try { startActivity(in); } catch (Exception ignored) { }
                } else {
                    MusicService.command(this, cmd);
                }
                fastUntil = System.currentTimeMillis() + 1600;
                h.removeCallbacks(tick);
                h.postDelayed(tick, 250);
                return;
            }
            return;
        }
        PendingIntent pi = null;
        // later zones sit on top in the widget, so search from the end
        for (int i = last.zones.size() - 1; i >= 0; i--) {
            Render.Zone z = last.zones.get(i);
            RectF r = z.r;
            if (r.contains(p[0], p[1])) {
                pi = Updater.zoneIntent(this, lastCfg, z.action, i);
                break;
            }
        }
        if (pi == null) pi = Updater.tapIntent(this, lastCfg);
        if (pi == null) return;
        try {
            pi.send();
        } catch (Exception ignored) { }
        fastUntil = System.currentTimeMillis() + 1600;
        h.removeCallbacks(tick);
        h.postDelayed(tick, 40);
    }

    void go(int d) {
        if (podOnly || ids.size() < 2) return;
        index = (index + d + ids.size()) % ids.size();
        buildDots();
        image.setAlpha(0.4f);
        image.setTranslationX(d * Ui.dp(this, 40));
        redraw();
        image.animate().alpha(1f).translationX(0).setDuration(180).start();
    }

    /** One volume step from the click wheel: haptic tick + on-screen bar. */
    void volume(int dir) {
        android.media.AudioManager am = getSystemService(android.media.AudioManager.class);
        if (am == null) return;
        am.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                dir > 0 ? android.media.AudioManager.ADJUST_RAISE : android.media.AudioManager.ADJUST_LOWER, 0);
        int max = Math.max(1, am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC));
        Ipod.VOL_LEVEL = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) / (float) max;
        Ipod.VOL_UNTIL = System.currentTimeMillis() + 1400;
        image.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        fastUntil = System.currentTimeMillis() + 1500;
        h.removeCallbacks(tick);
        h.post(tick);
    }

    class Touch implements View.OnTouchListener {
        float sx, sy;
        long t0;
        boolean onRing, rotating, seeking;
        int seekDir, seekCount;
        double lastAng, acc;

        final Runnable holdSeek = new Runnable() {
            @Override public void run() {
                if (rotating) return;
                seeking = true;
                long step = seekCount < 4 ? 5000 : seekCount < 10 ? 10000 : 20000;
                if (MusicService.seek(seekDir * step)) {
                    image.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                    fastUntil = System.currentTimeMillis() + 800;
                    h.removeCallbacks(tick);
                    h.post(tick);
                }
                seekCount++;
                h.postDelayed(this, 300);
            }
        };

        /** -1 for the ◀◀ zone, 1 for ▶▶, 0 elsewhere. */
        int skipZone(float bx, float by) {
            for (int i = last.zones.size() - 1; i >= 0; i--) {
                Render.Zone z = last.zones.get(i);
                if (!z.r.contains(bx, by)) continue;
                if (z.action.endsWith("ipod:prev")) return -1;
                if (z.action.endsWith("ipod:next")) return 1;
            }
            return 0;
        }
        static final double STEP = Math.PI / 9; // 20 degrees per volume step

        double angle(float bx, float by) {
            return Math.atan2(by - last.wcy, bx - last.wcx);
        }

        @Override public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    sx = e.getX();
                    sy = e.getY();
                    t0 = System.currentTimeMillis();
                    onRing = false;
                    rotating = false;
                    seeking = false;
                    seekCount = 0;
                    acc = 0;
                    h.removeCallbacks(holdSeek);
                    if (last != null && last.wheel) {
                        float[] p = toBitmap(e.getX(), e.getY());
                        double d = Math.hypot(p[0] - last.wcx, p[1] - last.wcy);
                        onRing = d > last.wRc * 1.05f && d < last.wR * 1.1f;
                        if (onRing) lastAng = angle(p[0], p[1]);
                        seekDir = skipZone(p[0], p[1]);
                        if (seekDir != 0) h.postDelayed(holdSeek, 450);
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (seeking) return true;
                    if (!onRing || last == null) return true;
                    float[] p = toBitmap(e.getX(), e.getY());
                    double a = angle(p[0], p[1]);
                    double da = a - lastAng;
                    if (da > Math.PI) da -= 2 * Math.PI;
                    if (da < -Math.PI) da += 2 * Math.PI;
                    lastAng = a;
                    acc += da;
                    if (Math.abs(acc) > Math.toRadians(6)) {
                        rotating = true;
                        h.removeCallbacks(holdSeek);
                    }
                    // screen y points down, so a positive angle change is clockwise = louder
                    while (acc >= STEP) { volume(1); acc -= STEP; }
                    while (acc <= -STEP) { volume(-1); acc += STEP; }
                    return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    h.removeCallbacks(holdSeek);
                    return true;
                case MotionEvent.ACTION_UP: {
                    h.removeCallbacks(holdSeek);
                    if (rotating || seeking) return true;
                    float dx = e.getX() - sx, dy = e.getY() - sy;
                    float slop = Ui.dp(CoverActivity.this, 48);
                    if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) go(dx < 0 ? 1 : -1);
                    else if (Math.abs(dx) < Ui.dp(CoverActivity.this, 12) && Math.abs(dy) < Ui.dp(CoverActivity.this, 12)
                            && System.currentTimeMillis() - t0 < 600) tap(e.getX(), e.getY());
                    return true;
                }
            }
            return true;
        }
    }
}
