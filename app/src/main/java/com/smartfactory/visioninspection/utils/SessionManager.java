package com.smartfactory.visioninspection.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.smartfactory.visioninspection.models.User;

import java.util.Locale;
import java.util.UUID;

public class SessionManager {

    private static final String PREF_NAME = "SmartFactorySession";

    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMP_ID = "empId";
    private static final String KEY_NAME = "name";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_ROLE = "role";
    private static final String KEY_MQTT_CLIENT_ID = "mqttClientId";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_EMP_ID, user.getEmployeeId());
        editor.putString(KEY_NAME, user.getName());
        editor.putString(KEY_PASSWORD, user.getPassword());
        editor.putString(KEY_DEPARTMENT, user.getDepartment());
        editor.putString(KEY_ROLE, user.getRole());

        // 로그인 세션이 새로 열리면 MQTT clientId도 새로 만들도록 제거
        editor.remove(KEY_MQTT_CLIENT_ID);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) return null;

        String empId = valueOrDefault(pref.getString(KEY_EMP_ID, null), "EMP001");
        String name = valueOrDefault(pref.getString(KEY_NAME, null), "김스마트");
        String password = valueOrDefault(pref.getString(KEY_PASSWORD, null), "1234");
        String department = valueOrDefault(pref.getString(KEY_DEPARTMENT, null), "품질관리팀");
        String role = valueOrDefault(pref.getString(KEY_ROLE, null), "관리자");

        return new User(empId, name, password, department, role);
    }

    public String getOrCreateSessionMqttClientId() {
        String cached = pref.getString(KEY_MQTT_CLIENT_ID, null);
        if (cached != null && !cached.trim().isEmpty()) {
            return cached;
        }

        String emp = valueOrDefault(pref.getString(KEY_EMP_ID, null), "mobile");
        String normalizedEmp = emp.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalizedEmp.isEmpty()) {
            normalizedEmp = "mobile";
        }

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String generated = "mobile_" + normalizedEmp + "_" + suffix;

        editor.putString(KEY_MQTT_CLIENT_ID, generated).apply();
        return generated;
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    private String valueOrDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value;
    }
}
