package com.salah.app.models;
import java.io.Serializable;
public class UserSettings implements Serializable {
    private static final long serialVersionUID = 1L;
    // الحساب والمذهب
    public int calculationMethod = 4;
    public String calculationMethodId = "MuslimWorldLeague";
    public int madhab = 0;
    public String madhabId = "Shafi";
    // الأذان - الصلوات الأربع
    public boolean adhanEnabled = true;
    public int adhanVoice = 0;
    public String selectedAdhanFile = "adhan_madinah";
    // ✅ أذان الفجر منفصل
    public String selectedFajrAdhanFile = "adhan_madinah";
    // أذكار الصباح
    public boolean morningAthkarEnabled = true;
    public int morningAthkarHour = 6;
    public int morningAthkarMinute = 0;
    // أذكار المساء
    public boolean eveningAthkarEnabled = true;
    public int eveningAthkarHour = 18;
    public int eveningAthkarMinute = 0;
    // أذكار النوم
    public boolean sleepAthkarEnabled = true;
    // الإعدادات العامة
    public boolean darkTheme = false;
    public boolean darkMode = false;
    public boolean downloadAudio = false;
    public boolean vibrateOnAlarm = true;
}
