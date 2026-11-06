package com.widgetstudio;

import java.util.Calendar;

/** Date maths for the counter widget (age, relationship, project timeline, countdown). */
final class Counter {
    static final long DAY = 86400000L;

    static final class R {
        long years, months, weeks, days;
        long remMs;        // leftover below one day (drives the live H:M:S clock)
        long totalMs;      // full span
        long totalMonths;
        boolean counting;  // true = counting down towards a future moment
        boolean done;      // countdown / timeline already finished
        float progress = -1; // timeline 0..1
    }

    /**
     * mode: "up" (since start), "down" (until end), "timeline" (start -> end, shows time left).
     */
    static R calc(Cfg cfg, long now) {
        String mode = cfg.s("mode", "up");
        long start = cfg.l("start", now);
        long end = cfg.l("end", now);
        R r = new R();
        long from, to;
        if (mode.equals("up")) {
            from = start;
            to = now;
            if (to < from) { from = now; to = start; r.counting = true; }
        } else {
            long target = end;
            if (mode.equals("timeline") && end > start) {
                r.progress = Math.max(0f, Math.min(1f, (now - start) / (float) (end - start)));
            }
            if (now <= target) {
                from = now;
                to = target;
                r.counting = true;
            } else {
                from = target;
                to = now;
                r.done = true;
            }
        }
        r.totalMs = to - from;
        boolean uY = cfg.b("uY", true), uM = cfg.b("uM", true), uW = cfg.b("uW", false);
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(from);
        if (uY) {
            while (true) {
                Calendar t = (Calendar) a.clone();
                t.add(Calendar.YEAR, 1);
                if (t.getTimeInMillis() > to) break;
                a = t;
                r.years++;
            }
        }
        if (uM) {
            while (true) {
                Calendar t = (Calendar) a.clone();
                t.add(Calendar.MONTH, 1);
                if (t.getTimeInMillis() > to) break;
                a = t;
                r.months++;
            }
        }
        long left = to - a.getTimeInMillis();
        long days = left / DAY;
        r.remMs = left - days * DAY;
        if (uW) {
            r.weeks = days / 7;
            days = days % 7;
        }
        r.days = days;
        // total months independent of unit toggles
        Calendar m = Calendar.getInstance();
        m.setTimeInMillis(from);
        long tm = 0;
        while (tm < 2400) {
            Calendar t = (Calendar) m.clone();
            t.add(Calendar.MONTH, 1);
            if (t.getTimeInMillis() > to) break;
            m = t;
            tm++;
        }
        r.totalMonths = tm;
        return r;
    }

    /** When the day count next changes (the live clock would roll over). */
    static long nextBoundary(Cfg cfg, long now) {
        R r = calc(cfg, now);
        if (r.counting) return now + Math.max(1000, r.remMs) + 500;
        return now + Math.max(1000, DAY - r.remMs) + 500;
    }
}
