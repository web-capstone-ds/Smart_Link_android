package com.smartfactory.visioninspection;

import com.google.gson.annotations.SerializedName;

public class StatusUpdateEvent {

    // 1. MQTT 토픽(주소)에서 추출할 정보
    private String equipmentId;
    private String eventType;
    private String rawPayload;

    // 2. 서버 JSON 데이터에서 추출할 정보 (회원님이 찾으신 훌륭한 코드!)
    @SerializedName("equipment_status")
    private String equipmentStatus;

    @SerializedName("lot_id")
    private String lotId;

    @SerializedName("recipe_id")
    private String recipeId;

    @SerializedName("current_unit_count")
    private Integer currentUnitCount;

    @SerializedName("expected_total_units")
    private Integer expectedTotalUnits;

    @SerializedName("current_yield_pct")
    private Double currentYieldPct;

    // --- 데이터를 담아주기 위한 Setter ---
    public void setEquipmentId(String equipmentId) { this.equipmentId = equipmentId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    // --- 데이터를 꺼내기 위한 Getter ---
    public String getEquipmentId() { return equipmentId; }
    public String getEventType() { return eventType; }
    public String getRawPayload() { return rawPayload; }

    // MainActivity 호환용 (기존 getStatus()를 장비 상태로 연결)
    public String getStatus() { return equipmentStatus != null ? equipmentStatus : eventType; }

    public String getLotId() { return lotId; }
    public String getRecipeId() { return recipeId; }
    public Integer getCurrentUnitCount() { return currentUnitCount; }
    public Integer getExpectedTotalUnits() { return expectedTotalUnits; }
    public Double getCurrentYieldPct() { return currentYieldPct; }
}