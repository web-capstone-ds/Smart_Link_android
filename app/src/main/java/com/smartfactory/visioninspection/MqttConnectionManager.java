package com.smartfactory.visioninspection;

import android.util.Log;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import com.google.gson.Gson;

public class MqttConnectionManager {
    private static final String TAG = "MQTT_SPEC_MANAGER";

    // 명세서 §3: 포트 1883, 망 분리 로컬 환경
    private static final String BROKER_URL = "tcp://10.0.2.2:1883";
    private static final String CLIENT_ID = "mobile_user_v3"; // 고유 ID 권장

    // 명세서 §4: 모바일 앱 전용 계정
    private static final String USERNAME = "mobile_app";
    private static final String PASSWORD = "testpass123"; // .env에 정의된 비번

    // 명세서 §5: 전체 구독 권한 (ds/#)
    private static final String SUB_TOPIC_ALL = "ds/#";

    private MqttClient mqttClient;
    private MqttEventListener listener;

    public void setMqttEventListener(MqttEventListener listener) {
        this.listener = listener;
    }

    public void initAndConnect() {
        try {
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            MqttConnectionOptions options = new MqttConnectionOptions();

            // 명세서 §4: ACL 인증 의무화
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.getBytes());
            options.setCleanStart(false);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "수신 토픽: " + topic + " | 내용: " + payload);

                    String[] parts = topic.split("/");
                    // 주파수가 ds/VIS-001/result 처럼 3조각 이상이고, MainActivity와 연결되어 있다면
                    if (parts.length >= 3 && listener != null) {
                        String eqId = parts[1];      // 예: VIS-001
                        String eventType = parts[2]; // 예: result 또는 status

                        try {
                            // ✉️ 1. 검사 결과(result) 데이터가 들어왔을 때
                            if ("result".equals(eventType)) {
                                // 방금 MainActivity에 만든 새 연락망으로 직행!
                                listener.onInspectionResultReceived(topic, payload);
                            }
                            // ⚙️ 2. 장비 상태(status) 데이터가 들어왔을 때
                            else if ("status".equals(eventType)) {
                                Gson gson = new Gson();
                                StatusUpdateEvent event = gson.fromJson(payload, StatusUpdateEvent.class);

                                if (event == null) event = new StatusUpdateEvent();
                                event.setEquipmentId(eqId);
                                event.setEventType(eventType);
                                event.setRawPayload(payload);

                                // 기존 상태 업데이트 연락망으로 직행!
                                listener.onStatusUpdateReceived(event);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "데이터 분류/파싱 실패!", e);
                        }
                    }
                }

                @Override
                public void disconnected(MqttDisconnectResponse response) {
                    Log.e(TAG, "연결 종료: " + response.getReasonString());
                }

                @Override
                public void mqttErrorOccurred(MqttException exception) {}
                @Override
                public void deliveryComplete(IMqttToken token) {}
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {}
                @Override
                public void authPacketArrived(int reasonCode, org.eclipse.paho.mqttv5.common.packet.MqttProperties properties) {}
            });

            new Thread(() -> {
                try {
                    mqttClient.connect(options);
                    // 명세서 §5: 모든 데이터 모니터링을 위해 ds/# 구독 (QoS 1)
                    mqttClient.subscribe(SUB_TOPIC_ALL, 1);
                    Log.d(TAG, "명세서 기준 연결 및 구독 성공");
                } catch (MqttException e) {
                    Log.e(TAG, "연결 실패: " + e.getMessage());
                }
            }).start();

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // 명세서 §5: 장비 제어 (CONTROL_CMD) - 모바일 앱 허용 작업
    public void sendControlCommand(String eqId, String command) {
        String topic = "ds/" + eqId + "/control";
        try {
            MqttMessage message = new MqttMessage(command.getBytes());
            // 명세서 §5: 제어 명령은 QoS 2 (정확히 한 번 전달)
            message.setQos(2);
            mqttClient.publish(topic, message);
            Log.d(TAG, "제어 명령 전송: " + topic + " -> " + command);
        } catch (MqttException e) {
            Log.e(TAG, "제어 실패 (ACL 권한 확인 필요): " + e.getMessage());
        }
    }
}