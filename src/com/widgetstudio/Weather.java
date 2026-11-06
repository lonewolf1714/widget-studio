package com.widgetstudio;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Weather from Open-Meteo (free, no key). City is chosen in the app. */
final class Weather {
    static final class Data {
        float temp, tmin, tmax;
        int code, humidity;
        boolean isDay;
        long time;
        String city;
        boolean fahrenheit;
    }

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences("global", Context.MODE_PRIVATE);
    }

    static boolean hasCity(Context c) {
        if (Render.DEMO) return true;
        return p(c).contains("lat");
    }

    static String city(Context c) {
        return p(c).getString("city", "");
    }

    static boolean fahrenheit(Context c) {
        return p(c).getBoolean("fahrenheit", false);
    }

    static void setCity(Context c, String name, double lat, double lon) {
        p(c).edit().putString("city", name).putFloat("lat", (float) lat).putFloat("lon", (float) lon)
                .remove("w_time").apply();
    }

    static Data get(Context c) {
        if (Render.DEMO) {
            Data d = new Data();
            d.temp = 24; d.tmin = 18; d.tmax = 29; d.code = 2; d.humidity = 58; d.isDay = true;
            d.city = "Gurugram"; d.time = System.currentTimeMillis();
            return d;
        }
        SharedPreferences p = p(c);
        if (!p.contains("w_time")) return null;
        Data d = new Data();
        d.temp = p.getFloat("w_temp", 0);
        d.tmin = p.getFloat("w_min", 0);
        d.tmax = p.getFloat("w_max", 0);
        d.code = p.getInt("w_code", 0);
        d.humidity = p.getInt("w_hum", 0);
        d.isDay = p.getBoolean("w_day", true);
        d.time = p.getLong("w_time", 0);
        d.city = p.getString("city", "");
        d.fahrenheit = p.getBoolean("w_f", false);
        return d;
    }

    /** Fetches if older than 25 minutes. Must run off the main thread. */
    static void refreshIfStale(Context c) {
        if (!hasCity(c)) return;
        long age = System.currentTimeMillis() - p(c).getLong("w_time", 0);
        boolean unitsChanged = p(c).getBoolean("w_f", false) != fahrenheit(c);
        if (age < 25 * 60000L && !unitsChanged) return;
        fetch(c);
    }

    static boolean fetch(Context c) {
        SharedPreferences p = p(c);
        if (!p.contains("lat")) return false;
        boolean f = fahrenheit(c);
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                        + "&current=temperature_2m,relative_humidity_2m,weather_code,is_day"
                        + "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1%s",
                p.getFloat("lat", 0), p.getFloat("lon", 0), f ? "&temperature_unit=fahrenheit" : "");
        try {
            JSONObject j = new JSONObject(http(url));
            JSONObject cur = j.getJSONObject("current");
            JSONObject daily = j.getJSONObject("daily");
            p.edit()
                    .putFloat("w_temp", (float) cur.getDouble("temperature_2m"))
                    .putInt("w_hum", cur.optInt("relative_humidity_2m", 0))
                    .putInt("w_code", cur.optInt("weather_code", 0))
                    .putBoolean("w_day", cur.optInt("is_day", 1) == 1)
                    .putFloat("w_max", (float) daily.getJSONArray("temperature_2m_max").getDouble(0))
                    .putFloat("w_min", (float) daily.getJSONArray("temperature_2m_min").getDouble(0))
                    .putBoolean("w_f", f)
                    .putLong("w_time", System.currentTimeMillis())
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Geocoding search: returns rows {label, lat, lon}. Off main thread. */
    static List<String[]> search(String q) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?count=6&language=en&format=json&name="
                + URLEncoder.encode(q, "UTF-8");
        JSONObject j = new JSONObject(http(url));
        List<String[]> out = new ArrayList<>();
        JSONArray arr = j.optJSONArray("results");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject r = arr.getJSONObject(i);
            StringBuilder label = new StringBuilder(r.optString("name"));
            String admin = r.optString("admin1", "");
            String country = r.optString("country", "");
            if (!admin.isEmpty() && !admin.equals(r.optString("name"))) label.append(", ").append(admin);
            if (!country.isEmpty()) label.append(", ").append(country);
            out.add(new String[]{label.toString(), r.optString("name"), String.valueOf(r.getDouble("latitude")),
                    String.valueOf(r.getDouble("longitude"))});
        }
        return out;
    }

    static String http(String u) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(u).openConnection();
        con.setConnectTimeout(8000);
        con.setReadTimeout(8000);
        con.setRequestProperty("User-Agent", "WidgetStudio/1.0 (Android)");
        try (InputStream in = con.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }

    static String text(int code) {
        if (code == 0) return "Clear";
        if (code == 1) return "Mostly clear";
        if (code == 2) return "Partly cloudy";
        if (code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Foggy";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Showers";
        if (code == 85 || code == 86) return "Snow showers";
        if (code >= 95) return "Thunderstorm";
        return "Cloudy";
    }

    static String deg(Data d) {
        return Math.round(d.temp) + "°";
    }

    static String sentence(Data d) {
        String unit = d.fahrenheit ? "°F" : "°C";
        String place = d.city == null || d.city.isEmpty() ? "" : " in " + d.city;
        return "Currently " + Math.round(d.temp) + unit + place + " with " + text(d.code).toLowerCase(Locale.ROOT)
                + " skies. Today " + Math.round(d.tmin) + "° to " + Math.round(d.tmax) + "°, humidity "
                + d.humidity + "%.";
    }
}
