package com.smartfactory.visioninspection.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.network.dto.LoginResponse;

import java.util.Locale;
import java.util.UUID;

public class SessionManager {

    private static final String PREF_NAME = "SmartFactorySession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMP_ID = "empId";
    private static final String KEY_NAME = "name";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_ROLE = "role";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_MQTT_CLIENT_ID = "mqttClientId";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_AUTH_EXPIRES_AT = "authExpiresAtMs";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveSession(LoginResponse response) {
        if (response == null) return;

        long expiresAtMs = System.currentTimeMillis()
                + (Math.max(1, response.getExpiresIn()) * 1000L);

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_EMP_ID, valueOrDefault(response.getOperatorId(), "EMP001"));
        editor.putString(KEY_NAME, valueOrDefault(response.getName(), "사용자"));
        editor.putString(KEY_DEPARTMENT, valueOrDefault(response.getDepartment(), "미지정"));
        editor.putString(KEY_ROLE, valueOrDefault(response.getRole(), "OPERATOR"));
        editor.putString(KEY_PHONE, valueOrDefault(response.getPhone(), ""));
        editor.putString(KEY_AUTH_TOKEN, valueOrDefault(response.getToken(), ""));
        editor.putLong(KEY_AUTH_EXPIRES_AT, expiresAtMs);

        // 로그인 세션이 바뀌면 MQTT clientId 재생성
        editor.remove(KEY_MQTT_CLIENT_ID);
        editor.apply();
    }

    // 디버그 우회 로그인용 로컬 세션 저장
    public void saveLocalSession(User user) {
        if (user == null) return;

        long expiresAtMs = System.currentTimeMillis() + (24L * 60L * 60L * 1000L);

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_EMP_ID, valueOrDefault(user.getEmployeeId(), "EMP001"));
        editor.putString(KEY_NAME, valueOrDefault(user.getName(), "사용자"));
        editor.putString(KEY_DEPARTMENT, valueOrDefault(user.getDepartment(), "미지정"));
        editor.putString(KEY_ROLE, valueOrDefault(user.getRoleCode(), "OPERATOR"));
        editor.putString(KEY_PHONE, valueOrDefault(user.getPhoneNumber(), ""));
        editor.putString(KEY_AUTH_TOKEN, "LOCAL_BYPASS_TOKEN");
        editor.putLong(KEY_AUTH_EXPIRES_AT, expiresAtMs);

        editor.remove(KEY_MQTT_CLIENT_ID);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean isTokenValid() {
        String token = getToken();
        long expiresAt = pref.getLong(KEY_AUTH_EXPIRES_AT, 0L);
        return token != null && !token.trim().isEmpty()
                && expiresAt > System.currentTimeMillis();
    }

    public String getToken() {
        return pref.getString(KEY_AUTH_TOKEN, "");
    }

    public String getAuthorizationHeader() {
        String token = getToken();
        if (token == null || token.trim().isEmpty()) return "";
        return "Bearer " + token;
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) return null;

        String empId = valueOrDefault(pref.getString(KEY_EMP_ID, null), "EMP001");
        String name = valueOrDefault(pref.getString(KEY_NAME, null), "사용자");
        String department = valueOrDefault(pref.getString(KEY_DEPARTMENT, null), "미지정");
        String role = valueOrDefault(pref.getString(KEY_ROLE, null), "OPERATOR");
        String phone = valueOrDefault(pref.getString(KEY_PHONE, null), "");

        return new User(empId, name, department, role, phone);
    }

    public String getOrCreateSessionMqttClientId() {
        String cached = pref.getString(KEY_MQTT_CLIENT_ID, null);
        if (cached != null && !cached.trim().isEmpty()) return cached;

        String emp = valueOrDefault(pref.getString(KEY_EMP_ID, null), "mobile");
        String normalizedEmp = emp.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalizedEmp.isEmpty()) normalizedEmp = "mobile";

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