package com.salah.app.utils;

import java.util.Calendar;
import java.util.TimeZone;

public class PrayerTimeCalculator {
    
    // حساب أوقات الصلاة باستخدام طريقة أم القرى (Umm Al-Qura)
    public static double[] calculatePrayerTimes(double latitude, double longitude, int year, int month, int day) {
        double[] times = new double[5]; // Fajr, Dhuhr, Asr, Maghrib, Isha
        
        // اليوم من السنة
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        
        // زاوية انحراف الشمس
        double declination = 23.45 * Math.sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)));
        
        // معادلة الزمن
        double eqTime = 9.87 * Math.sin(Math.toRadians(2 * (360.0 / 365.0) * (dayOfYear - 81))) 
                     - 7.53 * Math.cos(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)))
                     - 1.5 * Math.sin(Math.toRadians((360.0 / 365.0) * (dayOfYear - 81)));
        
        // الظهر (الزوال)
        double dhuhr = 12.0 + (4.0 * (longitude - getLocalLongitude())) / 60.0 - eqTime / 60.0;
        
        // حساب ارتفاع الشمس
        double latRad = Math.toRadians(latitude);
        double decRad = Math.toRadians(declination);
        
        // الفجر (زاوية -18.5 درجة)
        double fajrAngle = -18.5;
        double fajrHourAngle = Math.toDegrees(Math.acos(
            (Math.cos(Math.toRadians(90 + Math.abs(fajrAngle))) - Math.sin(latRad) * Math.sin(decRad))
            / (Math.cos(latRad) * Math.cos(decRad))
        ));
        double fajr = dhuhr - (fajrHourAngle / 15.0);
        
        // العصر (ظل 1 متر)
        double asrAngle = Math.toDegrees(Math.atan(1 / (Math.tan(Math.abs(latRad - decRad)) + 1)));
        double asrHourAngle = Math.toDegrees(Math.acos(
            (Math.sin(Math.toRadians(-asrAngle)) - Math.sin(latRad) * Math.sin(decRad))
            / (Math.cos(latRad) * Math.cos(decRad))
        ));
        double asr = dhuhr + (asrHourAngle / 15.0);
        
        // المغرب (غروب الشمس)
        double maghribHourAngle = Math.toDegrees(Math.acos(
            (Math.cos(Math.toRadians(90.833)) - Math.sin(latRad) * Math.sin(decRad))
            / (Math.cos(latRad) * Math.cos(decRad))
        ));
        double maghrib = dhuhr + (maghribHourAngle / 15.0);
        
        // العشاء (زاوية -19 درجة)
        double ishaAngle = -19.0;
        double ishaHourAngle = Math.toDegrees(Math.acos(
            (Math.cos(Math.toRadians(90 + Math.abs(ishaAngle))) - Math.sin(latRad) * Math.sin(decRad))
            / (Math.cos(latRad) * Math.cos(decRad))
        ));
        double isha = dhuhr + (ishaHourAngle / 15.0);
        
        times[0] = fajr;
        times[1] = dhuhr;
        times[2] = asr;
        times[3] = maghrib;
        times[4] = isha;
        
        return times;
    }
    
    private static double getLocalLongitude() {
        // خط الطول المرجعي (توقيت السعودية UTC+3)
        return 45.0;
    }
    
    // تحويل الوقت العشري إلى تنسيق HH:MM
    public static String decimalToTime(double decimalTime) {
        int hours = (int) decimalTime;
        int minutes = (int) ((decimalTime - hours) * 60);
        
        // تعديل التوقيت للتوقيت المحلي (السعودية +3)
        hours += 3;
        if (hours >= 24) hours -= 24;
        
        return String.format("%02d:%02d", hours, minutes);
    }
    
    // إحداثيات المدن
    public static double[] getCityCoordinates(String city) {
        switch (city) {
            case "مكة": return new double[]{21.4225, 39.8262};
            case "المدينة": return new double[]{24.5247, 39.5692};
            case "الرياض": return new double[]{24.7136, 46.6753};
            case "جدة": return new double[]{21.4858, 39.1925};
            case "أبها": return new double[]{18.2164, 42.5053};
            case "تبوك": return new double[]{28.3838, 36.5557};
            case "القدس": return new double[]{31.7683, 35.2137};
            case "صنعاء": return new double[]{15.3694, 44.1910};
            case "عدن": return new double[]{12.7855, 45.0187};
            case "المكلا": return new double[]{14.5428, 49.1259};
            default: return new double[]{21.4225, 39.8262}; // مكة افتراضياً
        }
    }
}
