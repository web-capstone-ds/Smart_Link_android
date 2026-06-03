package com.smartfactory.visioninspection.activities;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.smartfactory.visioninspection.MqttConnectionManager;
import com.smartfactory.visioninspection.MqttEventListener;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.StatusUpdateEvent;
import com.smartfactory.visioninspection.fragments.DashboardFragment;
import com.smartfactory.visioninspection.fragments.EquipmentFragment;
import com.smartfactory.visioninspection.fragments.FeedFragment;
import com.smartfactory.visioninspection.fragments.ReportFragment;
import com.smartfactory.visioninspection.fragments.SettingsFragment;
import com.smartfactory.visioninspection.utils.AlarmSettingsManager;
import com.smartfactory.visioninspection.utils.SessionManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements MqttEventListener {

    private static final int TAB_DASHBOARD = 0;
    private static final int TAB_EQUIPMENT = 1;
    private static final int TAB_FEED = 2;
    private static final int TAB_REPORT = 3;
    private static final int TAB_SETTINGS = 4;

    private BottomNavigationView bottomNav;
    private SessionManager sessionManager;
    private MqttConnectionManager mqttManager;
    private AlarmSettingsManager alarmSettingsManager;
    private ToneGenerator toneGenerator;
    private int toneVolume = -1;

    private MaterialCardView cardGlobalAlert;
    private TextView tvGlobalAlertTitle;
    private TextView tvGlobalAlertBody;
    private ImageButton btnCloseGlobalAlert;
    private String activeAlertEquipmentId;
    private FeedFragment.QuickFilter activeAlertQuickFilter;
    private static final long GLOBAL_ALERT_AUTO_HIDE_MS = 5000L;
    private final Handler globalAlertHandler = new Handler(Looper.getMainLooper());
    private final Runnable globalAlertAutoHideRunnable = this::hideGlobalAlert;

    private final Set<String> handledAlertMessageIds = new HashSet<>();
    private final Map<String, String> latestStatusByEquipment = new HashMap<>();

    private DashboardFragment dashboardFragment;
    private EquipmentFragment equipmentFragment;
    private FeedFragment feedFragment;
    private ReportFragment reportFragment;
    private SettingsFragment settingsFragment;

    private int currentTab = TAB_DASHBOARD;

    private enum AlertSeverity {
        FAIL, MARGINAL
    }

    private static class GlobalAlertPayload {
        String messageId;
        String equipmentId;
        AlertSeverity severity;
        String title;
        String body;
        boolean hwAlert;
        FeedFragment.QuickFilter quickFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isTokenValid()) {
            sessionManager.clearSession();
            logout();
            return;
        }

        bindViews();
        initFragments(savedInstanceState);
        setupBottomNavigation();
        setupGlobalAlertUi();

        mqttManager = MqttConnectionManager.getInstance();
        mqttManager.setMqttEventListener(this);
        mqttManager.initAndConnect(sessionManager.getOrCreateSessionMqttClientId());
        alarmSettingsManager = new AlarmSettingsManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager != null && (!sessionManager.isLoggedIn() || !sessionManager.isTokenValid())) {
            sessionManager.clearSession();
            logout();
        }
    }

    private void bindViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
        cardGlobalAlert = findViewById(R.id.card_global_alert);
        tvGlobalAlertTitle = findViewById(R.id.tv_global_alert_title);
        tvGlobalAlertBody = findViewById(R.id.tv_global_alert_body);
        btnCloseGlobalAlert = findViewById(R.id.btn_close_global_alert);
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

    private void setupGlobalAlertUi() {
        if (cardGlobalAlert == null) return;

        cardGlobalAlert.setOnClickListener(v -> {
            if (activeAlertQuickFilter != null && activeAlertEquipmentId != null) {
                openFeedWithLineQuickFilter(activeAlertEquipmentId, activeAlertQuickFilter);
            } else if (activeAlertQuickFilter != null) {
                openFeedWithQuickFilter(activeAlertQuickFilter);
            } else if (activeAlertEquipmentId != null) {
                openFeedWithLineFilter(activeAlertEquipmentId);
            } else {
                bottomNav.setSelectedItemId(R.id.nav_feed);
            }
            hideGlobalAlert();
        });

        if (btnCloseGlobalAlert != null) {
            btnCloseGlobalAlert.setOnClickListener(v -> hideGlobalAlert());
        }
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

    public void openFeedWithLineFilter(String equipmentId) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) return;

        bottomNav.setSelectedItemId(R.id.nav_feed);
        bottomNav.post(() -> {
            if (feedFragment != null) {
                feedFragment.applyEquipmentFilter(equipmentId);
            }
        });
    }

    public void playAlarmPreview() {
        if (alarmSettingsManager == null) {
            alarmSettingsManager = new AlarmSettingsManager(this);
        }
        if (alarmSettingsManager.isAlarmSoundEnabled()) {
            playBeep(alarmSettingsManager.getAlarmVolume());
        }
    }

    public void onAlarmSettingsChanged() {
        // no-op: settings are read at alert time from SharedPreferences.
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
        cancelGlobalAlertAutoHide();
        if (mqttManager != null) {
            mqttManager.clearMqttEventListener(this);
            if (isFinishing()) {
                mqttManager.disconnectAndClose();
            }
        }
        releaseToneGenerator();
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
            handleGlobalAlertFromMqtt(equipmentId, eventType, payload);
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

    private void handleGlobalAlertFromMqtt(String equipmentId, String eventType, String payload) {
        if (alarmSettingsManager == null) {
            alarmSettingsManager = new AlarmSettingsManager(this);
        }
        GlobalAlertPayload alert = parseGlobalAlert(equipmentId, eventType, payload);
        if (alert == null) return;

        if (alert.messageId != null && !alert.messageId.trim().isEmpty()) {
            if (handledAlertMessageIds.contains(alert.messageId)) return;
            handledAlertMessageIds.add(alert.messageId);
            if (handledAlertMessageIds.size() > 500) {
                handledAlertMessageIds.clear();
            }
        }

        boolean enabled = alert.hwAlert
                ? alarmSettingsManager.isHwAlertEnabled()
                : (alert.severity == AlertSeverity.FAIL
                    ? alarmSettingsManager.isFailAlertEnabled()
                    : alarmSettingsManager.isMarginalAlertEnabled());
        if (!enabled) return;

        showGlobalAlert(alert);

        if (alarmSettingsManager.isAlarmSoundEnabled()) {
            playBeep(alarmSettingsManager.getAlarmVolume());
        }
    }

    private GlobalAlertPayload parseGlobalAlert(String equipmentId, String eventType, String payload) {
        if (eventType == null || payload == null || payload.trim().isEmpty()) return null;
        String type = eventType.trim().toLowerCase(Locale.ROOT);

        try {
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            String messageId = optString(obj, "message_id");
            String eqId = safe(equipmentId, optString(obj, "equipment_id"));
            String lineLabel = toLineLabel(eqId);

            if ("alarm".equals(type)) {
                String alarmLevel = optString(obj, "alarm_level").toUpperCase(Locale.ROOT);
                String code = safe(optString(obj, "hw_error_code"), "HW_ALARM");

                GlobalAlertPayload out = new GlobalAlertPayload();
                out.messageId = messageId;
                out.equipmentId = eqId;
                out.severity = "CRITICAL".equals(alarmLevel) ? AlertSeverity.FAIL : AlertSeverity.MARGINAL;
                out.hwAlert = true;
                out.quickFilter = FeedFragment.QuickFilter.HW;
                out.title = (out.severity == AlertSeverity.FAIL ? "CRITICAL 알람 발생" : "WARNING 알람 발생");
                out.body = lineLabel + " · " + code;
                return out;
            }

            if ("status".equals(type)) {
                String status = optString(obj, "equipment_status").toUpperCase(Locale.ROOT);
                String timestamp = optString(obj, "timestamp");
                if (!rememberStatusAndShouldAlert(eqId, status)) return null;

                GlobalAlertPayload out = new GlobalAlertPayload();
                out.messageId = safe(messageId, "status-" + eqId + "-" + safe(timestamp, status));
                out.equipmentId = eqId;
                out.severity = AlertSeverity.FAIL;
                out.title = "HW 알람 발생";
                out.body = lineLabel + " · 장비 정지";
                out.hwAlert = true;
                out.quickFilter = FeedFragment.QuickFilter.HW;
                return out;
            }

            if ("oracle".equals(type)) {
                String judgment = optString(obj, "judgment").toUpperCase(Locale.ROOT);
                if ("NORMAL".equals(judgment)) return null;

                GlobalAlertPayload out = new GlobalAlertPayload();
                out.messageId = messageId;
                out.equipmentId = eqId;
                out.severity = "DANGER".equals(judgment) ? AlertSeverity.FAIL : AlertSeverity.MARGINAL;
                out.title = (out.severity == AlertSeverity.FAIL ? "오라클 위험 알람" : "오라클 경계 알람");
                out.body = lineLabel + " · " + safe(optString(obj, "ai_comment"), "AI 분석 경고");
                return out;
            }

            if ("lot".equals(type)) {
                int total = optInt(obj, "total_units");
                int fail = optInt(obj, "fail_count");
                float yield = optFloat(obj, "yield_pct");
                if (total <= 0 && fail <= 0) return null;

                AlertSeverity severity;
                if (fail <= 0) {
                    return null;
                } else if (yield >= 95f) {
                    severity = AlertSeverity.MARGINAL;
                } else {
                    severity = AlertSeverity.FAIL;
                }

                GlobalAlertPayload out = new GlobalAlertPayload();
                out.messageId = messageId;
                out.equipmentId = eqId;
                out.severity = severity;
                out.title = (severity == AlertSeverity.FAIL ? "LOT 불합격 발생" : "LOT 경계 발생");
                out.body = lineLabel + " · " + safe(optString(obj, "lot_id"), "LOT-UNKNOWN");
                return out;
            }
        } catch (Exception e) {
            Log.w("MainActivity", "Global alert parse failed", e);
        }

        return null;
    }

    private void showGlobalAlert(GlobalAlertPayload payload) {
        if (cardGlobalAlert == null) return;
        activeAlertEquipmentId = payload.equipmentId;
        activeAlertQuickFilter = payload.quickFilter;
        tvGlobalAlertTitle.setText(payload.title);
        tvGlobalAlertBody.setText(payload.body);

        if (payload.severity == AlertSeverity.FAIL) {
            cardGlobalAlert.setCardBackgroundColor(getColorCompat(R.color.global_alert_fail_bg));
            cardGlobalAlert.setStrokeColor(getColorCompat(R.color.global_alert_fail_stroke));
            tvGlobalAlertTitle.setTextColor(getColorCompat(R.color.global_alert_fail_title));
        } else {
            cardGlobalAlert.setCardBackgroundColor(getColorCompat(R.color.global_alert_marginal_bg));
            cardGlobalAlert.setStrokeColor(getColorCompat(R.color.global_alert_marginal_stroke));
            tvGlobalAlertTitle.setTextColor(getColorCompat(R.color.global_alert_marginal_title));
        }
        tvGlobalAlertBody.setTextColor(getColorCompat(R.color.global_alert_body_text));
        if (btnCloseGlobalAlert != null) {
            btnCloseGlobalAlert.setColorFilter(getColorCompat(R.color.global_alert_body_text));
        }

        cardGlobalAlert.setVisibility(View.VISIBLE);
        scheduleGlobalAlertAutoHide();
    }

    private void hideGlobalAlert() {
        cancelGlobalAlertAutoHide();
        activeAlertEquipmentId = null;
        activeAlertQuickFilter = null;
        if (cardGlobalAlert != null) {
            cardGlobalAlert.setVisibility(View.GONE);
        }
    }

    private void scheduleGlobalAlertAutoHide() {
        cancelGlobalAlertAutoHide();
        globalAlertHandler.postDelayed(globalAlertAutoHideRunnable, GLOBAL_ALERT_AUTO_HIDE_MS);
    }

    private void cancelGlobalAlertAutoHide() {
        globalAlertHandler.removeCallbacks(globalAlertAutoHideRunnable);
    }

    private void playBeep(int volume) {
        int safeVolume = Math.max(0, Math.min(100, volume));
        if (toneGenerator == null || toneVolume != safeVolume) {
            releaseToneGenerator();
            toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, safeVolume);
            toneVolume = safeVolume;
        }
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180);
    }

    private void releaseToneGenerator() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
            toneVolume = -1;
        }
    }

    private int getColorCompat(int colorRes) {
        return androidx.core.content.ContextCompat.getColor(this, colorRes);
    }

    private String toLineLabel(String equipmentId) {
        int lineNo = parseLineNo(equipmentId);
        if (lineNo <= 0) return safe(equipmentId, "UNKNOWN");
        return lineNo + "라인";
    }

    private int parseLineNo(String equipmentId) {
        if (equipmentId == null) return -1;
        String digits = equipmentId.replaceAll("[^0-9]", "");
        if (digits.length() >= 3) {
            digits = digits.substring(digits.length() - 3);
        }
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return -1;
        }
    }

    private String optString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return safe(obj.get(key).getAsString(), "");
    }

    private int optInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    private float optFloat(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0f;
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception e) {
            return 0f;
        }
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    public void openFeedWithQuickFilter(FeedFragment.QuickFilter filter) {
        bottomNav.setSelectedItemId(R.id.nav_feed);
        bottomNav.post(() -> {
            if (feedFragment != null) {
                feedFragment.applyDashboardQuickFilter(filter);
            }
        });
    }

    public void openFeedWithLineQuickFilter(String equipmentId, FeedFragment.QuickFilter filter) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) return;

        bottomNav.setSelectedItemId(R.id.nav_feed);
        bottomNav.post(() -> {
            if (feedFragment != null) {
                feedFragment.applyEquipmentQuickFilter(equipmentId, filter);
            }
        });
    }

    private boolean rememberStatusAndShouldAlert(String equipmentId, String status) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) return false;

        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String previous = latestStatusByEquipment.put(equipmentId, normalized);
        if (!"STOP".equals(normalized)) return false;
        return previous == null || !"STOP".equals(previous);
    }

}
