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
import com.smartfactory.visioninspection.bottomsheets.LotDetailBottomSheet;
import com.smartfactory.visioninspection.models.FeedEvent;
import com.smartfactory.visioninspection.models.InspectionEvent;
import com.smartfactory.visioninspection.models.User;
import com.smartfactory.visioninspection.utils.EventHistoryStore;
import com.smartfactory.visioninspection.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class FeedFragment extends Fragment {

    private static final int MAX_UI_ITEMS = 120;

    private static final String FILTER_ALL = "ALL";
    private static final String FILTER_PASS = "PASS";
    private static final String FILTER_MARGINAL = "MARGINAL";
    private static final String FILTER_FAIL = "FAIL";
    private static final String FILTER_HW = "HW";
    private static final String TZ_KST = "Asia/Seoul";

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

    private final List<FeedEvent> allEvents = new ArrayList<>();
    private final List<FeedEvent> filteredEvents = new ArrayList<>();

    private FeedAdapter adapter;
    private EventHistoryStore historyStore;

    private String selectedLine = FILTER_ALL;
    private final Set<String> selectedResultFilters = new LinkedHashSet<>();
    private boolean mqttConnected;
    private String loggedInEmployeeId = "EMP001";

    private final Map<String, String> latestRecipeByEquipment = new HashMap<>();
    private final Map<String, FeedAdapter.LotResult> latestLotResultByEquipment = new HashMap<>();
    private final Map<String, FeedAdapter.LotResult> latestLotResultByLotKey = new HashMap<>();
    private final Map<String, FeedEvent> latestOracleByLotKey = new HashMap<>();
    private final Map<String, FeedEvent> latestOracleByEquipment = new HashMap<>();
    private final Map<String, String> latestStatusByEquipment = new HashMap<>();

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
        resetResultFilter();

        historyStore = new EventHistoryStore(requireContext());
        allEvents.clear();
        allEvents.addAll(historyStore.loadFeedEvents());
        rebuildRealtimeContextFromEvents();

        RecyclerView rv = view.findViewById(R.id.rv_feed);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);

        adapter = new FeedAdapter(this::openLotBottomSheet);
        rv.setAdapter(adapter);

        btnClearFilter.setOnClickListener(v -> {
            selectedLine = FILTER_ALL;
            resetResultFilter();
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
        loggedInEmployeeId = user.getEmployeeId();
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
            if (!historyStore.appendFeedEvent(event)) {
                return;
            }
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
        lineIds.add(FILTER_ALL);
        getTodayDisplayEvents().stream()
                .map(FeedEvent::getEquipmentId)
                .filter(id -> id != null && !id.trim().isEmpty())
                .sorted(Comparator.comparingInt(this::parseLineNo))
                .forEach(lineIds::add);

        for (String lineId : lineIds) {
            boolean selected = lineId.equals(selectedLine);
            TextView chip = buildChip(lineId.equals(FILTER_ALL) ? "전체 장비" : toLineLabel(lineId), selected);
            chip.setOnClickListener(v -> {
                selectedLine = lineId;
                renderFilters();
                applyFilters();
            });
            lineFilterContainer.addView(chip);
        }

        // ORACLE 필터 제거 + HW 알람을 LOT 불합격 옆으로 배치
        String[] resultOptions = new String[]{FILTER_ALL, FILTER_PASS, FILTER_MARGINAL, FILTER_FAIL, FILTER_HW};
        for (String option : resultOptions) {
            boolean selected = selectedResultFilters.contains(option);
            TextView chip = buildChip(toResultLabel(option), selected);
            chip.setOnClickListener(v -> {
                toggleResultFilter(option);
                renderFilters();
                applyFilters();
            });
            resultFilterContainer.addView(chip);
        }
    }

    private void resetResultFilter() {
        selectedResultFilters.clear();
        selectedResultFilters.add(FILTER_ALL);
    }

    private void toggleResultFilter(String filterKey) {
        if (FILTER_ALL.equals(filterKey)) {
            resetResultFilter();
            return;
        }

        if (selectedResultFilters.contains(filterKey)) {
            selectedResultFilters.remove(filterKey);
        } else {
            selectedResultFilters.add(filterKey);
        }

        selectedResultFilters.remove(FILTER_ALL);
        if (selectedResultFilters.isEmpty()) {
            selectedResultFilters.add(FILTER_ALL);
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

        for (FeedEvent event : getTodayDisplayEvents()) {
            // ORACLE 카드는 리스트에 직접 표시하지 않음 (LOT 상세에서 노출)
            if (event.getEventType() == FeedEvent.EventType.ORACLE_ANALYSIS) continue;

            boolean lineMatch = FILTER_ALL.equals(selectedLine) || safe(selectedLine).equals(safe(event.getEquipmentId()));
            if (!lineMatch) continue;

            if (!matchesResultFilters(event)) continue;
            filteredEvents.add(event);
        }

        if (adapter != null) {
            adapter.submitList(filteredEvents);
        }

        boolean lineFiltered = !FILTER_ALL.equals(selectedLine);
        boolean resultFiltered = !(selectedResultFilters.size() == 1 && selectedResultFilters.contains(FILTER_ALL));
        if (lineFiltered || resultFiltered) {
            tvFilterNotice.setVisibility(View.VISIBLE);
            tvFilterNotice.setText(buildFilterNoticeText(lineFiltered, resultFiltered));
            btnClearFilter.setVisibility(View.VISIBLE);
        } else {
            tvFilterNotice.setVisibility(View.GONE);
            btnClearFilter.setVisibility(View.GONE);
        }
    }

    private boolean matchesResultFilters(FeedEvent event) {
        if (selectedResultFilters.contains(FILTER_ALL)) return true;
        for (String filter : selectedResultFilters) {
            if (matchesSingleResultFilter(event, filter)) return true;
        }
        return false;
    }

    private List<FeedEvent> getTodayDisplayEvents() {
        List<FeedEvent> out = new ArrayList<>();
        String today = formatKstDate(System.currentTimeMillis());

        for (FeedEvent event : allEvents) {
            if (event == null) continue;
            if (!today.equals(formatKstDate(event.getOccurredAtMillis()))) continue;

            out.add(event);
        }
        return out;
    }

    private String formatKstDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        sdf.setTimeZone(TimeZone.getTimeZone(TZ_KST));
        return sdf.format(new Date(millis > 0 ? millis : System.currentTimeMillis()));
    }

    private boolean matchesSingleResultFilter(FeedEvent event, String filter) {
        switch (filter) {
            case FILTER_PASS:
                return event.getEventType() == FeedEvent.EventType.LOT_END
                        && FeedAdapter.computeLotResult(event) == FeedAdapter.LotResult.PASS;
            case FILTER_MARGINAL:
                return event.getEventType() == FeedEvent.EventType.LOT_END
                        && FeedAdapter.computeLotResult(event) == FeedAdapter.LotResult.MARGINAL;
            case FILTER_FAIL:
                return event.getEventType() == FeedEvent.EventType.LOT_END
                        && FeedAdapter.computeLotResult(event) == FeedAdapter.LotResult.FAIL;
            case FILTER_HW:
                return event.getEventType() == FeedEvent.EventType.HW_ALARM;
            default:
                return true;
        }
    }

    private String buildFilterNoticeText(boolean lineFiltered, boolean resultFiltered) {
        StringBuilder sb = new StringBuilder("· ");
        if (lineFiltered) {
            sb.append(toLineLabel(selectedLine));
        }
        if (resultFiltered) {
            if (lineFiltered) sb.append(" + ");
            boolean first = true;
            for (String filter : selectedResultFilters) {
                if (FILTER_ALL.equals(filter)) continue;
                if (!first) sb.append(", ");
                sb.append(toResultLabel(filter));
                first = false;
            }
        }
        sb.append(" 필터 적용 중");
        return sb.toString();
    }

    private FeedEvent buildFeedEvent(String equipmentId, String eventType, String payload) {
        if (payload == null || payload.trim().isEmpty()) return null;

        try {
            String type = eventType == null ? "" : eventType.trim().toLowerCase(Locale.ROOT);
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();

            String eventId = optString(obj, "message_id", "feed-" + System.currentTimeMillis());
            String timestamp = optString(obj, "timestamp", "");
            long occurredAtMillis = parseEventTimeMillis(timestamp);
            String time = formatTime(timestamp);
            String eqId = (equipmentId == null || equipmentId.trim().isEmpty())
                    ? optString(obj, "equipment_id", "UNKNOWN")
                    : equipmentId;

            if ("lot".equals(type)) {
                String lotId = optString(obj, "lot_id", "LOT-UNKNOWN");
                int total = optInt(obj, "total_units");
                int pass = optInt(obj, "pass_count");
                int fail = optInt(obj, "fail_count");
                float yield = optFloat(obj, "yield_pct");
                String operator = optString(obj, "operator_id", loggedInEmployeeId);
                String recipe = optString(obj, "recipe_id",
                        latestRecipeByEquipment.getOrDefault(eqId, loggedInEmployeeId));

                latestRecipeByEquipment.put(eqId, recipe);
                FeedAdapter.LotResult lotResult = computeLotResult(total, fail, yield);
                rememberLotResult(eqId, lotId, lotResult);

                return FeedEvent.lotEnd(eventId, time, eqId, lotId, total, pass, fail, yield, operator, recipe)
                        .withOccurredAtMillis(occurredAtMillis);
            }

            if ("alarm".equals(type)) {
                String code = optString(obj, "hw_error_code", "HW_ALARM");
                String desc = optString(obj, "hw_error_detail", "-");
                String burst = optString(obj, "burst_id", "-");
                String levelText = optString(obj, "alarm_level", "WARNING");
                FeedEvent.AlarmLevel level = "CRITICAL".equalsIgnoreCase(levelText)
                        ? FeedEvent.AlarmLevel.CRITICAL
                        : FeedEvent.AlarmLevel.WARNING;

                return FeedEvent.hwAlarm(eventId, time, eqId, code, level, desc, burst)
                        .withOccurredAtMillis(occurredAtMillis);
            }

            if ("oracle".equals(type)) {
                String judgment = optString(obj, "judgment", "NORMAL");
                String lotId = optString(obj, "lot_id", "");
                String message = optString(obj, "ai_comment", "-");
                FeedEvent.AnalysisLevel level = mapOracleLevel(judgment, eqId, lotId);

                String[] codes = parseErrorCodes(obj.getAsJsonArray("error_codes"));
                FeedEvent oracleEvent = FeedEvent.oracleAnalysis(eventId, time, eqId, lotId, level, message, codes);
                oracleEvent.withOccurredAtMillis(occurredAtMillis);
                rememberOracleEvent(oracleEvent);
                return oracleEvent;
            }

            if ("status".equals(type)) {
                String recipe = optString(obj, "recipe_id", "");
                if (!recipe.trim().isEmpty()) {
                    latestRecipeByEquipment.put(eqId, recipe);
                }
                String status = optString(obj, "equipment_status", "").toUpperCase(Locale.ROOT);
                if (rememberStatusAndShouldEmitHwAlarm(eqId, status)) {
                    String ts = optString(obj, "timestamp", "");
                    long statusOccurredAt = parseEventTimeMillis(ts);
                    String id = optString(obj, "message_id", "status-" + eqId + "-" + safe(ts));
                    return FeedEvent.hwAlarm(
                            id,
                            formatTime(ts),
                            eqId,
                            "EQUIPMENT_STOP",
                            FeedEvent.AlarmLevel.CRITICAL,
                            "장비 정지 상태 진입",
                            "-"
                    ).withOccurredAtMillis(statusOccurredAt);
                }
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

    // java.time 대신 SimpleDateFormat 사용 (API 경고 제거)
    private String formatTime(String iso) {
        if (iso == null || iso.trim().isEmpty()) return "--:--:--";
        try {
            SimpleDateFormat isoParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            isoParser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoParser.parse(iso);
            if (date == null) return iso;

            SimpleDateFormat out = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            out.setTimeZone(TimeZone.getDefault());
            return out.format(date);
        } catch (Exception ignore) {
            return iso;
        }
    }

    private long parseEventTimeMillis(String iso) {
        if (iso == null || iso.trim().isEmpty()) return System.currentTimeMillis();

        String value = iso.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setLenient(false);
                if (pattern.contains("'Z'")) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                Date parsed = sdf.parse(value);
                if (parsed != null) return parsed.getTime();
            } catch (Exception ignore) {
            }
        }

        return System.currentTimeMillis();
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

    private void rebuildRealtimeContextFromEvents() {
        latestRecipeByEquipment.clear();
        latestLotResultByEquipment.clear();
        latestLotResultByLotKey.clear();
        latestOracleByLotKey.clear();
        latestOracleByEquipment.clear();

        for (int i = allEvents.size() - 1; i >= 0; i--) {
            FeedEvent event = allEvents.get(i);
            if (event == null) continue;
            if (event.getEventType() != FeedEvent.EventType.LOT_END) continue;

            String eqId = safe(event.getEquipmentId());
            String lotId = safe(event.getLotId());
            FeedAdapter.LotResult result = computeLotResult(
                    event.getTotalUnits(),
                    event.getFailUnits(),
                    event.getYieldRate()
            );
            rememberLotResult(eqId, lotId, result);

            if (event.getRecipeId() != null && !event.getRecipeId().trim().isEmpty()) {
                latestRecipeByEquipment.put(eqId, event.getRecipeId());
            }
        }

        for (int i = 0; i < allEvents.size(); i++) {
            FeedEvent event = allEvents.get(i);
            if (event == null || event.getEventType() != FeedEvent.EventType.ORACLE_ANALYSIS) continue;
            rememberOracleEvent(event);

            FeedEvent.AnalysisLevel level = mapOracleLevel("NORMAL", event.getEquipmentId(), safe(event.getLotId()));
            if (level != event.getAnalysisLevel()) {
                FeedEvent normalized = FeedEvent.oracleAnalysis(
                        event.getId(),
                        event.getTime(),
                        event.getEquipmentId(),
                        event.getLotId(),
                        level,
                        event.getAnalysisMessage(),
                        event.getErrorCodes()
                );
                allEvents.set(i, normalized);
                rememberOracleEvent(normalized);
            }
        }
    }

    private FeedEvent.AnalysisLevel mapOracleLevel(String judgment, String equipmentId, String lotId) {
        String j = judgment == null ? "" : judgment.trim().toUpperCase(Locale.ROOT);
        if ("ABNORMAL".equals(j) || "DANGER".equals(j) || "CRITICAL".equals(j)) {
            return FeedEvent.AnalysisLevel.DANGER;
        }
        if ("WARNING".equals(j) || "MARGINAL".equals(j)) {
            return FeedEvent.AnalysisLevel.WARNING;
        }
        if ("NORMAL".equals(j) || "PASS".equals(j)) {
            return FeedEvent.AnalysisLevel.NORMAL;
        }

        FeedAdapter.LotResult linkedResult = resolveLotResult(equipmentId, lotId);
        if (linkedResult == FeedAdapter.LotResult.FAIL) {
            return FeedEvent.AnalysisLevel.DANGER;
        }
        if (linkedResult == FeedAdapter.LotResult.MARGINAL) {
            return FeedEvent.AnalysisLevel.WARNING;
        }
        if (linkedResult == FeedAdapter.LotResult.PASS) {
            return FeedEvent.AnalysisLevel.NORMAL;
        }
        return FeedEvent.AnalysisLevel.WARNING;
    }

    private FeedAdapter.LotResult computeLotResult(int totalUnits, int failCount, float yieldPct) {
        int total = Math.max(1, totalUnits);
        int fail = Math.max(0, failCount);
        float yield = yieldPct;
        if (yield <= 0f && total > 0) {
            yield = ((total - fail) * 100f) / total;
        }
        if (fail <= 0) return FeedAdapter.LotResult.PASS;
        if (yield >= 95f) return FeedAdapter.LotResult.MARGINAL;
        return FeedAdapter.LotResult.FAIL;
    }

    private void rememberLotResult(String equipmentId, String lotId, FeedAdapter.LotResult result) {
        if (equipmentId == null || equipmentId.trim().isEmpty() || result == null) return;
        latestLotResultByEquipment.put(equipmentId, result);
        if (lotId != null && !lotId.trim().isEmpty()) {
            latestLotResultByLotKey.put(equipmentId + "|" + lotId, result);
        }
    }

    private FeedAdapter.LotResult resolveLotResult(String equipmentId, String lotId) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) {
            return FeedAdapter.LotResult.NONE;
        }
        if (lotId != null && !lotId.trim().isEmpty()) {
            FeedAdapter.LotResult byLot = latestLotResultByLotKey.get(equipmentId + "|" + lotId);
            if (byLot != null) return byLot;
        }
        FeedAdapter.LotResult byEq = latestLotResultByEquipment.get(equipmentId);
        return byEq == null ? FeedAdapter.LotResult.NONE : byEq;
    }

    private boolean rememberStatusAndShouldEmitHwAlarm(String equipmentId, String status) {
        if (equipmentId == null || equipmentId.trim().isEmpty()) return false;

        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        String previous = latestStatusByEquipment.put(equipmentId, normalized);
        if (!"STOP".equals(normalized)) return false;
        return previous == null || !"STOP".equals(previous);
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
            case FILTER_PASS:
                return "LOT 합격";
            case FILTER_MARGINAL:
                return "LOT 경계";
            case FILTER_FAIL:
                return "LOT 불합격";
            case FILTER_HW:
                return "HW 알람";
            default:
                return "전체 결과";
        }
    }

    private void openLotBottomSheet(FeedEvent lotEvent) {
        if (getActivity() == null || lotEvent == null) return;
        if (lotEvent.getEventType() != FeedEvent.EventType.LOT_END) return;

        SessionManager sessionManager = new SessionManager(getActivity());
        User user = sessionManager.getCurrentUser();

        FeedAdapter.LotResult lotResult = FeedAdapter.computeLotResult(lotEvent);
        InspectionEvent.Result result = toInspectionResult(lotResult);

        InspectionEvent inspectionEvent = new InspectionEvent.Builder(
                "feed-" + safe(lotEvent.getId()),
                safe(lotEvent.getLotId()),
                safe(lotEvent.getTime()),
                result
        )
                .equipmentId(safe(lotEvent.getEquipmentId()))
                .recipeId(safe(lotEvent.getRecipeId()))
                .operator(safe(lotEvent.getOperator()))
                .errorCodes(lotEvent.getErrorCodes() == null ? new String[0] : lotEvent.getErrorCodes())
                .build();

        FeedEvent oracleEvent = resolveLinkedOracleEvent(lotEvent);
        LotDetailBottomSheet sheet = LotDetailBottomSheet.newInstance(inspectionEvent, user, oracleEvent, lotEvent);
        sheet.show(getParentFragmentManager(), LotDetailBottomSheet.TAG);
    }

    private InspectionEvent.Result toInspectionResult(FeedAdapter.LotResult lotResult) {
        if (lotResult == FeedAdapter.LotResult.FAIL) return InspectionEvent.Result.FAIL;
        if (lotResult == FeedAdapter.LotResult.MARGINAL) return InspectionEvent.Result.MARGINAL;
        return InspectionEvent.Result.PASS;
    }

    private void rememberOracleEvent(FeedEvent oracleEvent) {
        if (oracleEvent == null) return;
        if (oracleEvent.getEventType() != FeedEvent.EventType.ORACLE_ANALYSIS) return;

        String eqId = safe(oracleEvent.getEquipmentId());
        if (!eqId.isEmpty()) {
            latestOracleByEquipment.put(eqId, oracleEvent);
        }

        String lotId = safe(oracleEvent.getLotId());
        if (!eqId.isEmpty() && !lotId.isEmpty()) {
            latestOracleByLotKey.put(eqId + "|" + lotId, oracleEvent);
        }
    }

    private FeedEvent resolveLinkedOracleEvent(FeedEvent lotEvent) {
        if (lotEvent == null) return null;
        String eqId = safe(lotEvent.getEquipmentId());
        String lotId = safe(lotEvent.getLotId());

        if (!eqId.isEmpty() && !lotId.isEmpty()) {
            FeedEvent byLot = latestOracleByLotKey.get(eqId + "|" + lotId);
            if (byLot != null) return byLot;
        }
        if (!eqId.isEmpty()) {
            return latestOracleByEquipment.get(eqId);
        }
        return null;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    public enum QuickFilter {
        PASS, MARGINAL, FAIL, HW
    }

    //대시보드 빠른 필터 추가
    public void applyDashboardQuickFilter(QuickFilter filter) {
        selectedLine = FILTER_ALL; // 전체 라인
        selectedResultFilters.clear();

        if (filter == QuickFilter.FAIL) {
            selectedResultFilters.add(FILTER_FAIL);
        } else if (filter == QuickFilter.MARGINAL) {
            selectedResultFilters.add(FILTER_MARGINAL);
        } else if (filter == QuickFilter.HW) {
            selectedResultFilters.add(FILTER_HW);
        } else {
            selectedResultFilters.add(FILTER_PASS);
        }

        renderFilters();
        applyFilters();
    }

    public void applyEquipmentQuickFilter(String equipmentId, QuickFilter filter) {
        selectedLine = (equipmentId == null || equipmentId.trim().isEmpty()) ? FILTER_ALL : equipmentId;
        selectedResultFilters.clear();

        if (filter == QuickFilter.FAIL) {
            selectedResultFilters.add(FILTER_FAIL);
        } else if (filter == QuickFilter.MARGINAL) {
            selectedResultFilters.add(FILTER_MARGINAL);
        } else if (filter == QuickFilter.HW) {
            selectedResultFilters.add(FILTER_HW);
        } else {
            selectedResultFilters.add(FILTER_PASS);
        }

        renderFilters();
        applyFilters();
    }
}
