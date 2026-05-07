package com.smartfactory.visioninspection.utils;

import com.smartfactory.visioninspection.models.*;
import java.util.*;

/**
 * 목업 데이터 생성 유틸 (mockData.ts + DetailBottomSheet 로직 통합)
 * 실제 환경에서는 MQTT / REST API 수신 데이터로 대체
 */
public class MockDataUtil {

    // ── 불량 코드 풀 ─────────────────────────────────────────
    private static final String[][] DEFECT_POOL = {
            {"CHIPPING_EXCEED", "엣지 치핑 초과"},
            {"OFFSET_X",        "X축 오프셋 이탈"},
            {"OFFSET_Y",        "Y축 오프셋 이탈"},
            {"SOLDER_VOID",     "솔더 보이드"},
            {"MISSING_BALL",    "볼 누락"},
            {"COPLANARITY",     "공면도 불량"},
            {"SCRATCH",         "표면 스크래치"},
    };

    // ── 오류 코드 → 설명 맵 ──────────────────────────────────
    private static final Map<String, String> ERROR_DESC_MAP;
    static {
        ERROR_DESC_MAP = new HashMap<>();
        for (String[] d : DEFECT_POOL) ERROR_DESC_MAP.put(d[0], d[1]);
    }

    public static String getErrorDescription(String code) {
        return ERROR_DESC_MAP.containsKey(code) ? ERROR_DESC_MAP.get(code) : code;
    }

    // ── 선형 합동 난수 (시드 기반 재현 가능) ─────────────────
    private static long seededNext(long[] s) {
        s[0] = (s[0] * 1664525L + 1013904223L) & 0xFFFFFFFFL;
        return s[0];
    }

    private static float seededFloat(long[] s) {
        return (float) (seededNext(s) / (double) 0xFFFFFFFFL);
    }

