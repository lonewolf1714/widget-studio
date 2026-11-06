package com.widgetstudio;

/**
 * Colour presets. frame/frame2 = outer card gradient (top-left -> bottom-right),
 * panel/panel2 = inner card gradient, border = hairline edge, glass = frosted rendering.
 */
final class Theme {
    final String key, label;
    final int frame, frame2, border, frameText, frameSub, panel, panel2, text, sub, accent;
    final boolean glass;

    Theme(String key, String label, int frame, int frame2, int border, int frameText, int frameSub,
          int panel, int panel2, int text, int sub, int accent, boolean glass) {
        this.key = key;
        this.label = label;
        this.frame = frame;
        this.frame2 = frame2;
        this.border = border;
        this.frameText = frameText;
        this.frameSub = frameSub;
        this.panel = panel;
        this.panel2 = panel2;
        this.text = text;
        this.sub = sub;
        this.accent = accent;
        this.glass = glass;
    }

    static final Theme[] ALL = {
            new Theme("ios_dark", "iOS Dark", 0xFF1C1C1E, 0xFF1C1C1E, 0x14FFFFFF, 0xFFFFFFFF, 0x99EBEBF5,
                    0xFF2C2C2E, 0xFF2C2C2E, 0xFFFFFFFF, 0x99EBEBF5, 0xFF0A84FF, false),
            new Theme("ios_light", "iOS Light", 0xFFFFFFFF, 0xFFFFFFFF, 0x0F000000, 0xFF000000, 0x993C3C43,
                    0xFFF2F2F7, 0xFFF2F2F7, 0xFF000000, 0x993C3C43, 0xFF007AFF, false),
            new Theme("liquid", "Liquid glass (iOS)", 0x24FFFFFF, 0x0AFFFFFF, 0xE6FFFFFF, 0xFFFFFFFF, 0xDEFFFFFF,
                    0x1FFFFFFF, 0x0AFFFFFF, 0xFFFFFFFF, 0xDEFFFFFF, 0xFFFFFFFF, true),
            new Theme("glass", "Frosted glass", 0x38FFFFFF, 0x12FFFFFF, 0x59FFFFFF, 0xFFFFFFFF, 0xC8FFFFFF,
                    0x24FFFFFF, 0x0FFFFFFF, 0xFFFFFFFF, 0xC8FFFFFF, 0xFFFFFFFF, true),
            new Theme("smoked", "Smoked glass", 0xA6141816, 0x73080A09, 0x33FFFFFF, 0xFFFFFFFF, 0xB3FFFFFF,
                    0x1FFFFFFF, 0x0AFFFFFF, 0xFFFFFFFF, 0xB3FFFFFF, 0xFFB9E86A, true),
            new Theme("black", "Pure black (AMOLED)", 0xFF000000, 0xFF000000, 0x1AFFFFFF, 0xFFFFFFFF, 0xFF8E8E93,
                    0xFF121212, 0xFF0A0A0A, 0xFFFFFFFF, 0xFF8E8E93, 0xFFFFFFFF, false),
            new Theme("charcoal", "Charcoal", 0xFF1E1E1E, 0xFF161616, 0x12FFFFFF, 0xFFFFFFFF, 0xFF8C8C8C,
                    0xFF2A2A2A, 0xFF222222, 0xFFFFFFFF, 0xFF8C8C8C, 0xFFFF6B3D, false),
            new Theme("slate", "Slate green", 0xFF22302B, 0xFF111815, 0x1FFFFFFF, 0xFFF2F5F3, 0xFF93A79E,
                    0xFF2E3C36, 0xFF232E2A, 0xFFEEF2EF, 0xFF9DB0A7, 0xFF9FB547, false),
            new Theme("norfolk_dark", "Norfolk dark", 0xFF1A1A1A, 0xFF040404, 0x14FFFFFF, 0xFFFFFFFF, 0xFF8E918C,
                    0xFFC9CCC6, 0xFFAEB1AB, 0xFF111111, 0xFF4E524E, 0xFFFF8A3D, false),
            new Theme("norfolk_light", "Norfolk light", 0xFFFFFFFF, 0xFFE9E9E6, 0x1A000000, 0xFF111111, 0xFF8A8D88,
                    0xFFC9CCC6, 0xFFAEB1AB, 0xFF111111, 0xFF4E524E, 0xFFFF8A3D, false),
            new Theme("midnight", "Midnight", 0xFF1E2229, 0xFF090A0D, 0x1AFFFFFF, 0xFFFFFFFF, 0xFF8A93A0,
                    0xFF2A2F38, 0xFF1F232A, 0xFFF2F4F7, 0xFF8E97A4, 0xFF7AA2FF, false),
            new Theme("aurora", "Aurora", 0xFF2A1650, 0xFF07343A, 0x26FFFFFF, 0xFFFFFFFF, 0xFFB9C6E0,
                    0x29FFFFFF, 0x0DFFFFFF, 0xFFFFFFFF, 0xFFC4D0E6, 0xFF7CF3D2, false),
            new Theme("gold", "Black & gold", 0xFF1C1A17, 0xFF070605, 0x40D9B45B, 0xFFF6EEDC, 0xFFB9A57A,
                    0xFF27241F, 0xFF1A1815, 0xFFF6EEDC, 0xFFB9A57A, 0xFFD9B45B, false),
            new Theme("ember", "Ember", 0xFF35190E, 0xFF0F0806, 0x1FFFB48A, 0xFFFFF3EC, 0xFFC09A86,
                    0xFF3D2216, 0xFF2A1810, 0xFFFFF3EC, 0xFFC09A86, 0xFFFF7A2F, false),
            new Theme("paper", "Paper & ink", 0xFF323232, 0xFF1A1A1A, 0x14FFFFFF, 0xFFF0EEE9, 0xFF9A978F,
                    0xFF444444, 0xFF363636, 0xFFF0EEE9, 0xFFA3A098, 0xFFF5C542, false),
            new Theme("light", "Porcelain", 0xFFFFFFFF, 0xFFEEEEEA, 0x14000000, 0xFF111111, 0xFF8A8A8A,
                    0xFFF1F1EE, 0xFFE3E3DF, 0xFF111111, 0xFF6B6B6B, 0xFF111111, false),
    };

