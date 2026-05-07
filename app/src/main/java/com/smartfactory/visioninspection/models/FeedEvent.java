package com.smartfactory.visioninspection.models;

/**
 * 피드 이벤트 (types.ts FeedEvent 대응)
 * LOT_END / HW_ALARM / ORACLE_ANALYSIS 토픽에서 수신
 */
public class FeedEvent {

    public enum EventType { LOT_END, HW_ALARM, ORACLE_ANALYSIS }
    public enum AlarmLevel { CRITICAL, WARNING }
    public enum AnalysisLevel { DANGER, WARNING }

    // ── 공통 필드 ──
    private String    id;
    private String    time;
    private String    equipmentId;
    private EventType eventType;

    // ── LOT_END 필드 ──
    private String  lotId;
    private int     totalUnits;
    private int     passUnits;
    private int     failUnits;
    private float   yieldRate;
    private String  operator;
    private String  recipeId;

    // ── HW_ALARM 필드 ──
    private String     alarmCode;
    private AlarmLevel alarmLevel;
    private String     alarmDescription;
    private String     burstId;

    // ── ORACLE_ANALYSIS 필드 ──
    private AnalysisLevel analysisLevel;
    private String        analysisMessage;
    private String[]      errorCodes;

    private FeedEvent() {}

    // ── 팩토리 메서드: LOT_END ──────────────────────────────
    public static FeedEvent lotEnd(String id, String time, String equipmentId,
                                   String lotId, int totalUnits, int passUnits,
                                   int failUnits, float yieldRate,
                                   String operator, String recipeId) {
        FeedEvent e = new FeedEvent();
        e.id = id; e.time = time; e.equipmentId = equipmentId;
        e.eventType = EventType.LOT_END;
        e.lotId = lotId; e.totalUnits = totalUnits;
        e.passUnits = passUnits; e.failUnits = failUnits;
        e.yieldRate = yieldRate; e.operator = operator; e.recipeId = recipeId;
        return e;
    }

    // ── 팩토리 메서드: HW_ALARM ─────────────────────────────
    public static FeedEvent hwAlarm(String id, String time, String equipmentId,
                                    String alarmCode, AlarmLevel alarmLevel,
                                    String alarmDescription, String burstId) {
        FeedEvent e = new FeedEvent();
        e.id = id; e.time = time; e.equipmentId = equipmentId;
        e.eventType = EventType.HW_ALARM;
        e.alarmCode = alarmCode; e.alarmLevel = alarmLevel;
        e.alarmDescription = alarmDescription; e.burstId = burstId;
        return e;
    }

    // ── 팩토리 메서드: ORACLE_ANALYSIS ──────────────────────
    public static FeedEvent oracleAnalysis(String id, String time, String equipmentId,
                                           AnalysisLevel analysisLevel,
                                           String analysisMessage, String[] errorCodes) {
        FeedEvent e = new FeedEvent();
        e.id = id; e.time = time; e.equipmentId = equipmentId;
        e.eventType = EventType.ORACLE_ANALYSIS;
        e.analysisLevel = analysisLevel;
        e.analysisMessage = analysisMessage;
        e.errorCodes = errorCodes;
        return e;
    }

    // ── Getters ─────────────────────────────────────────────
    public String        getId()              { return id; }
    public String        getTime()            { return time; }
    public String        getEquipmentId()     { return equipmentId; }
    public EventType     getEventType()       { return eventType; }
    public String        getLotId()           { return lotId; }
    public int           getTotalUnits()      { return totalUnits; }
    public int           getPassUnits()       { return passUnits; }
    public int           getFailUnits()       { return failUnits; }
    public float         getYieldRate()       { return yieldRate; }
    public String        getOperator()        { return operator; }
    public String        getRecipeId()        { return recipeId; }
    public String        getAlarmCode()       { return alarmCode; }
    public AlarmLevel    getAlarmLevel()      { return alarmLevel; }
    public String        getAlarmDescription(){ return alarmDescription; }
    public String        getBurstId()         { return burstId; }
    public AnalysisLevel getAnalysisLevel()   { return analysisLevel; }
    public String        getAnalysisMessage() { return analysisMessage; }
    public String[]      getErrorCodes()      { return errorCodes; }
}
