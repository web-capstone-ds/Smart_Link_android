package com.smartfactory.visioninspection.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.activities.MainActivity;
import com.smartfactory.visioninspection.adapters.FeedAdapter;
import com.smartfactory.visioninspection.models.FeedEvent;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.EventHistoryStore;
import com.smartfactory.visioninspection.utils.SessionManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FeedFragment extends Fragment {

    private static final int MAX_UI_ITEMS = 120;

    private TextView tvUserId;
    private TextView tvUserName;
    private TextView tvMqttChip;
    private TextView tvDeptRole;
    private TextView tvMesServer;

    private TextView tvFilterNotice;
    private TextView btnClearFilter;
    private LinearLayout lineFilterContainer;
    private LinearLayout resultFilterContainer;

    private ImageButton btnLogout;
    private ImageButton btnThemeToggle;

    private final List<FeedEvent> allEvents = new ArrayList<>();
    private final List<FeedEvent> filteredEvents = new ArrayList<>();

    private FeedAdapter adapter;
    private EventHistoryStore historyStore;

    private String selectedLine = "ALL";
    private String selectedResult = "ALL";
    private boolean mqttConnected;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupHeaderActions();
        setupHeaderUser();
        renderConnectionState();

        historyStore = new EventHistoryStore(requireContext());
        allEvents.clear();
        allEvents.addAll(historyStore.loadFeedEvents());

        RecyclerView rv = view.findViewById(R.id.rv_feed);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);

        adapter = new FeedAdapter();
        rv.setAdapter(adapter);

        btnClearFilter.setOnClickListener(v -> {
            selectedLine = "ALL";
            selectedResult = "ALL";
            renderFilters();
            applyFilters();
        });

        renderFilters();
        applyFilters();
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

        tvFilterNotice = view.findViewById(R.id.tv_feed_filter_notice);
        btnClearFilter = view.findViewById(R.id.btn_clear_filter);
        lineFilterContainer = view.findViewById(R.id.line_filter_container);
        resultFilterContainer = view.findViewById(R.id.result_filter_container);

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

    private void setupHeaderUser() {
        if (getActivity() == null) return;
        SessionManager sessionManager = new SessionManager(getActivity());
        User user = sessionManager.getCurrentUser();
        if (user == null) return;

        tvUserId.setText(user.getEmployeeId());
        tvUserName.setText(user.getName());
        tvDeptRole.setText(user.getDepartmentRolePhoneLabel());
    }

    public void applyEquipmentFilter(String equipmentId) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) return;
        selectedLine = equipmentId;
        renderFilters();
        applyFilters();
    }

    public void onMqttEvent(String equipmentId, String eventType, String payload) {
        FeedEvent event = buildFeedEvent(equipmentId, eventType, payload);
        if (event == null) return;

        if (historyStore == null && getContext() != null) {
            historyStore = new EventHistoryStore(getContext());
        }

        if (historyStore != null) {
            historyStore.appendFeedEvent(event);
        }

        for (FeedEvent old : allEvents) {
            if (safe(old.getId()).equals(safe(event.getId()))) {
                return;
            }
        }

        allEvents.add(0, event);
        if (allEvents.size() > MAX_UI_ITEMS) {
            allEvents.remove(allEvents.size() - 1);
        }

        renderFilters();
        applyFilters();
    }

    public void setMqttConnected(boolean connected) {
        mqttConnected = connected;
        if (isAdded()) {
            renderConnectionState();
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

    private void renderFilters() {
        if (lineFilterContainer == null || resultFilterContainer == null) return;

        lineFilterContainer.removeAllViews();
        resultFilterContainer.removeAllViews();

        Set<String> lineIds = new LinkedHashSet<>();
        lineIds.add("ALL");
        allEvents.stream()
                .map(FeedEvent::getEquipmentId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .sorted(Comparator.comparingInt(this::parseLineNo))
                .forEach(lineIds::add);

        for (String lineId : lineIds) {
            boolean selected = lineId.equals(selectedLine);
            TextView chip = buildChip(lineId.equals("ALL") ? "전체 장비" : toLineLabel(lineId), selected);
            chip.setOnClickListener(v -> {
                selectedLine = lineId;
                renderFilters();
                applyFilters();
            });
            lineFilterContainer.addView(chip);
        }

        String[] resultOptions = new String[]{"ALL", "PASS", "MARGINAL", "FAIL"};
        for (String option : resultOptions) {
            boolean selected = option.equals(selectedResult);
            TextView chip = buildChip(toResultLabel(option), selected);
            chip.setOnClickListener(v -> {
                selectedResult = option;
                renderFilters();
                applyFilters();
            });
            resultFilterContainer.addView(chip);
        }
    }

    private TextView buildChip(String text, boolean selected) {
        TextView chip = new TextView(requireContext());
        chip.setText(text);
        chip.setTextSize(12f);
        chip.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.color_info : R.color.text_secondary));
        chip.setBackgroundResource(selected ? R.drawable.bg_filter_chip_selected : R.drawable.bg_filter_chip);
        chip.setPadding(dp(12), dp(7), dp(12), dp(7));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMarginEnd(dp(8));
        chip.setLayoutParams(lp);
        return chip;
    }

    private void applyFilters() {
        filteredEvents.clear();

        for (FeedEvent event : allEvents) {
            boolean lineMatch = "ALL".equals(selectedLine) || safe(selectedLine).equals(safe(event.getEquipmentId()));
            if (!lineMatch) continue;

            if (!"ALL".equals(selectedResult)) {
                FeedAdapter.LotResult lotResult = FeedAdapter.computeLotResult(event);
                if ("PASS".equals(selectedResult) && lotResult != FeedAdapter.LotResult.PASS) continue;
                if ("MARGINAL".equals(selectedResult) && lotResult != FeedAdapter.LotResult.MARGINAL) continue;
                if ("FAIL".equals(selectedResult) && lotResult != FeedAdapter.LotResult.FAIL) continue;
            }

            filteredEvents.add(event);
        }

        if (adapter != null) {
            adapter.submitList(filteredEvents);
        }

        boolean lineFiltered = !"ALL".equals(selectedLine);
        if (lineFiltered) {
            tvFilterNotice.setVisibility(View.VISIBLE);
            tvFilterNotice.setText("• " + toLineLabel(selectedLine) + " 필터 적용 중");
            btnClearFilter.setVisibility(View.VISIBLE);
        } else {
            tvFilterNotice.setVisibility(View.GONE);
            btnClearFilter.setVisibility(View.GONE);
        }
    }

    private FeedEvent buildFeedEvent(String equipmentId, String eventType, String payload) {
        if (payload == null || payload.trim().isEmpty()) return null;

        try {
            String type = eventType == null ? "" : eventType.trim().toLowerCase(Locale.ROOT);
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();

            String eventId = optString(obj, "message_id", "feed-" + System.currentTimeMillis());
            String time = formatTime(optString(obj, "timestamp", ""));
            String eqId = (equipmentId == null || equipmentId.trim().isEmpty())
                    ? optString(obj, "equipment_id", "UNKNOWN")
                    : equipmentId;

            if ("lot".equals(type)) {
                String lotId = optString(obj, "lot_id", "LOT-UNKNOWN");
                int total = optInt(obj, "total_units");
                int pass = optInt(obj, "pass_count");
                int fail = optInt(obj, "fail_count");
                float yield = optFloat(obj, "yield_pct");
                String operator = optString(obj, "operator_id", "-");
                String recipe = optString(obj, "recipe_id", "-");

                return FeedEvent.lotEnd(eventId, time, eqId, lotId, total, pass, fail, yield, operator, recipe);
            }

            if ("alarm".equals(type)) {
                String code = optString(obj, "hw_error_code", "HW_ALARM");
                String desc = optString(obj, "hw_error_detail", "-");
                String burst = optString(obj, "burst_id", "-");
                String levelText = optString(obj, "alarm_level", "WARNING");
                FeedEvent.AlarmLevel level = "CRITICAL".equalsIgnoreCase(levelText)
                        ? FeedEvent.AlarmLevel.CRITICAL
                        : FeedEvent.AlarmLevel.WARNING;

                return FeedEvent.hwAlarm(eventId, time, eqId, code, level, desc, burst);
            }

            if ("oracle".equals(type)) {
                String judgment = optString(obj, "judgment", "NORMAL");
                String message = optString(obj, "ai_comment", "-");
                FeedEvent.AnalysisLevel level = "ABNORMAL".equalsIgnoreCase(judgment)
                        ? FeedEvent.AnalysisLevel.DANGER
                        : FeedEvent.AnalysisLevel.WARNING;

                String[] codes = parseErrorCodes(obj.getAsJsonArray("error_codes"));
                return FeedEvent.oracleAnalysis(eventId, time, eqId, level, message, codes);
            }
        } catch (Exception ignore) {
            return null;
        }

        return null;
    }

    private String[] parseErrorCodes(@Nullable JsonArray array) {
        if (array == null || array.size() == 0) return new String[0];
        String[] out = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            out[i] = array.get(i).getAsString();
        }
        return out;
    }

    private String formatTime(String iso) {
        if (iso == null || iso.trim().isEmpty()) return "--:--:--";
        try {
            Instant instant = Instant.parse(iso);
            LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return ldt.format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault()));
        } catch (Exception ignore) {
            return iso;
        }
    }

    private String optString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        String v = obj.get(key).getAsString();
        if (v == null || v.trim().isEmpty()) return fallback;
        return v;
    }

    private int optInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignore) {
            return 0;
        }
    }

    private float optFloat(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0f;
        try {
            return obj.get(key).getAsFloat();
        } catch (Exception ignore) {
            return 0f;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
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

    private String toLineLabel(String equipmentId) {
        int lineNo = parseLineNo(equipmentId);
        return lineNo == 999 ? equipmentId : (lineNo + "라인 비전센서");
    }

    private String toResultLabel(String value) {
        switch (value) {
            case "PASS":
                return "LOT 합격";
            case "MARGINAL":
                return "LOT 경계";
            case "FAIL":
                return "LOT 불합격";
            default:
                return "전체 결과";
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