    /** Customisable card parts: {cfg key, label}. "accent" is handled separately. */
    static final String[][] CARD_PARTS = {
            {"c_frame", "Background (top)"}, {"c_frame2", "Background (bottom)"}, {"c_border", "Border & rim"},
            {"c_frameText", "Text"}, {"c_frameSub", "Secondary text"}, {"c_panel", "Inner card (top)"},
            {"c_panel2", "Inner card (bottom)"}, {"c_text", "Inner card text"}, {"c_sub", "Inner card secondary text"},
            {"accent", "Accent"}};

    /** Customisable iPod parts: {cfg key, label}. */
    static final String[][] POD_PARTS = {
            {"p_body", "Body (top)"}, {"p_bodyDark", "Body (bottom)"}, {"p_edge", "Body edge"},
            {"p_wheel", "Click wheel"}, {"p_center", "Centre button"}, {"p_label", "Wheel labels & icons"},
            {"p_bezel", "Screen bezel"}, {"p_screen1", "Screen colour 1"}, {"p_screen2", "Screen colour 2"},
            {"p_title", "Song title & time"}, {"p_artist", "Artist"}, {"p_bar", "Progress bar"},
            {"p_track", "Progress track"}};

    static int ov(Cfg c, String k, int d) {
        return c != null && c.o.has(k) ? c.i(k, d) : d;
    }

    /** This theme with any per-widget colour overrides applied. */
    Theme with(Cfg c) {
        return new Theme(key, label, ov(c, "c_frame", frame), ov(c, "c_frame2", frame2), ov(c, "c_border", border),
                ov(c, "c_frameText", frameText), ov(c, "c_frameSub", frameSub), ov(c, "c_panel", panel),
                ov(c, "c_panel2", panel2), ov(c, "c_text", text), ov(c, "c_sub", sub), accent, glass);
    }

