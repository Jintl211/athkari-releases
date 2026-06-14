package com.salah.app.activities;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.salah.app.R;
import com.salah.app.models.DhikrItem;
import com.salah.app.utils.AdhkarRepository;
import com.salah.app.utils.PreferencesManager;
import java.util.List;

public class SleepAthkarActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private Button btnPlay, btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_athkar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnPlay = findViewById(R.id.btn_play);
        btnStop = findViewById(R.id.btn_stop);
        btnPlay.setOnClickListener(v -> playAudio());
        btnStop.setOnClickListener(v -> stopAudio());

        // تحميل الأذكار من JSON وعرضها ديناميكياً
        loadAdhkarFromJson();

        // تشغيل تلقائي عند الفتح
        playAudio();
    }

    private void loadAdhkarFromJson() {
        try {
            LinearLayout container = findViewById(R.id.container_adhkar);
            if (container == null) return;
            container.removeAllViews();

            List<DhikrItem> items = AdhkarRepository.getCategory(this, "sleep");
            for (DhikrItem item : items) {
                // CardView
                CardView card = new CardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 32);
                card.setLayoutParams(cardParams);
                card.setRadius(40f);
                card.setCardBackgroundColor(0xFF1E2A3A);
                card.setCardElevation(8f);

                LinearLayout inner = new LinearLayout(this);
                inner.setOrientation(LinearLayout.VERTICAL);
                inner.setPadding(40, 40, 40, 40);

                // العنوان
                TextView title = new TextView(this);
                title.setText(item.title);
                title.setTextColor(0xFFFFD700);
                title.setTextSize(18f);
                title.setGravity(Gravity.CENTER);
                title.setPadding(0, 0, 0, 20);
                title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                inner.addView(title);

                // النص
                TextView text = new TextView(this);
                text.setText(item.text);
                text.setTextColor(0xFFFFFFFF);
                text.setTextSize(19f);
                text.setGravity(Gravity.CENTER);
                text.setLineSpacing(10f, 1f);
                inner.addView(text);

                // عدد المرات إن وجد
                if (item.count > 1) {
                    TextView count = new TextView(this);
                    count.setText(item.count + " مرات");
                    count.setTextColor(0xFFFFD700);
                    count.setTextSize(14f);
                    count.setGravity(Gravity.CENTER);
                    count.setPadding(0, 16, 0, 0);
                    inner.addView(count);
                }

                card.addView(inner);
                container.addView(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playAudio() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.adhkar_sleep);
            }
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } catch (Exception ignored) {}
    }

    private void stopAudio() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
