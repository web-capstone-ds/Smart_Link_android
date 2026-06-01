package com.smartfactory.visioninspection.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.smartfactory.visioninspection.models.FeedEvent;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

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
    private static final String KEY_COUNTER_DATE = "dashboard_counter_date"; // yyyy-MM-dd (KST)
    private static final String KEY_FEED_RECORDS = "feed_records_json";
    private static final int MAX_FEED_HISTORY = 120;

    private static final String TZ_KST = "Asia/Seoul";

    private final SharedPreferences pref;
    private final Gson gson = new Gson();

    public EventHistoryStore(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 날짜 롤오버 체크.
     * - targetDate > savedDate: 카운터 초기화 + 새 날짜로 전환
     * - targetDate == savedDate: 유지
     * - targetDate < savedDate: 지연 이벤트로 간주, 무시(롤백 방지)
     *
     * @return true: 카운터 리셋 발생, false: 변화 없음
     */
    public synchronized boolean ensureCounterDate(@Nullable String lotTimestampIso) {
        String targetDate = resolveOperationalDate(lotTimestampIso);
        String savedDate = pref.getString(KEY_COUNTER_DATE, null);

        if (savedDate == null || savedDate.trim().isEmpty()) {
            pref.edit().putString(KEY_COUNTER_DATE, targetDate).apply();
            return false;
        }

        int cmp = targetDate.compareTo(savedDate);
        if (cmp > 0) {
            resetCountersForNewDay(targetDate);
            return true;
        }

        // cmp == 0: 같은 날 / cmp < 0: 과거 지연 이벤트 -> 무시
        return false;
    }

    public synchronized DashboardCounters loadDashboardCounters() {
        ensureCounterDate(null); // 앱 시작/복귀 시 오늘 기준 자동 보정

        DashboardCounters counters = new DashboardCounters();
        counters.fail = pref.getInt(KEY_FAIL, 0);
        counters.marginal = pref.getInt(KEY_MARGINAL, 0);
        counters.pass = pref.getInt(KEY_PASS, 0);

        Set<String> set = pref.getStringSet(KEY_LOT_KEYS, null);
        if (set != null) counters.countedLotKeys = new HashSet<>(set);
        return counters;
    }

    public synchronized void saveDashboardCounters(int fail, int marginal, int pass, Set<String> lotKeys) {
        // 저장 시점에도 날짜 필드가 없으면 생성
        ensureCounterDate(null);

        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(KEY_FAIL, fail);
        editor.putInt(KEY_MARGINAL, marginal);
        editor.putInt(KEY_PASS, pass);
        editor.putStringSet(KEY_LOT_KEYS, new HashSet<>(lotKeys));
        editor.apply();
    }

    private void resetCountersForNewDay(String newDate) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(KEY_FAIL, 0);
        editor.putInt(KEY_MARGINAL, 0);
        editor.putInt(KEY_PASS, 0);
        editor.putStringSet(KEY_LOT_KEYS, new HashSet<>());
        editor.putString(KEY_COUNTER_DATE, newDate);
        editor.apply();
    }

    private String resolveOperationalDate(@Nullable String lotTimestampIso) {
        Date parsed = parseIsoDate(lotTimestampIso);
        if (parsed == null) parsed = new Date();
        return formatKstDate(parsed);
    }

    private String formatKstDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        sdf.setTimeZone(TimeZone.getTimeZone(TZ_KST));
        return sdf.format(date);
    }

    @Nullable
    private Date parseIsoDate(@Nullable String iso) {
        if (iso == null || iso.trim().isEmpty()) return null;

        String value = iso.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                sdf.setLenient(false);
                if (p.contains("'Z'")) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                Date d = sdf.parse(value);
                if (d != null) return d;
            } catch (Exception ignore) {
            }
        }
        return null;
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