    // ── LOT 집계 생성 (DetailBottomSheet generateLotSummary 대응) ──
    public static LotSummary generateLotSummary(InspectionEvent event) {
        // 해시 시드 (LOT ID 기반 → 동일 LOT는 항상 같은 결과)
        long hash = 0;
        String lotId = event.getLotId() != null ? event.getLotId() : "LOT-DEFAULT";
        for (char c : lotId.toCharArray()) {
            hash = (hash * 31 + c) & 0xFFFFFFFFL;
        }
        long[] rng = {Math.abs(hash)};

        int totalUnits = (int) (2800 + seededFloat(rng) * 1400); // 2800~4200

        float baseFailRate;
        switch (event.getResult()) {
            case FAIL:     baseFailRate = 0.09f + seededFloat(rng) * 0.08f; break;
            case MARGINAL: baseFailRate = 0.03f + seededFloat(rng) * 0.04f; break;
            default:       baseFailRate = 0.004f + seededFloat(rng) * 0.012f; break;
        }

        int failUnits     = (int) (totalUnits * baseFailRate);
        int marginalUnits = (int) (totalUnits * baseFailRate * (0.3f + seededFloat(rng) * 0.3f));
        int passUnits     = totalUnits - failUnits - marginalUnits;
        float yieldRate   = (passUnits  / (float) totalUnits) * 100f;
        float failRate    = (failUnits  / (float) totalUnits) * 100f;

        // ── 주요 불량 유형 ────────────────────────────────────
        String[] codes = event.getErrorCodes();
        String[] primaryCodes;
        if (codes != null && codes.length > 0) {
            primaryCodes = Arrays.copyOfRange(codes, 0, Math.min(3, codes.length));
        } else {
            primaryCodes = new String[]{
                    DEFECT_POOL[(int) (seededFloat(rng) * DEFECT_POOL.length)][0]
            };
        }

        List<LotDefect> topDefects = new ArrayList<>();
        for (int i = 0; i < primaryCodes.length; i++) {
            String code    = primaryCodes[i];
            String[] found = DEFECT_POOL[0];
            for (String[] d : DEFECT_POOL) {
                if (d[0].equals(code)) { found = d; break; }
            }
            float ratio = i == 0 ? 35f + seededFloat(rng) * 25f
                    : i == 1 ? 15f + seededFloat(rng) * 20f
                    : 8f + seededFloat(rng) * 12f;
            int count = (int) (failUnits * ratio / 100f);
            topDefects.add(new LotDefect(found[0], found[1], count, ratio));
        }

        // ── Oracle 상태 ───────────────────────────────────────
        String oracleStatus;
        if      (yieldRate < 85f) oracleStatus = "DANGER";
        else if (yieldRate < 93f) oracleStatus = "WARNING";
        else                      oracleStatus = "NORMAL";

        // ── 알람 생성 ─────────────────────────────────────────
        String rawTime = event.getTime() != null ? event.getTime() : "08:00:00";
        String[] tp    = rawTime.split(":");
        int hh = Integer.parseInt(tp[0]);
        int mm = Integer.parseInt(tp[1]);
        int ss = Integer.parseInt(tp[2]);

        String defCode = topDefects.isEmpty() ? "UNKNOWN" : topDefects.get(0).getCode();
        String defDesc = topDefects.isEmpty() ? "알 수 없는 불량" : topDefects.get(0).getDescription();

        List<LotAlarm> alarms = new ArrayList<>();
        if (event.getResult() == InspectionEvent.Result.FAIL
                || event.getResult() == InspectionEvent.Result.MARGINAL) {
            alarms.add(new LotAlarm("a1", makeTime(hh, mm, ss, -280),
                    LotAlarm.Type.THRESHOLD,
                    event.getResult() == InspectionEvent.Result.FAIL
                            ? LotAlarm.Severity.CRITICAL : LotAlarm.Severity.WARNING,
                    "임계값 초과 — " + defCode,
                    defDesc + " 불량률이 허용 임계값을 초과하였습니다."));
        }
        if (event.getResult() == InspectionEvent.Result.FAIL) {
            alarms.add(new LotAlarm("a2", makeTime(hh, mm, ss, -140),
                    LotAlarm.Type.HW_ALARM, LotAlarm.Severity.CRITICAL,
                    "HW_ALARM — 비전 카메라 캘리브레이션",
                    "카메라 오프셋 드리프트 감지. 자동 재보정 시도 실패(에러 코드 CAM-0x3F)."));
        }

        LotAlarm.Severity oracleSev;
        if ("DANGER".equals(oracleStatus))       oracleSev = LotAlarm.Severity.CRITICAL;
        else if ("WARNING".equals(oracleStatus)) oracleSev = LotAlarm.Severity.WARNING;
        else                                     oracleSev = LotAlarm.Severity.INFO;

        String oracleDesc = "NORMAL".equals(oracleStatus)   ? "공정 데이터 정상. 수율 목표 달성 확인."
                : "WARNING".equals(oracleStatus)  ? "수율 하락 추세 감지. 파라미터 재검토 권장."
                : "수율 임계 미달. 즉각 점검 요청.";
        alarms.add(new LotAlarm("a3", makeTime(hh, mm, ss, -60),
                LotAlarm.Type.ORACLE, oracleSev,
                "Oracle 분석 — " + oracleStatus, oracleDesc));

        if (seededFloat(rng) > 0.5f) {
            alarms.add(new LotAlarm("a4", makeTime(hh, mm, ss, -20),
                    LotAlarm.Type.THRESHOLD, LotAlarm.Severity.INFO,
                    "레시피 파라미터 자동 보정",
                    "RCP-BGA-0.5mm 조명 강도 +2.3% 자동 보정 적용."));
        }

        return new LotSummary(
                lotId,
                event.getEquipmentId(),
                event.getRecipeId() != null ? event.getRecipeId() : "RCP-BGA-0.5mm",
                event.getOperator() != null ? event.getOperator() : "ENG-KIM",
                makeTime(hh, mm, ss, -1800),
                rawTime,
                totalUnits, passUnits, failUnits, marginalUnits,
                yieldRate, failRate,
                topDefects, oracleStatus, alarms, event.getResult());
    }

