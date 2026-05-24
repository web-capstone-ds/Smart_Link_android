package com.smartfactory.visioninspection;

public interface MqttEventListener {
    // 기획서 5.3: STATUS_UPDATE 수신 시 호출될 메서드[cite: 1]
    void onStatusUpdateReceived(StatusUpdateEvent event);

    // (나중에 알람이나 Lot 완료 DTO를 만들면 여기에 추가합니다)
    //MES에서 오는 검사 결과를 받는 새로운 연락망
    void onInspectionResultReceived(String topic, String message);
}