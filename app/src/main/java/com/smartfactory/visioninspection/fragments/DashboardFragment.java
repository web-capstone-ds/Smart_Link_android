package com.smartfactory.visioninspection.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.StatusUpdateEvent;
import com.smartfactory.visioninspection.activities.MainActivity;
import com.smartfactory.visioninspection.adapters.InspectionCardAdapter;
import com.smartfactory.visioninspection.adapters.InspectionCardAdapter.LotOutcome;
import com.smartfactory.visioninspection.bottomsheets.LotDetailBottomSheet;
import com.smartfactory.visioninspection.models.DashboardLineState;
import com.smartfactory.visioninspection.models.InspectionEvent;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.SessionManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DashboardFragment extends Fragment {

    private static final long BLINK_INTERVAL_MS = 220L;
    private static final long FAIL_BLINK_DURATION_MS = 2000L;
    private static final long MARGINAL_BLINK_DURATION_MS = 1000L;

    private RecyclerView recyclerView;
    private InspectionCardAdapter adapter;
    private TextView tvUserId;
    private TextView tvUserName;
    private TextView tvMqttChip;
    private TextView tvDeptRole;
    private TextView tvMesServer;
    private TextView tvCountFail;
    private TextView tvCountMarginal;
    private TextView tvCountPass;
    private TextView tvCountIdle;
    private TextView tvLiveState;
    private ImageButton btnLogout;
    private ImageButton btnThemeToggle;

    private boolean mqttConnected = false;
    private int lotPassCount = 0;
    private int lotMarginalCount = 0;
    private int lotFailCount = 0;
    private final Set<String> countedLotKeys = new HashSet<>();

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> blinkJobs = new HashMap<>();
    private final Set<String> blinkingEquipmentIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        bindViews(view);
        setupHeaderUser();
        setupRecycler();
        setupActions();
        updateMqttStateUi();
        updateSummaryCounts();

        recyclerView = view.findViewById(R.id.recycler_dashboard);
        return view;
    }

    @Override
    public void onDestroyView() {
        clearAllBlinkJobs();
        super.onDestroyView();
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_dashboard);
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvMqttChip = view.findViewById(R.id.tv_mqtt_chip);
        tvDeptRole = view.findViewById(R.id.tv_dept_role);
        tvMesServer = view.findViewById(R.id.tv_mes_server);
        tvCountFail = view.findViewById(R.id.tv_count_fail);
        tvCountMarginal = view.findViewById(R.id.tv_count_marginal);
        tvCountPass = view.findViewById(R.id.tv_count_pass);
        tvCountIdle = view.findViewById(R.id.tv_count_idle);
        tvLiveState = view.findViewById(R.id.tv_live_state);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnThemeToggle = view.findViewById(R.id.btn_theme_toggle);
    }

    private void setupRecycler() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InspectionCardAdapter(this::openLotBottomSheet);
        recyclerView.setAdapter(adapter);
    }

    private void setupHeaderUser() {
        if (getActivity() == null) return;
        SessionManager sm = new SessionManager(getActivity());
        User user = sm.getCurrentUser();
        if (user == null) return;
        tvUserId.setText(user.getEmployeeId());
        tvUserName.setText(user.getName());
    }

    private void setupActions() {
        btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });

        btnThemeToggle.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).toggleThemeMode();
            }
        });
    }

    private void openLotBottomSheet(DashboardLineState state) {
        if (getActivity() == null) return;
        SessionManager sm = new SessionManager(getActivity());
        User user = sm.getCurrentUser();

        InspectionEvent.Result result = toInspectionResult(state.getResult());
        String[] errorCodes = state.getLastErrorCode() == null
                ? new String[0]
                : new String[]{state.getLastErrorCode()};

        InspectionEvent event = new InspectionEvent.Builder(
                "dash-" + state.getEquipmentId(),
                state.getLotId(),
                state.getTimeText(),
                result)
                .equipmentId(state.getEquipmentId())
                .recipeId(state.getRecipeId())
                .operator(state.getOperatorId())
                .errorCodes(errorCodes)
                .build();

        LotDetailBottomSheet sheet = LotDetailBottomSheet.newInstance(event, user);
        sheet.show(getParentFragmentManager(), LotDetailBottomSheet.TAG);
    }

    private InspectionEvent.Result toInspectionResult(DashboardLineState.LineResult r) {
        if (r == DashboardLineState.LineResult.FAIL) return InspectionEvent.Result.FAIL;
        if (r == DashboardLineState.LineResult.MARGINAL) return InspectionEvent.Result.MARGINAL;
        return InspectionEvent.Result.PASS;
    }

    public void updateRealTimeData(StatusUpdateEvent event) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (adapter == null || event == null) return;

            if (event.getEquipmentId() != null && blinkingEquipmentIds.contains(event.getEquipmentId())) {
                return;
            }

            adapter.upsertFromStatus(event);
            updateSummaryCounts();
        });
    }

    public void onMqttEvent(String equipmentId, String eventType, String payload) {
        if (getActivity() == null || adapter == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                String type = eventType == null ? "" : eventType.trim().toLowerCase();
                JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();

                if ("alarm".equals(type)) {
                    String level = optString(obj, "alarm_level");
                    String code = optString(obj, "hw_error_code");
                    String detail = optString(obj, "hw_error_detail");
                    String ts = optString(obj, "timestamp");
                    adapter.applyAlarmEvent(equipmentId, level, code, detail, ts);
                    updateSummaryCounts();
                } else if ("oracle".equals(type)) {
                    String judgment = optString(obj, "judgment");
                    String comment = optString(obj, "ai_comment");
                    String ts = optString(obj, "timestamp");
                    adapter.applyOracleEvent(equipmentId, judgment, comment, ts);
                    updateSummaryCounts();
                } else if ("lot".equals(type)) {
                    String lotId = optString(obj, "lot_id");
                    String ts = optString(obj, "timestamp");
                    int totalUnits = optInt(obj, "total_units");
                    int passCount = optInt(obj, "pass_count");
                    int failCount = optInt(obj, "fail_count");

                    LotOutcome outcome = adapter.applyLotEndEvent(
                            equipmentId,
                            lotId,
                            totalUnits,
                            passCount,
                            failCount,
                            ts
                    );
                    accumulateLotOutcome(equipmentId, lotId, outcome);
                    runLotBlinkAndAdvance(equipmentId, outcome);
                    updateSummaryCounts();
                }
            } catch (Exception e) {
                Log.e("DashboardFragment", "MQTT raw event parse error", e);
            }
        });
    }

    public void setMqttConnected(boolean connected) {
        mqttConnected = connected;
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::updateMqttStateUi);
        }
    }

    private void runLotBlinkAndAdvance(String equipmentId, LotOutcome outcome) {
        if (equipmentId == null || adapter == null) return;

        cancelBlinkJob(equipmentId);

        if (outcome == LotOutcome.FAIL || outcome == LotOutcome.MARGINAL) {
            long durationMs = (outcome == LotOutcome.FAIL) ? FAIL_BLINK_DURATION_MS : MARGINAL_BLINK_DURATION_MS;
            long endAt = System.currentTimeMillis() + durationMs;
            blinkingEquipmentIds.add(equipmentId);
            final boolean[] on = {true};

            Runnable job = new Runnable() {
                @Override
                public void run() {
                    if (!isAdded() || adapter == null) {
                        blinkingEquipmentIds.remove(equipmentId);
                        return;
                    }

                    long now = System.currentTimeMillis();
                    if (now >= endAt) {
                        adapter.setBlinkState(equipmentId, outcome, true);
                        adapter.moveToNextLot(equipmentId);
                        blinkingEquipmentIds.remove(equipmentId);
                        blinkJobs.remove(equipmentId);
                        updateSummaryCounts();
                        return;
                    }

                    adapter.setBlinkState(equipmentId, outcome, on[0]);
                    on[0] = !on[0];
                    uiHandler.postDelayed(this, BLINK_INTERVAL_MS);
                }
            };

            blinkJobs.put(equipmentId, job);
            uiHandler.post(job);
        } else {
            adapter.moveToNextLot(equipmentId);
        }
    }

    private void cancelBlinkJob(String equipmentId) {
        Runnable prev = blinkJobs.remove(equipmentId);
        if (prev != null) {
            uiHandler.removeCallbacks(prev);
        }
        blinkingEquipmentIds.remove(equipmentId);
    }

    private void clearAllBlinkJobs() {
        for (Runnable job : blinkJobs.values()) {
            uiHandler.removeCallbacks(job);
        }
        blinkJobs.clear();
        blinkingEquipmentIds.clear();
    }

    private void updateMqttStateUi() {
        if (tvMqttChip == null || tvLiveState == null || tvMesServer == null) return;
        if (getContext() == null) return;

        if (mqttConnected) {
            tvMqttChip.setText("● MQTT 연결됨");
            tvMqttChip.setTextColor(ContextCompat.getColor(getContext(), R.color.color_pass));
            tvMqttChip.setBackgroundResource(R.drawable.bg_badge_pass);

            tvLiveState.setText("● LIVE");
            tvLiveState.setTextColor(ContextCompat.getColor(getContext(), R.color.color_pass));

            tvMesServer.setText("서버 MES 연결됨");
            tvMesServer.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
        } else {
            tvMqttChip.setText("● MQTT 연결 안됨");
            tvMqttChip.setTextColor(ContextCompat.getColor(getContext(), R.color.color_fail));
            tvMqttChip.setBackgroundResource(R.drawable.bg_badge_fail);

            tvLiveState.setText("● OFFLINE");
            tvLiveState.setTextColor(ContextCompat.getColor(getContext(), R.color.color_fail));

            tvMesServer.setText("서버 MES 연결 안됨");
            tvMesServer.setTextColor(ContextCompat.getColor(getContext(), R.color.color_fail));
        }
    }

    private void updateSummaryCounts() {
        if (adapter == null) return;
        List<DashboardLineState> items = adapter.getCurrentItems();
        int idle = 0;

        for (DashboardLineState item : items) {
            if (item.getResult() == DashboardLineState.LineResult.IDLE) {
                idle++;
            }
        }

        tvCountFail.setText(lotFailCount + "\n불합격");
        tvCountMarginal.setText(lotMarginalCount + "\n경계");
        tvCountPass.setText(lotPassCount + "\n운전중");
        tvCountIdle.setText(idle + "\n대기");
    }

    private void accumulateLotOutcome(String equipmentId, String lotId, LotOutcome outcome) {
        if (outcome == null || outcome == LotOutcome.UNKNOWN) return;

        String safeEq = equipmentId == null ? "unknown-eq" : equipmentId.trim();
        String safeLot = (lotId == null || lotId.trim().isEmpty()) ? "unknown-lot" : lotId.trim();
        String key = safeEq + "|" + safeLot;

        if (countedLotKeys.contains(key)) return;
        countedLotKeys.add(key);

        switch (outcome) {
            case FAIL:
                lotFailCount++;
                break;
            case MARGINAL:
                lotMarginalCount++;
                break;
            case PASS:
                lotPassCount++;
                break;
            default:
                break;
        }
    }

    private String optString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private int optInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignore) {
            return 0;
        }
    }
}
