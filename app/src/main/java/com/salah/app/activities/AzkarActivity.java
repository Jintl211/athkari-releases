package com.salah.app.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.salah.app.R;
import com.salah.app.utils.PreferencesManager;

public class AzkarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_azkar);

        findViewById(R.id.card_morning).setOnClickListener(v -> openAdhkar("morning"));
        findViewById(R.id.card_evening).setOnClickListener(v -> openAdhkar("evening"));
        findViewById(R.id.card_after_salah).setOnClickListener(v -> openAdhkar("afterSalah"));
        findViewById(R.id.card_wakeup).setOnClickListener(v -> openAdhkar("wakeup"));
        findViewById(R.id.card_duas).setOnClickListener(v -> openAdhkar("duas"));
        findViewById(R.id.card_iftitah).setOnClickListener(v -> openAdhkar("iftitah"));
        findViewById(R.id.card_sleep).setOnClickListener(v -> startActivity(new Intent(this, SleepAthkarActivity.class)));
    }

    private void openAdhkar(String category) {
        Intent i = new Intent(this, AdhkarSessionActivity.class);
        i.putExtra("category", category);
        startActivity(i);
    }
}
