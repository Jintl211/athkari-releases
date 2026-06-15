package com.salah.app.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private static final String VERSION_URL = "https://raw.githubusercontent.com/Jintl211/athkari-releases/main/version.json";
    private static final String PREFS_NAME = "update_prefs";
    private static final String KEY_INSTALLED_VERSION = "installed_version";

    private static String getInstalledVersion(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_INSTALLED_VERSION, null);
        if (saved != null) return saved;
        try {
            String v = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
            return v != null ? v : "1.0";
        } catch (Exception e) {
            return "1.0";
        }
    }

    private static void saveInstalledVersion(Context context, String version) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_INSTALLED_VERSION, version).apply();
    }

    public static void check(Activity activity, boolean showNoUpdate) {
        new Thread(() -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                String latestVersion = json.getString("version");
                String downloadUrl = json.getString("url");
                String notes = json.optString("notes", "");
                String currentVersion = getInstalledVersion(activity);

                activity.runOnUiThread(() -> {
                    if (!latestVersion.equals(currentVersion)) {
                        new AlertDialog.Builder(activity)
                            .setTitle("🎉 يوجد تحديث جديد!")
                            .setMessage("الإصدار " + latestVersion + " متوفر الآن\n\n" + notes)
                            .setPositiveButton("تحديث الآن", (d, w) -> startDownload(activity, downloadUrl, latestVersion))
                            .setNegativeButton("لاحقاً", null)
                            .show();
                    } else if (showNoUpdate) {
                        Toast.makeText(activity, "✅ التطبيق محدّث", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (showNoUpdate) {
                    activity.runOnUiThread(() ->
                        Toast.makeText(activity, "تعذر التحقق من التحديثات", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private static void startDownload(Activity activity, String downloadUrl, String version) {
        activity.runOnUiThread(() -> {
            try {
                DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
                Uri uri = Uri.parse(downloadUrl);
                        DownloadManager.Request request = new DownloadManager.Request(uri);
                        request.setTitle("تحديث أذكاري " + version);
                        request.setDescription("جارٍ تنزيل التحديث...");
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setAllowedOverRoaming(true);
                        request.setAllowedOverMetered(true);
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Athkari_" + version + ".apk");
                        long downloadId = dm.enqueue(request);
                        Toast.makeText(activity, "⬇️ بدأ التنزيل في الخلفية", Toast.LENGTH_SHORT).show();
                        BroadcastReceiver receiver = new BroadcastReceiver() {
                            @Override public void onReceive(Context ctx, Intent intent) {
                                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                                if (id == downloadId) {
                                    activity.unregisterReceiver(this);
                                    saveInstalledVersion(activity, version);
                                    File apk = new File(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS), "Athkari_" + version + ".apk");
                                    Intent install = new Intent(Intent.ACTION_VIEW);
                                    Uri apkUri;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apk);
                                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    } else {
                                        apkUri = Uri.fromFile(apk);
                                    }
                                    install.setDataAndType(apkUri, "application/vnd.android.package-archive");
                                    install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    activity.startActivity(install);
                                }
                            }
                        };
                        activity.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
            } catch (Exception e) {
                Toast.makeText(activity, "خطأ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
