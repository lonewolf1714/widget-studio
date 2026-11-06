package com.widgetstudio;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Home screen of the app: settings, permissions, add / edit widgets. */
public class MainActivity extends Activity {
    LinearLayout col, mine, results, coverList;
    TextView cityLabel, calStatus, musicStatus;
    Button calBtn, musicBtn;
    final Handler main = new Handler(Looper.getMainLooper());

    /** Renders every widget with sample data to PNGs (used to build widget-picker previews). */
    void exportPreviews() {
        new Thread(() -> {
            Render.DEMO = true;
            try {
                java.io.File dir = getExternalFilesDir(null);
                float d = getResources().getDisplayMetrics().density;
                try (java.io.FileOutputStream fo = new java.io.FileOutputStream(new java.io.File(dir, "preview_player.png"))) {
                    PlayerWidget.draw(this, Math.round(200 * d), Math.round(76 * d)).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fo);
                }
                for (String type : Cfg.TYPES) {
                    Cfg cfg = new Cfg(0, type, Cfg.defaults(type));
                    if (type.equals(Cfg.BIGDATE) || type.equals(Cfg.QUOTE)) { cfg.put("bg", true); cfg.put("theme", "slate"); }
                    if (type.equals(Cfg.GAME)) cfg.put("theme", "midnight");
                    if (type.equals(Cfg.COUNTER)) cfg.put("theme", "aurora");
                    int[] dp = Updater.defaultDp(type);
                    Render.Out o = Render.render(this, cfg, Math.round(dp[0] * d), Math.round(dp[1] * d), true);
                    try (java.io.FileOutputStream fo = new java.io.FileOutputStream(new java.io.File(dir, "preview_" + type + ".png"))) {
                        o.bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fo);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                Render.DEMO = false;
                main.post(() -> Toast.makeText(this, "Previews exported", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (getIntent().getBooleanExtra("export_previews", false)) exportPreviews();
        // "iPod Player" app shortcut (long-press the Widget Studio icon)
        if ("com.widgetstudio.OPEN_PLAYER".equals(getIntent().getAction())) {
            startActivity(new Intent(this, CoverActivity.class).putExtra("pod_only", true));
            finish();
            return;
        }
        // launched on the Flip cover screen: go straight to Cover mode
        if (CoverActivity.onCover(this) && !getIntent().getBooleanExtra("export_previews", false)) {
            startActivity(new Intent(this, CoverActivity.class));
            finish();
            return;
        }
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 18);
        col.setPadding(p, Ui.dp(this, 36), p, Ui.dp(this, 32));
        sv.addView(col);
        setContentView(sv);

        col.addView(Ui.text(this, "Widget Studio", 30, Ui.TEXT, true));
        TextView sub = Ui.text(this, "Your own home-screen widgets. Add one below, then tap it (or use Edit) to customise everything.", 14, Ui.SUB, false);
        sub.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 18));
        col.addView(sub);

        // weather
        LinearLayout w = Ui.card(this);
        w.addView(Ui.text(this, "Weather location", 17, Ui.TEXT, true));
        cityLabel = Ui.text(this, "", 14, Ui.SUB, false);
        cityLabel.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        w.addView(cityLabel);
        LinearLayout sr = new LinearLayout(this);
        EditText q = new EditText(this);
        q.setHint("Search a city, e.g. Gurugram");
        q.setTextColor(Ui.TEXT);
        q.setHintTextColor(Ui.SUB);
        q.setSingleLine(true);
        sr.addView(q, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button go = Ui.button(this, "Search", true);
        sr.addView(go);
        w.addView(sr, Ui.matchWrap());
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        w.addView(results, Ui.matchWrap());
        go.setOnClickListener(v -> search(q.getText().toString().trim()));
        LinearLayout units = new LinearLayout(this);
        units.setPadding(0, Ui.dp(this, 10), 0, 0);
        TextView c = Ui.chip(this, "°C", !Weather.fahrenheit(this));
        TextView f = Ui.chip(this, "°F", Weather.fahrenheit(this));
        c.setOnClickListener(v -> setUnits(false, c, f));
        f.setOnClickListener(v -> setUnits(true, c, f));
        units.addView(c);
        units.addView(f);
        w.addView(units);
        col.addView(w);

        // permissions
        LinearLayout perm = Ui.card(this);
        perm.addView(Ui.text(this, "Access", 17, Ui.TEXT, true));
        calStatus = Ui.text(this, "", 14, Ui.SUB, false);
        calStatus.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 4));
        perm.addView(calStatus);
        calBtn = Ui.button(this, "Allow calendar", false);
        calBtn.setOnClickListener(v -> requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, 7));
        perm.addView(calBtn);
        musicStatus = Ui.text(this, "", 14, Ui.SUB, false);
        musicStatus.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 4));
        perm.addView(musicStatus);
        musicBtn = Ui.button(this, "Open notification access", false);
        musicBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        perm.addView(musicBtn);
        col.addView(perm);

        // add widgets
        LinearLayout add = Ui.card(this);
        add.addView(Ui.text(this, "Add a widget", 17, Ui.TEXT, true));
        for (String t : Cfg.TYPES) if (!t.equals(Cfg.LUCKY)) add.addView(addRow(t)); // Lucky 7 lives in Games now
        col.addView(add);

        // cover mode
        LinearLayout cov = Ui.card(this);
        cov.addView(Ui.text(this, "Cover mode", 17, Ui.TEXT, true));
        TextView ch = Ui.text(this, "Shows your widgets full-size on the Flip cover screen, live and tappable — swipe to switch. "
                + "On the cover: add MultiStar's Launcher widget (Good Lock → MultiStar → I ♡ Galaxy Foldable → Launcher widget) "
                + "or allow Widget Studio in Settings → Advanced features → Labs → Apps allowed on cover screen, then open Widget Studio there.",
                13, Ui.SUB, false);
        ch.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 8));
        cov.addView(ch);
        android.widget.Switch podSw = new android.widget.Switch(this);
        podSw.setText("iPod player only (full screen)");
        podSw.setTextColor(Ui.TEXT);
        podSw.setTextSize(15);
        podSw.setChecked(CoverActivity.prefs(this).getBoolean("cover_ipod_only", true));
        podSw.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 10));
        podSw.setOnCheckedChangeListener((b2, on) -> CoverActivity.prefs(this).edit().putBoolean("cover_ipod_only", on).apply());
        cov.addView(podSw, Ui.matchWrap());
        coverList = new LinearLayout(this);
        coverList.setOrientation(LinearLayout.VERTICAL);
        cov.addView(coverList, Ui.matchWrap());
        Button tryCover = Ui.button(this, "Preview Cover mode", true);
        tryCover.setOnClickListener(v -> startActivity(new Intent(this, CoverActivity.class)));
        cov.addView(tryCover, Ui.matchWrap());
        col.addView(cov);

        // games
        LinearLayout gm = Ui.card(this);
        gm.addView(Ui.text(this, "Games", 17, Ui.TEXT, true));
        TextView gh = Ui.text(this, "Start fresh on every game widget: coin, dice, tic-tac-toe and Lucky 7 scores.", 13, Ui.SUB, false);
        gh.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 8));
        gm.addView(gh);
        Button resetGames = Ui.button(this, "Reset all games", false);
        resetGames.setOnClickListener(v -> Games.confirmResetAll(this, null));
        gm.addView(resetGames, Ui.matchWrap());
        col.addView(gm);

        // my widgets
        LinearLayout m = Ui.card(this);
        m.addView(Ui.text(this, "Your widgets", 17, Ui.TEXT, true));
        mine = new LinearLayout(this);
        mine.setOrientation(LinearLayout.VERTICAL);
        m.addView(mine, Ui.matchWrap());
        col.addView(m);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] g) {
        refresh();
        new Thread(() -> Updater.updateAll(this)).start();
    }

    void refresh() {
        String city = Weather.city(this);
        cityLabel.setText(city.isEmpty() ? "No city chosen yet — weather widgets will ask for one." : "Using " + city);
        boolean cal = Events.allowed(this);
        calStatus.setText(cal ? "Calendar: allowed ✓" : "Calendar: needed for events and the calendar widget.");
        calBtn.setVisibility(cal ? View.GONE : View.VISIBLE);
        boolean mus = MusicService.hasAccess(this);
        musicStatus.setText(mus ? "Music (notification access): on ✓" :
                "Music: the iPod player needs Notification access to read the song and control playback.");
        musicBtn.setVisibility(mus ? View.GONE : View.VISIBLE);

        mine.removeAllViews();
        List<int[]> all = Updater.all(this);
        coverList.removeAllViews();
        java.util.Set<String> hide = CoverActivity.hidden(this);
        for (int[] e : all) {
            android.widget.Switch sw = new android.widget.Switch(this);
            sw.setText("Show " + Cfg.label(Cfg.TYPES[e[1]]) + " on the cover");
            sw.setTextColor(Ui.TEXT);
            sw.setChecked(!hide.contains(String.valueOf(e[0])));
            sw.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
            final int wid = e[0];
            sw.setOnCheckedChangeListener((b2, on) -> CoverActivity.setHidden(this, wid, !on));
            coverList.addView(sw, Ui.matchWrap());
        }
        if (all.isEmpty()) {
            TextView t = Ui.text(this, "None yet. Add one above, or long-press your home screen → Widgets → Widget Studio.", 14, Ui.SUB, false);
            t.setPadding(0, Ui.dp(this, 6), 0, 0);
            mine.addView(t);
        }
        for (int[] e : all) {
            String type = Cfg.TYPES[e[1]];
            LinearLayout row = new LinearLayout(this);
            row.setPadding(0, Ui.dp(this, 10), 0, 0);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            ImageView iv = new ImageView(this);
            int[] sz = Updater.sizePx(this, e[0], type);
            iv.setImageBitmap(Render.render(this, Cfg.load(this, e[0], type), sz[0], sz[1], true).bmp);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            row.addView(iv, new LinearLayout.LayoutParams(Ui.dp(this, 120), Ui.dp(this, 64)));
            TextView t = Ui.text(this, Cfg.label(type), 15, Ui.TEXT, true);
            t.setPadding(Ui.dp(this, 12), 0, 0, 0);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Button ed = Ui.button(this, "Edit", true);
            ed.setOnClickListener(v -> startActivity(Updater.configIntent(this, e[0])));
            row.addView(ed);
            mine.addView(row, Ui.matchWrap());
        }
    }

    View addRow(String type) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.addView(Ui.text(this, Cfg.label(type), 15, Ui.TEXT, true));
        txt.addView(Ui.text(this, Cfg.describe(type), 13, Ui.SUB, false));
        row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button b = Ui.button(this, "Add", false);
        b.setOnClickListener(v -> pin(type));
        row.addView(b);
        return row;
    }

    void pin(String type) {
        AppWidgetManager m = getSystemService(AppWidgetManager.class);
        ComponentName cn = new ComponentName(this, Updater.providerFor(type));
        if (m != null && m.isRequestPinAppWidgetSupported()) {
            m.requestPinAppWidget(cn, null, null);
        } else {
            Toast.makeText(this, "Long-press your home screen → Widgets → Widget Studio → " + Cfg.label(type),
                    Toast.LENGTH_LONG).show();
        }
    }

    void setUnits(boolean f, TextView c, TextView fv) {
        Weather.p(this).edit().putBoolean("fahrenheit", f).apply();
        Ui.select(c, !f);
        Ui.select(fv, f);
        new Thread(() -> { Weather.fetch(this); Updater.updateAll(this); }).start();
    }

    void search(String q) {
        if (q.isEmpty()) return;
        results.removeAllViews();
        results.addView(Ui.text(this, "Searching…", 14, Ui.SUB, false));
        new Thread(() -> {
            List<String[]> r;
            try { r = Weather.search(q); } catch (Exception e) { r = null; }
            final List<String[]> res = r;
            main.post(() -> {
                results.removeAllViews();
                if (res == null) { results.addView(Ui.text(this, "No connection. Try again.", 14, 0xFFFFB86B, false)); return; }
                if (res.isEmpty()) { results.addView(Ui.text(this, "No match found.", 14, Ui.SUB, false)); return; }
                for (String[] row : res) {
                    Button b = Ui.button(this, row[0], false);
                    b.setOnClickListener(v -> {
                        Weather.setCity(this, row[1], Double.parseDouble(row[2]), Double.parseDouble(row[3]));
                        results.removeAllViews();
                        refresh();
                        new Thread(() -> { Weather.fetch(this); Updater.updateAll(this); }).start();
                    });
                    results.addView(b, Ui.matchWrap());
                }
            });
        }).start();
    }
}
