package com.widgetstudio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Editor for one widget: live preview on top, every option below. */
public class ConfigActivity extends Activity {
    int id;
    String type;
    Cfg cfg;
    ImageView preview;
    LinearLayout form;
    boolean edit;
    int slot = -1;          // >= 0: editing one block of a Custom Combo
    Cfg parent;             // the combo being edited (block mode)
    boolean editingBlock;   // combo editor launched a block editor; reload on return

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        edit = getIntent().getBooleanExtra("edit", false);
        Intent res = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        setResult(edit ? RESULT_OK : RESULT_CANCELED, res);
        type = getIntent().getStringExtra("type");
        if (type == null) type = Updater.typeOf(this, id);
        if (type == null) { finish(); return; }
        slot = getIntent().getIntExtra("slot", -1);
        if (slot >= 0) {
            parent = Cfg.load(this, id, Cfg.COMBO);
            org.json.JSONObject bj = Combo.block(parent, slot);
            type = bj.optString("type", Cfg.QUOTE);
            cfg = new Cfg(id, type, bj);
        } else {
            cfg = Cfg.load(this, id, type);
        }
        // optional preset from the launching intent (used for previews / testing)
        for (String k : new String[]{"theme", "font", "style"}) {
            String v = getIntent().getStringExtra(k);
            if (v != null) cfg.put(k, v);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        FrameLayout pv = new FrameLayout(this);
        GradientDrawable wall = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0C100F, 0xFF2A3833, 0xFF3D5A3A});
        pv.setBackground(wall);
        int pad = Ui.dp(this, 18);
        pv.setPadding(pad, Ui.dp(this, 40), pad, pad);
        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pv.addView(preview, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 210)));
        root.addView(pv, Ui.matchWrap());

        ScrollView sv = new ScrollView(this);
        form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int fp = Ui.dp(this, 16);
        form.setPadding(fp, fp, fp, Ui.dp(this, 24));
        sv.addView(form);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(fp, Ui.dp(this, 8), fp, Ui.dp(this, 12));
        Button save = Ui.button(this, slot >= 0 ? "Save block" : "Save widget", true);
        save.setOnClickListener(v -> save());
        bar.addView(save, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1f));
        root.addView(bar, Ui.matchWrap());
        setContentView(root);

        buildForm();
        changed();
        if ((type.equals(Cfg.CAL) || type.equals(Cfg.EVENTS)) && !Events.allowed(this)) {
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR}, 1);
        }
    }

    void pickPhoto(int code) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Choose a photo"), code);
    }

    @Override protected void onActivityResult(int rc, int res, Intent data) {
        super.onActivityResult(rc, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null || (rc != 41 && rc != 42 && rc != 43)) return;
        if (rc == 43) {
            String p = CoinPhoto.saveFull(this, data.getData(), id);
            if (p == null) {
                android.widget.Toast.makeText(this, "Couldn't read that photo", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            cfg.put("photo", p);
            rebuild();
            return;
        }
        String path = CoinPhoto.save(this, data.getData(), id, rc == 41 ? "h" : "t");
        if (path == null) {
            android.widget.Toast.makeText(this, "Couldn't read that photo", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        cfg.put(rc == 41 ? "photoH" : "photoT", path);
        rebuild();
    }

    @Override public void onRequestPermissionsResult(int rc, String[] p, int[] g) {
        changed();
    }

    @Override protected void onResume() {
        super.onResume();
        if (editingBlock && type.equals(Cfg.COMBO)) {
            editingBlock = false;
            cfg = Cfg.load(this, id, Cfg.COMBO);
            rebuild();
            return;
        }
        if (preview != null) changed();
    }

    void save() {
        if (slot >= 0) {
            Combo.setBlock(parent, slot, cfg.o);
            parent.save(this);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) Updater.update(this, id);
            finish();
            return;
        }
        cfg.save(this);
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            Updater.update(this, id);
            Updater.schedule(this);
        }
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id));
        finish();
    }

    void changed() {
        int[] sz = slot >= 0 ? Combo.slotSize(parent, slot, Updater.sizePx(this, id, Cfg.COMBO)[0],
                Updater.sizePx(this, id, Cfg.COMBO)[1], getResources().getDisplayMetrics().density)
                : Updater.sizePx(this, id, type);
        Render.Out out = Render.render(this, cfg, sz[0], sz[1], true);
        preview.setImageBitmap(out.bmp);
    }

    void rebuild() {
        form.removeAllViews();
        buildForm();
        changed();
    }

    // ------------------------------------------------------------------ form
    void buildForm() {
        form.addView(Ui.text(this, Cfg.label(type), 24, Ui.TEXT, true));
        TextView d = Ui.text(this, Cfg.describe(type), 14, Ui.SUB, false);
        d.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 8));
        form.addView(d);
        boolean ipod = type.equals(Cfg.MUSIC);

        switch (type) {
            case Cfg.CAL: {
                LinearLayout c = section("Content");
                toggle(c, "Weather tile", "showWeather", true);
                toggle(c, "Next event line", "showEvent", true);
                chips(c, "Week starts on", new String[]{"1", "0"}, new String[]{"Monday", "Sunday"}, "weekStart", "1", true);
                if (cfg.b("showWeather", true)) citySection();
                break;
            }
            case Cfg.EVENTS: {
                LinearLayout c = section("Content");
                seek(c, "Events shown", "max", 1, 6, 3, "");
                seek(c, "Look ahead (days)", "days", 1, 60, 14, "");
                toggle(c, "Use calendar colours", "eventColors", false);
                break;
            }
            case Cfg.STORAGE: {
                LinearLayout c = section("Content");
                toggle(c, "Usage bar", "bar", true);
                break;
            }
            case Cfg.BIGDATE: {
                LinearLayout c = section("Content");
                toggle(c, "Weather sentence", "weatherLine", true);
                toggle(c, "Arrow button", "button", true);
                appPicker(c, "Arrow button opens", "btnApp", "Calendar");
                toggle(c, "Text shadow", "shadow", true);
                citySection();
                break;
            }
            case Cfg.QUOTE: {
                LinearLayout qs = section("Quotes");
                chips(qs, "Show", new String[]{"collection", "mine"}, new String[]{"Quote collection", "My own text"}, "mode",
                        Quotes.on(cfg) ? "collection" : "mine", false, this::rebuild);
                if (Quotes.on(cfg)) {
                    chips(qs, "Quotes", Quotes.SETS, Quotes.SET_LABELS, "set", "mix", false);
                    chips(qs, "Change", Quotes.EVERY, Quotes.EVERY_LABELS, "every", "6h", false);
                    toggle(qs, "Tap the widget for the next quote", "tapNext", true);
                    toggle(qs, "Show author / shayar", "showAuthor", true);
                    Button nx = Ui.button(this, "Next quote", false);
                    nx.setOnClickListener(v -> {
                        Quotes.next(this, cfg);
                        changed();
                    });
                    qs.addView(nx);
                    TextView qh = Ui.text(this, Quotes.EN.length + " English quotes and " + Quotes.SHER.length
                            + " shayari by Jaun Elia, Ghalib, Mir, Iqbal, Faiz, Faraz, Bashir Badr, Nida Fazli, Rahat Indori, Gulzar and Dushyant Kumar. "
                            + "With tap-for-next on, a tap on the widget shows the next one instead of opening this editor.", 12, Ui.SUB, false);
                    qh.setPadding(0, Ui.dp(this, 8), 0, 0);
                    qs.addView(qh);
                }
                LinearLayout c = section(Quotes.on(cfg) ? "Text" : "Your text");
                if (!Quotes.on(cfg)) edit(c, "Quote", "text", true,
                        "Start a line with + to make it bigger (++ even bigger) or ~ to make it smaller.\n"
                                + "**bold**   *italic*   ==accent colour==");
                if (!Quotes.on(cfg)) edit(c, "Author (optional)", "author", false, null);
                seek(c, "Weight", "weight", 300, 900, 500, "");
                seek(c, "Line spacing", "spacing", 70, 200, 110, "%");
                toggle(c, "Text shadow (no background)", "shadow", true);
                break;
            }
            case Cfg.COMBO: {
                LinearLayout c = section("Layout");
                chips(c, "Arrangement", Combo.LAYOUTS, Combo.LAYOUT_LABELS, "layout", "hero", false, this::rebuild);
                seek(c, "Space between blocks", "gap", 0, 32, 14, "dp");
                if (cfg.s("layout", "hero").equals("custom")) {
                    String[] nk = {"1", "2", "3", "4", "5", "6", "7", "8"};
                    chips(c, "Number of blocks", nk, nk, "count", "3", true, this::rebuild);
                    TextView fl = Ui.text(this, "Free layout uses a 12 × 12 grid. Set each block's position and size below — blocks can be any shape and may overlap (later blocks sit on top).", 12, Ui.SUB, false);
                    fl.setPadding(0, Ui.dp(this, 8), 0, 0);
                    c.addView(fl);
                }
                String[] keys = new String[Theme.ALL.length], labels = new String[Theme.ALL.length];
                for (int i = 0; i < keys.length; i++) { keys[i] = Theme.ALL[i].key; labels[i] = Theme.ALL[i].label; }
                c.addView(Ui.label(this, "Apply a theme to every block"));
                LinearLayout row = chipRow(c);
                for (int i = 0; i < keys.length; i++) {
                    final String k = keys[i];
                    TextView ch = Ui.chip(this, labels[i], false);
                    ch.setOnClickListener(v -> {
                        cfg.put("theme", k);
                        for (int sIdx = 0; sIdx < Combo.slots(cfg); sIdx++) {
                            org.json.JSONObject bj = Combo.block(cfg, sIdx);
                            try { bj.put("theme", k); bj.remove("accent"); } catch (Exception ignored) { }
                        }
                        changed();
                    });
                    row.addView(ch);
                }
                int n = Combo.slots(cfg);
                for (int sIdx = 0; sIdx < n; sIdx++) {
                    final int si = sIdx;
                    org.json.JSONObject bj = Combo.block(cfg, si);
                    String bt = bj.optString("type", Cfg.QUOTE);
                    LinearLayout sc = section("Block " + (si + 1) + " · " + Cfg.label(bt));
                    LinearLayout br = chipRow(sc);
                    for (int bi = 0; bi < Combo.BLOCKS.length; bi++) {
                        final String nt = Combo.BLOCKS[bi];
                        TextView ch = Ui.chip(this, Combo.BLOCK_LABELS[bi], nt.equals(bt));
                        ch.setOnClickListener(v -> {
                            Combo.replaceBlock(cfg, si, nt);
                            rebuild();
                        });
                        br.addView(ch);
                    }
                    if (cfg.s("layout", "hero").equals("custom")) {
                        int[] gp = Combo.grid(bj, si, n);
                        seekBlock(sc, bj, "Left", "gx", 0, Combo.GRID - 1, gp[0]);
                        seekBlock(sc, bj, "Top", "gy", 0, Combo.GRID - 1, gp[1]);
                        seekBlock(sc, bj, "Width", "gw", 1, Combo.GRID, gp[2]);
                        seekBlock(sc, bj, "Height", "gh", 1, Combo.GRID, gp[3]);
                    }
                    Button eb = Ui.button(this, "Customise this block", false);
                    eb.setOnClickListener(v -> {
                        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
                            android.widget.Toast.makeText(this, "Add the widget to your home screen first", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        cfg.save(this);
                        editingBlock = true;
                        Intent i = new Intent(this, ConfigActivity.class)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("edit", true)
                                .putExtra("slot", si).setData(android.net.Uri.parse("ws://block/" + id + "/" + si));
                        startActivity(i);
                    });
                    sc.addView(eb, Ui.matchWrap());
                }
                break;
            }
            case Cfg.WEATHER: {
                citySection();
                break;
            }
            case Cfg.PHOTO: {
                LinearLayout c = section("Photo");
                Button pb = Ui.button(this, cfg.s("photo", "").isEmpty() ? "Choose a photo" : "Change photo ✓", true);
                pb.setOnClickListener(v -> pickPhoto(43));
                c.addView(pb, Ui.matchWrap());
                edit(c, "Caption (optional)", "caption", false, null);
                break;
            }
            case Cfg.CLOCK: {
                LinearLayout c = section("Clock");
                chips(c, "Style", Clocks.STYLES, Clocks.LABELS, "style", "lock", false, this::rebuild);
                String st = cfg.s("style", "lock");
                if (st.equals("analog")) toggle(c, "Sweeping seconds hand", "seconds", true);
                if (st.equals("world")) {
                    String[] zk = new String[Clocks.ZONES.length], zl = new String[Clocks.ZONES.length];
                    for (int i = 0; i < zk.length; i++) { zk[i] = Clocks.ZONES[i][0]; zl[i] = Clocks.ZONES[i][1]; }
                    chips(c, "First city", zk, zl, "tz1", "Asia/Kolkata", false);
                    chips(c, "Second city", zk, zl, "tz2", "America/New_York", false);
                }
                if (Clocks.perFont(st)) {
                    TextView nf = Ui.text(this, "Pick any font below (Style → Font) — the time stays live in that font. Turn off Background card for the launcher look.", 12, Ui.SUB, false);
                    nf.setPadding(0, Ui.dp(this, 6), 0, 0);
                    c.addView(nf);
                }
                TextView ch = Ui.text(this, "Clocks tick live on the home screen (12 / 24-hour follows your phone's setting). The preview here shows the current time.", 12, Ui.SUB, false);
                ch.setPadding(0, Ui.dp(this, 8), 0, 0);
                c.addView(ch);
                break;
            }
            case Cfg.DOTS: {
                LinearLayout c = section("Dots");
                chips(c, "Show", new String[]{"year", "month", "custom"},
                        new String[]{"This year", "This month", "Custom range"}, "period", "year", false, this::rebuild);
                if (cfg.s("period", "year").equals("custom")) {
                    dateTime(c, "Start date", "start");
                    dateTime(c, "End date", "end");
                }
                chips(c, "Dot shape", new String[]{"circle", "rounded", "square"},
                        new String[]{"Circle", "Rounded", "Square"}, "shape", "circle", false);
                chips(c, "Caption", new String[]{"left_pct", "day_of", "passed", "none"},
                        new String[]{"349d left · 4%", "Day 16 · of 365", "Passed · left", "No caption"}, "caption", "left_pct", false);
                TextView hint = Ui.text(this, "Passed days use the text colour, today uses the accent colour, days left are dimmed.", 12, Ui.SUB, false);
                hint.setPadding(0, Ui.dp(this, 8), 0, 0);
                c.addView(hint);
                break;
            }
            case Cfg.COUNTER: counterForm(); break;
            case Cfg.MUSIC: {
                LinearLayout c = section("Player");
                chips(c, "Style", new String[]{"modern", "classic"}, new String[]{"Modern (minimal)", "Classic iPod"},
                        "podStyle", "modern", false, this::rebuild);
                podChips(c);
                orient(c);
                chips(c, "Screen", new String[]{"dark", "light"}, new String[]{"Dark", "Classic light"}, "screen", "dark", false);
                appPicker(c, "MENU button opens (also used when nothing is playing)", "musicApp", "Current player");
                boolean ok = MusicService.hasAccess(this);
                TextView st = Ui.text(this, ok ? "Music access is on." :
                        "To show songs and use the wheel, turn on Notification access for Widget Studio.", 14,
                        ok ? Ui.ACCENT : 0xFFFFB86B, false);
                st.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 6));
                c.addView(st);
                if (!ok) {
                    Button b = Ui.button(this, "Open notification access", false);
                    b.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
                    c.addView(b);
                }
                break;
            }
            case Cfg.APPLIST: {
                AppList.clearIcons();
                LinearLayout c = section("Apps");
                chips(c, "Show", new String[]{"all", "pick"}, new String[]{"All apps", "Only apps I choose"}, "src", "all", false, this::rebuild);
                if (cfg.s("src", "all").equals("pick")) {
                    Button pick = Ui.button(this, "Choose apps (" + AppList.chosen(cfg).size() + " chosen)", true);
                    pick.setOnClickListener(v -> pickApps());
                    c.addView(pick, Ui.matchWrap());
                    chips(c, "Order", new String[]{"az", "mine"}, new String[]{"A - Z", "The order I picked"}, "sort", "az", false, this::rebuild);
                }
                if (!AppList.grid(cfg)) toggle(c, "A - Z letter headings", "sections", false);
                LinearLayout l = section("Layout");
                chips(l, "Layout", new String[]{"list", "grid"}, new String[]{"List", "Grid"}, "layout", "list", false, this::rebuild);
                if (AppList.grid(cfg)) seek(l, "Columns", "cols", 2, 7, 4, "");
                seek(l, "Icon size", "iconSize", 20, 72, 38, "dp");
                seek(l, "Space between apps", "rowGap", 0, 24, 7, "dp");
                chips(l, "Icons", new String[]{"launcher", "app"}, new String[]{"Match my home screen", "Apps' own icons"}, "iconSource", "launcher", false,
                        AppList::clearIcons);
                if (cfg.s("iconSource", "launcher").equals("launcher") && !AppList.launcherIcons(this)) {
                    TextView ni = Ui.text(this, "Reach Launcher isn't installed, so apps' own icons are used.", 12, Ui.SUB, false);
                    l.addView(ni);
                }
                chips(l, "Icon shape", new String[]{"original", "squircle", "circle"}, new String[]{"Original", "Squircle", "Circle"}, "iconShape", "original", false);
                toggle(l, "Black & white icons", "mono", false);
                toggle(l, "App names", "labels", true);
                seek(l, "Name size", "labelSize", 10, 26, 15, "sp");
                seek(l, "Name weight", "weight", 300, 800, 500, "");
                if (!AppList.grid(cfg)) toggle(l, "Divider lines", "dividers", false);
                LinearLayout hd = section("Header");
                toggle(hd, "Show header", "header", true);
                edit(hd, "Title", "title", false, null);
                seek(hd, "Title size", "titleSize", 12, 34, 19, "sp");
                toggle(hd, "App count", "count", true);
                TextView help = Ui.text(this, "On the home screen the list scrolls; tap an app to open it. Theme, colours, font, background and corners are in Style below. Turn off Background card for a transparent list over your wallpaper.", 13, Ui.SUB, false);
                help.setPadding(0, Ui.dp(this, 10), 0, 0);
                hd.addView(help);
                break;
            }
            case Cfg.LUCKY: {
                LinearLayout c = section("Lucky 7");
                toggle(c, "Sound effects", "sound", true);
                Button reset = Ui.button(this, "Reset wins and losses", false);
                reset.setOnClickListener(v -> {
                    Lucky.reset(this, cfg);
                    changed();
                });
                c.addView(reset);
                Button all = Ui.button(this, "Reset all games", false);
                all.setOnClickListener(v -> Games.confirmResetAll(this, this::changed));
                c.addView(all);
                TextView help = Ui.text(this, "Tap Below 7 (2–6), Lucky 7 (exactly 7) or Above 7 (8–12) to roll both dice. The widget counts your wins and losses. Tap the dice to repeat your last pick, or the ↻ button to reset the count.", 13, Ui.SUB, false);
                help.setPadding(0, Ui.dp(this, 10), 0, 0);
                c.addView(help);
                break;
            }
            case Cfg.GAME: {
                LinearLayout c = section("Games");
                chips(c, "Starting game", Games.ORDER, new String[]{"Coin flip", "Dice", "Tic-tac-toe", "Lucky 7"}, "game", "ttt", false,
                        () -> {
                            Games.S s = Games.load(this, cfg);
                            s.game = cfg.s("game", "ttt");
                            Games.save(this, cfg, s);
                        });
                chips(c, "Dice", new String[]{"1", "2"}, new String[]{"One die", "Two dice"}, "dice", "2", true);
                toggle(c, "Sound effects", "sound", true);
                LinearLayout cs = section("Coin");
                String[] ck = new String[Ipod.COINS.length], cl = new String[Ipod.COINS.length];
                for (int i = 0; i < ck.length; i++) { ck[i] = Ipod.COINS[i][0]; cl[i] = Ipod.COINS[i][1]; }
                chips(cs, "Coin style", ck, cl, "coinStyle", "gold", false);
                chips(cs, "Coin faces", new String[]{"letters", "rupee", "yes_no", "photo"},
                        new String[]{"H / T", "₹ / 1", "Yes / No", "My photos"}, "coinFace", "letters", false, this::rebuild);
                if (cfg.s("coinFace", "letters").equals("photo")) {
                    Button hb = Ui.button(this, cfg.s("photoH", "").isEmpty() ? "Choose heads photo" : "Change heads photo ✓", false);
                    hb.setOnClickListener(v -> pickPhoto(41));
                    cs.addView(hb, Ui.matchWrap());
                    Button tb = Ui.button(this, cfg.s("photoT", "").isEmpty() ? "Choose tails photo (optional)" : "Change tails photo ✓", false);
                    tb.setOnClickListener(v -> pickPhoto(42));
                    cs.addView(tb, Ui.matchWrap());
                    TextView ph = Ui.text(this, "Photos are cropped to a circle inside the coin. Without a tails photo, tails shows a T.", 12, Ui.SUB, false);
                    ph.setPadding(0, Ui.dp(this, 6), 0, 0);
                    cs.addView(ph);
                }
                chips(c, "Phone skill (tic-tac-toe)", new String[]{"easy", "hard"}, new String[]{"Easy", "Hard"}, "ai", "hard", false);
                Button reset = Ui.button(this, "Reset scores", false);
                reset.setOnClickListener(v -> {
                    Games.S s = new Games.S();
                    s.game = cfg.s("game", "ttt");
                    Games.save(this, cfg, s);
                    changed();
                });
                c.addView(reset);
                Button all = Ui.button(this, "Reset all games", false);
                all.setOnClickListener(v -> Games.confirmResetAll(this, this::changed));
                c.addView(all);
                TextView help = Ui.text(this, "On the widget: tap the Coin / Dice / Tic-Tac-Toe / Lucky 7 tabs to switch, the button to flip, roll or start a new round, and tap squares to play. In Lucky 7, tap Below 7, Lucky 7 or Above 7 to roll; tap the dice to repeat your pick. ↻ resets the score of the game you're on.", 13, Ui.SUB, false);
                help.setPadding(0, Ui.dp(this, 10), 0, 0);
                c.addView(help);
                break;
            }
        }

        LinearLayout s = section(ipod ? "Text & finish" : "Style");
        chips(s, "Finish", new String[]{"matte", "gloss"}, new String[]{"Matte", "Gloss"}, "finish", "matte", false);
        toggle(s, "Depth shadow (floating card)", "depth", true);
        if (!ipod) {
            String[] keys = new String[Theme.ALL.length], labels = new String[Theme.ALL.length];
            for (int i = 0; i < keys.length; i++) { keys[i] = Theme.ALL[i].key; labels[i] = Theme.ALL[i].label; }
            chips(s, "Theme", keys, labels, "theme", "slate", false);
            colors(s, "Accent colour", "accent");
            toggle(s, "Background card", "bg", true);
            seek(s, "Background opacity", "opacity", 0, 100, 100, "%");
            seek(s, "Corner radius", "radius", 0, 48, 24, "dp");
        }
        fontPicker(s);
        seek(s, "Text size", "scale", 50, 200, 100, "%");
        if (type.equals(Cfg.BIGDATE) || type.equals(Cfg.QUOTE) || type.equals(Cfg.COUNTER) || type.equals(Cfg.EVENTS)) {
            chips(s, "Alignment", new String[]{"0", "1", "2"}, new String[]{"Left", "Centre", "Right"}, "align", "0", true);
        }
        if (type.equals(Cfg.MUSIC)) customColors(Theme.POD_PARTS, true);
        else if (!type.equals(Cfg.COMBO)) customColors(Theme.CARD_PARTS, false);
        if (!ipod && slot < 0) {
            LinearLayout t = section("When tapped");
            chips(t, "Open", new String[]{"config", "calendar", "clock", "storage", "app", "none"},
                    new String[]{"This editor", "Calendar", "Clock", "Storage", "An app…", "Nothing"}, "tap",
                    cfg.s("tap", "config").startsWith("app:") ? "app" : cfg.s("tap", "config"), false,
                    () -> { if (cfg.s("tap", "").equals("app")) pickApp("tap"); });
        }
    }

    void counterForm() {
        LinearLayout p = section("What are you counting?");
        LinearLayout row = chipRow(p);
        String[][] presets = {{"Age", "My age", "up"}, {"Relationship", "Together", "up"},
                {"Project", "Project", "timeline"}, {"Countdown", "Countdown", "down"}, {"Sober / streak", "Streak", "up"}};
        for (String[] pr : presets) {
            TextView ch = Ui.chip(this, pr[0], false);
            ch.setOnClickListener(v -> {
                cfg.put("title", pr[1]);
                cfg.put("mode", pr[2]);
                long now = System.currentTimeMillis();
                if (pr[0].equals("Age")) {
                    cfg.put("uY", true); cfg.put("uM", true); cfg.put("uD", true); cfg.put("uW", false);
                    cfg.put("start", now - 25L * 365 * 86400000L);
                } else if (pr[2].equals("timeline")) {
                    cfg.put("start", now - 30L * 86400000L);
                    cfg.put("end", now + 60L * 86400000L);
                } else if (pr[2].equals("down")) {
                    cfg.put("end", now + 45L * 86400000L);
                } else {
                    cfg.put("start", now - 400L * 86400000L);
                }
                rebuild();
            });
            row.addView(ch);
        }
        LinearLayout c = section("Details");
        edit(c, "Title", "title", false, null);
        edit(c, "Note under the numbers (optional)", "subtitle", false, null);
        chips(c, "Mode", new String[]{"up", "down", "timeline"}, new String[]{"Count up (since)", "Count down (until)", "Timeline (start → end)"},
                "mode", "up", false, this::rebuild);
        String mode = cfg.s("mode", "up");
        dateTime(c, mode.equals("down") ? "Start (unused for countdown)" : "Start date & time (e.g. birth, first date)", "start");
        if (!mode.equals("up")) dateTime(c, "End / target date & time", "end");
        LinearLayout f = section("Format");
        chips(f, "Show as", new String[]{"breakdown", "days", "weeks", "months", "hours", "minutes"},
                new String[]{"Years · months · days", "Total days", "Total weeks", "Total months", "Total hours", "Total minutes"},
                "style", "breakdown", false, this::rebuild);
        if (cfg.s("style", "breakdown").equals("breakdown")) {
            toggle(f, "Years", "uY", true);
            toggle(f, "Months", "uM", true);
            toggle(f, "Weeks", "uW", false);
            toggle(f, "Days", "uD", true);
        }
        toggle(f, "Live hours : minutes : seconds", "live", true);
    }

    /** Slider that edits a value inside one combo block (free-layout position / size). */
    void seekBlock(LinearLayout parent, org.json.JSONObject bj, String label, String key, int min, int max, int cur) {
        TextView l = Ui.label(this, label + ": " + cur);
        parent.addView(l);
        SeekBar sb = new SeekBar(this);
        sb.setMin(min);
        sb.setMax(max);
        sb.setProgress(cur);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
                if (!user) return;
                try { bj.put(key, v); } catch (Exception ignored) { }
                l.setText(label + ": " + v);
                changed();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        parent.addView(sb, Ui.matchWrap());
    }

    int partDefault(String key, boolean pod) {
        if (pod) return Theme.podPart(Theme.pod(cfg.s("ipod", "silver")), cfg.s("screen", "dark").equals("light"), key);
        return Theme.get(cfg.s("theme", "slate")).part(key);
    }

    /** Every colour of the widget, individually customisable. */
    void customColors(String[][] parts, boolean pod) {
        LinearLayout c = section("Custom colours");
        TextView h = Ui.text(this, "Tap any part to choose its exact colour. The preview updates live; Reset returns it to the theme.", 13, Ui.SUB, false);
        h.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 6));
        c.addView(h);
        for (String[] part : parts) {
            final String key = part[0], label = part[1];
            final int def = partDefault(key, pod);
            final boolean custom = cfg.o.has(key);
            int cur = custom ? cfg.i(key, def) : def;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Ui.dp(this, 9), 0, Ui.dp(this, 9));
            TextView l = Ui.text(this, label, 15, Ui.TEXT, false);
            row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (custom) {
                TextView tag = Ui.text(this, "custom", 12, Ui.ACCENT, true);
                tag.setPadding(0, 0, Ui.dp(this, 10), 0);
                row.addView(tag);
            }
            View sw = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(cur);
            g.setStroke(Ui.dp(this, 1), 0x66FFFFFF);
            sw.setBackground(g);
            row.addView(sw, Ui.lp(Ui.dp(this, 32), Ui.dp(this, 32)));
            row.setOnClickListener(v -> {
                final boolean had = cfg.o.has(key);
                final int prev = cfg.i(key, def);
                ColorPicker.show(this, label, had ? prev : def, col -> { cfg.put(key, col); changed(); }, r -> {
                    if (r == 1 || (r == 2 && !had)) cfg.o.remove(key);
                    else if (r == 2) cfg.put(key, prev);
                    rebuild();
                });
            });
            c.addView(row, Ui.matchWrap());
        }
        Button reset = Ui.button(this, "Reset all colours to the theme", false);
        reset.setOnClickListener(v -> {
            for (String[] part : parts) cfg.o.remove(part[0]);
            rebuild();
        });
        c.addView(reset, Ui.matchWrap());
    }

    /** City search for weather (shared by all widgets), usable right inside the editor. */
    void citySection() {
        LinearLayout c = section("Weather city");
        String cur = Weather.city(this);
        TextView now = Ui.text(this, cur.isEmpty() ? "No city chosen yet." : "Using " + cur
                + (Weather.fahrenheit(this) ? " (°F)" : " (°C)"), 14, Ui.SUB, false);
        now.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 6));
        c.addView(now);
        LinearLayout sr = new LinearLayout(this);
        EditText q = new EditText(this);
        q.setHint("Search a city, e.g. Gurugram");
        q.setTextColor(Ui.TEXT);
        q.setHintTextColor(Ui.SUB);
        q.setSingleLine(true);
        sr.addView(q, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button go = Ui.button(this, "Search", true);
        sr.addView(go);
        c.addView(sr, Ui.matchWrap());
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        c.addView(results, Ui.matchWrap());
        android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        go.setOnClickListener(v -> {
            String text = q.getText().toString().trim();
            if (text.isEmpty()) return;
            results.removeAllViews();
            results.addView(Ui.text(this, "Searching…", 14, Ui.SUB, false));
            new Thread(() -> {
                List<String[]> r;
                try { r = Weather.search(text); } catch (Exception e) { r = null; }
                final List<String[]> res = r;
                main.post(() -> {
                    results.removeAllViews();
                    if (res == null) { results.addView(Ui.text(this, "No connection. Try again.", 14, 0xFFFFB86B, false)); return; }
                    if (res.isEmpty()) { results.addView(Ui.text(this, "No match found.", 14, Ui.SUB, false)); return; }
                    for (String[] row : res) {
                        Button b = Ui.button(this, row[0], false);
                        b.setOnClickListener(x -> {
                            Weather.setCity(this, row[1], Double.parseDouble(row[2]), Double.parseDouble(row[3]));
                            now.setText("Using " + row[1] + " — loading weather…");
                            results.removeAllViews();
                            new Thread(() -> {
                                Weather.fetch(this);
                                Updater.updateAll(this);
                                main.post(() -> { now.setText("Using " + row[1]); changed(); });
                            }).start();
                        });
                        results.addView(b, Ui.matchWrap());
                    }
                });
            }).start();
        });
    }

    // ------------------------------------------------------------------ helpers
    LinearLayout section(String title) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, title, 17, Ui.TEXT, true));
        form.addView(c);
        return c;
    }

    LinearLayout chipRow(LinearLayout parent) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        hs.addView(row);
        parent.addView(hs, Ui.matchWrap());
        return row;
    }

    void chips(LinearLayout parent, String label, String[] keys, String[] labels, String key, String def, boolean asInt) {
        chips(parent, label, keys, labels, key, def, asInt, null);
    }

    void chips(LinearLayout parent, String label, String[] keys, String[] labels, String key, String def, boolean asInt,
               Runnable after) {
        chips(parent, label, keys, labels, key, def, false, asInt, after);
    }

    void chips(LinearLayout parent, String label, String[] keys, String[] labels, String key, String current,
               boolean unused, boolean asInt, Runnable after) {
        parent.addView(Ui.label(this, label));
        LinearLayout row = chipRow(parent);
        String cur = asInt ? String.valueOf(cfg.i(key, Integer.parseInt(current))) : (cfg.o.has(key) && !key.equals("tap") ? cfg.s(key, current) : current);
        List<TextView> all = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            final String k = keys[i];
            TextView ch = Ui.chip(this, labels[i], k.equals(cur));
            all.add(ch);
            ch.setOnClickListener(v -> {
                if (asInt) cfg.put(key, Integer.parseInt(k)); else cfg.put(key, k);
                for (TextView o : all) Ui.select(o, o == ch);
                if (after != null) after.run();
                changed();
            });
            row.addView(ch);
        }
    }

    void podChips(LinearLayout c) {
        String[] keys = new String[Theme.PODS.length], labels = new String[Theme.PODS.length];
        for (int i = 0; i < keys.length; i++) { keys[i] = Theme.PODS[i].key; labels[i] = Theme.PODS[i].label; }
        chips(c, "Body colour", keys, labels, "ipod", type.equals(Cfg.MUSIC) ? "silver" : "black", false);
    }

    void orient(LinearLayout c) {
        chips(c, "Layout", new String[]{"auto", "horizontal", "vertical"}, new String[]{"Auto (fits widget)", "Horizontal", "Vertical"},
                "orient", "auto", false);
    }

    void toggle(LinearLayout parent, String label, String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(Ui.TEXT);
        s.setTextSize(15);
        s.setChecked(cfg.b(key, def));
        s.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        s.setOnCheckedChangeListener((b, on) -> { cfg.put(key, on); changed(); });
        parent.addView(s, Ui.matchWrap());
    }

    void seek(LinearLayout parent, String label, String key, int min, int max, int def, String suffix) {
        TextView l = Ui.label(this, label + ": " + cfg.i(key, def) + suffix);
        parent.addView(l);
        SeekBar sb = new SeekBar(this);
        sb.setMin(min);
        sb.setMax(max);
        sb.setProgress(cfg.i(key, def));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
                cfg.put(key, v);
                l.setText(label + ": " + v + suffix);
                changed();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        parent.addView(sb, Ui.matchWrap());
    }

    void edit(LinearLayout parent, String label, String key, boolean multi, String hint) {
        parent.addView(Ui.label(this, label));
        EditText e = new EditText(this);
        e.setTextColor(Ui.TEXT);
        e.setHintTextColor(Ui.SUB);
        e.setText(cfg.s(key, ""));
        e.setBackground(Ui.round(Ui.CARD2, Ui.dp(this, 12)));
        int p = Ui.dp(this, 12);
        e.setPadding(p, p, p, p);
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        }
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { cfg.put(key, s.toString()); changed(); }
        });
        parent.addView(e, Ui.matchWrap());
        if (hint != null) {
            TextView h = Ui.text(this, hint, 12, Ui.SUB, false);
            h.setPadding(0, Ui.dp(this, 6), 0, 0);
            parent.addView(h);
        }
    }

    void colors(LinearLayout parent, String label, String key) {
        parent.addView(Ui.label(this, label));
        LinearLayout row = chipRow(parent);
        List<View> sw = new ArrayList<>();
        int cur = cfg.i(key, Theme.get(cfg.s("theme", "slate")).accent);
        for (int col : Theme.SWATCHES) {
            View v = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(col);
            g.setStroke(Ui.dp(this, col == cur ? 3 : 1), col == cur ? Ui.TEXT : 0x55FFFFFF);
            v.setBackground(g);
            LinearLayout.LayoutParams q = Ui.lp(Ui.dp(this, 36), Ui.dp(this, 36));
            q.rightMargin = Ui.dp(this, 10);
            v.setTag(col);
            sw.add(v);
            v.setOnClickListener(x -> {
                cfg.put(key, col);
                for (View o : sw) {
                    int oc = (int) o.getTag();
                    ((GradientDrawable) o.getBackground()).setStroke(Ui.dp(this, oc == col ? 3 : 1), oc == col ? Ui.TEXT : 0x55FFFFFF);
                }
                changed();
            });
            row.addView(v, q);
        }
        EditText hex = new EditText(this);
        hex.setHint("#RRGGBB");
        hex.setTextColor(Ui.TEXT);
        hex.setHintTextColor(Ui.SUB);
        hex.setSingleLine(true);
        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                String t = s.toString().trim();
                if (!t.startsWith("#")) t = "#" + t;
                if (t.length() == 7 || t.length() == 9) {
                    try { cfg.put(key, Color.parseColor(t)); changed(); } catch (Exception ignored) { }
                }
            }
        });
        row.addView(hex, Ui.lp(Ui.dp(this, 120), ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    void fontPicker(LinearLayout parent) {
        parent.addView(Ui.label(this, "Font"));
        Spinner sp = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (String[] r : Fonts.LIST) labels.add(r[1]);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, labels) {
            TextView style(View v, int pos) {
                TextView t = (TextView) v;
                t.setTypeface(Fonts.base(getContext(), Fonts.LIST[pos][0]));
                t.setTextSize(18);
                t.setTextColor(Ui.TEXT);
                t.setPadding(Ui.dp(getContext(), 8), Ui.dp(getContext(), 10), Ui.dp(getContext(), 8), Ui.dp(getContext(), 10));
                return t;
            }
            @Override public View getView(int pos, View cv, ViewGroup p) { return style(super.getView(pos, cv, p), pos); }
            @Override public View getDropDownView(int pos, View cv, ViewGroup p) { return style(super.getDropDownView(pos, cv, p), pos); }
        };
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        String cur = cfg.s("font", "sans");
        for (int i = 0; i < Fonts.LIST.length; i++) if (Fonts.LIST[i][0].equals(cur)) sp.setSelection(i);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> a, View v, int pos, long l) {
                cfg.put("font", Fonts.LIST[pos][0]);
                changed();
            }
            @Override public void onNothingSelected(AdapterView<?> a) { }
        });
        sp.setBackground(Ui.round(Ui.CARD2, Ui.dp(this, 12)));
        parent.addView(sp, Ui.matchWrap());
    }

    void appPicker(LinearLayout parent, String label, String key, String none) {
        parent.addView(Ui.label(this, label));
        Button b = Ui.button(this, appLabel(cfg.s(key, ""), none), false);
        b.setOnClickListener(v -> pickApp(key, b, none));
        parent.addView(b, Ui.matchWrap());
    }

    String appLabel(String v, String none) {
        if (v == null || !v.startsWith("app:")) return none;
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(v.substring(4), 0)).toString();
        } catch (Exception e) {
            return v.substring(4);
        }
    }

    /** Multi-select app chooser for the App List widget; keeps the existing order and appends new picks. */
    void pickApps() {
        List<AppList.App> all = AppList.installed(this);
        List<String> cur = AppList.chosen(cfg);
        String[] names = new String[all.size()];
        boolean[] on = new boolean[all.size()];
        for (int i = 0; i < all.size(); i++) {
            names[i] = all.get(i).label;
            on[i] = cur.contains(all.get(i).key());
        }
        new AlertDialog.Builder(this).setTitle("Choose apps")
                .setMultiChoiceItems(names, on, (d, w, checked) -> on[w] = checked)
                .setPositiveButton("Done", (d, w) -> {
                    List<String> keep = new ArrayList<>();
                    for (String k : cur) {
                        for (int i = 0; i < all.size(); i++) if (on[i] && all.get(i).key().equals(k)) keep.add(k);
                    }
                    for (int i = 0; i < all.size(); i++) if (on[i] && !keep.contains(all.get(i).key())) keep.add(all.get(i).key());
                    cfg.put("apps", new org.json.JSONArray(keep));
                    rebuild();
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear all", (d, w) -> { cfg.put("apps", new org.json.JSONArray()); rebuild(); })
                .show();
    }

    void pickApp(String key) { pickApp(key, null, null); }

    void pickApp(String key, Button target, String none) {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        List<String[]> apps = new ArrayList<>();
        for (ResolveInfo r : list) apps.add(new String[]{r.loadLabel(pm).toString(), r.activityInfo.packageName});
        Collections.sort(apps, (a, c) -> a[0].compareToIgnoreCase(c[0]));
        String[] names = new String[apps.size() + (none != null ? 1 : 0)];
        int off = 0;
        if (none != null) { names[0] = none + " (default)"; off = 1; }
        for (int i = 0; i < apps.size(); i++) names[i + off] = apps.get(i)[0];
        final int o = off;
        new AlertDialog.Builder(this).setTitle("Choose an app").setItems(names, (d, w) -> {
            String val = (none != null && w == 0) ? "" : "app:" + apps.get(w - o)[1];
            cfg.put(key, val);
            if (target != null) target.setText(appLabel(val, none));
            changed();
        }).show();
    }

    void dateTime(LinearLayout parent, String label, String key) {
        parent.addView(Ui.label(this, label));
        SimpleDateFormat df = new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault());
        Button b = Ui.button(this, df.format(new Date(cfg.l(key, System.currentTimeMillis()))), false);
        b.setOnClickListener(v -> {
            Calendar k = Calendar.getInstance();
            k.setTimeInMillis(cfg.l(key, System.currentTimeMillis()));
            new DatePickerDialog(this, (dp, y, m, d) -> {
                k.set(y, m, d);
                new TimePickerDialog(this, (tp, hh, mm) -> {
                    k.set(Calendar.HOUR_OF_DAY, hh);
                    k.set(Calendar.MINUTE, mm);
                    k.set(Calendar.SECOND, 0);
                    cfg.put(key, k.getTimeInMillis());
                    b.setText(df.format(k.getTime()));
                    changed();
                }, k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE), true).show();
            }, k.get(Calendar.YEAR), k.get(Calendar.MONTH), k.get(Calendar.DAY_OF_MONTH)).show();
        });
        parent.addView(b, Ui.matchWrap());
    }
}
