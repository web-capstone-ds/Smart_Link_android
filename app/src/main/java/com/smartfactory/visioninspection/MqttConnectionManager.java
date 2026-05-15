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
    private static final String PASSWORD = "mobile_user_v3"; // .env에 정의된 비번

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
            options.setCleanStart(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "수신 토픽: " + topic + " | 내용: " + payload);

                    String[] parts = topic.split("/");
                    if (parts.length >= 3 && listener != null) {
                        String eqId = parts[1];
                        String eventType = parts[2];

                        try {
                            // 💡 마법의 Gson 출동! JSON 글자를 StatusUpdateEvent 상자로 한 번에 변환!
                            Gson gson = new Gson();
                            StatusUpdateEvent event = gson.fromJson(payload, StatusUpdateEvent.class);

                            // 혹시 변환에 실패해 null이면 빈 상자로 만듦
                            if (event == null) event = new StatusUpdateEvent();

                            // 토픽에서 뽑아낸 정보도 상자에 마저 담아줍니다.
                            event.setEquipmentId(eqId);
                            event.setEventType(eventType);
                            event.setRawPayload(payload);

                            // 메인 화면으로 완성된 상자 던지기!
                            listener.onStatusUpdateReceived(event);

                        } catch (Exception e) {
                            Log.e(TAG, "JSON 파싱 실패! 데이터가 깨졌습니다.", e);
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