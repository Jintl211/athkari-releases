package com.salah.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.salah.app.R;
import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.PrayerApiClient;
import com.salah.app.utils.PrayerTimesCache;
import com.salah.app.utils.PrayerTimesCalculator;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PrayerTimesActivity extends AppCompatActivity {

    private TextView txtFajr, txtDhuhr, txtAsr, txtMaghrib, txtIsha;
    private TextView txtSunriseTime, txtCountdown;
    private android.os.Handler countdownHandler;
    private long nextPrayerMillis = 0;
    private TextView txtFajrTime, txtDhuhrTime, txtAsrTime, txtMaghribTime, txtIshaTime;
    private TextView txtSunrise, txtNextPrayerName, txtNextPrayerTime;
    private TextView txtStatus;
    private Spinner spinnerCity;

    private final String[] cityNames = {
        "مكة المكرمة", "المدينة المنورة", "الرياض", "جدة",
        "أبها", "تبوك", "صنعاء", "عدن", "المكلا"
    };

    private final PrayerApiClient.City[] cities = {
        PrayerApiClient.City.MAKKAH,
        PrayerApiClient.City.MADINAH,
        PrayerApiClient.City.RIYADH,
        PrayerApiClient.City.JEDDAH,
        PrayerApiClient.City.ABHA,
        PrayerApiClient.City.TABUK,
        PrayerApiClient.City.SANAA,
        PrayerApiClient.City.ADEN,
        PrayerApiClient.City.MUKALLA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_times);

        txtFajr         = findViewById(R.id.txt_fajr);
        txtDhuhr        = findViewById(R.id.txt_dhuhr);
        txtAsr          = findViewById(R.id.txt_asr);
        txtMaghrib      = findViewById(R.id.txt_maghrib);
        txtIsha         = findViewById(R.id.txt_isha);
        txtFajrTime     = findViewById(R.id.txt_fajr_time);
        txtDhuhrTime    = findViewById(R.id.txt_dhuhr_time);
        txtAsrTime      = findViewById(R.id.txt_asr_time);
        txtMaghribTime  = findViewById(R.id.txt_maghrib_time);
        txtIshaTime     = findViewById(R.id.txt_isha_time);
        // txtSunrise removed
        txtSunriseTime  = findViewById(R.id.txt_sunrise_time);
        txtCountdown    = findViewById(R.id.txt_countdown);
        txtNextPrayerName = findViewById(R.id.txt_next_prayer_name);
        txtNextPrayerTime = findViewById(R.id.txt_next_prayer_time);
        txtStatus       = findViewById(R.id.txt_status);
        spinnerCity     = findViewById(R.id.spinner_city);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, cityNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(adapter);

        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadCity(cities[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadCity(PrayerApiClient.City city) {
        txtStatus.setText("جاري التحميل...");

        // أولاً: حاول من الكاش
        String today = PrayerTimesCache.todayKey();
        List<PrayerTime> cached = PrayerTimesCache.loadDay(this, city.key, today);
        if (cached != null && !cached.isEmpty()) {
            showTimes(cached);
            txtStatus.setText("✓ من الكاش");
            return;
        }

        // ثانياً: جلب من الإنترنت
        txtStatus.setText("جاري الجلب من الإنترنت...");
        PrayerApiClient.fetchWeekAsync(this, city, new PrayerApiClient.Callback() {
            @Override
            public void onSuccess(PrayerApiClient.City c, String date, List<PrayerTime> times) {
                if (date.equals(today)) {
                    runOnUiThread(() -> {
                        showTimes(times);
                        txtStatus.setText("✓ تم الجلب من الإنترنت");
                    });
                }
            }
            @Override
            public void onError(PrayerApiClient.City c, String reason) {
                runOnUiThread(() -> {
                    // Fallback: حساب محلي
                    Location loc = getDefaultLocation(city);
                    UserSettings s = PreferencesManager.load(PrayerTimesActivity.this);
                    List<PrayerTime> local = PrayerTimesCalculator.getTodayTimes(loc, s);
                    showTimes(local);
                    txtStatus.setText("⚠ بدون إنترنت - حساب تقريبي");
                });
            }
        });
    }

    private void showTimes(List<PrayerTime> times) {
        SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", new Locale("ar"));
        PrayerTime nextPrayer = null;
        long now = System.currentTimeMillis();
        for (PrayerTime pt : times) {
            String t = tf.format(pt.time);
            switch (pt.prayer) {
                case FAJR:
                    txtFajr.setText("الفجر");
                    if (txtFajrTime != null) txtFajrTime.setText(t);
                    break;
                case DHUHR:
                    txtDhuhr.setText("الظهر");
                    if (txtDhuhrTime != null) txtDhuhrTime.setText(t);
                    break;
                case ASR:
                    txtAsr.setText("العصر");
                    if (txtAsrTime != null) txtAsrTime.setText(t);
                    break;
                case MAGHRIB:
                    txtMaghrib.setText("المغرب");
                    if (txtMaghribTime != null) txtMaghribTime.setText(t);
                    break;
                case ISHA:
                    txtIsha.setText("العشاء");
                    if (txtIshaTime != null) txtIshaTime.setText(t);
                    break;
                case SUNRISE:
                    if (txtSunriseTime != null) txtSunriseTime.setText(t);
                    break;
            }
            if (pt.prayer != PrayerTime.Prayer.SUNRISE && pt.time.getTime() > now && nextPrayer == null) {
                nextPrayer = pt;
            }
        }
        if (nextPrayer != null && txtNextPrayerName != null) {
            txtNextPrayerName.setText("الصلاة القادمة: " + nextPrayer.getArabicName());
            txtNextPrayerTime.setText(tf.format(nextPrayer.time));
            nextPrayerMillis = nextPrayer.time.getTime();
            startCountdown();
        }
    }

    private void startCountdown() {
        if (countdownHandler != null) countdownHandler.removeCallbacksAndMessages(null);
        countdownHandler = new android.os.Handler(getMainLooper());
        countdownHandler.post(new Runnable() {
            @Override public void run() {
                if (txtCountdown == null || nextPrayerMillis == 0) return;
                long diff = nextPrayerMillis - System.currentTimeMillis();
                if (diff <= 0) {
                    txtCountdown.setText("حان وقت الصلاة");
                    return;
                }
                long h = diff / 3600000;
                long m = (diff % 3600000) / 60000;
                long s = (diff % 60000) / 1000;
                txtCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
                countdownHandler.postDelayed(this, 1000);
            }
        });
    }

    @Override protected void onDestroy() {
        if (countdownHandler != null) countdownHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private Location getDefaultLocation(PrayerApiClient.City city) {
        switch (city) {
            case MAKKAH:  return new Location(21.4225,  39.8262, "مكة المكرمة",    "Asia/Riyadh");
            case MADINAH: return new Location(24.5247,  39.5692, "المدينة المنورة", "Asia/Riyadh");
            case RIYADH:  return new Location(24.7136,  46.6753, "الرياض",          "Asia/Riyadh");
            case JEDDAH:  return new Location(21.4858,  39.1925, "جدة",             "Asia/Riyadh");
            case ABHA:    return new Location(18.2164,  42.5053, "أبها",            "Asia/Riyadh");
            case TABUK:   return new Location(28.3998,  36.5715, "تبوك",            "Asia/Riyadh");
            case SANAA:   return new Location(15.3694,  44.1910, "صنعاء",           "Asia/Aden");
            case ADEN:    return new Location(12.7797,  45.0367, "عدن",             "Asia/Aden");
            case MUKALLA: return new Location(14.5425,  49.1243, "المكلا",          "Asia/Aden");
            default:      return new Location(21.4225,  39.8262, "مكة المكرمة",    "Asia/Riyadh");
        }
    }
}
