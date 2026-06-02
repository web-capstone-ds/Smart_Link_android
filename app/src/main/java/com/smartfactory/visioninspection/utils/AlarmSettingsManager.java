package com.smartfactory.visioninspection.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AlarmSettingsManager {

    private static final String PREF_NAME = "AlarmSettings";
    private static final String KEY_ALARM_SOUND_ENABLED = "alarm_sound_enabled";
    private static final String KEY_ALARM_VOLUME = "alarm_volume";
    private static final String KEY_FAIL_ALERT_ENABLED = "fail_alert_enabled";
    private static final String KEY_MARGINAL_ALERT_ENABLED = "marginal_alert_enabled";
    private static final String KEY_HW_ALERT_ENABLED = "hw_alert_enabled";

    private static final boolean DEFAULT_ALARM_SOUND_ENABLED = true;
    private static final int DEFAULT_ALARM_VOLUME = 60;
    private static final boolean DEFAULT_FAIL_ALERT_ENABLED = true;
    private static final boolean DEFAULT_MARGINAL_ALERT_ENABLED = false;
    private static final boolean DEFAULT_HW_ALERT_ENABLED = true;

    private final SharedPreferences pref;

    public AlarmSettingsManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAlarmSoundEnabled() {
        return pref.getBoolean(KEY_ALARM_SOUND_ENABLED, DEFAULT_ALARM_SOUND_ENABLED);
    }

    public void setAlarmSoundEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_ALARM_SOUND_ENABLED, enabled).apply();
    }

    public int getAlarmVolume() {
        int value = pref.getInt(KEY_ALARM_VOLUME, DEFAULT_ALARM_VOLUME);
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }

    public void setAlarmVolume(int volume) {
        int safe = Math.max(0, Math.min(100, volume));
        pref.edit().putInt(KEY_ALARM_VOLUME, safe).apply();
    }

    public boolean isFailAlertEnabled() {
        return pref.getBoolean(KEY_FAIL_ALERT_ENABLED, DEFAULT_FAIL_ALERT_ENABLED);
    }

    public void setFailAlertEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_FAIL_ALERT_ENABLED, enabled).apply();
    }

    public boolean isMarginalAlertEnabled() {
        return pref.getBoolean(KEY_MARGINAL_ALERT_ENABLED, DEFAULT_MARGINAL_ALERT_ENABLED);
    }

    public void setMarginalAlertEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_MARGINAL_ALERT_ENABLED, enabled).apply();
    }

    public boolean isHwAlertEnabled() {
        return pref.getBoolean(KEY_HW_ALERT_ENABLED, DEFAULT_HW_ALERT_ENABLED);
    }

    public void setHwAlertEnabled(boolean enabled) {
        pref.edit().putBoolean(KEY_HW_ALERT_ENABLED, enabled).apply();
    }
}
