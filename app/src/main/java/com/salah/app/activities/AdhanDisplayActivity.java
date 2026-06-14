package com.salah.app.activities;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.salah.app.R;
import com.salah.app.utils.PreferencesManager;

public class AdhanDisplayActivity extends AppCompatActivity {

    private ImageView imgPrayer;
    private TextView txtAdhanLine;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private PowerManager.WakeLock wakeLock;

    // كلمات الأذان الكامل
    private static final String[] ADHAN_WORDS = {
        "الله أكبر الله أكبر",
        "الله أكبر الله أكبر",
        "أشهد أن لا إله إلا الله",
        "أشهد أن لا إله إلا الله",
        "أشهد أن محمداً رسول الله",
        "أشهد أن محمداً رسول الله",
        "حي على الصلاة",
        "حي على الصلاة",
        "حي على الفلاح",
        "حي على الفلاح",
        "الله أكبر الله أكبر",
        "لا إله إلا الله"
    };

    private static final String[] ADHAN_WORDS_FAJR = {
        "الله أكبر الله أكبر",
        "الله أكبر الله أكبر",
        "أشهد أن لا إله إلا الله",
        "أشهد أن لا إله إلا الله",
        "أشهد أن محمداً رسول الله",
        "أشهد أن محمداً رسول الله",
        "حي على الصلاة",
        "حي على الصلاة",
        "حي على الفلاح",
        "حي على الفلاح",
        "الصلاة خير من النوم",
        "الصلاة خير من النوم",
        "الله أكبر الله أكبر",
        "لا إله إلا الله"
    };

    // التوقيتات بالمللي ثانية (تراكمية)
    private static final long[] TIMINGS = {
        0, 5000, 11000, 16000, 22000, 27000,
        33000, 38000, 43000, 48000, 53000, 58000
    };

    private static final long[] TIMINGS_FAJR = {
        0, 5000, 11000, 16000, 22000, 27000,
        33000, 38000, 43000, 48000, 53000, 58000, 63000, 68000
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adhan_display);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SalahApp:AdhanWakeLock");
        wakeLock.acquire(10 * 60 * 1000L);

        String prayerName = getIntent().getStringExtra("prayer_name");
        String adhanFile  = getIntent().getStringExtra("adhan_file");
        boolean isFajr    = "fajr".equalsIgnoreCase(getIntent().getStringExtra("prayer_id"))
                         || "الفجر".equals(prayerName);

        if (adhanFile == null || adhanFile.isEmpty()) adhanFile = "adhan_madinah";

        imgPrayer    = findViewById(R.id.img_prayer);
        txtAdhanLine = findViewById(R.id.txt_adhan_line);

        // صورة حسب الصلاة
        imgPrayer.setImageResource(getPrayerImage(prayerName));

        handler = new Handler(Looper.getMainLooper());

        String[] words   = isFajr ? ADHAN_WORDS_FAJR : ADHAN_WORDS;
        long[]   timings = isFajr ? TIMINGS_FAJR      : TIMINGS;

        // تشغيل الصوت
        playSound(adhanFile);

        // عرض الكلمات متزامنة
        for (int i = 0; i < words.length; i++) {
            final String word = words[i];
            handler.postDelayed(() -> showWord(word), timings[i]);
        }

        long totalDuration = timings[timings.length - 1] + 6000;
        handler.postDelayed(this::finish, totalDuration);
    }

    private void showWord(String word) {
        txtAdhanLine.setText(word);
        txtAdhanLine.setAlpha(0f);
        txtAdhanLine.animate().alpha(1f).setDuration(600).start();
    }

    private int getPrayerImage(String name) {
        if (name == null) return R.drawable.mosque_fajr;
        switch (name) {
            case "الفجر":   return R.drawable.mosque_fajr;
            case "الظهر":   return R.drawable.mosque_dhuhr;
            case "العصر":   return R.drawable.mosque_asr;
            case "المغرب":  return R.drawable.mosque_maghrib;
            case "العشاء":  return R.drawable.mosque_isha;
            default:        return R.drawable.mosque_fajr;
        }
    }

    private void playSound(String adhanFile) {
        try {
            int resId = getResources().getIdentifier(adhanFile, "raw", getPackageName());
            if (resId == 0) resId = R.raw.adhan_madinah;
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer != null) {
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
                mediaPlayer.setOnCompletionListener(mp -> mp.release());
                mediaPlayer.start();
            }
        } catch (Exception e) {
            android.util.Log.e("AdhanDisplay", "playSound error", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}
