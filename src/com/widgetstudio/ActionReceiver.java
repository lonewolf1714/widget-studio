package com.widgetstudio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Click-wheel and game taps (standalone or inside a Custom Combo block): plays the sound,
 * animates the flip / roll, then shows the result.
 */
public class ActionReceiver extends BroadcastReceiver {
    static final int FRAMES = 14;
    static final long FRAME_MS = 50;

    @Override public void onReceive(final Context c, Intent intent) {
        final int id = intent.getIntExtra("id", -1);
        final int slot = intent.getIntExtra("slot", -1);
        String cmd = intent.getStringExtra("cmd");
        if (id < 0 || cmd == null) return;
        String type = Updater.typeOf(c, id);
        if (type == null) return;
        Cfg widget = Cfg.load(c, id, type);
        // the settings that the tap acts on: the widget itself, or one block of a combo
        final Cfg cfg = (type.equals(Cfg.COMBO) && slot >= 0) ? Combo.blockCfg(widget, slot) : widget;

        if (cfg.type.equals(Cfg.QUOTE)) {
            if (cmd.equals("qnext")) {
                Quotes.next(c, cfg);
                Updater.update(c, id);
            }
            return;
        }
        if (cfg.type.equals(Cfg.MUSIC)) {
            MusicService.command(c, cmd);
            if (MusicService.inst == null) new Handler(Looper.getMainLooper()).postDelayed(() -> Updater.update(c, id), 400);
            return;
        }
        if (cmd.equals("reset") && (cfg.type.equals(Cfg.LUCKY) || cfg.type.equals(Cfg.GAME))) {
            if (cfg.type.equals(Cfg.LUCKY)) {
                Lucky.reset(c, cfg);
            } else if (Games.load(c, cfg).game.equals("lucky")) {
                Lucky.reset(c, cfg);
            } else {
                Games.S old = Games.load(c, cfg);
                Games.S s = new Games.S();
                s.game = old.game;
                Games.save(c, cfg, s);
            }
            Updater.update(c, id);
            return;
        }
        if (cfg.type.equals(Cfg.LUCKY)) {
            if (!Lucky.act(c, cfg, cmd)) return;
            Sfx.init(c);
            animate(c, id, cfg, R.raw.dice, cfg.b("sound", true));
            return;
        }
        if (!cfg.type.equals(Cfg.GAME)) return;
        Sfx.init(c);

        String game = Games.load(c, cfg).game;
        if (game.equals("lucky")) {
            if (cmd.equals("action") || cmd.equals("play") || cmd.equals("center")) cmd = "bet:again";
            if (cmd.startsWith("bet:")) {
                if (Lucky.act(c, cfg, cmd)) animate(c, id, cfg, R.raw.dice, cfg.b("sound", true));
                return;
            }
        }
        boolean roll = (cmd.equals("action") || cmd.equals("play") || cmd.equals("center"))
                && (game.equals("coin") || game.equals("dice"));
        boolean sound = cfg.b("sound", true);
        Games.act(c, cfg, cmd);
        if (!roll) {
            if (sound && cmd.startsWith("cell:")) Sfx.play(c, R.raw.tap);
            Updater.update(c, id);
            return;
        }
        animate(c, id, cfg, game.equals("coin") ? R.raw.coin : R.raw.dice, sound);
    }

    /** Plays the sound and the flip / roll frames, then shows the final result. */
    void animate(final Context c, final int id, final Cfg cfg, final int snd, final boolean sound) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                if (sound) Sfx.play(c, snd);
                Games.animId = cfg.id;
                long start = SystemClock.uptimeMillis();
                for (int f = 0; f < FRAMES; f++) {
                    Games.animFrame = f;
                    float t = f / (float) FRAMES;
                    Games.animPhase = 1f - (1f - t) * (1f - t); // ease-out: fast start, gentle landing
                    Updater.frame(c, id, 0.5f);
                    long next = start + (f + 1) * FRAME_MS;
                    long wait = next - SystemClock.uptimeMillis();
                    if (wait > 0) Thread.sleep(wait);
                }
            } catch (InterruptedException ignored) {
            } finally {
                Games.animId = -1;
                Games.animPhase = -1f;
                Updater.update(c, id);
                // keep the process alive until the sound has finished (Samsung freezes idle apps)
                try { Thread.sleep(sound ? 450 : 0); } catch (InterruptedException ignored) { }
                pr.finish();
            }
        }).start();
    }
}
