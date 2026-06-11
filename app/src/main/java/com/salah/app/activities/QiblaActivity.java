package com.salah.app.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.salah.app.R;
import com.salah.app.utils.LocationHelper;
import com.salah.app.utils.PreferencesManager;

public class QiblaActivity extends AppCompatActivity implements SensorEventListener {

    private static final double KAABA_LAT = 21.4225;
    private static final double KAABA_LNG = 39.8262;
    private static final int LOCATION_PERMISSION_CODE = 1001;

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] gravity    = new float[3];
    private float[] geomagnetic = new float[3];
    private boolean hasGravity     = false;
    private boolean hasGeomagnetic = false;

    private ImageView ivCompassRing, ivQiblaArrow;
    private TextView  tvStatus, tvDegrees, tvCity, tvFacing, tvDistance;
    private Button    btnRetry;

    private double userLat = 0, userLng = 0;
    private boolean locationReady = false;
    private float currentDegree = 0f, currentArrowDegree = 0f;
    private float qiblaAngle = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qibla);

        ivCompassRing = findViewById(R.id.iv_compass_ring);
        ivQiblaArrow  = findViewById(R.id.iv_qibla_arrow);
        tvStatus      = findViewById(R.id.tv_status);
        tvDegrees     = findViewById(R.id.tv_degrees);
        tvCity        = findViewById(R.id.tv_city);
        tvFacing      = findViewById(R.id.tv_facing);
        tvDistance    = findViewById(R.id.tv_distance);
        btnRetry      = findViewById(R.id.btn_retry);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor == null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer  = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        btnRetry.setOnClickListener(v -> requestLocation());
        requestLocation();
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                             Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_CODE);
            return;
        }
        tvStatus.setText("جارٍ تحديد الموقع...");
        btnRetry.setVisibility(View.GONE);

        LocationHelper.getCurrentLocation(this, new LocationHelper.Callback() {
            @Override
            public void onResult(com.salah.app.models.Location loc) {
                runOnUiThread(() -> {
                    userLat = loc.latitude;
                    userLng = loc.longitude;
                    locationReady = true;
                    qiblaAngle = (float) calculateQiblaAngle(userLat, userLng);

                    String city = (loc.cityName != null && !loc.cityName.isEmpty())
                            ? loc.cityName : "";
                    tvCity.setText(city);

                    double distKm = calculateDistance(userLat, userLng, KAABA_LAT, KAABA_LNG);
                    tvDistance.setText(formatDistance(distKm));

                    tvStatus.setText("حرّك جهازك ببطء لمعايرة البوصلة");
                    btnRetry.setVisibility(View.GONE);
                });
            }
            @Override
            public void onError(String msg) {
                runOnUiThread(() -> {
                    tvStatus.setText("تعذّر تحديد الموقع");
                    btnRetry.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    /** Great-Circle bearing من موقع المستخدم إلى الكعبة */
    private double calculateQiblaAngle(double lat, double lng) {
        double lat1  = Math.toRadians(lat);
        double lat2  = Math.toRadians(KAABA_LAT);
        double dLng  = Math.toRadians(KAABA_LNG - lng);
        double x = Math.sin(dLng) * Math.cos(lat2);
        double y = Math.cos(lat1) * Math.sin(lat2)
                 - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
        return (Math.toDegrees(Math.atan2(x, y)) + 360) % 360;
    }

    /** Haversine distance بالكيلومتر */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng/2) * Math.sin(dLng/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    private String formatDistance(double km) {
        if (km < 1) return String.format("%.0f م من الكعبة", km * 1000);
        if (km < 100) return String.format("%.1f كم من الكعبة", km);
        return String.format("%.0f كم من الكعبة", km);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            if (accelerometer != null)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            if (magnetometer != null)
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float azimuth = 0f;

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] R = new float[9];
            SensorManager.getRotationMatrixFromVector(R, event.values);
            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);
            azimuth = (float)((Math.toDegrees(orientation[0]) + 360) % 360);
            updateCompass(azimuth);

        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();
            hasGravity = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone();
            hasGeomagnetic = true;
        }

        if (hasGravity && hasGeomagnetic) {
            float[] R = new float[9], I = new float[9];
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float)((Math.toDegrees(orientation[0]) + 360) % 360);
                updateCompass(azimuth);
            }
        }
    }

    private void updateCompass(float azimuth) {
        if (!locationReady) return;

        // دوران حلقة البوصلة
        float newRing = -azimuth;
        RotateAnimation ringAnim = new RotateAnimation(currentDegree, newRing,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ringAnim.setDuration(120); ringAnim.setFillAfter(true);
        ivCompassRing.startAnimation(ringAnim);
        currentDegree = newRing;

        // دوران سهم القبلة
        float newArrow = qiblaAngle - azimuth;
        RotateAnimation arrowAnim = new RotateAnimation(currentArrowDegree, newArrow,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        arrowAnim.setDuration(120); arrowAnim.setFillAfter(true);
        ivQiblaArrow.startAnimation(arrowAnim);
        currentArrowDegree = newArrow;

        // عرض الزاوية
        float displayAngle = (qiblaAngle - azimuth + 360) % 360;
        tvDegrees.setText(String.format("%.1f°", displayAngle));

        // هل يواجه القبلة؟
        float diff = displayAngle > 180 ? 360 - displayAngle : displayAngle;
        if (diff < 5) {
            tvFacing.setText("✦ أنت تواجه القبلة ✦");
            tvFacing.setTextColor(getResources().getColor(R.color.success_green, null));
            tvFacing.setVisibility(View.VISIBLE);
            tvStatus.setText("الله أكبر! استقبلت القبلة");
        } else if (diff < 15) {
            tvFacing.setText("قريب جداً من القبلة");
            tvFacing.setTextColor(getResources().getColor(R.color.gold, null));
            tvFacing.setVisibility(View.VISIBLE);
            tvStatus.setText("اضبط الاتجاه قليلاً");
        } else {
            tvFacing.setVisibility(View.INVISIBLE);
            tvStatus.setText("اتبع السهم الذهبي نحو القبلة");
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onRequestPermissionsResult(int code,
            @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == LOCATION_PERMISSION_CODE) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
                requestLocation();
            else {
                tvStatus.setText("يحتاج إذن الموقع لتحديد القبلة");
                btnRetry.setVisibility(View.VISIBLE);
            }
        }
    }
}
