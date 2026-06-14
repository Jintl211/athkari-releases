package com.salah.app.utils;
import android.content.Context;
import android.os.Environment;
import java.io.*;
import java.util.Date;

public class CrashLogger {
    public static void setup(Context ctx) {
        File f = new File(ctx.getExternalFilesDir(null), "crash.txt");
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
                pw.println("=== " + new Date() + " ===");
                ex.printStackTrace(pw);
                pw.flush();
            } catch (Exception ignored) {}
            android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
}
