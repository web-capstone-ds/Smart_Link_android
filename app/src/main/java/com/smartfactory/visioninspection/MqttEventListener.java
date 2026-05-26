package com.smartfactory.visioninspection;

public interface MqttEventListener {
    void onStatusUpdateReceived(StatusUpdateEvent event);
    void onInspectionResultReceived(String topic, String message);
    void onMqttEventReceived(String equipmentId, String eventType, String payload);
    void onMqttConnectionChanged(boolean connected);
}
