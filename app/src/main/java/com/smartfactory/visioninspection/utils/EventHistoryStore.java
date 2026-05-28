package com.smartfactory.visioninspection.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfactory.visioninspection.models.FeedEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EventHistoryStore {

    public static class DashboardCounters {
        public int fail;
        public int marginal;
        public int pass;
        public Set<String> countedLotKeys = new HashSet<>();
    }

    private static final String PREF_NAME = "SmartFactoryHistory";
    private static final String KEY_FAIL = "dashboard_fail";
    private static final String KEY_MARGINAL = "dashboard_marginal";
    private static final String KEY_PASS = "dashboard_pass";
    private static final String KEY_LOT_KEYS = "dashboard_lot_keys";
    private static final String KEY_FEED_RECORDS = "feed_records_json";
    private static final int MAX_FEED_HISTORY = 120;

    private final SharedPreferences pref;
    private final Gson gson = new Gson();

    public EventHistoryStore(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized DashboardCounters loadDashboardCounters() {
        DashboardCounters counters = new DashboardCounters();
        counters.fail = pref.getInt(KEY_FAIL, 0);
        counters.marginal = pref.getInt(KEY_MARGINAL, 0);
        counters.pass = pref.getInt(KEY_PASS, 0);

        Set<String> set = pref.getStringSet(KEY_LOT_KEYS, null);
        if (set != null) counters.countedLotKeys = new HashSet<>(set);
        return counters;
    }

    public synchronized void saveDashboardCounters(int fail, int marginal, int pass, Set<String> lotKeys) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(KEY_FAIL, fail);
        editor.putInt(KEY_MARGINAL, marginal);
        editor.putInt(KEY_PASS, pass);
        editor.putStringSet(KEY_LOT_KEYS, new HashSet<>(lotKeys));
        editor.apply();
    }

    public synchronized List<FeedEvent> loadFeedEvents() {
        String json = pref.getString(KEY_FEED_RECORDS, null);
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();

        Type type = new TypeToken<List<FeedEventRecord>>() {}.getType();
        List<FeedEventRecord> records = gson.fromJson(json, type);
        if (records == null) return new ArrayList<>();

        List<FeedEvent> out = new ArrayList<>();
        for (FeedEventRecord r : records) {
            FeedEvent ev = fromRecord(r);
            if (ev != null) out.add(ev);
        }
        return out;
    }

    public synchronized void appendFeedEvent(FeedEvent event) {
        if (event == null) return;
        List<FeedEvent> list = loadFeedEvents();

        String eventId = event.getId();
        if (eventId != null && !eventId.trim().isEmpty()) {
            for (FeedEvent old : list) {
                if (eventId.equals(old.getId())) {
                    return;
                }
            }
        }

        list.add(0, event);
        if (list.size() > MAX_FEED_HISTORY) {
            list = new ArrayList<>(list.subList(0, MAX_FEED_HISTORY));
        }
        saveFeedEvents(list);
    }

    public synchronized void saveFeedEvents(List<FeedEvent> events) {
        List<FeedEventRecord> records = new ArrayList<>();
        if (events != null) {
            for (FeedEvent e : events) {
                records.add(toRecord(e));
            }
        }
        pref.edit().putString(KEY_FEED_RECORDS, gson.toJson(records)).apply();
    }

    private FeedEventRecord toRecord(FeedEvent e) {
        FeedEventRecord r = new FeedEventRecord();
        r.id = e.getId();
        r.time = e.getTime();
        r.equipmentId = e.getEquipmentId();
        r.eventType = e.getEventType() != null ? e.getEventType().name() : "";
        r.lotId = e.getLotId();
        r.totalUnits = e.getTotalUnits();
        r.passUnits = e.getPassUnits();
        r.failUnits = e.getFailUnits();
        r.yieldRate = e.getYieldRate();
        r.operator = e.getOperator();
        r.recipeId = e.getRecipeId();
        r.alarmCode = e.getAlarmCode();
        r.alarmLevel = e.getAlarmLevel() != null ? e.getAlarmLevel().name() : "";
        r.alarmDescription = e.getAlarmDescription();
        r.burstId = e.getBurstId();
        r.analysisLevel = e.getAnalysisLevel() != null ? e.getAnalysisLevel().name() : "";
        r.analysisMessage = e.getAnalysisMessage();
        r.errorCodes = e.getErrorCodes() == null ? null : Arrays.copyOf(e.getErrorCodes(), e.getErrorCodes().length);
        return r;
    }

    private FeedEvent fromRecord(FeedEventRecord r) {
        if (r == null || r.eventType == null) return null;
        try {
            FeedEvent.EventType type = FeedEvent.EventType.valueOf(r.eventType);
            switch (type) {
                case LOT_END:
                    return FeedEvent.lotEnd(
                            safe(r.id, "feed-unknown"),
                            safe(r.time, "--:--:--"),
                            safe(r.equipmentId, "UNKNOWN"),
                            safe(r.lotId, "LOT-UNKNOWN"),
                            r.totalUnits,
                            r.passUnits,
                            r.failUnits,
                            r.yieldRate,
                            safe(r.operator, "-"),
                            safe(r.recipeId, "-")
                    );
                case HW_ALARM:
                    FeedEvent.AlarmLevel level = "CRITICAL".equalsIgnoreCase(r.alarmLevel)
                            ? FeedEvent.AlarmLevel.CRITICAL
                            : FeedEvent.AlarmLevel.WARNING;
                    return FeedEvent.hwAlarm(
                            safe(r.id, "feed-unknown"),
                            safe(r.time, "--:--:--"),
                            safe(r.equipmentId, "UNKNOWN"),
                            safe(r.alarmCode, "UNKNOWN_ALARM"),
                            level,
                            safe(r.alarmDescription, "-"),
                            safe(r.burstId, "-")
                    );
                case ORACLE_ANALYSIS:
                    FeedEvent.AnalysisLevel aLevel;
                    if ("DANGER".equalsIgnoreCase(r.analysisLevel)) {
                        aLevel = FeedEvent.AnalysisLevel.DANGER;
                    } else if ("NORMAL".equalsIgnoreCase(r.analysisLevel)) {
                        aLevel = FeedEvent.AnalysisLevel.NORMAL;
                    } else {
                        aLevel = FeedEvent.AnalysisLevel.WARNING;
                    }
                    return FeedEvent.oracleAnalysis(
                            safe(r.id, "feed-unknown"),
                            safe(r.time, "--:--:--"),
                            safe(r.equipmentId, "UNKNOWN"),
                            safe(r.lotId, ""),
                            aLevel,
                            safe(r.analysisMessage, "-"),
                            r.errorCodes == null ? new String[0] : r.errorCodes
                    );
                default:
                    return null;
            }
        } catch (Exception ignore) {
            return null;
        }
    }

    private String safe(String v, String fallback) {
        return (v == null || v.trim().isEmpty()) ? fallback : v;
    }

    private static class FeedEventRecord {
        String id;
        String time;
        String equipmentId;
        String eventType;
        String lotId;
        int totalUnits;
        int passUnits;
        int failUnits;
        float yieldRate;
        String operator;
        String recipeId;
        String alarmCode;
        String alarmLevel;
        String alarmDescription;
        String burstId;
        String analysisLevel;
        String analysisMessage;
        String[] errorCodes;
    }
}
