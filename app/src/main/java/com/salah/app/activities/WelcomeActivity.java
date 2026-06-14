package com.salah.app.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.utils.AudioDownloader;
import com.salah.app.utils.PermissionHelper;
import com.salah.app.utils.PreferencesManager;
import java.util.concurrent.Executors;

public class WelcomeActivity extends AppCompatActivity {
    public static final String PREF_KEY_SHOWN = "welcome_shown";
    private ProgressBar progressBar;
    private TextView statusText, percentText;
    private MaterialButton btnDownload, btnSkip;
    private View progressLayout;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        PreferencesManager.applyTheme(this);
        requestAllPermissions();
        SharedPreferences prefs = getSharedPreferences("salah_prefs", MODE_PRIVATE);
        if (prefs.getBoolean(PREF_KEY_SHOWN, false) || AudioDownloader.isDownloaded(this)) {
            goToMain();
            return;
        }
        setContentView(R.layout.activity_welcome);
        progressBar    = findViewById(R.id.progress_bar);
        statusText     = findViewById(R.id.status_text);
        percentText    = findViewById(R.id.percent_text);
        btnSkip        = findViewById(R.id.btn_skip);
        progressLayout = findViewById(R.id.progress_layout);
        btnSkip.setOnClickListener(v -> {
            prefs.edit().putBoolean(PREF_KEY_SHOWN, true).apply();
            goToMain();
        });
    }

    private void requestAllPermissions() {
        if (PermissionHelper.needsNotifications(this))
            PermissionHelper.requestNotifications(this);
        if (PermissionHelper.needsLocation(this))
            PermissionHelper.requestLocation(this);
        ui.postDelayed(() -> {
            if (!PermissionHelper.isIgnoringBatteryOptimizations(this))
                PermissionHelper.requestIgnoreBatteryOptimizations(this);
        }, 1500);
        ui.postDelayed(() -> {
            if (!PermissionHelper.canScheduleExactAlarms(this))
                PermissionHelper.openExactAlarmSettings(this);
        }, 3000);
    }

    private void startDownload() {
        btnDownload.setEnabled(false);
        btnSkip.setEnabled(false);
        progressLayout.setVisibility(View.VISIBLE);
        statusText.setText(R.string.welcome_preparing);
        Executors.newSingleThreadExecutor().execute(() ->
            AudioDownloader.downloadAll(this, new AudioDownloader.Progress() {
                @Override public void onProgress(int i, int t, String title) {
                    ui.post(() -> {
                        int pct = t == 0 ? 0 : (i * 100 / t);
                        progressBar.setProgress(pct);
                        percentText.setText(getString(R.string.welcome_pct_fmt, pct));
                        statusText.setText(getString(R.string.welcome_dl_fmt, i + 1, t));
                    });
                }
                @Override public void onComplete(int ok, int fail) {
                    ui.post(() -> {
                        progressBar.setProgress(100);
                        percentText.setText("100%");
                        statusText.setText(getString(R.string.welcome_done_fmt, ok, fail));
                        getSharedPreferences("salah_prefs", MODE_PRIVATE)
                            .edit().putBoolean(PREF_KEY_SHOWN, true).apply();
                        btnSkip.setEnabled(true);
                        btnSkip.setText(R.string.welcome_enter_app);
                        btnSkip.setOnClickListener(v -> goToMain());
                    });
                }
                @Override public void onError(String msg) {
                    ui.post(() -> {
                        Toast.makeText(WelcomeActivity.this, msg, Toast.LENGTH_LONG).show();
                        btnDownload.setEnabled(true);
                        btnSkip.setEnabled(true);
                    });
                }
            }));
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
