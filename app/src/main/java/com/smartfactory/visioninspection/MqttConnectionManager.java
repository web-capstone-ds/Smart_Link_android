package com.smartfactory.visioninspection;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

public final class MqttConnectionManager {
    private static final String TAG = "MQTT_SPEC_MANAGER";

    private static final String BROKER_URL = BuildConfig.MQTT_BROKER_URL;
    private static final String USERNAME = "mobile_app";
    private static final String PASSWORD = "testpass123";
    private static final String SUB_TOPIC_ALL = "ds/#";

    private static volatile MqttConnectionManager instance;

    private final Object lock = new Object();
    private final Gson gson = new Gson();

    private volatile MqttClient mqttClient;
    private volatile MqttEventListener listener;
    private volatile boolean connecting = false;
    private volatile boolean connected = false;
    private volatile String activeClientId = "";

    private MqttConnectionManager() {}

    public static MqttConnectionManager getInstance() {
        if (instance == null) {
            synchronized (MqttConnectionManager.class) {
                if (instance == null) {
                    instance = new MqttConnectionManager();
                }
            }
        }
        return instance;
    }

    public void setMqttEventListener(MqttEventListener listener) {
        this.listener = listener;
    }

    public void clearMqttEventListener(MqttEventListener target) {
        if (this.listener == target) {
            this.listener = null;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public void initAndConnect(String clientId) {
        if (!isBrokerConfigured()) {
            connected = false;
            connecting = false;
            Log.w(TAG, "MQTT broker URL is not configured. MQTT connection skipped.");
            notifyConnectionChanged(false);
            return;
        }

        synchronized (lock) {
            if (mqttClient != null && mqttClient.isConnected()) {
                connected = true;
                connecting = false;
                Log.d(TAG, "이미 MQTT 연결 상태 유지: clientId=" + activeClientId);
                notifyConnectionChanged(true);
                return;
            }

            if (connecting) {
                Log.d(TAG, "이미 MQTT 연결 시도 중: clientId=" + activeClientId);
                return;
            }

            connecting = true;
            connected = false;
            activeClientId = clientId;

            try {
                if (mqttClient != null) {
                    safeCloseClient(mqttClient);
                }

                mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());
                mqttClient.setCallback(createCallback());
            } catch (MqttException e) {
                connecting = false;
                connected = false;
                Log.e(TAG, "MQTT 클라이언트 생성 실패", e);
                notifyConnectionChanged(false);
                return;
            }
        }

        new Thread(() -> {
            try {
                MqttConnectionOptions options = new MqttConnectionOptions();
                options.setUserName(USERNAME);
                options.setPassword(PASSWORD.getBytes());
                options.setCleanStart(false);
                options.setAutomaticReconnect(true);
                options.setKeepAliveInterval(30);
                options.setSessionExpiryInterval(3600L);

                MqttClient clientRef = mqttClient;
                if (clientRef == null) {
                    throw new MqttException(new Throwable("mqttClient is null before connect"));
                }

                clientRef.connect(options);
                clientRef.subscribe(SUB_TOPIC_ALL, 1);

                synchronized (lock) {
                    connected = true;
                    connecting = false;
                }

                Log.d(TAG, "명세서 기준 연결 및 구독 성공, clientId=" + activeClientId);
                notifyConnectionChanged(true);
            } catch (Exception e) {
                synchronized (lock) {
                    connected = false;
                    connecting = false;
                }
                Log.e(TAG, "MQTT 연결 실패: " + e.getMessage(), e);
                notifyConnectionChanged(false);
            }
        }, "mqtt-connect-thread").start();
    }

    public void disconnectAndClose() {
        synchronized (lock) {
            connecting = false;
            connected = false;

            if (mqttClient != null) {
                safeCloseClient(mqttClient);
                mqttClient = null;
            }
        }
    }

    public void sendControlCommand(String eqId, String command) {
        String topic = "ds/" + eqId + "/control";
        try {
            MqttClient clientRef = mqttClient;
            if (clientRef == null || !clientRef.isConnected()) {
                Log.w(TAG, "제어 명령 무시: MQTT 미연결 상태");
                return;
            }

            MqttMessage message = new MqttMessage(command.getBytes());
            message.setQos(2);
            clientRef.publish(topic, message);
            Log.d(TAG, "제어 명령 전송: " + topic + " -> " + command);
        } catch (Exception e) {
            Log.e(TAG, "제어 전송 실패", e);
        }
    }

    private MqttCallback createCallback() {
        return new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                Log.d(TAG, "수신 토픽: " + topic + " | 내용: " + payload);

                String[] parts = topic.split("/");
                MqttEventListener localListener = listener;
                if (parts.length >= 3 && localListener != null) {
                    String eqId = parts[1];
                    String eventType = parts[2];
                    if (!"recommendation".equals(eventType) && isControlRecommendationPayload(payload)) {
                        eventType = "recommendation";
                    }

                    try {
                        localListener.onMqttEventReceived(eqId, eventType, payload);

                        if ("result".equals(eventType)) {
                            localListener.onInspectionResultReceived(topic, payload);
                        } else if ("status".equals(eventType)) {
                            StatusUpdateEvent event = gson.fromJson(payload, StatusUpdateEvent.class);
                            if (event == null) event = new StatusUpdateEvent();
                            event.setEquipmentId(eqId);
                            event.setEventType(eventType);
                            event.setRawPayload(payload);
                            localListener.onStatusUpdateReceived(event);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "데이터 분류/파싱 실패", e);
                    }
                }
            }

            @Override
            public void disconnected(MqttDisconnectResponse response) {
                synchronized (lock) {
                    connected = false;
                }
                String reason = response != null ? response.getReasonString() : "null";
                Log.e(TAG, "연결 종료: " + reason + " clientId=" + activeClientId);
                notifyConnectionChanged(false);
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {}

            @Override
            public void deliveryComplete(IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                synchronized (lock) {
                    connected = true;
                    connecting = false;
                }
                Log.d(TAG, "MQTT 연결 완료: " + serverURI + " reconnect=" + reconnect + " clientId=" + activeClientId);
                notifyConnectionChanged(true);
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        };
    }

    private boolean isControlRecommendationPayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) return false;
        try {
            JsonElement parsed = JsonParser.parseString(payload);
            if (parsed == null || !parsed.isJsonObject()) return false;
            JsonObject obj = parsed.getAsJsonObject();
            if (!obj.has("event_type") || obj.get("event_type").isJsonNull()) return false;
            return "CONTROL_RECOMMENDATION".equalsIgnoreCase(obj.get("event_type").getAsString());
        } catch (Exception ignore) {
            return false;
        }
    }

    private void notifyConnectionChanged(boolean isConnected) {
        MqttEventListener localListener = listener;
        if (localListener != null) {
            localListener.onMqttConnectionChanged(isConnected);
        }
    }

    private boolean isBrokerConfigured() {
        return BROKER_URL != null && !BROKER_URL.trim().isEmpty();
    }

    private void safeCloseClient(MqttClient client) {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception e) {
            Log.w(TAG, "disconnect 예외(무시)", e);
        }

        try {
            client.close();
        } catch (Exception e) {
            Log.w(TAG, "close 예외(무시)", e);
        }
    }
}
