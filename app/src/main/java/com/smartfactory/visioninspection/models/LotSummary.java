package com.smartfactory.visioninspection.models;

import java.util.List;

/**
 * LOT 집계 요약 (DetailBottomSheet generateLotSummary 대응)
 * 수율 현황 탭 + 로트 알람 탭 데이터를 모두 담음
 */
public class LotSummary {

    private final String              lotId;
    private final String              equipmentId;
    private final String              recipeId;
    private final String              operator;
    private final String              startTime;
    private final String              endTime;
    private final int                 totalUnits;
    private final int                 passUnits;
    private final int                 failUnits;
    private final int                 marginalUnits;
    private final float               yieldRate;
    private final float               failRate;
    private final List<LotDefect>     topDefects;
    private final String              oracleStatus;  // NORMAL / WARNING / DANGER
    private final List<LotAlarm>      alarms;
    private final InspectionEvent.Result result;

    public LotSummary(String lotId, String equipmentId, String recipeId, String operator,
                      String startTime, String endTime,
                      int totalUnits, int passUnits, int failUnits, int marginalUnits,
                      float yieldRate, float failRate,
                      List<LotDefect> topDefects, String oracleStatus,
                      List<LotAlarm> alarms, InspectionEvent.Result result) {
        this.lotId         = lotId;
        this.equipmentId   = equipmentId;
        this.recipeId      = recipeId;
        this.operator      = operator;
        this.startTime     = startTime;
        this.endTime       = endTime;
        this.totalUnits    = totalUnits;
        this.passUnits     = passUnits;
        this.failUnits     = failUnits;
        this.marginalUnits = marginalUnits;
        this.yieldRate     = yieldRate;
        this.failRate      = failRate;
        this.topDefects    = topDefects;
        this.oracleStatus  = oracleStatus;
        this.alarms        = alarms;
        this.result        = result;
    }

    public String              getLotId()         { return lotId; }
    public String              getEquipmentId()   { return equipmentId; }
    public String              getRecipeId()      { return recipeId; }
    public String              getOperator()      { return operator; }
    public String              getStartTime()     { return startTime; }
    public String              getEndTime()       { return endTime; }
    public int                 getTotalUnits()    { return totalUnits; }
    public int                 getPassUnits()     { return passUnits; }
    public int                 getFailUnits()     { return failUnits; }
    public int                 getMarginalUnits() { return marginalUnits; }
    public float               getYieldRate()     { return yieldRate; }
    public float               getFailRate()      { return failRate; }
    public List<LotDefect>     getTopDefects()    { return topDefects; }
    public String              getOracleStatus()  { return oracleStatus; }
    public List<LotAlarm>      getAlarms()        { return alarms; }
    public InspectionEvent.Result getResult()     { return result; }
}
