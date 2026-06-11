package com.salah.app.worker;

import android.content.Context;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.salah.app.network.PrayerTimeFetcher;
import com.salah.app.utils.PrayerTimeCalculator;
import java.util.Calendar;
import java.util.Map;

public class PrayerTimeWorker extends Worker {
    
    private String[] cities = {"مكة", "المدينة", "الرياض", "جدة", "أبها", "تبوك", "القدس", "صنعاء", "عدن", "المكلا"};
    
    public PrayerTimeWorker(Context context, WorkerParameters params) {
        super(context, params);
    }
    
    @Override
    public Result doWork() {
        PrayerTimeFetcher fetcher = new PrayerTimeFetcher(getApplicationContext());
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        
        for (String city : cities) {
            try {
                Map<String, String[]> times;
                
                if (city.equals("صنعاء") || city.equals("عدن") || city.equals("المكلا")) {
                    // جلب من موقع اليمن
                    times = fetcher.fetchYemeniCity(city);
                } else {
                    // جلب من موقع أم القرى
                    times = fetcher.fetchSaudiCity(city, year, month);
                }
                
                if (times != null && !times.isEmpty()) {
                    fetcher.savePrayerTimes(city, times);
                } else {
                    // استخدام الحسابات إذا فشل الجلب
                    calculateAndSave(fetcher, city);
                }
                
            } catch (Exception e) {
                // في حالة الخطأ، استخدم الحسابات
                calculateAndSave(fetcher, city);
            }
        }
        
        return Result.success();
    }
    
    private void calculateAndSave(PrayerTimeFetcher fetcher, String city) {
        double[] coords = PrayerTimeCalculator.getCityCoordinates(city);
        Calendar cal = Calendar.getInstance();
        
        for (int day = 1; day <= 7; day++) {
            double[] decimalTimes = PrayerTimeCalculator.calculatePrayerTimes(
                coords[0], coords[1], 
                cal.get(Calendar.YEAR), 
                cal.get(Calendar.MONTH) + 1, 
                day
            );
            
            java.util.Map<String, String[]> timesMap = new java.util.HashMap<>();
            String[] times = new String[5];
            for (int i = 0; i < 5; i++) {
                times[i] = PrayerTimeCalculator.decimalToTime(decimalTimes[i]);
            }
            
            timesMap.put(city + "_" + day, times);
            fetcher.savePrayerTimes(city + "_" + day, timesMap);
        }
    }
}
