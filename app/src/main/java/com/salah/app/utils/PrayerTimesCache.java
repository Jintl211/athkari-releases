package com.salah.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.salah.app.models.PrayerTime;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * يخزن أوقات الصلاة لأسبوع كامل في SharedPreferences.
 * المفتاح: "city_YYYY-MM-DD" → JSONArray [ {fajr,dhuhr,asr,maghrib,isha} ]
 */
public class PrayerTimesCache {

    private static final String PREFS  = "prayer_cache";
    private static final String TAG    = "PrayerCache";
    private static final int    DAYS   = 7;
    // مفتاح لتاريخ آخر تحديث لكل مدينة
    private static final String KEY_LAST_FETCH = "last_fetch_";

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** هل الكاش للمدينة لا يزال صالحاً (أقل من 7 أيام)؟ */
    public static boolean isValid(Context ctx, String cityKey) {
        long lastFetch = prefs(ctx).getLong(KEY_LAST_FETCH + cityKey, 0);
        if (lastFetch == 0) return false;
        long ageDays = (System.currentTimeMillis() - lastFetch) / (1000L * 60 * 60 * 24);
        return ageDays < DAYS;
    }

    /** يحفظ أوقات يوم واحد في الكاش */
    public static void saveDay(Context ctx, String cityKey, String date, List<PrayerTime> times) {
        try {
            JSONObject day = new JSONObject();
            for (PrayerTime pt : times) {
                switch (pt.prayer) {
                    case FAJR:    day.put("fajr",    pt.formatTime24h()); break;
                    case SUNRISE: day.put("sunrise", pt.formatTime24h()); break;
                    case DHUHR:   day.put("dhuhr",   pt.formatTime24h()); break;
                    case ASR:     day.put("asr",     pt.formatTime24h()); break;
                    case MAGHRIB: day.put("maghrib", pt.formatTime24h()); break;
                    case ISHA:    day.put("isha",    pt.formatTime24h()); break;
                }
            }
            prefs(ctx).edit()
                .putString(cityKey + "_" + date, day.toString())
                .putLong(KEY_LAST_FETCH + cityKey, System.currentTimeMillis())
                .apply();
        } catch (Exception e) {
            Log.e(TAG, "saveDay failed", e);
        }
    }

    /** يجلب أوقات يوم من الكاش، يرجع null إذا غير موجود */
    public static List<PrayerTime> loadDay(Context ctx, String cityKey, String date) {
        String json = prefs(ctx).getString(cityKey + "_" + date, null);
        if (json == null) return null;
        try {
            JSONObject obj = new JSONObject(json);
            List<PrayerTime> list = new ArrayList<>();
            SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.US);

            String todayPrefix = date + " ";
            addIfPresent(list, obj, "fajr",    todayPrefix, PrayerTime.Prayer.FAJR,    fmt);
            addIfPresent(list, obj, "sunrise", todayPrefix, PrayerTime.Prayer.SUNRISE, fmt);
            addIfPresent(list, obj, "dhuhr",   todayPrefix, PrayerTime.Prayer.DHUHR,   fmt);
            addIfPresent(list, obj, "asr",     todayPrefix, PrayerTime.Prayer.ASR,     fmt);
            addIfPresent(list, obj, "maghrib", todayPrefix, PrayerTime.Prayer.MAGHRIB, fmt);
            addIfPresent(list, obj, "isha",    todayPrefix, PrayerTime.Prayer.ISHA,    fmt);
            return list.isEmpty() ? null : list;
        } catch (Exception e) {
            Log.e(TAG, "loadDay failed", e);
            return null;
        }
    }

    private static void addIfPresent(List<PrayerTime> list, JSONObject obj,
                                     String key, String datePrefix,
                                     PrayerTime.Prayer prayer,
                                     SimpleDateFormat fmt) {
        try {
            String t = obj.optString(key, null);
            if (t == null || t.isEmpty()) return;
            Date d = fmt.parse(datePrefix + t);
            if (d == null) return;
            // Adjust: parse gave epoch-relative time, rebuild with today's date
            Calendar c = Calendar.getInstance();
            String[] parts = t.split(":");
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            c.set(Calendar.MINUTE,      Integer.parseInt(parts[1]));
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            list.add(new PrayerTime(prayer, c.getTime()));
        } catch (Exception ignored) {}
    }

    /** تاريخ اليوم بصيغة YYYY-MM-DD */
    public static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** تاريخ يوم بعد n يوم */
    public static String dayKey(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, offsetDays);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    /** مسح كاش مدينة معينة */
    public static void invalidate(Context ctx, String cityKey) {
        prefs(ctx).edit().remove(KEY_LAST_FETCH + cityKey).apply();
    }
}
