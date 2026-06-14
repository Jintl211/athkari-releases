package com.salah.app.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import com.salah.app.R;
import java.util.HashMap;
import java.util.Map;

public class SyncedAdhanActivity extends Activity {
    public static final String EXTRA_PRAYER  = "prayer";
    public static final String EXTRA_MUEZZIN = "muezzin";
    private static final String TAG = "SyncedAdhanActivity";

    private TextView tvAdhanLine, tvNextLine;
    private Button btnStop;
    private Handler handler;
    private PowerManager.WakeLock wakeLock;

    private Map<String, long[]>   adhanTimings;
    private Map<String, String[]> adhanLyrics;
    private String currentMuezzin, currentPrayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON  |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        setContentView(R.layout.activity_synced_adhan);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SalahApp:AdhanWakeLock");
        wakeLock.acquire(10 * 60 * 1000L);

        currentPrayer  = getIntent().getStringExtra(EXTRA_PRAYER);
        currentMuezzin = getIntent().getStringExtra(EXTRA_MUEZZIN);
        if (currentMuezzin == null || currentMuezzin.isEmpty()) currentMuezzin = "madinah";

        initTimingsAndLyrics();

        tvAdhanLine = findViewById(R.id.tv_adhan_line);
        tvNextLine  = findViewById(R.id.tv_next_line);
        btnStop     = findViewById(R.id.btn_stop_adhan);

        TextView tvPrayerName = findViewById(R.id.tv_prayer_name);
        tvPrayerName.setText("حان وقت صلاة " + getArabicPrayerName(currentPrayer));

        ImageView bgMosque = findViewById(R.id.bg_mosque);
        if (bgMosque != null) {
            switch (currentPrayer != null ? currentPrayer : "") {
                case "fajr":    bgMosque.setImageResource(R.drawable.mosque_fajr);    break;
                case "dhuhr":   bgMosque.setImageResource(R.drawable.mosque_dhuhr);   break;
                case "asr":     bgMosque.setImageResource(R.drawable.mosque_asr);     break;
                case "maghrib": bgMosque.setImageResource(R.drawable.mosque_maghrib); break;
                case "isha":    bgMosque.setImageResource(R.drawable.mosque_isha);    break;
                default:        bgMosque.setImageResource(R.drawable.mosque_fajr);    break;
            }
        }

        // ✅ زر الإغلاق - يوقف AdhanService والشاشة
        btnStop.setText("إغلاق");
        btnStop.setOnClickListener(v -> closeAdhan());

