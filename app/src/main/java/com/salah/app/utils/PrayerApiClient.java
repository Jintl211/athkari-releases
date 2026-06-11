package com.salah.app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * يجلب أوقات الصلاة من:
 *   - aladhan.com/v1/calendar  (مدن السعودية — طريقة أم القرى method=4)
 *   - aladhan.com/v1/calendar  (مدن اليمن — طريقة الرابطة الإسلامية method=3)
 * ويخزن النتيجة لأسبوع كامل عبر PrayerTimesCache.
 *
 * في حال انعدام الاتصال يعود تلقائياً للحساب المحلي (PrayerTimesCalculator).
 *
 * ──────────────────────────────────────────────────────────
 * الجدول الزمني لإعادة الجلب:
 *   كل 7 أيام تُحذف قيمة last_fetch → يُعاد الجلب في أول فرصة
 * ──────────────────────────────────────────────────────────
 */
public class PrayerApiClient {

    private static final String TAG = "PrayerApiClient";

    // API الرئيسي: aladhan.com
    // method=4  → أم القرى (السعودية)
    // method=3  → رابطة العالم الإسلامي (اليمن وبقية الدول)
    private static final String BASE = "https://api.aladhan.com/v1/calendar";

    /** ─── تعريف المدن المدعومة ─── */
    public enum City {
        MAKKAH   ("makkah",   "Mecca",  "SA", 4, "Asia/Riyadh"),
        MADINAH  ("madinah",  "Medina", "SA", 4, "Asia/Riyadh"),
        RIYADH   ("riyadh",   "Riyadh", "SA", 4, "Asia/Riyadh"),
        JEDDAH   ("jeddah",   "Jeddah", "SA", 4, "Asia/Riyadh"),
        ABHA     ("abha",     "Abha",   "SA", 4, "Asia/Riyadh"),
        TABUK    ("tabuk",    "Tabuk",  "SA", 4, "Asia/Riyadh"),
        SANAA    ("sanaa",    "Sanaa",  "YE", 3, "Asia/Aden"),
        ADEN     ("aden",     "Aden",   "YE", 3, "Asia/Aden"),
        MUKALLA  ("mukalla",  "Mukalla","YE", 3, "Asia/Aden");

        public final String key;
        public final String cityEn;
        public final String country;
        public final int    method;   // 4=UmmAlQura, 3=MuslimWorldLeague
        public final String timezone;

        City(String key, String cityEn, String country, int method, String timezone) {
            this.key      = key;
            this.cityEn   = cityEn;
            this.country  = country;
            this.method   = method;
            this.timezone = timezone;
        }
    }

    public interface Callback {
        void onSuccess(City city, String date, List<PrayerTime> times);
        void onError(City city, String reason);
    }

    // ─────────────────────────────────────────────────────────
    /** جلب وكاش أسبوع كامل لمدينة محددة — يُشغَّل في Thread خلفية */
    // ─────────────────────────────────────────────────────────
    public static void fetchWeekAsync(Context ctx, City city, Callback cb) {
        new Thread(() -> {
            if (!isOnline(ctx)) {
                if (cb != null) cb.onError(city, "offline");
                return;
            }
            if (PrayerTimesCache.isValid(ctx, city.key)) {
                Log.d(TAG, city.key + " cache still valid, skipping fetch");
                return;
            }
            try {
                Calendar cal = Calendar.getInstance();
                int month = cal.get(Calendar.MONTH) + 1;
                int year  = cal.get(Calendar.YEAR);

                // جلب الشهر الكامل (أرخص من 7 طلبات يومية)
                String urlStr = BASE
                    + "?city="    + city.cityEn
                    + "&country=" + city.country
                    + "&method="  + city.method
                    + "&month="   + month
                    + "&year="    + year;

                JSONObject root = httpGet(urlStr);
                if (root == null || !"OK".equals(root.optString("status"))) {
                    if (cb != null) cb.onError(city, "bad response");
                    return;
                }

                JSONArray data = root.getJSONArray("data");
                SimpleDateFormat dateFmt = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

                for (int i = 0; i < data.length(); i++) {
                    JSONObject day  = data.getJSONObject(i);
                    JSONObject timings = day.getJSONObject("timings");
                    JSONObject date = day.getJSONObject("date");
                    String gregorian = date.getJSONObject("gregorian").getString("date");
                    // تحويل dd-MM-yyyy → yyyy-MM-dd
                    String[] parts = gregorian.split("-");
                    String cacheDate = parts[2] + "-" + parts[1] + "-" + parts[0];

                    List<PrayerTime> list = parseTimings(timings, cacheDate, city.timezone);
                    PrayerTimesCache.saveDay(ctx, city.key, cacheDate, list);
                    if (cb != null) cb.onSuccess(city, cacheDate, list);
                }
                Log.i(TAG, "Fetched & cached " + data.length() + " days for " + city.key);

            } catch (Exception e) {
                Log.e(TAG, "fetchWeekAsync failed for " + city.key, e);
                if (cb != null) cb.onError(city, e.getMessage());
            }
        }).start();
    }

    /** جلب جميع المدن المدعومة دفعة واحدة */
    public static void fetchAllCities(Context ctx) {
        for (City c : City.values()) {
            fetchWeekAsync(ctx, c, null);
        }
    }

    // ─────────────────────────────────────────────────────────
    /** أوقات اليوم لمدينة — من الكاش أولاً، ثم الحساب المحلي كـ fallback */
    // ─────────────────────────────────────────────────────────
    public static List<PrayerTime> getTodayTimes(Context ctx,
                                                  City city,
                                                  Location loc,
                                                  UserSettings settings) {
        String today = PrayerTimesCache.todayKey();
        List<PrayerTime> cached = PrayerTimesCache.loadDay(ctx, city.key, today);
        if (cached != null && !cached.isEmpty()) return cached;
        // Fallback: حساب محلي
        return PrayerTimesCalculator.getTodayTimes(loc, settings);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private static List<PrayerTime> parseTimings(JSONObject t, String date, String tz) {
        List<PrayerTime> list = new ArrayList<>();
        list.add(make(PrayerTime.Prayer.FAJR,    clean(t.optString("Fajr")),    date, tz));
        list.add(make(PrayerTime.Prayer.SUNRISE,  clean(t.optString("Sunrise")), date, tz));
        list.add(make(PrayerTime.Prayer.DHUHR,   clean(t.optString("Dhuhr")),   date, tz));
        list.add(make(PrayerTime.Prayer.ASR,     clean(t.optString("Asr")),     date, tz));
        list.add(make(PrayerTime.Prayer.MAGHRIB, clean(t.optString("Maghrib")), date, tz));
        list.add(make(PrayerTime.Prayer.ISHA,    clean(t.optString("Isha")),    date, tz));
        return list;
    }

    /** يزيل " (EAT)" أو أي suffix من الوقت */
    private static String clean(String t) {
        if (t == null) return "00:00";
        return t.trim().replaceAll("\\s+\\(.*\\)$", "").trim();
    }

    private static PrayerTime make(PrayerTime.Prayer prayer,
                                   String hhmm, String date, String tz) {
        try {
            // date = "yyyy-MM-dd", hhmm = "HH:mm"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone(tz));
            java.util.Date d = sdf.parse(date + " " + hhmm);
            return new PrayerTime(prayer, d != null ? d : new java.util.Date());
        } catch (Exception e) {
            return new PrayerTime(prayer, new java.util.Date());
        }
    }

    private static JSONObject httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code != 200) return null;
            BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "httpGet failed: " + urlStr, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager)
            ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }
}
