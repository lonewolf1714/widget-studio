package com.widgetstudio;

import android.content.Context;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/** State and rules for the iPod games: coin flip, dice, tic-tac-toe (you = X vs phone = O). */
final class Games {
    static final String[] ORDER = {"coin", "dice", "ttt", "lucky"};
    static final SecureRandom RNG = new SecureRandom();

    /** Transient flip/roll animation: widget id being animated and progress 0..1 (or -1). */
    static volatile int animId = -1;
    static volatile float animPhase = -1f;
    static volatile int animFrame = 0;

    static float phase(int id) {
        return id == animId ? animPhase : -1f;
    }
    static final int[][] LINES = {{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, {0, 4, 8}, {2, 4, 6}};

    static final class S {
        String game = "ttt";
        int coin = -1, heads, tails;
        int d1, d2, rolls;
        String board = ".........";
        int win; // 0 playing, 1 you, 2 phone, 3 draw
        int you, cpu, draws;
    }

    static String title(String g) {
        switch (g) {
            case "coin": return "Coin Flip";
            case "dice": return "Dice";
            case "lucky": return "Lucky 7";
            default: return "Tic-Tac-Toe";
        }
    }

    static S load(Context c, Cfg cfg) {
        S s = new S();
        if (Render.DEMO) {
            s.game = "dice"; s.d1 = 5; s.d2 = 3; s.rolls = 12;
            return s;
        }
        s.game = cfg.s("game", "ttt");
        String raw = Cfg.prefs(c).getString("g" + cfg.id, null);
        if (raw != null) {
            try {
                JSONObject o = new JSONObject(raw);
                s.game = o.optString("game", s.game);
                s.coin = o.optInt("coin", -1);
                s.heads = o.optInt("heads");
                s.tails = o.optInt("tails");
                s.d1 = o.optInt("d1");
                s.d2 = o.optInt("d2");
                s.rolls = o.optInt("rolls");
                s.board = o.optString("board", s.board);
                if (s.board.length() != 9) s.board = ".........";
                s.win = o.optInt("win");
                s.you = o.optInt("you");
                s.cpu = o.optInt("cpu");
                s.draws = o.optInt("draws");
            } catch (Exception ignored) { }
        }
        return s;
    }

    static void save(Context c, Cfg cfg, S s) {
        try {
            JSONObject o = new JSONObject();
            o.put("game", s.game);
            o.put("coin", s.coin);
            o.put("heads", s.heads);
            o.put("tails", s.tails);
            o.put("d1", s.d1);
            o.put("d2", s.d2);
            o.put("rolls", s.rolls);
            o.put("board", s.board);
            o.put("win", s.win);
            o.put("you", s.you);
            o.put("cpu", s.cpu);
            o.put("draws", s.draws);
            Cfg.prefs(c).edit().putString("g" + cfg.id, o.toString()).apply();
        } catch (Exception ignored) { }
    }

    /** Clears every game's scores and boards (Games + Lucky 7, all widgets and combo blocks), then redraws. */
    static void resetAll(Context c) {
        android.content.SharedPreferences p = Cfg.prefs(c);
        android.content.SharedPreferences.Editor e = p.edit();
        for (String k : p.getAll().keySet()) {
            if (k.length() > 1 && (k.charAt(0) == 'g' || k.charAt(0) == 'l') && k.substring(1).matches("[0-9]+")) e.remove(k);
        }
        e.commit();
        new Thread(() -> Updater.updateAll(c)).start();
    }

    /** Asks first, then resets all games. */
    static void confirmResetAll(android.app.Activity a, Runnable after) {
        new android.app.AlertDialog.Builder(a)
                .setTitle("Reset all games?")
                .setMessage("Clears coin, dice, tic-tac-toe and Lucky 7 scores on all your widgets.")
                .setPositiveButton("Reset all", (d, w) -> {
                    resetAll(a);
                    android.widget.Toast.makeText(a, "All games reset", android.widget.Toast.LENGTH_SHORT).show();
                    if (after != null) after.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static void act(Context c, Cfg cfg, String cmd) {
        S s = load(c, cfg);
        int idx = 0;
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i].equals(s.game)) idx = i;
        if (cmd.startsWith("tab:")) {
            s.game = cmd.substring(4);
            save(c, cfg, s);
            return;
        }
        if (cmd.equals("action")) cmd = "play";
        // Lucky 7's rolls are handled by Lucky.act; here only switching games applies
        if (s.game.equals("lucky") && !cmd.equals("menu") && !cmd.equals("next") && !cmd.equals("prev")) return;
        switch (cmd) {
            case "menu":
            case "next":
                s.game = ORDER[(idx + 1) % ORDER.length];
                break;
            case "prev":
                s.game = ORDER[(idx + ORDER.length - 1) % ORDER.length];
                break;
            case "play":
            case "center":
                if (s.game.equals("coin")) {
                    s.coin = RNG.nextInt(2);
                    if (s.coin == 0) s.heads++; else s.tails++;
                } else if (s.game.equals("dice")) {
                    s.d1 = 1 + RNG.nextInt(6);
                    s.d2 = 1 + RNG.nextInt(6);
                    s.rolls++;
                } else if (cmd.equals("play") || s.win != 0) {
                    s.board = ".........";
                    s.win = 0;
                }
                break;
            default:
                if (cmd.startsWith("cell:") && s.game.equals("ttt") && s.win == 0) {
                    int k;
                    try { k = Integer.parseInt(cmd.substring(5)); } catch (Exception e) { break; }
                    if (k < 0 || k > 8 || s.board.charAt(k) != '.') break;
                    s.board = set(s.board, k, 'X');
                    s.win = result(s.board);
                    if (s.win == 0) {
                        int m = ai(s.board, cfg.s("ai", "hard"));
                        if (m >= 0) s.board = set(s.board, m, 'O');
                        s.win = result(s.board);
                    }
                    if (s.win == 1) s.you++;
                    else if (s.win == 2) s.cpu++;
                    else if (s.win == 3) s.draws++;
                }
        }
        save(c, cfg, s);
    }

    static String set(String b, int k, char ch) {
        char[] a = b.toCharArray();
        a[k] = ch;
        return new String(a);
    }

    static int result(String b) {
        for (int[] l : LINES) {
            char c = b.charAt(l[0]);
            if (c != '.' && c == b.charAt(l[1]) && c == b.charAt(l[2])) return c == 'X' ? 1 : 2;
        }
        return b.indexOf('.') < 0 ? 3 : 0;
    }

    static int[] winLine(String b) {
        for (int[] l : LINES) {
            char c = b.charAt(l[0]);
            if (c != '.' && c == b.charAt(l[1]) && c == b.charAt(l[2])) return l;
        }
        return null;
    }

    /** Phone's move: win, block, centre, corner, side ("easy" plays randomly half the time). */
    static int ai(String b, String level) {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < 9; i++) if (b.charAt(i) == '.') free.add(i);
        if (free.isEmpty()) return -1;
        if (level.equals("easy") && RNG.nextBoolean()) return free.get(RNG.nextInt(free.size()));
        for (char who : new char[]{'O', 'X'}) {
            for (int k : free) {
                String t = set(b, k, who);
                if (result(t) == (who == 'O' ? 2 : 1)) return k;
            }
        }
        if (b.charAt(4) == '.') return 4;
        List<Integer> corners = new ArrayList<>();
        for (int k : new int[]{0, 2, 6, 8}) if (b.charAt(k) == '.') corners.add(k);
        if (!corners.isEmpty()) return corners.get(RNG.nextInt(corners.size()));
        return free.get(RNG.nextInt(free.size()));
    }
}
