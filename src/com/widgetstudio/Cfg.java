package com.widgetstudio;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** Per-widget settings, stored as JSON in SharedPreferences under "w<id>". */
final class Cfg {
    static final String CAL = "cal", EVENTS = "events", STORAGE = "storage", BIGDATE = "bigdate",
            QUOTE = "quote", COUNTER = "counter", MUSIC = "ipod_music", GAME = "ipod_game", DOTS = "dots",
            COMBO = "combo", WEATHER = "weather", PHOTO = "photo", CLOCK = "clock", LUCKY = "lucky7", APPLIST = "applist";

    static final String[] TYPES = {CAL, EVENTS, STORAGE, BIGDATE, QUOTE, COUNTER, MUSIC, GAME, DOTS, COMBO, WEATHER, PHOTO, CLOCK, LUCKY, APPLIST};

    final int id;
    final String type;
    final JSONObject o;

    Cfg(int id, String type, JSONObject o) {
        this.id = id;
        this.type = type;
        this.o = o;
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("widgets", Context.MODE_PRIVATE);
    }

    static boolean exists(Context c, int id) {
        return prefs(c).contains("w" + id);
    }

    static Cfg load(Context c, int id, String type) {
        String s = id == 0 ? null : prefs(c).getString("w" + id, null);
        JSONObject o = null;
        if (s != null) {
            try { o = new JSONObject(s); } catch (Exception ignored) { }
        }
        if (o == null) o = defaults(type);
        String t = o.optString("type", type);
        if (t == null || t.isEmpty()) t = type;
        return new Cfg(id, t, o);
    }

    Cfg copy() {
        try { return new Cfg(id, type, new JSONObject(o.toString())); } catch (Exception e) { return this; }
    }

    void save(Context c) {
        if (id == 0) return;
        put("type", type);
        prefs(c).edit().putString("w" + id, o.toString()).apply();
    }

    static void delete(Context c, int id) {
        prefs(c).edit().remove("w" + id).remove("g" + id).apply();
    }

    String s(String k, String d) { return o.has(k) ? o.optString(k, d) : d; }
    int i(String k, int d) { return o.has(k) ? o.optInt(k, d) : d; }
    long l(String k, long d) { return o.has(k) ? o.optLong(k, d) : d; }
    boolean b(String k, boolean d) { return o.has(k) ? o.optBoolean(k, d) : d; }

    void put(String k, Object v) {
        try { o.put(k, v); } catch (Exception ignored) { }
    }

    static String label(String type) {
        switch (type) {
            case CAL: return "Calendar + Weather";
            case EVENTS: return "Next Events";
            case STORAGE: return "Storage";
            case BIGDATE: return "Big Date";
            case QUOTE: return "Quote";
            case COUNTER: return "Counter";
            case MUSIC: return "iPod Music";
            case GAME: return "Games";
            case DOTS: return "Year in Dots";
            case COMBO: return "Custom Combo";
            case WEATHER: return "Weather";
            case PHOTO: return "Photo";
            case CLOCK: return "Clocks";
            case LUCKY: return "Lucky 7";
            case APPLIST: return "App List";
        }
        return "Widget";
    }

    static String describe(String type) {
        switch (type) {
            case CAL: return "Month, week with today circled, next event and a weather tile.";
            case EVENTS: return "Your upcoming calendar events with coloured tags.";
            case STORAGE: return "Free and used storage with a bar.";
            case BIGDATE: return "Huge date, a weather sentence and an arrow button.";
            case QUOTE: return "Quotes and shayari that change by themselves, or your own text.";
            case COUNTER: return "Age, relationship, project timeline or countdown with live seconds.";
            case MUSIC: return "A click-wheel music player for whatever is playing.";
            case GAME: return "Coin flip, 3D dice, tic-tac-toe and Lucky 7.";
            case DOTS: return "Every day of the year as a dot: passed, today and left.";
            case COMBO: return "Build your own: combine several widgets in one layout.";
            case WEATHER: return "Temperature, condition, high / low and your city.";
            case PHOTO: return "Any picture from your gallery, with an optional caption.";
            case APPLIST: return "A scrolling list or grid of apps: all apps or the ones you pick. Tap to open.";
            case LUCKY: return "Two dice: bet Below 7, Lucky 7 or Above 7.";
            case CLOCK: return "Live clocks: lock screen, bold, analog, flip, world and word clock.";
        }
        return "";
    }

