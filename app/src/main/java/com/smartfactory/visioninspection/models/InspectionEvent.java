package com.smartfactory.visioninspection.models;

/**
 * 개별 유닛 검사 결과 이벤트 (types.ts InspectionEvent 대응)
 * INSPECTION_RESULT 토픽에서 수신
 */
public class InspectionEvent {

    public enum Result { PASS, FAIL, MARGINAL }

    private String   id;
    private String   time;
    private String   unitId;
    private String   lotId;
    private String   equipmentId;
    private Result   result;
    private int      errorCount;
    private String[] errorCodes;
    private String   recipeId;
    private String   operator;

    // ── Builder ─────────────────────────────────────────────
    private InspectionEvent() {}

    public static class Builder {
        private String   id;
        private String   time;
        private String   unitId        = "";
        private String   lotId;
        private String   equipmentId   = "DS-VIS-001";
        private Result   result        = Result.PASS;
        private int      errorCount    = 0;
        private String[] errorCodes    = new String[0];
        private String   recipeId      = "RCP-BGA-0.5mm";
        private String   operator      = "ENG-KIM";

        public Builder(String id, String lotId, String time, Result result) {
            this.id     = id;
            this.lotId  = lotId;
            this.time   = time;
            this.result = result;
        }

        public Builder unitId(String v)      { this.unitId      = v; return this; }
        public Builder equipmentId(String v) { this.equipmentId = v; return this; }
        public Builder errorCount(int v)     { this.errorCount  = v; return this; }
        public Builder errorCodes(String... v) {
            this.errorCodes = (v != null) ? v : new String[0];
            this.errorCount = this.errorCodes.length;
            return this;
        }
        public Builder recipeId(String v)    { this.recipeId    = v; return this; }
        public Builder operator(String v)    { this.operator    = v; return this; }

        public InspectionEvent build() {
            InspectionEvent e = new InspectionEvent();
            e.id          = id;
            e.time        = time;
            e.unitId      = unitId;
            e.lotId       = lotId;
            e.equipmentId = equipmentId;
            e.result      = result;
            e.errorCount  = errorCount;
            e.errorCodes  = errorCodes;
            e.recipeId    = recipeId;
            e.operator    = operator;
            return e;
        }
    }

    // ── Getters ─────────────────────────────────────────────
    public String   getId()          { return id; }
    public String   getTime()        { return time; }
    public String   getUnitId()      { return unitId; }
    public String   getLotId()       { return lotId; }
    public String   getEquipmentId() { return equipmentId; }
    public Result   getResult()      { return result; }
    public int      getErrorCount()  { return errorCount; }
    public String[] getErrorCodes()  { return errorCodes; }
    public String   getRecipeId()    { return recipeId; }
    public String   getOperator()    { return operator; }
}
