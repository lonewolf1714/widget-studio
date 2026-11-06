package com.widgetstudio;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Reads upcoming events from the phone's calendars. */
final class Events {
    static final class Ev {
        String title;
        long begin, end;
        boolean allDay;
        int color;
    }

    static final long DAY = 86400000L;

    static boolean allowed(Context c) {
        if (Render.DEMO) return true;
        return c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    static List<Ev> range(Context c, long from, long to, int max) {
        List<Ev> out = new ArrayList<>();
        if (!allowed(c)) return out;
        Uri.Builder b = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(b, from);
        ContentUris.appendId(b, to);
        String[] cols = {CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DISPLAY_COLOR};
        try (Cursor cur = c.getContentResolver().query(b.build(), cols, null, null,
                CalendarContract.Instances.BEGIN + " ASC")) {
            if (cur == null) return out;
            while (cur.moveToNext() && out.size() < max) {
                Ev e = new Ev();
                e.title = cur.getString(0);
                if (e.title == null || e.title.trim().isEmpty()) e.title = "(No title)";
                e.begin = cur.getLong(1);
                e.end = cur.getLong(2);
                e.allDay = cur.getInt(3) == 1;
                e.color = cur.getInt(4) | 0xFF000000;
                if (e.allDay) {
                    // all-day times are UTC midnights; shift to local midnights
                    int off = TimeZone.getDefault().getOffset(e.begin);
                    e.begin -= off;
                    e.end -= off;
                }
                out.add(e);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static List<Ev> demo() {
        long today = startOfDay(System.currentTimeMillis());
        String[][] s = {{"Dinner with Aarav", "1290"}, {"Morning gym", "1860"}, {"Project review", "4980"},
                {"Movie night", "7380"}};
        int[] colors = {0xFFF5C542, 0xFF9CCB5A, 0xFF5B8CFF, 0xFFFF5C8A};
        List<Ev> out = new ArrayList<>();
        for (int i = 0; i < s.length; i++) {
            Ev e = new Ev();
            e.title = s[i][0];
            e.begin = today + Long.parseLong(s[i][1]) * 60000L;
            e.end = e.begin + 3600000L;
            e.color = colors[i];
            out.add(e);
        }
        return out;
    }

    static List<Ev> upcoming(Context c, int days, int max) {
        if (Render.DEMO) {
            List<Ev> d = demo();
            return d.subList(0, Math.min(max, d.size()));
        }
        long now = System.currentTimeMillis();
        List<Ev> all = range(c, now - DAY, now + days * DAY, 60);
        List<Ev> out = new ArrayList<>();
        for (Ev e : all) {
            if (e.end > now) out.add(e);
            if (out.size() >= max) break;
        }
        return out;
    }

    /** Which of the 7 days starting at weekStart have events. */
    static boolean[] week(Context c, long weekStart) {
        if (Render.DEMO) return new boolean[]{false, true, false, true, true, false, false};
        boolean[] has = new boolean[7];
        for (Ev e : range(c, weekStart, weekStart + 7 * DAY, 200)) {
            int i = (int) ((e.begin - weekStart) / DAY);
            if (e.begin < weekStart) i = 0;
            if (i >= 0 && i < 7) has[i] = true;
        }
        return has;
    }

    static long startOfDay(long t) {
        Calendar k = Calendar.getInstance();
        k.setTimeInMillis(t);
        k.set(Calendar.HOUR_OF_DAY, 0);
        k.set(Calendar.MINUTE, 0);
        k.set(Calendar.SECOND, 0);
        k.set(Calendar.MILLISECOND, 0);
        return k.getTimeInMillis();
    }

    /** "Today, 21:30" / "Tomorrow, 09:00" / "Sat 8 Nov, 10:00". */
    static String when(Ev e) {
        long today = startOfDay(System.currentTimeMillis());
        long d = startOfDay(e.begin);
        String day;
        if (d == today) day = "Today";
        else if (d == today + DAY) day = "Tomorrow";
        else day = new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(new Date(e.begin));
        if (e.allDay) return day + " · all day";
        return day + ", " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(e.begin));
    }

    /** "27/07 - 17:00". */
    static String shortWhen(Ev e) {
        String d = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(e.begin));
        if (e.allDay) return d + " - all day";
        return d + " - " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(e.begin));
    }
}