    /** Theme default for a card part (before overrides). */
    int part(String k) {
        switch (k) {
            case "c_frame": return frame;
            case "c_frame2": return frame2;
            case "c_border": return border;
            case "c_frameText": return frameText;
            case "c_frameSub": return frameSub;
            case "c_panel": return panel;
            case "c_panel2": return panel2;
            case "c_text": return text;
            case "c_sub": return sub;
            default: return accent;
        }
    }

    static Theme get(String k) {
        for (Theme t : ALL) if (t.key.equals(k)) return t;
        return ALL[0];
    }

    static final int[] SWATCHES = {
            0xFF8FA23B, 0xFF9CCB5A, 0xFF35C2B0, 0xFF5B8CFF, 0xFF9B59D0, 0xFFFF5C8A,
            0xFFFF7A2F, 0xFFF5C542, 0xFFE5484D, 0xFFFFFFFF, 0xFFB7BAB4, 0xFF111111
    };

    /** iPod body colours: body, bodyDark, wheel, center, label, edge. */
    static final class Pod {
        final String key, label;
        final int body, bodyDark, wheel, center, label2, edge;

        Pod(String key, String label, int body, int bodyDark, int wheel, int center, int labelColor, int edge) {
            this.key = key;
            this.label = label;
            this.body = body;
            this.bodyDark = bodyDark;
            this.wheel = wheel;
            this.center = center;
            this.label2 = labelColor;
            this.edge = edge;
        }
    }

    static final Pod[] PODS = {
            new Pod("graphite", "Graphite", 0xFF30343D, 0xFF272A32, 0xFF1C1F25, 0xFF262930, 0xFFF2F2F4, 0xFF15171B),
            new Pod("silver", "Silver", 0xFFE4E5E7, 0xFFC9CACD, 0xFFF6F6F6, 0xFFE2E2E3, 0xFFA2A3A6, 0xFFB5B6B9),
            new Pod("black", "Jet black", 0xFF0B0B0C, 0xFF000000, 0xFF141415, 0xFF050505, 0xFF8E8E93, 0xFF000000),
            new Pod("white", "White", 0xFFFAFAFA, 0xFFE9E9EB, 0xFFFFFFFF, 0xFFF0F0F1, 0xFFB0B0B4, 0xFFD5D5D8),
            new Pod("red", "Red", 0xFFD0202E, 0xFFA3121D, 0xFFF7F7F7, 0xFFE9E9EA, 0xFFB03038, 0xFF7E0E16),
            new Pod("blue", "Blue", 0xFF3A78C2, 0xFF245A99, 0xFFF4F6F9, 0xFFE3E8EF, 0xFF6F8FB8, 0xFF1B4677),
            new Pod("gold", "Gold", 0xFFE6D3B1, 0xFFCDB68D, 0xFFFBF8F2, 0xFFEFE8DB, 0xFFB09A73, 0xFFB59D73),
            new Pod("green", "Olive", 0xFF7F9437, 0xFF5E6F25, 0xFFF3F5EC, 0xFFE3E8D2, 0xFF7F8F52, 0xFF46531B),
            new Pod("pink", "Pink", 0xFFF3BFCB, 0xFFE29DAE, 0xFFFFFFFF, 0xFFF7E6EA, 0xFFC98496, 0xFFC98496),
            new Pod("natural_ti", "Natural titanium", 0xFFC4BEB4, 0xFFA59F93, 0xFFEFEDE9, 0xFFDDD9D2, 0xFF8C877E, 0xFF7E796F),
            new Pod("desert_ti", "Desert titanium", 0xFFDCC6AB, 0xFFBDA283, 0xFFF6EFE6, 0xFFE8DCCD, 0xFFA88E70, 0xFF9C8364),
            new Pod("black_ti", "Black titanium", 0xFF3D3D40, 0xFF1F1F21, 0xFF4B4B4E, 0xFF2E2E31, 0xFFA8A8AD, 0xFF111112),
            new Pod("white_ti", "White titanium", 0xFFF3F2EF, 0xFFD8D6D1, 0xFFFFFFFF, 0xFFF0EFEC, 0xFFA9A7A2, 0xFFC9C6C0),
            new Pod("starlight", "Starlight", 0xFFF2EADD, 0xFFDACDB8, 0xFFFFFDF8, 0xFFF3ECE0, 0xFFB4A68F, 0xFFC8B9A0),
            new Pod("midnight", "Midnight", 0xFF262D39, 0xFF12161D, 0xFF323A47, 0xFF1D232C, 0xFF9AA3B2, 0xFF0A0D11),
            new Pod("sierra", "Sierra blue", 0xFFAAC4DC, 0xFF83A0BD, 0xFFF4F8FC, 0xFFDFE8F1, 0xFF6F8BA8, 0xFF6884A0),
            new Pod("alpine", "Alpine green", 0xFF60776A, 0xFF43554A, 0xFFEEF2EE, 0xFFD9E1DA, 0xFF6E8573, 0xFF34433A),
            new Pod("deep_purple", "Deep purple", 0xFF5E5166, 0xFF41384A, 0xFFEFEBF1, 0xFFDCD4E0, 0xFF7D6E88, 0xFF322A38),
            new Pod("space_gray", "Space gray", 0xFF818285, 0xFF5C5D60, 0xFF3B3C3E, 0xFF2A2B2D, 0xFFC6C7CA, 0xFF4A4B4E),
            new Pod("black_red", "Black & red wheel", 0xFF1C1C1D, 0xFF0A0A0A, 0xFFC8102E, 0xFF1C1C1D, 0xFFFFFFFF, 0xFF000000),
            new Pod("yellow", "Nano yellow", 0xFFF4D862, 0xFFDDBB3A, 0xFFFFFFFF, 0xFFF8F1D6, 0xFFB79A2C, 0xFFB79A2C),
            new Pod("purple", "Nano purple", 0xFFA486D6, 0xFF8466BB, 0xFFFFFFFF, 0xFFEEE7F7, 0xFF7D62AA, 0xFF6E54A0),
    };