    // ── 시간 오프셋 계산 ─────────────────────────────────────
    private static String makeTime(int hh, int mm, int ss, int offsetSec) {
        int total = hh * 3600 + mm * 60 + ss + offsetSec;
        int th = ((total / 3600) % 24 + 24) % 24;
        int tm = (((total % 3600) / 60) + 60) % 60;
        int ts = ((total % 60) + 60) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", th, tm, ts);
    }

    // ── 목업 검사 이벤트 목록 (inspectionEvents 대응) ─────────
    public static List<InspectionEvent> getMockInspectionEvents() {
        List<InspectionEvent> list = new ArrayList<>();
        list.add(new InspectionEvent.Builder("evt-001", "LOT-20250711-042", "09:32:00", InspectionEvent.Result.FAIL)
                .unitId("UNIT-20250711-041").equipmentId("DS-VIS-001")
                .errorCodes("CHIPPING_EXCEED", "OFFSET_X").operator("ENG-KIM").build());
        list.add(new InspectionEvent.Builder("evt-002", "LOT-20250711-042", "09:31:45", InspectionEvent.Result.PASS)
                .unitId("UNIT-20250711-040").equipmentId("DS-VIS-001").operator("ENG-KIM").build());
        list.add(new InspectionEvent.Builder("evt-003", "LOT-20250711-042", "09:31:30", InspectionEvent.Result.MARGINAL)
                .unitId("UNIT-20250711-039").equipmentId("DS-VIS-001")
                .errorCodes("SOLDER_VOID").operator("ENG-KIM").build());
        list.add(new InspectionEvent.Builder("evt-004", "LOT-20250711-041", "09:31:15", InspectionEvent.Result.PASS)
                .unitId("UNIT-20250711-038").equipmentId("DS-VIS-002").operator("ENG-LEE").build());
        list.add(new InspectionEvent.Builder("evt-005", "LOT-20250711-041", "09:31:00", InspectionEvent.Result.PASS)
                .unitId("UNIT-20250711-037").equipmentId("DS-VIS-002").operator("ENG-LEE").build());
        list.add(new InspectionEvent.Builder("evt-006", "LOT-20250711-041", "09:30:45", InspectionEvent.Result.FAIL)
                .unitId("UNIT-20250711-036").equipmentId("DS-VIS-002")
                .errorCodes("MISSING_BALL").operator("ENG-LEE").build());
        list.add(new InspectionEvent.Builder("evt-007", "LOT-20250711-040", "09:30:30", InspectionEvent.Result.MARGINAL)
                .unitId("UNIT-20250711-035").equipmentId("DS-VIS-003")
                .errorCodes("COPLANARITY").operator("ENG-PARK").build());
        list.add(new InspectionEvent.Builder("evt-008", "LOT-20250711-040", "09:30:15", InspectionEvent.Result.PASS)
                .unitId("UNIT-20250711-034").equipmentId("DS-VIS-003").operator("ENG-PARK").build());
        list.add(new InspectionEvent.Builder("evt-009", "LOT-20250711-040", "09:30:00", InspectionEvent.Result.PASS)
                .unitId("UNIT-20250711-033").equipmentId("DS-VIS-003").operator("ENG-PARK").build());
        list.add(new InspectionEvent.Builder("evt-010", "LOT-20250711-040", "09:29:45", InspectionEvent.Result.FAIL)
                .unitId("UNIT-20250711-032").equipmentId("DS-VIS-003")
                .errorCodes("CHIPPING_EXCEED", "OFFSET_Y", "SCRATCH").operator("ENG-PARK").build());
        return list;
    }

