package com.smartfactory.visioninspection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.fragments.DashboardFragment;
import com.smartfactory.visioninspection.fragments.EquipmentFragment;
import com.smartfactory.visioninspection.fragments.FeedFragment;
import com.smartfactory.visioninspection.fragments.ReportFragment;
import com.smartfactory.visioninspection.fragments.SettingsFragment;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.SessionManager;

// 1. 우리가 만든 통신 패키지 import 추가
import com.smartfactory.visioninspection.MqttConnectionManager;
import com.smartfactory.visioninspection.MqttEventListener;
import com.smartfactory.visioninspection.StatusUpdateEvent;

/**
 * 메인 화면 (App.tsx + BottomNav.tsx 대응)
 * BottomNavigationView 5탭 + MQTT 중앙 통제실 역할
 */
// 2. MqttEventListener 인터페이스 구현 추가
public class MainActivity extends AppCompatActivity implements MqttEventListener {

    // ── 뷰 ───────────────────────────────────────────────────
    private BottomNavigationView bottomNav;
    private TextView             tvHeaderTitle;
    private TextView             tvUserBadge;

    // ── 유틸 및 통신 ─────────────────────────────────────────
    private SessionManager sessionManager;
    private MqttConnectionManager mqttManager; // 3. MQTT 매니저 변수 추가

    // ── 탭 인덱스 상수 ────────────────────────────────────────
    private static final int TAB_DASHBOARD  = 0;
    private static final int TAB_EQUIPMENT  = 1;
    private static final int TAB_FEED       = 2;
    private static final int TAB_REPORT     = 3;
    private static final int TAB_SETTINGS   = 4;

    private int currentTab = TAB_DASHBOARD;

    // ── onCreate ─────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        // 세션 만료 시 로그인으로 복귀
        if (!sessionManager.isLoggedIn()) {
            logout();
            return;
        }

        bindViews();
        setupHeader();
        setupBottomNavigation();

        // 앱 첫 진입 시 대시보드 표시
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment(), false);
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }

        // 4. 대망의 MQTT 통신 시동 걸기!
        mqttManager = new MqttConnectionManager();
        mqttManager.setMqttEventListener(this);
        mqttManager.initAndConnect();
    }

    // ── 뷰 바인딩 ────────────────────────────────────────────
    private void bindViews() {
        bottomNav     = findViewById(R.id.bottom_navigation);
        tvHeaderTitle = findViewById(R.id.tv_header_title);
        tvUserBadge   = findViewById(R.id.tv_user_badge);
    }

    // ── 헤더 초기화 (현재 로그인 사용자 표시) ─────────────────
    private void setupHeader() {
        User user = sessionManager.getCurrentUser();
        if (user != null) {
            tvUserBadge.setText(user.getDisplayName());
            tvUserBadge.setVisibility(View.VISIBLE);
        }
    }

    // ── BottomNavigationView 탭 연결 ─────────────────────────
    private void setupBottomNavigation() {
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                if (currentTab != TAB_DASHBOARD) {
                    loadFragment(new DashboardFragment(), false);
                    setHeaderTitle(getString(R.string.nav_dashboard));
                    currentTab = TAB_DASHBOARD;
                }
                return true;
            } else if (id == R.id.nav_equipment) {
                if (currentTab != TAB_EQUIPMENT) {
                    loadFragment(new EquipmentFragment(), false);
                    setHeaderTitle(getString(R.string.nav_equipment));
                    currentTab = TAB_EQUIPMENT;
                }
                return true;
            } else if (id == R.id.nav_feed) {
                if (currentTab != TAB_FEED) {
                    loadFragment(new FeedFragment(), false);
                    setHeaderTitle(getString(R.string.nav_feed));
                    currentTab = TAB_FEED;
                }
                return true;
            } else if (id == R.id.nav_report) {
                if (currentTab != TAB_REPORT) {
                    loadFragment(new ReportFragment(), false);
                    setHeaderTitle(getString(R.string.nav_report));
                    currentTab = TAB_REPORT;
                }
                return true;
            } else if (id == R.id.nav_settings) {
                if (currentTab != TAB_SETTINGS) {
                    loadFragment(new SettingsFragment(), false);
                    setHeaderTitle(getString(R.string.nav_settings));
                    currentTab = TAB_SETTINGS;
                }
                return true;
            }
            return false;
        });
    }

    // ── Fragment 교체 ─────────────────────────────────────────
    private void loadFragment(@NonNull Fragment fragment, boolean addToBackStack) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        tx.replace(R.id.fragment_container, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commitAllowingStateLoss();
    }

    // ── 헤더 타이틀 갱신 ─────────────────────────────────────
    private void setHeaderTitle(String title) {
        if (tvHeaderTitle != null) tvHeaderTitle.setText(title);
    }

    // ── 로그아웃 ──────────────────────────────────────────────
    public void logout() {
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

    // 5. 백그라운드 MQTT 쓰레드에서 데이터가 도착했을 때 UI로 전달!
    @Override
    public void onStatusUpdateReceived(StatusUpdateEvent event) {
        runOnUiThread(() -> {
            Log.d("MainActivity", "실시간 데이터 수신: " + event.getEquipmentId());

            // 현재 화면에 떠 있는 프래그먼트를 찾아냅니다.
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            // 만약 현재 화면이 대시보드라면, 대시보드의 데이터를 갱신하라고 신호를 보냅니다.
            if (currentFragment instanceof DashboardFragment) {
                ((DashboardFragment) currentFragment).updateRealTimeData(event);
            }
            // 만약 장비 상태 화면이라면 그쪽으로 신호를 보냅니다.
            else if (currentFragment instanceof EquipmentFragment) {
                ((EquipmentFragment) currentFragment).updateRealTimeData(event);
            }
        });
    }
}