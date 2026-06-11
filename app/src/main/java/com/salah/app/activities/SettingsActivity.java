package com.salah.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import android.media.MediaPlayer;
import com.salah.app.R;
import com.salah.app.utils.UpdateChecker;
import com.salah.app.models.UserSettings;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.PermissionHelper;
import com.salah.app.models.Location;
import com.salah.app.utils.PrayerApiClient;
import com.salah.app.utils.PreferencesManager;

public class SettingsActivity extends AppCompatActivity {

    private UserSettings s;
    private MediaPlayer previewPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferencesManager.applyTheme(this);
        setContentView(R.layout.activity_settings);

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_settings);
        }

        s = PreferencesManager.load(this);

        // ===== اختيار المدينة =====
        String[] cityNames = {
            "مكة المكرمة", "المدينة المنورة", "الرياض", "جدة",
            "أبها", "تبوك", "صنعاء", "عدن", "المكلا"
        };
        Spinner spCity = findViewById(R.id.spinner_city_settings);
        if (spCity != null) {
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cityNames);
            cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCity.setAdapter(cityAdapter);
            // تحديد المدينة الحالية
            Location currentLoc = PreferencesManager.loadLocation(this);
            if (currentLoc != null) {
                for (int ci = 0; ci < cityNames.length; ci++) {
                    if (cityNames[ci].equals(currentLoc.cityName)) {
                        spCity.setSelection(ci); break;
                    }
                }
            }
            spCity.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                    Location loc = getCityLocation(pos);
                    PreferencesManager.saveLocation(SettingsActivity.this, loc);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }
        // ===========================

        // Calculation method spinner
        Spinner spMethod = findViewById(R.id.spinner_method);
        ArrayAdapter<CharSequence> methodAdapter = ArrayAdapter.createFromResource(this,
            R.array.calc_method_labels, android.R.layout.simple_spinner_item);
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMethod.setAdapter(methodAdapter);
        spMethod.setSelection(indexForMethod(s.calculationMethodId));
        spMethod.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                s.calculationMethodId = methodIdForIndex(pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // Madhab spinner
        Spinner spMadhab = findViewById(R.id.spinner_madhab);
        ArrayAdapter<CharSequence> madhabAdapter = ArrayAdapter.createFromResource(this,
            R.array.madhab_labels, android.R.layout.simple_spinner_item);
        madhabAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMadhab.setAdapter(madhabAdapter);
        spMadhab.setSelection("Hanafi".equalsIgnoreCase(s.madhabId) ? 1 : 0);
        spMadhab.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                s.madhabId = pos == 1 ? "Hanafi" : "Shafi";
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // Adhan file spinner
        Spinner spAdhan = findViewById(R.id.spinner_adhan);
        ArrayAdapter<CharSequence> adhanAdapter = ArrayAdapter.createFromResource(this,
            R.array.adhan_labels, android.R.layout.simple_spinner_item);
        adhanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAdhan.setAdapter(adhanAdapter);
        spAdhan.setSelection(indexForAdhan(s.selectedAdhanFile));
        spAdhan.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                s.selectedAdhanFile = adhanFileForIndex(pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // زر معاينة الأذان
        com.google.android.material.button.MaterialButton btnPreview = findViewById(R.id.btn_preview_adhan);
        if (btnPreview != null) {
            btnPreview.setOnClickListener(v -> previewAdhan());
        }

        // Switches
        Switch swAdhan = findViewById(R.id.sw_adhan);
        swAdhan.setChecked(s.adhanEnabled);
        swAdhan.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.adhanEnabled = v);

        Switch swMorning = findViewById(R.id.sw_morning);
        swMorning.setChecked(s.morningAthkarEnabled);
        swMorning.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.morningAthkarEnabled = v);

        Switch swEvening = findViewById(R.id.sw_evening);
        swEvening.setChecked(s.eveningAthkarEnabled);
        swEvening.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.eveningAthkarEnabled = v);

        Switch swDark = findViewById(R.id.sw_dark);
        swDark.setChecked(s.darkMode);
        swDark.setOnCheckedChangeListener((CompoundButton b, boolean v) -> { s.darkMode = v; PreferencesManager.save(this, s); PreferencesManager.applyTheme(this); recreate(); });

        Switch swVib = findViewById(R.id.sw_vibrate);
        swVib.setChecked(s.vibrateOnAlarm);
        swVib.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.vibrateOnAlarm = v);

        TextView lblExactAlarm = findViewById(R.id.lbl_exact_alarm);
        MaterialButton btnExactAlarm = findViewById(R.id.btn_exact_alarm);
        if (PermissionHelper.canScheduleExactAlarms(this)) {
            lblExactAlarm.setText(R.string.exact_alarm_ok);
            btnExactAlarm.setVisibility(android.view.View.GONE);
        } else {
            lblExactAlarm.setText(R.string.exact_alarm_needed);
            btnExactAlarm.setVisibility(android.view.View.VISIBLE);
            btnExactAlarm.setOnClickListener(v -> PermissionHelper.openExactAlarmSettings(this));
        }

        MaterialButton btnBattery = findViewById(R.id.btn_battery);
        btnBattery.setOnClickListener(v -> PermissionHelper.requestIgnoreBatteryOptimizations(this));

        MaterialButton btnTiktok = findViewById(R.id.btn_tiktok);
        if (btnTiktok != null) btnTiktok.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.tiktok.com/@_qra_n"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show();
            }
        });

        MaterialButton btnCheckUpdate = findViewById(R.id.btn_check_update);
        if (btnCheckUpdate != null) btnCheckUpdate.setOnClickListener(v ->
            UpdateChecker.check(this, true));

        MaterialButton btnDescription = findViewById(R.id.btn_description);
        if (btnDescription != null) btnDescription.setOnClickListener(v -> {
            android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this);
            b.setView(getLayoutInflater().inflate(R.layout.dialog_description, null));
            b.setPositiveButton("حسناً", null);
            b.show();
        });

        MaterialButton btnAbout = findViewById(R.id.btn_about);
        if (btnAbout != null) btnAbout.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_about, null);
            builder.setView(dialogView);
            builder.setPositiveButton("حسناً", null);
            builder.show();
        });

        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> {
            PreferencesManager.save(this, s);
            PreferencesManager.applyTheme(this);
            AlarmScheduler.rescheduleAll(this);
            Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void previewAdhan() {
        try {
            Spinner sp = findViewById(R.id.spinner_adhan);
            if (sp != null) s.selectedAdhanFile = adhanFileForIndex(sp.getSelectedItemPosition());
            String muezzin = s.selectedAdhanFile;
            if (muezzin == null || muezzin.isEmpty()) muezzin = "adhan_madinah";
            if (muezzin.startsWith("adhan_")) muezzin = muezzin.substring(6);
            Intent sync = new Intent(this, SyncedAdhanActivity.class);
            sync.putExtra(SyncedAdhanActivity.EXTRA_PRAYER, "fajr");
            sync.putExtra(SyncedAdhanActivity.EXTRA_MUEZZIN, muezzin);
            startActivity(sync);
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "خطأ: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (previewPlayer != null) { previewPlayer.release(); previewPlayer = null; }
    }

    // ---- Helpers (keep arrays.xml in sync with these indexes) ----

    private Location getCityLocation(int pos) {
        switch (pos) {
            case 1: return new Location(24.5247, 39.5692, "المدينة المنورة", "Asia/Riyadh");
            case 2: return new Location(24.7136, 46.6753, "الرياض",          "Asia/Riyadh");
            case 3: return new Location(21.4858, 39.1925, "جدة",             "Asia/Riyadh");
            case 4: return new Location(18.2164, 42.5053, "أبها",            "Asia/Riyadh");
            case 5: return new Location(28.3998, 36.5715, "تبوك",            "Asia/Riyadh");
            case 6: return new Location(15.3694, 44.1910, "صنعاء",           "Asia/Aden");
            case 7: return new Location(12.7797, 45.0367, "عدن",             "Asia/Aden");
            case 8: return new Location(14.5425, 49.1243, "المكلا",          "Asia/Aden");
            default: return new Location(21.4225, 39.8262, "مكة المكرمة",    "Asia/Riyadh");
        }
    }
    private int indexForMethod(String id) { // REPLACED
        switch (id) {
            case "MuslimWorldLeague": return 1;
            case "Egyptian": return 2;
            case "Karachi": return 3;
            case "Dubai": return 4;
            case "Qatar": return 5;
            case "Kuwait": return 6;
            case "NorthAmerica": return 7;
            case "MoonsightingCommittee": return 8;
            case "UmmAlQura":
            default: return 0;
        }
    }
    private String methodIdForIndex(int pos) {
        switch (pos) {
            case 1: return "MuslimWorldLeague";
            case 2: return "Egyptian";
            case 3: return "Karachi";
            case 4: return "Dubai";
            case 5: return "Qatar";
            case 6: return "Kuwait";
            case 7: return "NorthAmerica";
            case 8: return "MoonsightingCommittee";
            default: return "UmmAlQura";
        }
    }
    private int indexForAdhan(String fileName) {
        switch (fileName) {
            case "adhan_kuwait":      return 1;
            case "adhan_haram_makki": return 2;
            case "adhan_makkah":      return 3;
            case "adhan_quds":        return 4;
            case "adhan_brunei":      return 5;
            case "adhan_afasy":       return 6;
            case "adhan_other":       return 7;
            case "adhan_madinah":
            default:                  return 0;
        }
    }
    private String adhanFileForIndex(int pos) {
        switch (pos) {
            case 1: return "adhan_kuwait";
            case 2: return "adhan_haram_makki";
            case 3: return "adhan_makkah";
            case 4: return "adhan_quds";
            case 5: return "adhan_brunei";
            case 6: return "adhan_afasy";
            case 7: return "adhan_other";
            default: return "adhan_madinah";
        }
    }
}
