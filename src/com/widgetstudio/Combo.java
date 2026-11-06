package com.widgetstudio;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONObject;

/** Custom Combo: several widgets ("blocks") arranged in one widget, preset or free layout. */
final class Combo {
    static final String[] LAYOUTS = {"custom", "hero", "side", "stack2", "stack3", "row2", "grid4"};
    static final String[] LAYOUT_LABELS = {"Free layout (any size)", "Big top + 2", "Big left + 2", "2 stacked",
            "3 stacked", "Side by side", "2 × 2 grid"};
    static final String[] BLOCKS = {Cfg.BIGDATE, Cfg.WEATHER, Cfg.DOTS, Cfg.MUSIC, Cfg.GAME, Cfg.CAL, Cfg.EVENTS,
            Cfg.COUNTER, Cfg.QUOTE, Cfg.STORAGE, Cfg.PHOTO};
    static final String[] BLOCK_LABELS = {"Big date", "Weather", "Dots", "iPod music", "Games", "Calendar", "Events",
            "Counter", "Text", "Storage", "Photo"};
    static final int GRID = 12, MAX_BLOCKS = 8;
    /** Last full render of each block, reused for the still blocks while one block animates. */
    static final java.util.Map<Integer, Render.Out> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static int slots(Cfg c) {
        String layout = c.s("layout", "hero");
        switch (layout) {
            case "custom": return Math.max(1, Math.min(MAX_BLOCKS, c.i("count", 3)));
            case "stack2": case "row2": return 2;
            case "grid4": return 4;
            default: return 3;
        }
    }

    static String defaultBlock(String layout, int i) {
        String[] d = layout.equals("hero") || layout.equals("side")
                ? new String[]{Cfg.BIGDATE, Cfg.WEATHER, Cfg.DOTS}
                : new String[]{Cfg.WEATHER, Cfg.DOTS, Cfg.EVENTS, Cfg.QUOTE, Cfg.MUSIC, Cfg.GAME, Cfg.STORAGE, Cfg.PHOTO};
        return d[i % d.length];
    }

    /** Unique id for per-block state (game scores, animation). */
    static int blockId(int widgetId, int slot) {
        return widgetId * 100 + slot + 1;
    }

    static Cfg blockCfg(Cfg parent, int slot) {
        JSONObject bj = block(parent, slot);
        return new Cfg(blockId(parent.id, slot), bj.optString("type", Cfg.QUOTE), bj);
    }

    static RectF[] rects(Cfg c, float w, float h, float g) {
        String layout = c.s("layout", "hero");
        if (layout.equals("custom")) {
            int n = slots(c);
            RectF[] out = new RectF[n];
            for (int i = 0; i < n; i++) {
                JSONObject b = block(c, i);
                int[] p = grid(b, i, n);
                float cw = w / GRID, ch = h / GRID;
                RectF r = new RectF(p[0] * cw, p[1] * ch, (p[0] + p[2]) * cw, (p[1] + p[3]) * ch);
                r.inset(g / 2f, g / 2f);
                out[i] = r;
            }
            return out;
        }
        float hw = (w - g) / 2f, hh = (h - g) / 2f;
        switch (layout) {
            case "stack2":
                return new RectF[]{new RectF(0, 0, w, hh), new RectF(0, hh + g, w, h)};
            case "stack3": {
                float th = (h - 2 * g) / 3f;
                return new RectF[]{new RectF(0, 0, w, th), new RectF(0, th + g, w, 2 * th + g), new RectF(0, 2 * th + 2 * g, w, h)};
            }
            case "row2":
                return new RectF[]{new RectF(0, 0, hw, h), new RectF(hw + g, 0, w, h)};
            case "grid4":
                return new RectF[]{new RectF(0, 0, hw, hh), new RectF(hw + g, 0, w, hh),
                        new RectF(0, hh + g, hw, h), new RectF(hw + g, hh + g, w, h)};
            case "side": {
                float lw = (w - g) * 0.56f;
                return new RectF[]{new RectF(0, 0, lw, h), new RectF(lw + g, 0, w, hh), new RectF(lw + g, hh + g, w, h)};
            }
            default: { // hero
                float th = (h - g) * 0.56f;
                return new RectF[]{new RectF(0, 0, w, th), new RectF(0, th + g, hw, h), new RectF(hw + g, th + g, w, h)};
            }
        }
    }

