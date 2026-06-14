package com.salah.app.activities;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.salah.app.R;
import com.salah.app.utils.PreferencesManager;

public class AudioDuasActivity extends AppCompatActivity {

    private MediaPlayer currentPlayer;
    private ImageButton currentPlayingBtn;

    private static final String[] TITLES = {
        "دعاء - الشيخ فؤاد محمد",
        "دعاء القنوت - الشيخ عبد المحسن القاسم",
        "دعاء القنوت - الشيخ ياسر الدوسري",
        "دعاء القنوت - الشيخ يوسف القرضاوي",
        "دعاء - الشيخ احمد العجمي",
        "دعاء - الشيخ ابراهيم الجبرين",
        "دعاء - الشيخ السيد السعدلي",
        "دعاء - الشيخ صلاح أبو خاطر",
        "دعاء - الشيخ طارق الحواس",
        "دعاء - الشيخ عادل الكلباني",
        "دعاء - الشيخ عبدالرحمن السديس",
        "دعاء - الشيخ عبدالله المطرود",
        "دعاء - الشيخ عيسى العجمي",
        "دعاء - الشيخ فيصل الحليبي",
        "دعاء - الشيخ محمد أيوب",
        "دعاء - الشيخ جمال شاكر عبدالله",
        "دعاء - الشيخ صلاح صالح الراشد",
        "دعاء - الشيخ عبد الرشيد الصوفي",
        "دعاء - الشيخ حسن قاري",
        "دعاء - الشيخ ماهر المعيقلي",
        "دعاء القنوت - الشيخ خالد عبد الطيف",
        "دعاء - الشيخ",
        "دعاء القنوت - الشيخ فيصل الخلافي",
        "دعاء القنوت - الشيخ قاسم المالكي",
        "دعاء القنوت - الشيخ محمد عابد",
        "دعاء القنوت - الشيخ محمد عبد الكريم",
        "دعاء القنوت - الشيخ ناصر العبيد",
        "دعاء القنوت - الشيخ ناصر الغامدي",
        "دعاء - الشيخ محمد ابراهيم السيد",
        "دعاء - الشيخ محمد يوسف",
        "دعاء - الشيخ مصطفى غربي",
        "دعاء - الشيخ مهدي البيشي",
        "دعاء - الشيخ الرفاعي",
        "دعاء - الشيخ عبد الرشيد بن الشيخ",
        "دعاء قنوت - الشيخ ابو عبدالله",
        "دعاء - الشيخ الحمد لله",
        "دعاء - الشيخ فيصل الشدي",
        "دعاء - محمد طه القارئ الصغير",
        "دعاء"
    };

    private static final int[] RAW_IDS = {
        R.raw.dua_001, R.raw.dua_002, R.raw.dua_003, R.raw.dua_004,
        R.raw.dua_006, R.raw.dua_007, R.raw.dua_009, R.raw.dua_011,
        R.raw.dua_012, R.raw.dua_013, R.raw.dua_015, R.raw.dua_017,
        R.raw.dua_018, R.raw.dua_019, R.raw.dua_020, R.raw.dua_029,
        R.raw.dua_040, R.raw.dua_043, R.raw.dua_045, R.raw.dua_046,
        R.raw.dua_049, R.raw.dua_062040221435, R.raw.dua_053, R.raw.dua_054,
        R.raw.dua_056, R.raw.dua_057, R.raw.dua_058, R.raw.dua_059,
        R.raw.dua_064, R.raw.dua_065, R.raw.dua_066, R.raw.dua_067,
        R.raw.dua_068, R.raw.dua_069, R.raw.dua_075, R.raw.dua_086,
        R.raw.dua_099, R.raw.dua_120, R.raw.dua_062040221435
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_duas);

        LinearLayout container = findViewById(R.id.duas_container);

        for (int i = 0; i < TITLES.length; i++) {
            final int index = i;
            View row = LayoutInflater.from(this).inflate(R.layout.item_dua_audio, container, false);
            TextView title = row.findViewById(R.id.dua_title);
            ImageButton btnPlay = row.findViewById(R.id.btn_play);
            ImageButton btnStop = row.findViewById(R.id.btn_stop);

            title.setText(TITLES[i]);
            btnStop.setVisibility(View.GONE);

            btnPlay.setOnClickListener(v -> {
                stopCurrent();
                currentPlayer = MediaPlayer.create(this, RAW_IDS[index]);
                if (currentPlayer != null) {
                    currentPlayer.start();
                    currentPlayingBtn = btnPlay;
                    btnPlay.setVisibility(View.GONE);
                    btnStop.setVisibility(View.VISIBLE);
                    currentPlayer.setOnCompletionListener(mp -> {
                        mp.release();
                        currentPlayer = null;
                        btnPlay.setVisibility(View.VISIBLE);
                        btnStop.setVisibility(View.GONE);
                    });
                }
            });

            btnStop.setOnClickListener(v -> {
                stopCurrent();
                btnPlay.setVisibility(View.VISIBLE);
                btnStop.setVisibility(View.GONE);
            });

            container.addView(row);
        }
    }

    private void stopCurrent() {
        if (currentPlayer != null) {
            try {
                if (currentPlayer.isPlaying()) currentPlayer.stop();
                currentPlayer.release();
            } catch (Exception ignored) {}
            currentPlayer = null;
        }
        if (currentPlayingBtn != null) {
            // reset previous buttons - handled per row
        }
    }

    @Override
    protected void onDestroy() {
        stopCurrent();
        super.onDestroy();
    }
}
