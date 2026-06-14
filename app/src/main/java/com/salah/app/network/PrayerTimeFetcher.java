package com.salah.app.network;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PrayerTimeFetcher {
    
    private static final String PREFS_NAME = "PrayerTimes";
    private static final String BASE_URL_UMMULQURA = "https://www.ummulqura.org.sa";
    private static final String BASE_URL_YEMEN = "https://yemen.prayertiming.net";
    
    private Context context;
    private OkHttpClient client;
    
    public PrayerTimeFetcher(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }
    
    // جلب أوقات الصلاة لمدينة سعودية
    public Map<String, String[]> fetchSaudiCity(String city, int year, int month) throws IOException {
        Map<String, String[]> weeklyTimes = new HashMap<>();
        
        String cityCode = getSaudiCityCode(city);
        String url = BASE_URL_UMMULQURA + "/PrayerTime/GetCalendar?year=" + year + "&month=" + month + "&city=" + cityCode;
        
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        
        if (response.isSuccessful() && response.body() != null) {
            String html = response.body().string();
            Document doc = Jsoup.parse(html);
            
            Elements rows = doc.select("table tr");
            for (int i = 1; i < rows.size() && i <= 7; i++) {
                Elements cells = rows.get(i).select("td");
                if (cells.size() >= 7) {
                    String day = cells.get(0).text();
                    String fajr = cells.get(1).text();
                    String sunrise = cells.get(2).text();
                    String dhuhr = cells.get(3).text();
                    String asr = cells.get(4).text();
                    String maghrib = cells.get(5).text();
                    String isha = cells.get(6).text();
                    
                    weeklyTimes.put(day, new String[]{fajr, dhuhr, asr, maghrib, isha});
                }
            }
        }
        
        return weeklyTimes;
    }
    
    // جلب أوقات الصلاة لمدينة يمنية
    public Map<String, String[]> fetchYemeniCity(String city) throws IOException {
        Map<String, String[]> weeklyTimes = new HashMap<>();
        
        String url = BASE_URL_YEMEN + "/" + city.toLowerCase();
        
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        
        if (response.isSuccessful() && response.body() != null) {
            String html = response.body().string();
            Document doc = Jsoup.parse(html);
            
            Elements rows = doc.select(".prayer-table tr, .salah-time tr");
            int dayCount = 0;
            for (Element row : rows) {
                if (dayCount >= 7) break;
                Elements cells = row.select("td");
                if (cells.size() >= 6) {
                    String day = cells.get(0).text();
                    String fajr = cells.get(1).text();
                    String dhuhr = cells.get(2).text();
                    String asr = cells.get(3).text();
                    String maghrib = cells.get(4).text();
                    String isha = cells.get(5).text();
                    
                    weeklyTimes.put(String.valueOf(dayCount + 1), new String[]{fajr, dhuhr, asr, maghrib, isha});
                    dayCount++;
                }
            }
        }
        
        return weeklyTimes;
    }
    
    private String getSaudiCityCode(String city) {
        switch (city) {
            case "مكة المكرمة":
            case "مكة": return "Makkah";
            case "المدينة المنورة":
            case "المدينة": return "Madinah";
            case "الرياض": return "Riyadh";
            case "جدة": return "Jeddah";
            case "أبها": return "Abha";
            case "تبوك": return "Tabuk";
            case "القدس الشريف":
            case "القدس": return "Jerusalem";
            default: return "Makkah";
        }
    }
    
    // حفظ الأوقات محلياً
    public void savePrayerTimes(String city, Map<String, String[]> times) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        for (Map.Entry<String, String[]> entry : times.entrySet()) {
            String day = entry.getKey();
            String[] prayers = entry.getValue();
            editor.putString(city + "_" + day + "_fajr", prayers[0]);
            editor.putString(city + "_" + day + "_dhuhr", prayers[1]);
            editor.putString(city + "_" + day + "_asr", prayers[2]);
            editor.putString(city + "_" + day + "_maghrib", prayers[3]);
            editor.putString(city + "_" + day + "_isha", prayers[4]);
        }
        
        editor.putLong(city + "_lastUpdate", System.currentTimeMillis());
        editor.apply();
    }
    
    // قراءة الأوقات المحفوظة
    public String[] getSavedPrayerTimes(String city, int dayOfWeek) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String day = String.valueOf(dayOfWeek);
        
        String[] times = new String[5];
        times[0] = prefs.getString(city + "_" + day + "_fajr", null);
        times[1] = prefs.getString(city + "_" + day + "_dhuhr", null);
        times[2] = prefs.getString(city + "_" + day + "_asr", null);
        times[3] = prefs.getString(city + "_" + day + "_maghrib", null);
        times[4] = prefs.getString(city + "_" + day + "_isha", null);
        
        return times;
    }
    
    // التحقق من الحاجة للتحديث (مر أسبوع؟)
    public boolean needsUpdate(String city) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUpdate = prefs.getLong(city + "_lastUpdate", 0);
        long oneWeek = 7 * 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - lastUpdate) > oneWeek;
    }
}