    // ── 목업 피드 이벤트 목록 (initialFeedEvents 대응) ────────
    public static List<FeedEvent> getMockFeedEvents() {
        List<FeedEvent> list = new ArrayList<>();
        list.add(FeedEvent.hwAlarm("feed-001", "09:32:00", "DS-VIS-001",
                "AXIS_ENCODER_FAIL", FeedEvent.AlarmLevel.CRITICAL,
                "X축 엔코더 신호 오류 – 즉시 확인 필요", "burst-001"));
        list.add(FeedEvent.lotEnd("feed-002", "09:28:00", "DS-VIS-001",
                "LOT-20250711-041", 4000, 3856, 144, 96.4f, "ENG-KIM", "RCP-BGA-0.5mm"));
        list.add(FeedEvent.hwAlarm("feed-003", "09:25:30", "DS-VIS-002",
                "LIGHT_INTENSITY_DROP", FeedEvent.AlarmLevel.WARNING,
                "조명 광도 정상 범위 하한 접근", "burst-002"));
        list.add(FeedEvent.oracleAnalysis("feed-004", "09:20:15", "DS-VIS-003",
                FeedEvent.AnalysisLevel.WARNING,
                "솔더 보이드 발생 패턴 증가 추세 감지",
                new String[]{"SOLDER_VOID"}));
        list.add(FeedEvent.lotEnd("feed-005", "09:15:00", "DS-VIS-002",
                "LOT-20250711-040", 3500, 3395, 105, 97.0f, "ENG-LEE", "RCP-BGA-0.5mm"));
        list.add(FeedEvent.oracleAnalysis("feed-006", "09:10:30", "DS-VIS-004",
                FeedEvent.AnalysisLevel.DANGER,
                "치핑 불량률 임계값(3%) 초과 위험",
                new String[]{"CHIPPING_EXCEED", "OFFSET_X"}));
        list.add(FeedEvent.lotEnd("feed-007", "09:05:00", "DS-VIS-003",
                "LOT-20250711-039", 4000, 3920, 80, 98.0f, "ENG-PARK", "RCP-BGA-0.5mm"));
        list.add(FeedEvent.hwAlarm("feed-008", "09:01:45", "DS-VIS-005",
                "CONVEYOR_JAMMED", FeedEvent.AlarmLevel.CRITICAL,
                "컨베이어 잼 감지 – 즉시 확인 필요", "burst-003"));
        list.add(FeedEvent.lotEnd("feed-009", "08:55:00", "DS-VIS-006",
                "LOT-20250711-038", 2800, 2744, 56, 98.0f, "ENG-CHOI", "RCP-BGA-0.5mm"));
        list.add(FeedEvent.hwAlarm("feed-010", "08:50:00", "DS-VIS-007",
                "SERVO_OVERLOAD", FeedEvent.AlarmLevel.WARNING,
                "서보모터 과부하 감지", "burst-004"));
        return list;
    }

    // ── 장비 상태 목록 (10라인 DS-VIS-001~010) ────────────────
    public static List<String[]> getMockEquipmentList() {
        // [equipmentId, alias, status, yieldStr, lastCheck, currentUnit, totalUnit]
        List<String[]> list = new ArrayList<>();
        list.add(new String[]{"DS-VIS-001", "1라인 비전센서", "ERROR",   "72.3", "09:32", "2847", "4000"});
        list.add(new String[]{"DS-VIS-002", "2라인 비전센서", "RUNNING", "97.0", "09:25", "1923", "4000"});
        list.add(new String[]{"DS-VIS-003", "3라인 비전센서", "WARNING", "88.5", "09:30", "3421", "4000"});
        list.add(new String[]{"DS-VIS-004", "4라인 비전센서", "RUNNING", "98.1", "09:10",  "812", "4000"});
        list.add(new String[]{"DS-VIS-005", "5라인 비전센서", "ERROR",   "65.2", "09:01", "1540", "4000"});
        list.add(new String[]{"DS-VIS-006", "6라인 비전센서", "RUNNING", "99.0", "08:55", "2100", "4000"});
        list.add(new String[]{"DS-VIS-007", "7라인 비전센서", "WARNING", "91.3", "08:50",  "480", "4000"});
        list.add(new String[]{"DS-VIS-008", "8라인 비전센서", "RUNNING", "96.8", "08:40",  "960", "4000"});
        list.add(new String[]{"DS-VIS-009", "9라인 비전센서", "IDLE",    "—",   "—",       "0",  "4000"});
        list.add(new String[]{"DS-VIS-010", "10라인 비전센서","IDLE",    "—",   "—",       "0",  "4000"});
        return list;
    }
}