        handler = new Handler();
        startSyncedAdhan();
    }

    private void initTimingsAndLyrics() {
        adhanLyrics = new HashMap<>();

        String[] lyricsNormal = {
            "اللهُ أَكْبَرُ، اللهُ أَكْبَرُ",
            "اللهُ أَكْبَرُ، اللهُ أَكْبَرُ",
            "أَشْهَدُ أَنْ لا إِلَهَ إِلَّا اللهُ",
            "أَشْهَدُ أَنْ لا إِلَهَ إِلَّا اللهُ",
            "أَشْهَدُ أَنَّ مُحَمَّداً رَسُولُ اللهِ",
            "أَشْهَدُ أَنَّ مُحَمَّداً رَسُولُ اللهِ",
            "حَيَّ عَلَى الصَّلاةِ",
            "حَيَّ عَلَى الصَّلاةِ",
            "حَيَّ عَلَى الْفَلَاحِ",
            "حَيَّ عَلَى الْفَلَاحِ",
            "اللهُ أَكْبَرُ، اللهُ أَكْبَرُ",
            "لَا إِلَهَ إِلَّا اللهُ"
        };

        String[] lyricsFajr = {
            "اللهُ أَكْبَرُ، اللهُ أَكْبَرُ",
            "اللهُ أَكْبَرُ، اللهُ أَكْبَرُ",
            "أَشْهَدُ أَنْ لا إِلَهَ إِلَّا اللهُ",
            "أَشْهَدُ أَنْ لا إِلَهَ إِلَّا اللهُ",
            "أَشْهَدُ أَنَّ مُحَمَّداً رَسُولُ اللهِ",
            "أَشْهَدُ أَنَّ مُحَمَّداً رَسُولُ اللهِ",
            "حَيَّ عَلَى الصَّلاةِ",
            "حَيَّ عَلَى الصَّلاةِ",
            "حَيَّ عَلَى الْفَلَاحِ",
            "حَيَّ عَلَى الْفَلَاحِ",
            "الصَّلاةُ خَيْرٌ مِنَ النَّوْمِ",
            "الصَّلاةُ خَيْرٌ مِنَ النَّوْمِ",
            "اللهُ أَكْبَرُ، اللهُ أَكْبَرُ",
            "لَا إِلَهَ إِلَّا اللهُ"
        };

        adhanLyrics.put("makkah",           lyricsNormal);
        adhanLyrics.put("madinah",           lyricsNormal);
        adhanLyrics.put("kuwait",            lyricsNormal);
        adhanLyrics.put("quds",              lyricsNormal);
        adhanLyrics.put("brunei",            lyricsNormal);
        adhanLyrics.put("afasy",             lyricsNormal);
        adhanLyrics.put("haram_makki",       lyricsNormal);
        adhanLyrics.put("makkah_fajr",       lyricsFajr);
        adhanLyrics.put("madinah_fajr",      lyricsFajr);
        adhanLyrics.put("kuwait_fajr",       lyricsFajr);
        adhanLyrics.put("quds_fajr",         lyricsFajr);
        adhanLyrics.put("brunei_fajr",       lyricsFajr);
        adhanLyrics.put("afasy_fajr",        lyricsFajr);
        adhanLyrics.put("haram_makki_fajr",  lyricsFajr);
        adhanLyrics.put("default",           lyricsNormal);

        adhanTimings = new HashMap<>();
        adhanTimings.put("makkah",    new long[]{0,14000,31000,51000,70000,89000,109000,120000,138000,156000,177000,185000});
        adhanTimings.put("quds",      new long[]{0,14000,29000,38000,51000,61000,75000,82000,107000,114000,135000,142000});
        adhanTimings.put("brunei",    new long[]{0,14000,29000,43000,64000,81000,103000,117000,137000,147000,167000,178000});
        adhanTimings.put("afasy",     new long[]{0,17000,31000,48000,71000,91000,116000,141000,167000,189000,213000,228000});
        adhanTimings.put("madinah",   new long[]{0,17000,40000,57000,78000,101000,129000,151000,165000,186000,205000,217000});
        adhanTimings.put("kuwait",    new long[]{0,12000,24000,36000,48000,63000,77000,89000,103000,118000,132000,143000});
        adhanTimings.put("haram_makki", new long[]{0,14000,31000,51000,70000,89000,109000,120000,138000,156000,177000,185000});

        adhanTimings.put("makkah_fajr",    new long[]{0,14000,31000,51000,70000,89000,109000,120000,138000,156000,177000,187000,197000,205000});
        adhanTimings.put("quds_fajr",      new long[]{0,14000,29000,38000,51000,61000,75000,82000,107000,114000,135000,145000,155000,162000});
        adhanTimings.put("brunei_fajr",    new long[]{0,14000,29000,43000,64000,81000,103000,117000,137000,147000,167000,177000,187000,198000});
        adhanTimings.put("afasy_fajr",     new long[]{0,17000,31000,48000,71000,91000,116000,141000,167000,189000,213000,223000,233000,248000});
        adhanTimings.put("madinah_fajr",   new long[]{0,17000,40000,57000,78000,101000,129000,151000,165000,186000,205000,215000,225000,237000});
        adhanTimings.put("kuwait_fajr",    new long[]{0,12000,24000,36000,48000,63000,77000,89000,103000,118000,132000,142000,152000,163000});
        adhanTimings.put("haram_makki_fajr", new long[]{0,14000,31000,51000,70000,89000,109000,120000,138000,156000,177000,187000,197000,205000});
        adhanTimings.put("default", adhanTimings.get("madinah"));
    }

    private String getArabicPrayerName(String prayerId) {
        if (prayerId == null) return "الصلاة";
        switch (prayerId) {
            case "fajr":    return "الفجر";
            case "dhuhr":   return "الظهر";
            case "asr":     return "العصر";
            case "maghrib": return "المغرب";
            case "isha":    return "العشاء";
            default:        return "الصلاة";
        }
    }

    private void startSyncedAdhan() {
        boolean isFajr = "fajr".equals(currentPrayer);
        String key = isFajr ? currentMuezzin + "_fajr" : currentMuezzin;

        String[] lyrics = adhanLyrics.containsKey(key) ? adhanLyrics.get(key)
            : adhanLyrics.containsKey(currentMuezzin) ? adhanLyrics.get(currentMuezzin)
            : adhanLyrics.get("default");

        long[] timings = adhanTimings.containsKey(key) ? adhanTimings.get(key)
            : adhanTimings.containsKey(currentMuezzin) ? adhanTimings.get(currentMuezzin)
            : adhanTimings.get("default");

        // ✅ لا يشغل صوت هنا - الصوت فقط من AdhanService
        for (int i = 0; i < timings.length && i < lyrics.length; i++) {
            final int index = i;
            final String[] lyricsRef = lyrics;
            handler.postDelayed(() -> displayLine(lyricsRef, index), timings[i]);
        }

        long maxDuration = timings[timings.length - 1] + 15000;
        if (maxDuration < 60000) maxDuration = 300000;
        handler.postDelayed(this::closeAdhan, maxDuration);
    }

    private void displayLine(String[] lyrics, int index) {
        tvAdhanLine.setText(lyrics[index]);
        tvAdhanLine.setAlpha(0f);
        tvAdhanLine.animate().alpha(1f).setDuration(500).start();
        if (index + 1 < lyrics.length) {
            tvNextLine.setText(lyrics[index + 1]);
            tvNextLine.setAlpha(0.5f);
        } else {
            tvNextLine.setText("");
        }
    }

    // ✅ إغلاق الأذان - يوقف AdhanService ويغلق الشاشة مباشرة
    private void closeAdhan() {
        try {
            Intent stopSvc = new Intent(this, com.salah.app.services.AdhanService.class);
            stopSvc.setAction(com.salah.app.services.AdhanService.ACTION_STOP);
            startService(stopSvc);
        } catch (Exception ignored) {}

        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        finish();
    }

    @Override
    protected void onDestroy() {
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // منع الرجوع - اضغط إغلاق
    }
}