    static JSONObject defaults(String type) {
        JSONObject o = new JSONObject();
        try {
            o.put("type", type);
            o.put("theme", "ios_dark");
            o.put("font", "inter");
            o.put("depth", true);
            o.put("scale", 100);
            o.put("radius", 24);
            o.put("opacity", 100);
            o.put("bg", true);
            o.put("align", 0);
            o.put("finish", "matte");
            long now = System.currentTimeMillis();
            switch (type) {
                case CAL:
                    o.put("theme", "norfolk_dark");
                    o.put("tap", "calendar");
                    o.put("showWeather", true);
                    o.put("showEvent", true);
                    o.put("weekStart", 1);
                    break;
                case EVENTS:
                    o.put("theme", "paper");
                    o.put("tap", "calendar");
                    o.put("max", 3);
                    o.put("days", 14);
                    break;
                case STORAGE:
                    o.put("theme", "paper");
                    o.put("tap", "storage");
                    o.put("bar", true);
                    break;
                case BIGDATE:
                    o.put("bg", false);
                    o.put("font", "sans_black");
                    o.put("tap", "calendar");
                    o.put("shadow", true);
                    o.put("weatherLine", true);
                    o.put("button", true);
                    o.put("btnApp", "");
                    break;
                case QUOTE:
                    o.put("bg", false);
                    o.put("font", "playfair");
                    o.put("text", "+Small steps\nevery single day.");
                    o.put("mode", "collection");
                    o.put("set", "mix");
                    o.put("every", "6h");
                    o.put("tapNext", true);
                    o.put("showAuthor", true);
                    o.put("author", "");
                    o.put("tap", "config");
                    o.put("spacing", 110);
                    o.put("shadow", true);
                    o.put("align", 0);
                    break;
                case COUNTER:
                    o.put("tap", "config");
                    o.put("title", "Together");
                    o.put("mode", "up");
                    o.put("start", now - 365L * 86400000L);
                    o.put("end", now + 90L * 86400000L);
                    o.put("style", "breakdown");
                    o.put("uY", true);
                    o.put("uM", true);
                    o.put("uW", false);
                    o.put("uD", true);
                    o.put("live", true);
                    o.put("subtitle", "");
                    break;
                case MUSIC:
                    o.put("ipod", "graphite");
                    o.put("podStyle", "modern");
                    o.put("orient", "vertical");
                    o.put("orient", "auto");
                    o.put("screen", "dark");
                    o.put("musicApp", "app:com.google.android.apps.youtube.music");
                    o.put("font", "sans_medium");
                    break;
                case CLOCK:
                    o.put("theme", "black");
                    o.put("style", "niagara");
                    o.put("bg", false);
                    o.put("shadow", true);
                    o.put("seconds", true);
                    o.put("tz1", "Asia/Kolkata");
                    o.put("tz2", "America/New_York");
                    o.put("tap", "clock");
                    o.put("accent", 0xFFFF9F0A);
                    break;
                case COMBO:
                    o.put("theme", "midnight");
                    o.put("bg", false);
                    o.put("layout", "hero");
                    o.put("gap", 14);
                    o.put("count", 3);
                    o.put("tap", "config");
                    break;
                case WEATHER:
                    o.put("theme", "liquid");
                    o.put("tap", "config");
                    break;
                case PHOTO:
                    o.put("tap", "config");
                    o.put("photo", "");
                    o.put("caption", "");
                    break;
                case DOTS:
                    o.put("theme", "charcoal");
                    o.put("accent", 0xFFFF6B3D);
                    o.put("tap", "calendar");
                    o.put("period", "year");
                    o.put("shape", "circle");
                    o.put("caption", "left_pct");
                    o.put("start", now);
                    o.put("end", now + 90L * 86400000L);
                    break;
                case APPLIST:
                    o.put("tap", "none");
                    o.put("src", "all");
                    o.put("sort", "az");
                    o.put("layout", "list");
                    o.put("cols", 4);
                    o.put("iconSize", 38);
                    o.put("labelSize", 15);
                    o.put("rowGap", 7);
                    o.put("labels", true);
                    o.put("iconShape", "original");
                    o.put("iconSource", "launcher");
                    o.put("header", true);
                    o.put("title", "Apps");
                    o.put("count", true);
                    o.put("titleSize", 19);
                    break;
                case LUCKY:
                    o.put("theme", "black");
                    o.put("tap", "none");
                    o.put("accent", 0xFFFFC233);
                    o.put("sound", true);
                    break;
                case GAME:
                    o.put("theme", "smoked");
                    o.put("tap", "none");
                    o.put("game", "ttt");
                    o.put("dice", 2);
                    o.put("font", "sans_medium");
                    break;
            }
        } catch (Exception ignored) { }
        return o;
    }
}
