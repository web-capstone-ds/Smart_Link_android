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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.StatusUpdateEvent;
import com.smartfactory.visioninspection.activities.MainActivity;
import com.smartfactory.visioninspection.adapters.EquipmentAdapter;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.SessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EquipmentFragment extends Fragment {
    private static final int MAX_VISIBLE_LINES = 4;

    private TextView tvUserId;
    private TextView tvUserName;
    private TextView tvMqttChip;
    private TextView tvDeptRole;
    private TextView tvMesServer;

    private TextView tvRunChip;
    private TextView tvIdleChip;
    private TextView tvOffChip;
    private TextView tvTotal;

    private ImageButton btnLogout;
    private ImageButton btnThemeToggle;

    private EquipmentAdapter adapter;
    private final Map<String, EquipmentAdapter.EquipmentUiItem> stateMap = new HashMap<>();
    private boolean mqttConnected;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_equipment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupHeaderActions();
        setupRecycler(view);
        seedDefaultLines();
        setupHeaderUser();
        renderConnectionState();
        renderListAndSummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupHeaderUser();
        renderConnectionState();
    }

    private void bindViews(View view) {
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvMqttChip = view.findViewById(R.id.tv_mqtt_chip);
        tvDeptRole = view.findViewById(R.id.tv_dept_role);
        tvMesServer = view.findViewById(R.id.tv_mes_server);

        tvRunChip = view.findViewById(R.id.tv_eq_chip_run);
        tvIdleChip = view.findViewById(R.id.tv_eq_chip_idle);
        tvOffChip = view.findViewById(R.id.tv_eq_chip_off);
        tvTotal = view.findViewById(R.id.tv_eq_total);

        btnLogout = view.findViewById(R.id.btn_logout);
        btnThemeToggle = view.findViewById(R.id.btn_theme_toggle);
    }

    private void setupHeaderActions() {
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

    private void setupRecycler(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.rv_equipment);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new EquipmentAdapter(item -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openFeedWithLineFilter(item.equipmentId);
            }
        });

        recyclerView.setAdapter(adapter);
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

    private void seedDefaultLines() {
        if (!stateMap.isEmpty()) return;

        for (int i = 1; i <= MAX_VISIBLE_LINES; i++) {
            String equipmentId = String.format(Locale.getDefault(), "DS-VIS-%03d", i);
            EquipmentAdapter.EquipmentUiItem item = new EquipmentAdapter.EquipmentUiItem();
            item.equipmentId = equipmentId;
            item.lineNo = i;
            item.state = EquipmentAdapter.EquipmentState.OFF;
            item.expectedUnits = 40;
            item.latestMessage = "Latest: 장비 꺼짐";
            stateMap.put(equipmentId, item);
        }
    }

    public void updateRealTimeData(StatusUpdateEvent event) {
        if (!isAdded() || event == null || event.getEquipmentId() == null) return;

        EquipmentAdapter.EquipmentUiItem item = getOrCreate(event.getEquipmentId());

        String status = upper(event.getEquipmentStatus());
        if ("RUN".equals(status)) {
            item.state = EquipmentAdapter.EquipmentState.RUN;
            item.latestMessage = "Latest: 정상 운전";
        } else if ("IDLE".equals(status)) {
            item.state = EquipmentAdapter.EquipmentState.IDLE;
            item.latestMessage = "Latest: 대기 상태";
        } else if ("STOP".equals(status)) {
            item.state = EquipmentAdapter.EquipmentState.OFF;
            item.latestMessage = "Latest: 장비 정지";
        } else if ("OFF".equals(status)) {
            item.state = EquipmentAdapter.EquipmentState.OFF;
            item.latestMessage = "Latest: 장비 꺼짐";
        }

        if (event.getCurrentUnitCount() != null) {
            item.currentUnits = Math.max(0, event.getCurrentUnitCount());
        }
        if (event.getExpectedTotalUnits() != null && event.getExpectedTotalUnits() > 0) {
            item.expectedUnits = event.getExpectedTotalUnits();
        } else if (item.expectedUnits <= 0) {
            item.expectedUnits = 40;
        }

        String time = toTimeText(event.getTimestamp());
        if (!"--:--:--".equals(time)) {
            item.timeText = time;
        }

        renderListAndSummary();
    }

    public void onMqttEvent(String equipmentId, String eventType, String payload) {
        if (!isAdded() || equipmentId == null || eventType == null || payload == null) return;

        EquipmentAdapter.EquipmentUiItem item = getOrCreate(equipmentId);

        try {
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            String type = eventType.trim().toLowerCase(Locale.ROOT);

            if ("alarm".equals(type)) {
                String level = upper(optString(obj, "alarm_level"));
                String code = optString(obj, "hw_error_code");
                item.latestMessage = "Latest: " + (code.isEmpty() ? "HW 알람" : code);
                if ("CRITICAL".equals(level)) {
                    item.state = EquipmentAdapter.EquipmentState.OFF;
                } else {
                    item.state = EquipmentAdapter.EquipmentState.IDLE;
                }
                item.timeText = toTimeText(optString(obj, "timestamp"));
            } else if ("oracle".equals(type)) {
                String comment = optString(obj, "ai_comment");
                if (!comment.isEmpty()) {
                    item.latestMessage = "Latest: " + comment;
                }
                item.timeText = toTimeText(optString(obj, "timestamp"));
            } else if ("lot".equals(type)) {
                item.latestMessage = "Latest: LOT 완료";
                if (item.state == EquipmentAdapter.EquipmentState.OFF) {
                    item.state = EquipmentAdapter.EquipmentState.IDLE;
                }
                item.timeText = toTimeText(optString(obj, "timestamp"));
            }
        } catch (Exception ignore) {
            // keep previous state if payload parse fails
        }

        renderListAndSummary();
    }

    public void setMqttConnected(boolean connected) {
        mqttConnected = connected;
        if (isAdded()) {
            renderConnectionState();
        }
    }

    private EquipmentAdapter.EquipmentUiItem getOrCreate(String equipmentId) {
        EquipmentAdapter.EquipmentUiItem item = stateMap.get(equipmentId);
        if (item != null) return item;

        EquipmentAdapter.EquipmentUiItem created = new EquipmentAdapter.EquipmentUiItem();
        created.equipmentId = equipmentId;
        created.lineNo = parseLineNo(equipmentId);
        created.expectedUnits = 40;
        stateMap.put(equipmentId, created);
        return created;
    }

    private void renderConnectionState() {
        if (getContext() == null) return;
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

    private void renderListAndSummary() {
        if (adapter == null) return;

        List<EquipmentAdapter.EquipmentUiItem> allSorted = new ArrayList<>(stateMap.values());
        allSorted.sort(Comparator
                .comparingInt((EquipmentAdapter.EquipmentUiItem it) -> priority(it.state))
                .thenComparingInt(it -> it.lineNo));

        List<EquipmentAdapter.EquipmentUiItem> visible = new ArrayList<>();
        for (int i = 0; i < allSorted.size() && i < MAX_VISIBLE_LINES; i++) {
            visible.add(allSorted.get(i));
        }

        int run = 0;
        int idle = 0;
        int off = 0;

        for (EquipmentAdapter.EquipmentUiItem item : visible) {
            if (item.state == EquipmentAdapter.EquipmentState.RUN) run++;
            else if (item.state == EquipmentAdapter.EquipmentState.IDLE) idle++;
            else off++;
        }

        tvRunChip.setText("● RUN " + run);
        tvIdleChip.setText("● IDLE " + idle);
        tvOffChip.setText("● OFF " + off);
        tvTotal.setText(visible.size() + "개 장비");

        adapter.submitList(visible);
    }

    private int priority(EquipmentAdapter.EquipmentState state) {
        if (state == EquipmentAdapter.EquipmentState.RUN) return 0;
        if (state == EquipmentAdapter.EquipmentState.IDLE) return 1;
        return 2; // OFF
    }

    private String toTimeText(String iso) {
        if (iso == null || iso.trim().isEmpty()) return "--:--:--";
        if (iso.length() >= 19 && iso.contains("T")) return iso.substring(11, 19);
        if (iso.length() >= 8) return iso.substring(Math.max(0, iso.length() - 8));
        return iso;
    }

    private String optString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private int parseLineNo(String equipmentId) {
        if (equipmentId == null) return 999;
        String digits = equipmentId.replaceAll("[^0-9]", "");
        if (digits.length() >= 3) {
            digits = digits.substring(digits.length() - 3);
        }
        try {
            return Integer.parseInt(digits);
        } catch (Exception ignore) {
            return 999;
        }
    }
}
