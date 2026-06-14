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
        String[] cityNames = getResources().getStringArray(R.array.city_names);
        Spinner spCity = findViewById(R.id.spinner_city_settings);
        if (spCity != null) {
            ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cityNames);
            cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCity.setAdapter(cityAdapter);
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

        // ===== Calculation method =====
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

        // ===== Madhab =====
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

        // ===== ✅ أذان الصلوات الأربع (ظهر عصر مغرب عشاء) =====
        Spinner spAdhan = findViewById(R.id.spinner_adhan);
        if (spAdhan != null) {
            ArrayAdapter<CharSequence> adhanAdapter = ArrayAdapter.createFromResource(this,
                R.array.adhan_four_prayers_labels, android.R.layout.simple_spinner_item);
            adhanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spAdhan.setAdapter(adhanAdapter);
            spAdhan.setSelection(indexForFourPrayersAdhan(s.selectedAdhanFile));
            spAdhan.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                    s.selectedAdhanFile = fourPrayersAdhanFileForIndex(pos);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }

        // زر معاينة أذان الصلوات الأربع
        MaterialButton btnPreviewFour = findViewById(R.id.btn_preview_adhan);
        if (btnPreviewFour != null) {
            btnPreviewFour.setOnClickListener(v -> previewAdhan(false));
        }

        // ===== ✅ أذان الفجر منفصل =====
        Spinner spFajrAdhan = findViewById(R.id.spinner_fajr_adhan);
        if (spFajrAdhan != null) {
            ArrayAdapter<CharSequence> fajrAdapter = ArrayAdapter.createFromResource(this,
                R.array.adhan_fajr_labels, android.R.layout.simple_spinner_item);
            fajrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spFajrAdhan.setAdapter(fajrAdapter);
            spFajrAdhan.setSelection(indexForFajrAdhan(s.selectedFajrAdhanFile));
            spFajrAdhan.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                    s.selectedFajrAdhanFile = fajrAdhanFileForIndex(pos);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }

        // زر معاينة أذان الفجر
        MaterialButton btnPreviewFajr = findViewById(R.id.btn_preview_fajr_adhan);
        if (btnPreviewFajr != null) {
            btnPreviewFajr.setOnClickListener(v -> previewAdhan(true));
        }

        // ===== Switches =====
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
        swDark.setOnCheckedChangeListener((CompoundButton b, boolean v) -> {
            s.darkMode = v;
            PreferencesManager.save(this, s);
            PreferencesManager.applyTheme(this);
            recreate();
        });

        Switch swVib = findViewById(R.id.sw_vibrate);
        swVib.setChecked(s.vibrateOnAlarm);
        swVib.setOnCheckedChangeListener((CompoundButton b, boolean v) -> s.vibrateOnAlarm = v);

        // ===== Permissions =====
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
        if (btnCheckUpdate != null) btnCheckUpdate.setOnClickListener(v -> UpdateChecker.check(this, true));

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

        // ===== حفظ =====
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

    // ✅ معاينة الأذان - isFajr=true لأذان الفجر، false للصلوات الأربع
    private void previewAdhan(boolean isFajr) {
        try {
            String adhanFile;
            if (isFajr) {
                Spinner sp = findViewById(R.id.spinner_fajr_adhan);
                if (sp != null) s.selectedFajrAdhanFile = fajrAdhanFileForIndex(sp.getSelectedItemPosition());
                adhanFile = s.selectedFajrAdhanFile;
            } else {
                Spinner sp = findViewById(R.id.spinner_adhan);
                if (sp != null) s.selectedAdhanFile = fourPrayersAdhanFileForIndex(sp.getSelectedItemPosition());
                adhanFile = s.selectedAdhanFile;
            }
            if (adhanFile == null || adhanFile.isEmpty()) adhanFile = "adhan_madinah";
            String muezzin = adhanFile.startsWith("adhan_") ? adhanFile.substring(6) : adhanFile;

            // ✅ تشغيل صوت الأذان عبر AdhanService أثناء المعاينة
            String previewPrayerId = isFajr ? "fajr" : "dhuhr";
            Intent svc = new Intent(this, com.salah.app.services.AdhanService.class);
            svc.putExtra(com.salah.app.utils.AlarmScheduler.EXTRA_PRAYER_ID, previewPrayerId);
            svc.putExtra(com.salah.app.utils.AlarmScheduler.EXTRA_PRAYER_NAME_AR, isFajr ? "الفجر" : "الظهر");
            svc.putExtra("adhan_file", adhanFile);
            svc.putExtra("is_preview", true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(svc);
            } else {
                startService(svc);
            }

            Intent sync = new Intent(this, SyncedAdhanActivity.class);
            sync.putExtra(SyncedAdhanActivity.EXTRA_PRAYER, previewPrayerId);
            sync.putExtra(SyncedAdhanActivity.EXTRA_MUEZZIN, muezzin);
            startActivity(sync);
        } catch (Exception e) {
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    // ===== Helpers =====

    // ✅ أذان الصلوات الأربع: مكة - القدس - بروناي - آخر
    private int indexForFourPrayersAdhan(String fileName) {
        if (fileName == null) return 0;
        switch (fileName) {
            case "adhan_quds":   return 1;
            case "adhan_brunei": return 2;
            case "adhan_other":  return 3;
            case "adhan_makkah":
            default:             return 0;
        }
    }

    private String fourPrayersAdhanFileForIndex(int pos) {
        switch (pos) {
            case 1: return "adhan_quds";
            case 2: return "adhan_brunei";
            case 3: return "adhan_other";
            default: return "adhan_makkah";
        }
    }

    // ✅ أذان الفجر: الحرم المدني - الكويت - الحرم المكي - مشاري العفاسي
    private int indexForFajrAdhan(String fileName) {
        if (fileName == null) return 0;
        switch (fileName) {
            case "adhan_kuwait":      return 1;
            case "adhan_haram_makki": return 2;
            case "adhan_afasy":       return 3;
            case "adhan_madinah":
            default:                  return 0;
        }
    }

    private String fajrAdhanFileForIndex(int pos) {
        switch (pos) {
            case 1: return "adhan_kuwait";
            case 2: return "adhan_haram_makki";
            case 3: return "adhan_afasy";
            default: return "adhan_madinah";
        }
    }

    private int indexForMethod(String id) {
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

    private Location getCityLocation(int pos) {
        String[] names = getResources().getStringArray(R.array.city_names);
        String name = pos < names.length ? names[pos] : "مكة المكرمة";
        switch (pos) {
            case 0:  return new Location(21.4225, 39.8262, name, "Asia/Riyadh");
            case 1:  return new Location(24.5247, 39.5692, name, "Asia/Riyadh");
            case 2:  return new Location(24.7136, 46.6753, name, "Asia/Riyadh");
            case 3:  return new Location(21.4858, 39.1925, name, "Asia/Riyadh");
            case 4:  return new Location(26.4207, 50.0888, name, "Asia/Riyadh");
            case 5:  return new Location(26.2172, 50.1971, name, "Asia/Riyadh");
            case 6:  return new Location(18.2164, 42.5053, name, "Asia/Riyadh");
            case 7:  return new Location(28.3998, 36.5715, name, "Asia/Riyadh");
            case 8:  return new Location(26.3292, 43.9697, name, "Asia/Riyadh");
            case 9:  return new Location(27.5114, 41.7208, name, "Asia/Riyadh");
            case 10: return new Location(17.4924, 44.1277, name, "Asia/Riyadh");
            case 11: return new Location(16.8892, 42.5611, name, "Asia/Riyadh");
            case 12: return new Location(21.2854, 40.4148, name, "Asia/Riyadh");
            case 13: return new Location(24.4539, 54.3773, name, "Asia/Dubai");
            case 14: return new Location(25.2048, 55.2708, name, "Asia/Dubai");
            case 15: return new Location(25.3463, 55.4209, name, "Asia/Dubai");
            case 16: return new Location(25.4052, 55.5136, name, "Asia/Dubai");
            case 17: return new Location(25.7895, 55.9432, name, "Asia/Dubai");
            case 18: return new Location(29.3759, 47.9774, name, "Asia/Kuwait");
            case 19: return new Location(25.2854, 51.5310, name, "Asia/Qatar");
            case 20: return new Location(26.2235, 50.5876, name, "Asia/Bahrain");
            case 21: return new Location(23.5880, 58.3829, name, "Asia/Muscat");
            case 22: return new Location(17.0151, 54.0924, name, "Asia/Muscat");
            case 23: return new Location(15.3694, 44.1910, name, "Asia/Aden");
            case 24: return new Location(12.7797, 45.0367, name, "Asia/Aden");
            case 25: return new Location(14.5425, 49.1243, name, "Asia/Aden");
            case 26: return new Location(13.5789, 44.0186, name, "Asia/Aden");
            case 27: return new Location(33.3152, 44.3661, name, "Asia/Baghdad");
            case 28: return new Location(30.5085, 47.7835, name, "Asia/Baghdad");
            case 29: return new Location(36.3350, 43.1189, name, "Asia/Baghdad");
            case 30: return new Location(36.1901, 44.0091, name, "Asia/Baghdad");
            case 31: return new Location(33.5138, 36.2765, name, "Asia/Damascus");
            case 32: return new Location(36.2021, 37.1343, name, "Asia/Damascus");
            case 33: return new Location(34.7324, 36.7137, name, "Asia/Damascus");
            case 34: return new Location(33.8886, 35.4955, name, "Asia/Beirut");
            case 35: return new Location(31.9454, 35.9284, name, "Asia/Amman");
            case 36: return new Location(32.0853, 36.0880, name, "Asia/Amman");
            case 37: return new Location(31.7683, 35.2137, name, "Asia/Jerusalem");
            case 38: return new Location(31.5017, 34.4668, name, "Asia/Gaza");
            case 39: return new Location(31.9038, 35.2034, name, "Asia/Jerusalem");
            case 40: return new Location(30.0444, 31.2357, name, "Africa/Cairo");
            case 41: return new Location(31.2001, 29.9187, name, "Africa/Cairo");
            case 42: return new Location(30.0131, 31.2089, name, "Africa/Cairo");
            case 43: return new Location(24.0889, 32.8998, name, "Africa/Cairo");
            case 44: return new Location(25.6872, 32.6396, name, "Africa/Cairo");
            case 45: return new Location(32.9011, 13.1800, name, "Africa/Tripoli");
            case 46: return new Location(32.1194, 20.0868, name, "Africa/Tripoli");
            case 47: return new Location(36.8190, 10.1658, name, "Africa/Tunis");
            case 48: return new Location(34.7406, 10.7603, name, "Africa/Tunis");
            case 49: return new Location(36.7372, 3.0865,  name, "Africa/Algiers");
            case 50: return new Location(35.6969, -0.6331, name, "Africa/Algiers");
            case 51: return new Location(33.9716, -6.8498, name, "Africa/Casablanca");
            case 52: return new Location(33.5731, -7.5898, name, "Africa/Casablanca");
            case 53: return new Location(31.6295, -7.9811, name, "Africa/Casablanca");
            case 54: return new Location(34.0181, -5.0078, name, "Africa/Casablanca");
            case 55: return new Location(18.0735, -15.9582,name, "Africa/Nouakchott");
            case 56: return new Location(15.5007, 32.5599, name, "Africa/Khartoum");
            case 57: return new Location(2.0469,  45.3182, name, "Africa/Mogadishu");
            case 58: return new Location(11.5720, 43.1456, name, "Africa/Djibouti");
            case 59: return new Location(-11.7022,43.2551, name, "Indian/Comoro");
            case 60: return new Location(41.0082, 28.9784, name, "Europe/Istanbul");
            case 61: return new Location(39.9334, 32.8597, name, "Europe/Istanbul");
            case 62: return new Location(35.6892, 51.3890, name, "Asia/Tehran");
            case 63: return new Location(34.5553, 69.2075, name, "Asia/Kabul");
            case 64: return new Location(24.8607, 67.0011, name, "Asia/Karachi");
            case 65: return new Location(31.5204, 74.3587, name, "Asia/Karachi");
            case 66: return new Location(33.6844, 73.0479, name, "Asia/Karachi");
            case 67: return new Location(-6.2088, 106.8456,name, "Asia/Jakarta");
            case 68: return new Location(3.1390,  101.6869,name, "Asia/Kuala_Lumpur");
            case 69: return new Location(51.5074, -0.1278, name, "Europe/London");
            case 70: return new Location(48.8566, 2.3522,  name, "Europe/Paris");
            case 71: return new Location(52.5200, 13.4050, name, "Europe/Berlin");
            case 72: return new Location(40.7128, -74.0060,name, "America/New_York");
            case 73: return new Location(34.0522, -118.2437,name,"America/Los_Angeles");
            default: return new Location(21.4225, 39.8262, "مكة المكرمة", "Asia/Riyadh");
        }
    }
}
