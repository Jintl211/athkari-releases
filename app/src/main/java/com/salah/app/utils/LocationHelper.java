package com.salah.app.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import com.salah.app.models.Location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class LocationHelper {

    public interface Callback {
        void onResult(Location location);
        void onError(String message);
    }

    public static boolean hasPermission(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void getCurrentLocation(Context ctx, Callback cb) {
        if (!hasPermission(ctx)) { cb.onError("NO_PERMISSION"); return; }

        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) { cb.onError("NO_LOCATION_MANAGER"); return; }

        Handler handler = new Handler(Looper.getMainLooper());

        // جرب GPS أولاً ثم Network
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean netEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !netEnabled) {
            cb.onError("PROVIDERS_DISABLED");
            return;
        }

        android.location.LocationListener[] listenerHolder = new android.location.LocationListener[1];
        boolean[] done = {false};

        listenerHolder[0] = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(android.location.Location loc) {
                if (done[0]) return;
                done[0] = true;
                try { lm.removeUpdates(listenerHolder[0]); } catch (Exception ignored) {}
                cb.onResult(toLocation(ctx, loc.getLatitude(), loc.getLongitude()));
            }
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
            @Override public void onStatusChanged(String p, int s, android.os.Bundle e) {}
        };

        // timeout 10 ثواني
        handler.postDelayed(() -> {
            if (done[0]) return;
            done[0] = true;
            try { lm.removeUpdates(listenerHolder[0]); } catch (Exception ignored) {}
            // جرب آخر موقع معروف
            try {
                android.location.Location last = null;
                if (gpsEnabled) last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last == null && netEnabled) last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (last != null) { cb.onResult(toLocation(ctx, last.getLatitude(), last.getLongitude())); return; }
            } catch (SecurityException ignored) {}
            cb.onError("TIMEOUT");
        }, 10000);

        try {
            if (gpsEnabled)
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listenerHolder[0], Looper.getMainLooper());
            if (netEnabled)
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listenerHolder[0], Looper.getMainLooper());
        } catch (SecurityException e) {
            cb.onError(e.getMessage());
        }
    }

    private static Location toLocation(Context ctx, double lat, double lng) {
        String city = "";
        try {
            Geocoder g = new Geocoder(ctx, new Locale("ar"));
            List<Address> list = g.getFromLocation(lat, lng, 1);
            if (list != null && !list.isEmpty()) {
                Address a = list.get(0);
                city = a.getLocality() != null ? a.getLocality()
                     : a.getSubAdminArea() != null ? a.getSubAdminArea()
                     : a.getAdminArea() != null ? a.getAdminArea() : "";
            }
        } catch (IOException ignored) {}
        return new Location(lat, lng, city, TimeZone.getDefault().getID());
    }
}