    /** Grid position {x, y, w, h} of a free-layout block (defaults: stacked rows). */
    static int[] grid(JSONObject b, int i, int n) {
        int rowH = Math.max(1, GRID / Math.max(1, n));
        int x = clamp(b.optInt("gx", 0), 0, GRID - 1);
        int y = clamp(b.optInt("gy", Math.min(GRID - 1, i * rowH)), 0, GRID - 1);
        int w = clamp(b.optInt("gw", GRID), 1, GRID - x);
        int h = clamp(b.optInt("gh", i == n - 1 ? GRID - y : rowH), 1, GRID - y);
        return new int[]{x, y, w, h};
    }

    static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    static JSONObject blockDefaults(Cfg parent, String type) {
        JSONObject o = Cfg.defaults(type);
        try {
            if (!type.equals(Cfg.MUSIC)) o.put("theme", parent.s("theme", "midnight"));
            o.put("bg", true);
            o.put("radius", parent.i("radius", 22));
            if (type.equals(Cfg.DOTS) && parent.s("theme", "midnight").equals("charcoal")) o.put("accent", 0xFFFF6B3D);
            else o.remove("accent");
        } catch (Exception ignored) { }
        return o;
    }

    static JSONObject block(Cfg parent, int i) {
        JSONArray a = parent.o.optJSONArray("blocks");
        JSONObject b = a != null ? a.optJSONObject(i) : null;
        if (b == null) {
            b = blockDefaults(parent, defaultBlock(parent.s("layout", "hero"), i));
            setBlock(parent, i, b);
        }
        return b;
    }

    /** Replaces a block's widget type but keeps its free-layout position. */
    static void replaceBlock(Cfg parent, int i, String type) {
        JSONObject old = block(parent, i);
        JSONObject b = blockDefaults(parent, type);
        try {
            for (String k : new String[]{"gx", "gy", "gw", "gh"}) if (old.has(k)) b.put(k, old.getInt(k));
        } catch (Exception ignored) { }
        setBlock(parent, i, b);
    }

    static void setBlock(Cfg parent, int i, JSONObject b) {
        try {
            JSONArray a = parent.o.optJSONArray("blocks");
            if (a == null) {
                a = new JSONArray();
                parent.o.put("blocks", a);
            }
            while (a.length() <= i) a.put(JSONObject.NULL);
            a.put(i, b);
        } catch (Exception ignored) { }
    }

    /** Pixel size of one slot for a combo of size w x h (used by the block editor preview). */
    static int[] slotSize(Cfg parent, int slot, int w, int h, float density) {
        RectF[] r = rects(parent, w, h, parent.i("gap", 14) * density);
        RectF s = r[Math.max(0, Math.min(r.length - 1, slot))];
        return new int[]{Math.max(1, Math.round(s.width())), Math.max(1, Math.round(s.height()))};
    }

    static void draw(Render.Ctx x, Canvas cv, Render.Out out) {
        x.frame(cv);
        float gap = x.cfg.i("gap", 14) * x.density;
        float inset = x.card ? gap : gap * 0.25f;
        RectF[] rs = rects(x.cfg, x.w - 2 * inset, x.h - 2 * inset, gap);
        Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG);
        for (int i = 0; i < rs.length; i++) {
            RectF r = rs[i];
            r.offset(inset, inset);
            if (r.width() < 4 || r.height() < 4) continue;
            Cfg bc = blockCfg(x.cfg, i);
            if (bc.type.equals(Cfg.COMBO)) continue;
            int bw = Math.max(1, Math.round(r.width())), bh = Math.max(1, Math.round(r.height()));
            boolean animating = Games.animId != -1;
            Render.Out o = animating && bc.id != Games.animId ? CACHE.get(bc.id) : null;
            if (o == null || (!animating && (o.bmp.getWidth() != bw || o.bmp.getHeight() != bh))) {
                Render.IN_BLOCK = true;
                try {
                    o = Render.render(x.c, bc, bw, bh, true);
                } finally {
                    Render.IN_BLOCK = false;
                }
                if (!animating) CACHE.put(bc.id, o);
            }
            cv.drawBitmap(o.bmp, null, r, bp);
            // carry the block's buttons over into the combo, tagged with the slot
            float kx = r.width() / o.bmp.getWidth(), ky = r.height() / o.bmp.getHeight();
            for (Render.Zone z : o.zones) {
                RectF zr = new RectF(r.left + z.r.left * kx, r.top + z.r.top * ky, r.left + z.r.right * kx, r.top + z.r.bottom * ky);
                out.zones.add(new Render.Zone(zr, "slot:" + i + ":" + z.action));
            }
        }
    }
}
