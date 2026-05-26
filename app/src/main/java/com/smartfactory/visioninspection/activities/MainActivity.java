package com.smartfactory.visioninspection.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.fragments.DashboardFragment;
import com.smartfactory.visioninspection.fragments.EquipmentFragment;
import com.smartfactory.visioninspection.fragments.FeedFragment;
import com.smartfactory.visioninspection.fragments.ReportFragment;
import com.smartfactory.visioninspection.fragments.SettingsFragment;
import com.smartfactory.visioninspection.utils.SessionManager;

import com.smartfactory.visioninspection.MqttConnectionManager;
import com.smartfactory.visioninspection.MqttEventListener;
import com.smartfactory.visioninspection.StatusUpdateEvent;

/**
 * 메인 화면 (App.tsx + BottomNav.tsx 대응)
 * BottomNavigationView 5탭 + MQTT 중앙 통제실 역할
 */
// 2. MqttEventListener 인터페이스 구현 추가
public class MainActivity extends AppCompatActivity implements MqttEventListener {

    private static final String PREFS_UI = "ui_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    private BottomNavigationView bottomNav;
    private SessionManager sessionManager;
    private MqttConnectionManager mqttManager;
    private String mqttClientId;
    private DashboardFragment dashboardFragment;
    private EquipmentFragment equipmentFragment;
    private FeedFragment feedFragment;
    private ReportFragment reportFragment;
    private SettingsFragment settingsFragment;

    private static final int TAB_DASHBOARD  = 0;
    private static final int TAB_EQUIPMENT  = 1;
    private static final int TAB_FEED       = 2;
    private static final int TAB_REPORT     = 3;
    private static final int TAB_SETTINGS   = 4;

    private int currentTab = TAB_DASHBOARD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedThemeMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            logout();
            return;
        }

        bindViews();
        initFragments(savedInstanceState);
        setupBottomNavigation();

        mqttClientId = sessionManager.getOrCreateSessionMqttClientId();
        mqttManager = MqttConnectionManager.getInstance();
        mqttManager.setMqttEventListener(this);
        mqttManager.initAndConnect(mqttClientId);
    }

    private void applySavedThemeMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_UI, MODE_PRIVATE);
        int mode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_YES);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public void toggleThemeMode() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode != AppCompatDelegate.MODE_NIGHT_YES
                && currentMode != AppCompatDelegate.MODE_NIGHT_NO) {
            currentMode = isNightMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        }

        int nextMode = currentMode == AppCompatDelegate.MODE_NIGHT_YES
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES;

        getSharedPreferences(PREFS_UI, MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME_MODE, nextMode)
                .apply();

        AppCompatDelegate.setDefaultNightMode(nextMode);
    }

    public boolean isNightMode() {
        int nightMask = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMask == Configuration.UI_MODE_NIGHT_YES;
    }

    private void bindViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void initFragments(Bundle savedInstanceState) {
        dashboardFragment = new DashboardFragment();
        equipmentFragment = new EquipmentFragment();
        feedFragment = new FeedFragment();
        reportFragment = new ReportFragment();
        settingsFragment = new SettingsFragment();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, "settings").hide(settingsFragment)
                    .add(R.id.fragment_container, reportFragment, "report").hide(reportFragment)
                    .add(R.id.fragment_container, feedFragment, "feed").hide(feedFragment)
                    .add(R.id.fragment_container, equipmentFragment, "equipment").hide(equipmentFragment)
                    .add(R.id.fragment_container, dashboardFragment, "dashboard")
                    .commitNowAllowingStateLoss();
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
            currentTab = TAB_DASHBOARD;
        }
    }

    private void setupBottomNavigation() {
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                if (currentTab != TAB_DASHBOARD) {
                    switchTo(dashboardFragment);
                    currentTab = TAB_DASHBOARD;
                }
                return true;
            } else if (id == R.id.nav_equipment) {
                if (currentTab != TAB_EQUIPMENT) {
                    switchTo(equipmentFragment);
                    currentTab = TAB_EQUIPMENT;
                }
                return true;
            } else if (id == R.id.nav_feed) {
                if (currentTab != TAB_FEED) {
                    switchTo(feedFragment);
                    currentTab = TAB_FEED;
                }
                return true;
            } else if (id == R.id.nav_report) {
                if (currentTab != TAB_REPORT) {
                    switchTo(reportFragment);
                    currentTab = TAB_REPORT;
                }
                return true;
            } else if (id == R.id.nav_settings) {
                if (currentTab != TAB_SETTINGS) {
                    switchTo(settingsFragment);
                    currentTab = TAB_SETTINGS;
                }
                return true;
            }
            return false;
        });
    }

    private void switchTo(@NonNull Fragment target) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f == null || f.getId() != R.id.fragment_container) continue;
            if (f == target) tx.show(f);
            else tx.hide(f);
        }
        tx.commitAllowingStateLoss();
    }

    public void logout() {
        if (mqttManager != null) {
            mqttManager.disconnectAndClose();
        }
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public void onBackPressed() {
        if (currentTab != TAB_DASHBOARD) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (mqttManager != null) {
            mqttManager.clearMqttEventListener(this);

            // 액티비티가 완전히 종료될 때만 MQTT 소켓을 정리한다.
            // (테마/설정 변경으로 재생성되는 경우는 기존 연결 재사용)
            if (isFinishing()) {
                mqttManager.disconnectAndClose();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onStatusUpdateReceived(StatusUpdateEvent event) {
        runOnUiThread(() -> {
            Log.d("MainActivity", "STATUS_UPDATE: " + event.getEquipmentId());
            if (dashboardFragment != null) dashboardFragment.updateRealTimeData(event);
            if (equipmentFragment != null) equipmentFragment.updateRealTimeData(event);
        });
    }

    @Override
    public void onInspectionResultReceived(String topic, String message) {
        runOnUiThread(() -> Log.d("MQTT_TEST", "INSPECTION_RESULT topic=" + topic + " payload=" + message));
    }

    @Override
    public void onMqttEventReceived(String equipmentId, String eventType, String payload) {
        runOnUiThread(() -> {
            if (dashboardFragment != null) {
                dashboardFragment.onMqttEvent(equipmentId, eventType, payload);
            }
        });
    }

    @Override
    public void onMqttConnectionChanged(boolean connected) {
        runOnUiThread(() -> {
            if (dashboardFragment != null) {
                dashboardFragment.setMqttConnected(connected);
            }
        });
    }
}
