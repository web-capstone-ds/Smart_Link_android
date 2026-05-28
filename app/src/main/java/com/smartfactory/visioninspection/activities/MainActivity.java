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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartfactory.visioninspection.MqttConnectionManager;
import com.smartfactory.visioninspection.MqttEventListener;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.StatusUpdateEvent;
import com.smartfactory.visioninspection.fragments.DashboardFragment;
import com.smartfactory.visioninspection.fragments.EquipmentFragment;
import com.smartfactory.visioninspection.fragments.FeedFragment;
import com.smartfactory.visioninspection.fragments.ReportFragment;
import com.smartfactory.visioninspection.fragments.SettingsFragment;
import com.smartfactory.visioninspection.utils.SessionManager;

public class MainActivity extends AppCompatActivity implements MqttEventListener {

    private static final String PREFS_UI = "ui_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    private static final int TAB_DASHBOARD = 0;
    private static final int TAB_EQUIPMENT = 1;
    private static final int TAB_FEED = 2;
    private static final int TAB_REPORT = 3;
    private static final int TAB_SETTINGS = 4;

    private BottomNavigationView bottomNav;
    private SessionManager sessionManager;
    private MqttConnectionManager mqttManager;

    private DashboardFragment dashboardFragment;
    private EquipmentFragment equipmentFragment;
    private FeedFragment feedFragment;
    private ReportFragment reportFragment;
    private SettingsFragment settingsFragment;

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

        mqttManager = MqttConnectionManager.getInstance();
        mqttManager.setMqttEventListener(this);
        mqttManager.initAndConnect(sessionManager.getOrCreateSessionMqttClientId());
    }

    private void bindViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void initFragments(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            dashboardFragment = new DashboardFragment();
            equipmentFragment = new EquipmentFragment();
            feedFragment = new FeedFragment();
            reportFragment = new ReportFragment();
            settingsFragment = new SettingsFragment();

            fm.beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, "settings").hide(settingsFragment)
                    .add(R.id.fragment_container, reportFragment, "report").hide(reportFragment)
                    .add(R.id.fragment_container, feedFragment, "feed").hide(feedFragment)
                    .add(R.id.fragment_container, equipmentFragment, "equipment").hide(equipmentFragment)
                    .add(R.id.fragment_container, dashboardFragment, "dashboard")
                    .commitNowAllowingStateLoss();

            currentTab = TAB_DASHBOARD;
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
            return;
        }

        dashboardFragment = (DashboardFragment) fm.findFragmentByTag("dashboard");
        equipmentFragment = (EquipmentFragment) fm.findFragmentByTag("equipment");
        feedFragment = (FeedFragment) fm.findFragmentByTag("feed");
        reportFragment = (ReportFragment) fm.findFragmentByTag("report");
        settingsFragment = (SettingsFragment) fm.findFragmentByTag("settings");

        FragmentTransaction tx = fm.beginTransaction();
        if (dashboardFragment == null) {
            dashboardFragment = new DashboardFragment();
            tx.add(R.id.fragment_container, dashboardFragment, "dashboard");
        }
        if (equipmentFragment == null) {
            equipmentFragment = new EquipmentFragment();
            tx.add(R.id.fragment_container, equipmentFragment, "equipment").hide(equipmentFragment);
        }
        if (feedFragment == null) {
            feedFragment = new FeedFragment();
            tx.add(R.id.fragment_container, feedFragment, "feed").hide(feedFragment);
        }
        if (reportFragment == null) {
            reportFragment = new ReportFragment();
            tx.add(R.id.fragment_container, reportFragment, "report").hide(reportFragment);
        }
        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
            tx.add(R.id.fragment_container, settingsFragment, "settings").hide(settingsFragment);
        }
        tx.commitNowAllowingStateLoss();

        currentTab = resolveCurrentTab();
        bottomNav.setSelectedItemId(menuIdFromTab(currentTab));
    }

    private int resolveCurrentTab() {
        if (equipmentFragment != null && equipmentFragment.isVisible()) return TAB_EQUIPMENT;
        if (feedFragment != null && feedFragment.isVisible()) return TAB_FEED;
        if (reportFragment != null && reportFragment.isVisible()) return TAB_REPORT;
        if (settingsFragment != null && settingsFragment.isVisible()) return TAB_SETTINGS;
        return TAB_DASHBOARD;
    }

    private int menuIdFromTab(int tab) {
        if (tab == TAB_EQUIPMENT) return R.id.nav_equipment;
        if (tab == TAB_FEED) return R.id.nav_feed;
        if (tab == TAB_REPORT) return R.id.nav_report;
        if (tab == TAB_SETTINGS) return R.id.nav_settings;
        return R.id.nav_dashboard;
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                if (currentTab != TAB_DASHBOARD) {
                    switchTo(dashboardFragment);
                    currentTab = TAB_DASHBOARD;
                }
                return true;
            }
            if (id == R.id.nav_equipment) {
                if (currentTab != TAB_EQUIPMENT) {
                    switchTo(equipmentFragment);
                    currentTab = TAB_EQUIPMENT;
                }
                return true;
            }
            if (id == R.id.nav_feed) {
                if (currentTab != TAB_FEED) {
                    switchTo(feedFragment);
                    currentTab = TAB_FEED;
                }
                return true;
            }
            if (id == R.id.nav_report) {
                if (currentTab != TAB_REPORT) {
                    switchTo(reportFragment);
                    currentTab = TAB_REPORT;
                }
                return true;
            }
            if (id == R.id.nav_settings) {
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

    public void openFeedWithLineFilter(String equipmentId) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) return;

        bottomNav.setSelectedItemId(R.id.nav_feed);
        bottomNav.post(() -> {
            if (feedFragment != null) {
                feedFragment.applyEquipmentFilter(equipmentId);
            }
        });
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
            if (equipmentFragment != null) {
                equipmentFragment.onMqttEvent(equipmentId, eventType, payload);
            }
            if (feedFragment != null) {
                feedFragment.onMqttEvent(equipmentId, eventType, payload);
            }
            if (reportFragment != null) {
                reportFragment.onMqttEvent();
            }
        });
    }

    @Override
    public void onMqttConnectionChanged(boolean connected) {
        runOnUiThread(() -> {
            if (dashboardFragment != null) dashboardFragment.setMqttConnected(connected);
            if (equipmentFragment != null) equipmentFragment.setMqttConnected(connected);
            if (feedFragment != null) feedFragment.setMqttConnected(connected);
            if (reportFragment != null) reportFragment.setMqttConnected(connected);
            if (settingsFragment != null) settingsFragment.setMqttConnected(connected);
        });
    }
}
