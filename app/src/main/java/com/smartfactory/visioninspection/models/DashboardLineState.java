package com.smartfactory.visioninspection.models;

public class DashboardLineState {

    public enum LineResult { FAIL, MARGINAL, PASS, IDLE }

    private String equipmentId;
    private int lineNo;
    private String lotId;
    private String recipeId;
    private String operatorId;
    private String timeText;
    private String eventMessage;
    private String lastErrorCode;
    private int currentUnits;
    private int expectedUnits;
    private LineResult result = LineResult.IDLE;
    private long updatedAt;

    public DashboardLineState(String equipmentId, int lineNo) {
        this.equipmentId = equipmentId;
        this.lineNo = lineNo;
    }

    public String getEquipmentId() { return equipmentId; }
    public int getLineNo() { return lineNo; }
    public String getLotId() { return lotId; }
    public String getRecipeId() { return recipeId; }
    public String getOperatorId() { return operatorId; }
    public String getTimeText() { return timeText; }
    public String getEventMessage() { return eventMessage; }
    public String getLastErrorCode() { return lastErrorCode; }
    public int getCurrentUnits() { return currentUnits; }
    public int getExpectedUnits() { return expectedUnits; }
    public LineResult getResult() { return result; }
    public long getUpdatedAt() { return updatedAt; }

    public void setLotId(String lotId) { this.lotId = lotId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public void setTimeText(String timeText) { this.timeText = timeText; }
    public void setEventMessage(String eventMessage) { this.eventMessage = eventMessage; }
    public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }
    public void setCurrentUnits(int currentUnits) { this.currentUnits = currentUnits; }
    public void setExpectedUnits(int expectedUnits) { this.expectedUnits = expectedUnits; }
    public void setResult(LineResult result) { this.result = result; }
    public void touch(long updatedAt) { this.updatedAt = updatedAt; }
}