    /** Default colour of an iPod part for the given finish and screen style (before overrides). */
    static int podPart(Pod p, boolean light, String k) {
        switch (k) {
            case "p_body": return p.body;
            case "p_bodyDark": return p.bodyDark;
            case "p_edge": return p.edge;
            case "p_wheel": return p.wheel;
            case "p_center": return p.center;
            case "p_label": return p.label2;
            case "p_bezel": return 0xFF070707;
            case "p_screen1": return light ? 0xFFEEF4F8 : 0xFF141414;
            case "p_screen2": return light ? 0xFFC5D3DE : 0xFF5E5E5E;
            case "p_title": return light ? 0xFF14202B : 0xFFFFFFFF;
            case "p_artist": return light ? 0xFF51606D : 0xFFA6A6A6;
            case "p_bar": return light ? 0xFF3A78C2 : 0xFF6E6E6E;
            default: return light ? 0xFFA3B2BE : 0xFF2E2E2E; // p_track
        }
    }

    /** Pod with per-widget overrides. */
    static Pod podWith(Pod p, Cfg c) {
        return new Pod(p.key, p.label, ov(c, "p_body", p.body), ov(c, "p_bodyDark", p.bodyDark), ov(c, "p_wheel", p.wheel),
                ov(c, "p_center", p.center), ov(c, "p_label", p.label2), ov(c, "p_edge", p.edge));
    }

    static Pod pod(String k) {
        for (Pod p : PODS) if (p.key.equals(k)) return p;
        return PODS[0];
    }

    static int alpha(int color, float a) {
        int al = Math.round(((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, a)));
        return (color & 0x00FFFFFF) | (al << 24);
    }

    static int luminance(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    /** Readable text colour on top of the given fill. */
    static int on(int fill) {
        return luminance(fill) > 150 ? 0xFF111111 : 0xFFFFFFFF;
    }
}
