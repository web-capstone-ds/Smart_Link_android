package com.smartfactory.visioninspection;

import com.google.gson.annotations.SerializedName;

/**
 * 기획서 5.1: 모든 이벤트가 공유하는 공통 헤더
 */
public class BaseEvent {
    @SerializedName("message_id")
    private String messageId;

    @SerializedName("event_type")
    private String eventType;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("equipment_id")
    private String equipmentId;

    // Getter 메서드
    public String getMessageId() { return messageId; }
    public String getEventType() { return eventType; }
    public String getTimestamp() { return timestamp; }
    public String getEquipmentId() { return equipmentId; }
}