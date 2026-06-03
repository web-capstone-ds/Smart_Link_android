package com.smartfactory.visioninspection.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.activities.MainActivity;
import com.smartfactory.visioninspection.adapters.FeedAdapter;
import com.smartfactory.visioninspection.models.FeedEvent;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.EventHistoryStore;
import com.smartfactory.visioninspection.utils.SessionManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ReportFragment extends Fragment {

    private TextView tvUserId;
    private TextView tvUserName;
    private TextView tvMqttChip;
    private TextView tvDeptRole;
    private TextView tvMesServer;

    private TextView tvReceivedAt;
    private TextView btnDaily;
    private TextView btnWeekly;
    private TextView btnRefresh;

    private TextView tvReportTitle;
    private TextView tvReportDate;
    private TextView tvLotTotal;
    private TextView tvLotPass;
    private TextView tvLotMarginal;
    private TextView tvLotFail;
    private TextView tvYield;
    private TextView tvFailRate;
    private TextView tvSummary;

    private ImageButton btnLogout;

    private EventHistoryStore historyStore;
    private boolean dailyMode = true;
    private boolean mqttConnected;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupHeaderActions();
        setupHeaderUser();
        renderConnectionState();

        historyStore = new EventHistoryStore(requireContext());

        btnDaily.setOnClickListener(v -> {
            dailyMode = true;
            updateModeButtons();
            refreshReport();
        });

        btnWeekly.setOnClickListener(v -> {
            dailyMode = false;
            updateModeButtons();
            refreshReport();
        });

        btnRefresh.setOnClickListener(v -> refreshReport());

        updateModeButtons();
        refreshReport();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupHeaderUser();
        renderConnectionState();
        refreshReport();
    }

    private void bindViews(View view) {
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvMqttChip = view.findViewById(R.id.tv_mqtt_chip);
        tvDeptRole = view.findViewById(R.id.tv_dept_role);
        tvMesServer = view.findViewById(R.id.tv_mes_server);

        tvReceivedAt = view.findViewById(R.id.tv_report_received_at);
        btnDaily = view.findViewById(R.id.btn_daily_report);
        btnWeekly = view.findViewById(R.id.btn_weekly_report);
        btnRefresh = view.findViewById(R.id.btn_report_refresh);

        tvReportTitle = view.findViewById(R.id.tv_report_title);
        tvReportDate = view.findViewById(R.id.tv_report_date);
        tvLotTotal = view.findViewById(R.id.tv_report_lot_total);
        tvLotPass = view.findViewById(R.id.tv_report_lot_pass);
        tvLotMarginal = view.findViewById(R.id.tv_report_lot_marginal);
        tvLotFail = view.findViewById(R.id.tv_report_lot_fail);
        tvYield = view.findViewById(R.id.tv_report_yield);
        tvFailRate = view.findViewById(R.id.tv_report_fail_rate);
        tvSummary = view.findViewById(R.id.tv_report_summary);

        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupHeaderActions() {
        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });

    }

    private void setupHeaderUser() {
        if (getActivity() == null) return;
        SessionManager sessionManager = new SessionManager(getActivity());
        User user = sessionManager.getCurrentUser();
        if (user == null) return;

        tvUserId.setText(user.getEmployeeId());
        tvUserName.setText(user.getName());
        tvDeptRole.setText(user.getDepartmentRolePhoneLabel());
    }

    public void setMqttConnected(boolean connected) {
        mqttConnected = connected;
        if (isAdded()) {
            renderConnectionState();
        }
    }

    public void onMqttEvent() {
        if (isAdded()) {
            refreshReport();
        }
    }

    private void renderConnectionState() {
        if (!isAdded()) return;
        if (mqttConnected) {
            tvMqttChip.setText("● MQTT 연결됨");
            tvMqttChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_pass));
            tvMqttChip.setBackgroundResource(R.drawable.bg_badge_pass);
            tvMesServer.setText("서버 MES 연결됨");
            tvMesServer.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        } else {
            tvMqttChip.setText("● MQTT 연결 안됨");
            tvMqttChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_fail));
            tvMqttChip.setBackgroundResource(R.drawable.bg_badge_fail);
            tvMesServer.setText("서버 MES 연결 안됨");
            tvMesServer.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_fail));
        }
    }

    private void updateModeButtons() {
        if (dailyMode) {
            btnDaily.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            btnDaily.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_info));
            btnWeekly.setBackgroundResource(R.drawable.bg_filter_chip);
            btnWeekly.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            btnDaily.setBackgroundResource(R.drawable.bg_filter_chip);
            btnDaily.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            btnWeekly.setBackgroundResource(R.drawable.bg_filter_chip_selected);
            btnWeekly.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_info));
        }
    }

    private void refreshReport() {
        if (historyStore == null) return;

        List<FeedEvent> all = historyStore.loadFeedEvents();
        List<FeedEvent> lotEvents = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = dailyMode ? today : today.minusDays(6);
        Set<String> seenLotKeys = new HashSet<>();

        for (FeedEvent e : all) {
            if (e.getEventType() == FeedEvent.EventType.LOT_END && isInReportScope(e, startDate, today)) {
                String key = safe(e.getEquipmentId(), "UNKNOWN") + "|" + safe(e.getLotId(), "LOT-UNKNOWN");
                if (seenLotKeys.contains(key)) continue;
                seenLotKeys.add(key);
                lotEvents.add(e);
            }
        }

        List<FeedEvent> scope = lotEvents;

        int totalLots = scope.size();
        int passLots = 0;
        int marginalLots = 0;
        int failLots = 0;
        int totalUnits = 0;
        int totalPassUnits = 0;
        int totalFailUnits = 0;

        for (FeedEvent event : scope) {
            FeedAdapter.LotResult result = FeedAdapter.computeLotResult(event);
            if (result == FeedAdapter.LotResult.PASS) passLots++;
            else if (result == FeedAdapter.LotResult.MARGINAL) marginalLots++;
            else if (result == FeedAdapter.LotResult.FAIL) failLots++;

            totalUnits += Math.max(0, event.getTotalUnits());
            totalPassUnits += Math.max(0, event.getPassUnits());
            totalFailUnits += Math.max(0, event.getFailUnits());
        }

        float yield = totalUnits > 0 ? (totalPassUnits * 100f / totalUnits) : 0f;
        float failRate = totalUnits > 0 ? (totalFailUnits * 100f / totalUnits) : 0f;

        if (dailyMode) {
            tvReportTitle.setText("일일 LOT 검사 보고서");
            tvReportDate.setText(today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())));
        } else {
            tvReportTitle.setText("주간 LOT 검사 보고서");
            tvReportDate.setText(startDate + " ~ " + today);
        }

        tvLotTotal.setText(String.valueOf(totalLots));
        tvLotPass.setText(String.valueOf(passLots));
        tvLotMarginal.setText(String.valueOf(marginalLots));
        tvLotFail.setText(String.valueOf(failLots));
        tvYield.setText(String.format(Locale.getDefault(), "%.1f%%", yield));
        tvFailRate.setText(String.format(Locale.getDefault(), "%.1f%%", failRate));

        if (totalLots == 0) {
            tvSummary.setText("수신된 LOT 결과가 아직 없습니다. MQTT 이벤트 수신 후 갱신 버튼을 눌러 최신 보고서를 확인하세요.");
        } else {
            tvSummary.setText(String.format(Locale.getDefault(),
                    "총 %d개 LOT 기준 자동 분석 요약입니다. 합격 %d, 경계 %d, 불합격 %d이며, 수율은 %.1f%% 입니다.",
                    totalLots,
                    passLots,
                    marginalLots,
                    failLots,
                    yield));
        }

        tvReceivedAt.setText("서버 수신 완료 · " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())));
    }

    private boolean isInReportScope(FeedEvent event, LocalDate startDate, LocalDate endDate) {
        LocalDate eventDate = toEventDate(event);
        return !eventDate.isBefore(startDate) && !eventDate.isAfter(endDate);
    }

    private LocalDate toEventDate(FeedEvent event) {
        long occurredAt = event == null ? 0L : event.getOccurredAtMillis();
        if (occurredAt <= 0L) return LocalDate.now();
        return Instant.ofEpochMilli(occurredAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private String safe(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }
}
