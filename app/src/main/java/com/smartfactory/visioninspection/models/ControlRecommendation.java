package com.smartfactory.visioninspection.models;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ControlRecommendation {
    private final String recommendationId;
    private final String equipmentId;
    private final String rule;
    private final String severity;
    private final String reason;
    private final String lotId;
    private final String timestamp;
    private final String status;
    private final List<String> suggestedActions;

    public ControlRecommendation(String recommendationId,
                                 String equipmentId,
                                 String rule,
                                 String severity,
                                 List<String> suggestedActions,
                                 String reason,
                                 String lotId,
                                 String timestamp,
                                 String status) {
        this.recommendationId = recommendationId;
        this.equipmentId = equipmentId;
        this.rule = rule;
        this.severity = severity;
        this.suggestedActions = suggestedActions == null ? new ArrayList<>() : suggestedActions;
        this.reason = reason;
        this.lotId = lotId;
        this.timestamp = timestamp;
        this.status = status;
    }

    @Nullable
    public static ControlRecommendation fromPayload(String topicEquipmentId, String payload) {
        if (payload == null || payload.trim().isEmpty()) return null;

        JsonElement parsed = JsonParser.parseString(payload);
        if (parsed == null || !parsed.isJsonObject()) return null;

        JsonObject obj = parsed.getAsJsonObject();
        String eventType = optString(obj, "event_type", "");
        if (!"CONTROL_RECOMMENDATION".equalsIgnoreCase(eventType)) return null;

        String equipmentId = optString(obj, "equipment_id", topicEquipmentId);
        return new ControlRecommendation(
                optString(obj, "recommendation_id", ""),
                equipmentId,
                optString(obj, "rule", ""),
                optString(obj, "severity", "INFO"),
                parseActions(obj.get("suggested_actions")),
                optString(obj, "reason", ""),
                optNullableString(obj, "lot_id"),
                optString(obj, "timestamp", ""),
                optString(obj, "status", "OPEN")
        );
    }

    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(status);
    }

    public boolean isCritical() {
        return "CRITICAL".equalsIgnoreCase(severity);
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public String getRule() {
        return rule;
    }

    public String getSeverity() {
        return severity;
    }

    public String getReason() {
        return reason;
    }

    public String getLotId() {
        return lotId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public String getSuggestedActionLabel() {
        if (suggestedActions.isEmpty()) return "권고 조치: 확인 필요";

        List<String> labels = new ArrayList<>();
        for (String action : suggestedActions) {
            String label = toActionLabel(action);
            if (!label.isEmpty()) labels.add(label);
        }

        if (labels.isEmpty()) return "권고 조치: 확인 필요";
        return "권고 조치: " + join(labels);
    }

    public String getBannerTitle() {
        String ruleLabel = rule == null || rule.trim().isEmpty()
                ? "제어 추천"
                : "제어 추천 " + rule.trim().toUpperCase(Locale.ROOT);
        String severityLabel = severity == null || severity.trim().isEmpty()
                ? "INFO"
                : severity.trim().toUpperCase(Locale.ROOT);
        return "[권고] " + ruleLabel + " · " + severityLabel;
    }

    private static List<String> parseActions(JsonElement element) {
        List<String> out = new ArrayList<>();
        if (element == null || element.isJsonNull()) return out;

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                if (item != null && !item.isJsonNull()) {
                    out.add(item.getAsString());
                }
            }
            return out;
        }

        out.add(element.getAsString());
        return out;
    }

    private static String optString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        String value = obj.get(key).getAsString();
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private static String optNullableString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        String value = obj.get(key).getAsString();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String toActionLabel(String action) {
        if (action == null) return "";
        switch (action.trim().toUpperCase(Locale.ROOT)) {
            case "LOT_ABORT":
                return "LOT 중단";
            case "RECIPE_LOAD":
                return "레시피 재로드";
            case "EMERGENCY_STOP":
                return "비상 정지";
            case "ALARM_CLEAR":
                return "알람 해제";
            case "ALARM_ACK":
                return "알람 확인";
            case "STATUS_QUERY":
                return "상태 조회";
            default:
                return action.trim();
        }
    }

    private static String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(values.get(i));
        }
        return sb.toString();
    }
}
