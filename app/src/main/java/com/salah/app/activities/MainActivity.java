package com.salah.app.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;
import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.LocationHelper;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.utils.PermissionHelper;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import com.salah.app.utils.PrayerTimesCalculator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // ===== Prayer Times =====
    private TextView txtCity, txtDate, txtNextPrayer, txtCountdown;
    private Handler countdownHandler;

    // ===== Tasbih =====
    private static final String[] PHRASES = {"سبحان اللّه","الحمد للّه","لا إله إلا اللّه","اللّه أكبر","أستغفر اللّه"};
    private static final int[] TARGETS = {33, 33, 33, 33, 100};
    private int tasbihStage = 0, tasbihCount = 0, tasbihTotal = 0;
    private TextView txtPhrase, txtCount, txtTarget, txtStage, txtTotal;

    // ===== Qibla =====
    private static final double KAABA_LAT = 21.4225, KAABA_LNG = 39.8262;
    private static final int LOCATION_PERMISSION_CODE = 1001;
    private SensorManager sensorManager;
    private Sensor rotationSensor, accelerometer, magnetometer;
    private float[] gravity = new float[3], geomagnetic = new float[3];
    private boolean hasGravity = false, hasGeomagnetic = false;
    private ImageView ivCompassRing, ivQiblaArrow;
    private TextView tvStatusQ, tvDegreesQ, tvCityQ, tvFacingQ, tvDistanceQ;
    private double userLat = 0, userLng = 0;
    private boolean locationReady = false;
    private float currentDegree = 0f, currentArrowDegree = 0f, qiblaAngle = 0f;

    // ===== Tabs =====
    private View tabPrayer, tabAzkar, tabTasbih, tabQibla, tabSettings;
    private int currentTab = R.id.nav_prayer_times;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            PreferencesManager.applyTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            initPrayerViews();
            initTasbihViews();
            initQiblaViews();
            initTabs();
            initBottomNav();
            initAzkarListeners();
            initCalendar();

            SimpleDateFormat sdf = new SimpleDateFormat("EEEE، dd/MM/yyyy", new Locale("ar"));
            if (txtDate != null) txtDate.setText(sdf.format(new Date()));

            // طلب إذن الموقع عند البداية
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            } else {
                updatePrayerTimesFromLocation();
            }
            displayPrayerTimes();
            startCountdown();
            AlarmScheduler.rescheduleAll(this);

        } catch (Exception e) {
            Toast.makeText(this, "خطأ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initTabs() {
        tabPrayer   = findViewById(R.id.tab_prayer);
        tabAzkar    = findViewById(R.id.tab_azkar);
        tabTasbih   = findViewById(R.id.tab_tasbih);
        tabQibla    = findViewById(R.id.tab_qibla);
        tabSettings = findViewById(R.id.tab_settings);
        showTab(R.id.nav_prayer_times);
    }

    private void showTab(int tabId) {
        tabPrayer.setVisibility(View.GONE);
        tabAzkar.setVisibility(View.GONE);
        tabTasbih.setVisibility(View.GONE);
        tabQibla.setVisibility(View.GONE);
        tabSettings.setVisibility(View.GONE);

        if (tabId == R.id.nav_prayer_times)      tabPrayer.setVisibility(View.VISIBLE);
        else if (tabId == R.id.nav_azkar)         tabAzkar.setVisibility(View.VISIBLE);
        else if (tabId == R.id.nav_tasbih)        tabTasbih.setVisibility(View.VISIBLE);
        else if (tabId == R.id.nav_qibla)         { tabQibla.setVisibility(View.VISIBLE); startQiblaSensors(); }
        else if (tabId == R.id.nav_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), 999);
            currentTab = R.id.nav_prayer_times;
            return;
        }
        currentTab = tabId;
    }

    private void initBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentTab && id != R.id.nav_settings) return true;
            showTab(id);
            return true;
        });
    }

    // =================== PRAYER TIMES ===================
    private void initPrayerViews() {
        txtCity       = findViewById(R.id.txt_city);
        txtDate       = findViewById(R.id.txt_date);
        txtNextPrayer = findViewById(R.id.txt_next_prayer);
        txtCountdown  = findViewById(R.id.txt_countdown);
    }

    private void displayPrayerTimes() {
        Location loc = PreferencesManager.loadLocation(this);
        if (loc == null) { loc = new Location(21.4225, 39.8262, "مكة المكرمة", "Asia/Riyadh"); PreferencesManager.saveLocation(this, loc); }
        if (txtCity != null) txtCity.setText(loc.cityName);
        try {
            UserSettings s = PreferencesManager.load(this);
            List<PrayerTime> times = PrayerTimesCalculator.getTodayTimes(loc, s);
            SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", new Locale("ar"));
            for (PrayerTime pt : times) {
                String t = tf.format(pt.time);
                switch (pt.prayer.id) {
                    case "fajr":    setPrayer(R.id.txt_fajr, R.id.txt_fajr_val, "الفجر", t); break;
                    case "dhuhr":   setPrayer(R.id.txt_dhuhr, R.id.txt_dhuhr_val, "الظهر", t); break;
                    case "asr":     setPrayer(R.id.txt_asr, R.id.txt_asr_val, "العصر", t); break;
                    case "maghrib": setPrayer(R.id.txt_maghrib, R.id.txt_maghrib_val, "المغرب", t); break;
                    case "isha":    setPrayer(R.id.txt_isha, R.id.txt_isha_val, "العشاء", t); break;
                }
            }
        } catch (Exception e) { if (txtNextPrayer!=null) txtNextPrayer.setText("تعذر حساب الأوقات"); }
    }

    private void setPrayer(int nameId, int valId, String name, String val) {
        TextView n = findViewById(nameId); if (n!=null) n.setText(name);
        TextView v = findViewById(valId);  if (v!=null) v.setText(val);
    }

    private void startCountdown() {
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownHandler.post(new Runnable() {
            @Override public void run() {
                updateCountdown();
                countdownHandler.postDelayed(this, 1000);
            }
        });
    }

    private void updateCountdown() {
        try {
            Location loc = PreferencesManager.loadLocation(this);
            if (loc == null) loc = new Location(21.4225, 39.8262, "مكة المكرمة", "Asia/Riyadh");
            PrayerTime next = PrayerTimesCalculator.nextPrayer(loc, PreferencesManager.load(this));
            if (next == null) return;
            long diff = next.time.getTime() - System.currentTimeMillis();
            if (diff < 0) return;
            long h = diff/3600000, m=(diff%3600000)/60000, sec=(diff%60000)/1000;
            if (txtNextPrayer!=null) txtNextPrayer.setText("الصلاة القادمة: " + next.getArabicName());
            if (txtCountdown!=null) txtCountdown.setText(String.format(Locale.getDefault(),"%02d:%02d:%02d",h,m,sec));
        } catch (Exception ignored) {}
    }

    // =================== AZKAR ===================
    private void initCalendar() {
        try {
            android.widget.TextView tvHijri = findViewById(R.id.tv_hijri_date);
            android.widget.TextView tvMiladi = findViewById(R.id.tv_miladi_date);
            if (tvHijri == null || tvMiladi == null) return;

            // التقويم الميلادي
            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.text.SimpleDateFormat sdfM = new java.text.SimpleDateFormat("EEEE d MMMM yyyy", new java.util.Locale("ar"));
            tvMiladi.setText(sdfM.format(cal.getTime()));

            // التقويم الهجري
            android.icu.util.IslamicCalendar hijri = new android.icu.util.IslamicCalendar();
            hijri.setTimeInMillis(cal.getTimeInMillis());
            int day   = hijri.get(android.icu.util.Calendar.DAY_OF_MONTH);
            int month = hijri.get(android.icu.util.Calendar.MONTH);
            int year  = hijri.get(android.icu.util.Calendar.YEAR);
            String[] months = {"محرم","صفر","ربيع الأول","ربيع الثاني","جمادى الأولى","جمادى الثانية","رجب","شعبان","رمضان","شوال","ذو القعدة","ذو الحجة"};
            tvHijri.setText(day + " " + months[month] + " " + year + " هـ");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void initAzkarListeners() {
        try { findViewById(R.id.card_morning).setOnClickListener(v -> openAdhkar("morning")); } catch (Exception ignored) {}
        try { findViewById(R.id.card_evening).setOnClickListener(v -> openAdhkar("evening")); } catch (Exception ignored) {}
        try { findViewById(R.id.card_after_salah).setOnClickListener(v -> openAdhkar("afterSalah")); } catch (Exception ignored) {}
        try { findViewById(R.id.card_wakeup).setOnClickListener(v -> openAdhkar("wakeup")); } catch (Exception ignored) {}
        try { findViewById(R.id.card_duas).setOnClickListener(v -> openAdhkar("duas")); } catch (Exception ignored) {}
        try { findViewById(R.id.card_iftitah).setOnClickListener(v -> openAdhkar("iftitah")); } catch (Exception ignored) {}
        try { findViewById(R.id.card_sleep).setOnClickListener(v -> startActivity(new Intent(this, SleepAthkarActivity.class))); } catch (Exception ignored) {}
        try { findViewById(R.id.card_mutanawia).setOnClickListener(v -> openAdhkar("mutanawia")); } catch (Exception ignored) {}
    }

    private void openAdhkar(String category) {
        Intent i = new Intent(this, AdhkarSessionActivity.class);
        i.putExtra("category", category);
        startActivity(i);
    }

    // =================== TASBIH ===================
    private void initTasbihViews() {
        txtPhrase = findViewById(R.id.txt_phrase);
        txtCount  = findViewById(R.id.txt_count);
        txtTarget = findViewById(R.id.txt_target);
        txtStage  = findViewById(R.id.txt_stage);
        txtTotal  = findViewById(R.id.txt_total);
        MaterialButton btnTap   = findViewById(R.id.btn_tap);
        MaterialButton btnReset = findViewById(R.id.btn_reset);
        if (btnTap   != null) btnTap.setOnClickListener(v -> onTasbihTap());
        if (btnReset != null) btnReset.setOnClickListener(v -> resetTasbih());
        updateTasbih();
    }

    private void onTasbihTap() {
        tasbihCount++; tasbihTotal++;
        vibrate(40);
        if (tasbihCount >= TARGETS[tasbihStage]) {
            vibrate(200); tasbihStage++; tasbihCount = 0;
            if (tasbihStage >= PHRASES.length) {
                Toast.makeText(this, R.string.tasbih_completed, Toast.LENGTH_LONG).show();
                tasbihStage = 0;
            }
        }
        updateTasbih();
    }

    private void resetTasbih() { tasbihStage=0; tasbihCount=0; tasbihTotal=0; updateTasbih(); }

    private void updateTasbih() {
        if (txtPhrase!=null) txtPhrase.setText(PHRASES[tasbihStage]);
        if (txtCount !=null) txtCount.setText(String.valueOf(tasbihCount));
        if (txtTarget!=null) txtTarget.setText(getString(R.string.tasbih_target_fmt, TARGETS[tasbihStage]));
        if (txtStage !=null) txtStage.setText(getString(R.string.tasbih_stage_fmt, tasbihStage+1, PHRASES.length));
        if (txtTotal !=null) txtTotal.setText(String.valueOf(tasbihTotal));
    }

    private void vibrate(long ms) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                v = vm!=null ? vm.getDefaultVibrator() : null;
            } else { v = (Vibrator) getSystemService(VIBRATOR_SERVICE); }
            if (v==null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(ms);
        } catch (Throwable ignored) {}
    }

    // =================== QIBLA ===================
    private void initQiblaViews() {
        ivCompassRing = findViewById(R.id.iv_compass_ring_q);
        ivQiblaArrow  = findViewById(R.id.iv_qibla_arrow_q);
        tvStatusQ     = findViewById(R.id.tv_status_q);
        tvDegreesQ    = findViewById(R.id.tv_degrees_q);
        tvCityQ       = findViewById(R.id.tv_city_q);
        tvFacingQ     = findViewById(R.id.tv_facing_q);
        tvDistanceQ   = findViewById(R.id.tv_distance_q);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor == null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
    }

    private void startQiblaSensors() {
        requestQiblaLocation();
        if (rotationSensor!=null) sensorManager.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_UI);
        else { if(accelerometer!=null) sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_UI); if(magnetometer!=null) sensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_UI); }
    }

    private void requestQiblaLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }
        if (tvStatusQ != null) tvStatusQ.setText("جارٍ تحديد الموقع...");
        LocationHelper.getCurrentLocation(this, new LocationHelper.Callback() {
            @Override public void onResult(Location loc) {
                runOnUiThread(() -> {
                    userLat = loc.latitude; userLng = loc.longitude; locationReady = true;
                    qiblaAngle = (float) calcQibla(userLat, userLng);
                    if (tvCityQ != null) tvCityQ.setText(loc.cityName != null ? loc.cityName : "");
                    double dist = calcDistance(userLat, userLng, KAABA_LAT, KAABA_LNG);
                    if (tvDistanceQ != null) tvDistanceQ.setText(formatDist(dist));
                    if (tvStatusQ != null) tvStatusQ.setText("اتبع السهم الذهبي نحو القبلة");
                });
            }
            @Override public void onError(String msg) {
                runOnUiThread(() -> { if (tvStatusQ!=null) tvStatusQ.setText("تعذّر: " + msg); });
            }
        });
    }

    private double calcQibla(double lat, double lng) {
        double lat1=Math.toRadians(lat), lat2=Math.toRadians(KAABA_LAT), dLng=Math.toRadians(KAABA_LNG-lng);
        double x=Math.sin(dLng)*Math.cos(lat2);
        double y=Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLng);
        return (Math.toDegrees(Math.atan2(x,y))+360)%360;
    }

    private double calcDistance(double lat1,double lng1,double lat2,double lng2) {
        double R=6371.0, dLat=Math.toRadians(lat2-lat1), dLng=Math.toRadians(lng2-lng1);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLng/2)*Math.sin(dLng/2);
        return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }

    private String formatDist(double km) {
        if (km<1) return String.format("%.0f م من الكعبة",km*1000);
        if (km<100) return String.format("%.1f كم من الكعبة",km);
        return String.format("%.0f كم من الكعبة",km);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (currentTab != R.id.nav_qibla) return;
        float azimuth = 0f;
        if (event.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR) {
            float[] R=new float[9]; SensorManager.getRotationMatrixFromVector(R,event.values);
            float[] o=new float[3]; SensorManager.getOrientation(R,o);
            azimuth=(float)((Math.toDegrees(o[0])+360)%360); updateCompass(azimuth);
        } else if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) { gravity=event.values.clone(); hasGravity=true; }
          else if (event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD) { geomagnetic=event.values.clone(); hasGeomagnetic=true; }
        if (hasGravity && hasGeomagnetic) {
            float[] R=new float[9],I=new float[9];
            if (SensorManager.getRotationMatrix(R,I,gravity,geomagnetic)) {
                float[] o=new float[3]; SensorManager.getOrientation(R,o);
                azimuth=(float)((Math.toDegrees(o[0])+360)%360); updateCompass(azimuth);
            }
        }
    }

    private void updateCompass(float azimuth) {
        if (!locationReady || ivCompassRing==null || ivQiblaArrow==null) return;
        float newRing=-azimuth;
        RotateAnimation ra=new RotateAnimation(currentDegree,newRing,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        ra.setDuration(120); ra.setFillAfter(true); ivCompassRing.startAnimation(ra); currentDegree=newRing;
        float newArrow=qiblaAngle-azimuth;
        RotateAnimation aa=new RotateAnimation(currentArrowDegree,newArrow,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        aa.setDuration(120); aa.setFillAfter(true); ivQiblaArrow.startAnimation(aa); currentArrowDegree=newArrow;
        float diff=(qiblaAngle-azimuth+360)%360;
        if (tvDegreesQ!=null) tvDegreesQ.setText(String.format("%.1f°",diff));
        float d=diff>180?360-diff:diff;
        if (d<5) { if(tvFacingQ!=null){tvFacingQ.setText("✦ أنت تواجه القبلة ✦");tvFacingQ.setTextColor(getResources().getColor(R.color.success_green,null));tvFacingQ.setVisibility(View.VISIBLE);}
                   if(tvStatusQ!=null) tvStatusQ.setText("الله أكبر! استقبلت القبلة"); }
        else if (d<15) { if(tvFacingQ!=null){tvFacingQ.setText("قريب من القبلة");tvFacingQ.setTextColor(getResources().getColor(R.color.gold,null));tvFacingQ.setVisibility(View.VISIBLE);}
                         if(tvStatusQ!=null) tvStatusQ.setText("اضبط الاتجاه قليلاً"); }
        else { if(tvFacingQ!=null) tvFacingQ.setVisibility(View.INVISIBLE);
               if(tvStatusQ!=null) tvStatusQ.setText("اتبع السهم الذهبي نحو القبلة"); }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 999) {
            currentTab = R.id.nav_prayer_times;
            showTab(R.id.nav_prayer_times);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                BottomNavigationView bnv = findViewById(R.id.bottomNav);
                if (bnv != null) bnv.getMenu().findItem(R.id.nav_prayer_times).setChecked(true);
            });
        }
            PreferencesManager.applyTheme(this);
            recreate();
    }
    @Override protected void onResume() {
        super.onResume();
        if (currentTab == R.id.nav_settings) {
            currentTab = R.id.nav_prayer_times;
            showTab(R.id.nav_prayer_times);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                BottomNavigationView bnv = findViewById(R.id.bottomNav);
                if (bnv != null) bnv.getMenu().findItem(R.id.nav_prayer_times).setChecked(true);
            });
        }
        if (currentTab == R.id.nav_qibla) {
            if (rotationSensor!=null) sensorManager.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_UI);
            else { if(accelerometer!=null) sensorManager.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_UI);
                   if(magnetometer!=null)  sensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_UI); }
        }
    }

    @Override protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    // =================== SETTINGS ===================
    private void initSettingsViews() {
        UserSettings s = PreferencesManager.load(this);

        String[] cityNames = {"مكة المكرمة","المدينة المنورة","الرياض","جدة","أبها","تبوك","صنعاء","عدن","المكلا"};
        Spinner spCity = findViewById(R.id.spinner_city_settings);
        if (spCity != null) {
            ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cityNames);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spCity.setAdapter(a);
            Location curLoc = PreferencesManager.loadLocation(this);
            if (curLoc != null) for (int i=0;i<cityNames.length;i++) if (cityNames[i].equals(curLoc.cityName)) { spCity.setSelection(i); break; }
            spCity.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                    PreferencesManager.saveLocation(MainActivity.this, getSettingsCityLocation(pos));
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }

        Spinner spMethod = findViewById(R.id.spinner_method);
        if (spMethod != null) {
            ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(this, R.array.calc_method_labels, android.R.layout.simple_spinner_item);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spMethod.setAdapter(a);
            spMethod.setSelection(settingsIndexForMethod(s.calculationMethodId));
            spMethod.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) { s.calculationMethodId = settingsMethodIdForIndex(pos); }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }

        Spinner spMadhab = findViewById(R.id.spinner_madhab);
        if (spMadhab != null) {
            ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(this, R.array.madhab_labels, android.R.layout.simple_spinner_item);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spMadhab.setAdapter(a);
            spMadhab.setSelection("Hanafi".equalsIgnoreCase(s.madhabId) ? 1 : 0);
            spMadhab.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) { s.madhabId = pos==1?"Hanafi":"Shafi"; }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }

        Spinner spAdhan = findViewById(R.id.spinner_adhan);
        if (spAdhan != null) {
            ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(this, R.array.adhan_labels, android.R.layout.simple_spinner_item);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spAdhan.setAdapter(a);
            spAdhan.setSelection(settingsIndexForAdhan(s.selectedAdhanFile));
            spAdhan.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) { s.selectedAdhanFile = settingsAdhanFileForIndex(pos); }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }

        com.google.android.material.button.MaterialButton btnPreview = findViewById(R.id.btn_preview_adhan);
        if (btnPreview != null) btnPreview.setOnClickListener(v -> {
            try {
                Spinner sp = findViewById(R.id.spinner_adhan);
                if (sp != null) s.selectedAdhanFile = settingsAdhanFileForIndex(sp.getSelectedItemPosition());
                String muezzin = s.selectedAdhanFile;
                if (muezzin==null||muezzin.isEmpty()) muezzin="adhan_madinah";
                if (muezzin.startsWith("adhan_")) muezzin=muezzin.substring(6);
                Intent sync = new Intent(this, SyncedAdhanActivity.class);
                sync.putExtra(SyncedAdhanActivity.EXTRA_PRAYER, "fajr");
                sync.putExtra(SyncedAdhanActivity.EXTRA_MUEZZIN, muezzin);
                startActivity(sync);
            } catch (Exception e) { Toast.makeText(this,"خطأ: "+e.getMessage(),Toast.LENGTH_SHORT).show(); }
        });

        Switch swAdhan = findViewById(R.id.sw_adhan);
        if (swAdhan!=null) { swAdhan.setChecked(s.adhanEnabled); swAdhan.setOnCheckedChangeListener((b,v)->s.adhanEnabled=v); }
        Switch swMorning = findViewById(R.id.sw_morning);
        if (swMorning!=null) { swMorning.setChecked(s.morningAthkarEnabled); swMorning.setOnCheckedChangeListener((b,v)->s.morningAthkarEnabled=v); }
        Switch swEvening = findViewById(R.id.sw_evening);
        if (swEvening!=null) { swEvening.setChecked(s.eveningAthkarEnabled); swEvening.setOnCheckedChangeListener((b,v)->s.eveningAthkarEnabled=v); }
        Switch swDark = findViewById(R.id.sw_dark);
        if (swDark!=null) { swDark.setChecked(s.darkMode); swDark.setOnCheckedChangeListener((b,v)->s.darkMode=v); }
        Switch swVib = findViewById(R.id.sw_vibrate);
        if (swVib!=null) { swVib.setChecked(s.vibrateOnAlarm); swVib.setOnCheckedChangeListener((b,v)->s.vibrateOnAlarm=v); }

        TextView lblExact = findViewById(R.id.lbl_exact_alarm);
        com.google.android.material.button.MaterialButton btnExact = findViewById(R.id.btn_exact_alarm);
        if (lblExact!=null && btnExact!=null) {
            if (PermissionHelper.canScheduleExactAlarms(this)) { lblExact.setText(R.string.exact_alarm_ok); btnExact.setVisibility(View.GONE); }
            else { lblExact.setText(R.string.exact_alarm_needed); btnExact.setVisibility(View.VISIBLE); btnExact.setOnClickListener(v->PermissionHelper.openExactAlarmSettings(this)); }
        }

        com.google.android.material.button.MaterialButton btnBattery = findViewById(R.id.btn_battery);
        if (btnBattery!=null) btnBattery.setOnClickListener(v->PermissionHelper.requestIgnoreBatteryOptimizations(this));

        com.google.android.material.button.MaterialButton btnAbout = findViewById(R.id.btn_about);
        if (btnAbout != null) btnAbout.setOnClickListener(v -> showAboutDialog());

        com.google.android.material.button.MaterialButton btnSave = findViewById(R.id.btn_save);
        if (btnSave!=null) btnSave.setOnClickListener(v -> {
            PreferencesManager.save(this, s);
            PreferencesManager.applyTheme(this);
            AlarmScheduler.rescheduleAll(this);
            Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
            Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (i!=null) { i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
            finish();
        });
    }

    private void showAboutDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_about, null);
        builder.setView(dialogView);
        builder.setPositiveButton("حسناً", null);
        builder.show();
    }

    private Location getSettingsCityLocation(int pos) {
        switch (pos) {
            case 1: return new Location(24.5247,39.5692,"المدينة المنورة","Asia/Riyadh");
            case 2: return new Location(24.7136,46.6753,"الرياض","Asia/Riyadh");
            case 3: return new Location(21.4858,39.1925,"جدة","Asia/Riyadh");
            case 4: return new Location(18.2164,42.5053,"أبها","Asia/Riyadh");
            case 5: return new Location(28.3998,36.5715,"تبوك","Asia/Riyadh");
            case 6: return new Location(15.3694,44.1910,"صنعاء","Asia/Aden");
            case 7: return new Location(12.7797,45.0367,"عدن","Asia/Aden");
            case 8: return new Location(14.5425,49.1243,"المكلا","Asia/Aden");
            default: return new Location(21.4225,39.8262,"مكة المكرمة","Asia/Riyadh");
        }
    }
    private int settingsIndexForMethod(String id) {
        switch(id) { case "MuslimWorldLeague":return 1;case "Egyptian":return 2;case "Karachi":return 3;case "Dubai":return 4;case "Qatar":return 5;case "Kuwait":return 6;case "NorthAmerica":return 7;case "MoonsightingCommittee":return 8;default:return 0; }
    }
    private String settingsMethodIdForIndex(int pos) {
        switch(pos) { case 1:return "MuslimWorldLeague";case 2:return "Egyptian";case 3:return "Karachi";case 4:return "Dubai";case 5:return "Qatar";case 6:return "Kuwait";case 7:return "NorthAmerica";case 8:return "MoonsightingCommittee";default:return "UmmAlQura"; }
    }
    private int settingsIndexForAdhan(String f) {
        switch(f) { case "adhan_kuwait":return 1;case "adhan_haram_makki":return 2;case "adhan_makkah":return 3;case "adhan_quds":return 4;case "adhan_brunei":return 5;case "adhan_afasy":return 6;case "adhan_other":return 7;default:return 0; }
    }
    private String settingsAdhanFileForIndex(int pos) {
        switch(pos) { case 1:return "adhan_kuwait";case 2:return "adhan_haram_makki";case 3:return "adhan_makkah";case 4:return "adhan_quds";case 5:return "adhan_brunei";case 6:return "adhan_afasy";case 7:return "adhan_other";default:return "adhan_madinah"; }
    }

    private void updatePrayerTimesFromLocation() {
        LocationHelper.getCurrentLocation(this, new LocationHelper.Callback() {
            @Override public void onResult(Location loc) {
                runOnUiThread(() -> {
                    PreferencesManager.saveLocation(MainActivity.this, loc);
                    if (txtCity != null) txtCity.setText(loc.cityName);
                    displayPrayerTimes();
                });
            }
            @Override public void onError(String msg) {}
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (countdownHandler!=null) countdownHandler.removeCallbacksAndMessages(null);
    }

    @Override public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code,perms,results);
        if (code == LOCATION_PERMISSION_CODE && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            // تحديث القبلة
            requestQiblaLocation();
            // تسجيل الـ sensors
            if (rotationSensor != null)
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            else {
                if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                if (magnetometer != null)  sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            }
            // تحديث أوقات الصلاة بالموقع الجديد
            updatePrayerTimesFromLocation();
        }
    }
}
