package com.smartfactory.visioninspection.models;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

public class ThresholdProposal {
    private final String proposalId;
    private final String equipmentId;
    private final String recipeId;
    private final String ruleId;
    private final String metric;
    private final String currentWarning;
    private final String proposedWarning;
    private final String basis;
    private final int lotBasis;
    private final String status;
    private final String processedBy;
    private final String processedAt;
    private final String reason;

    public ThresholdProposal(String proposalId,
                             String equipmentId,
                             String recipeId,
                             String ruleId,
                             String metric,
                             String currentWarning,
                             String proposedWarning,
                             String basis,
                             int lotBasis,
                             String status,
                             String processedBy,
                             String processedAt,
                             String reason) {
        this.proposalId = proposalId;
        this.equipmentId = equipmentId;
        this.recipeId = recipeId;
        this.ruleId = ruleId;
        this.metric = metric;
        this.currentWarning = currentWarning;
        this.proposedWarning = proposedWarning;
        this.basis = basis;
        this.lotBasis = lotBasis;
        this.status = status == null || status.trim().isEmpty() ? "PENDING" : status.trim().toUpperCase(Locale.ROOT);
        this.processedBy = processedBy;
        this.processedAt = processedAt;
        this.reason = reason;
    }

    @Nullable
    public static ThresholdProposal fromOraclePayload(String equipmentId, JsonObject oraclePayload) {
        if (oraclePayload == null || !oraclePayload.has("threshold_proposal")) return null;
        JsonElement element = oraclePayload.get("threshold_proposal");
        if (element == null || element.isJsonNull() || !element.isJsonObject()) return null;

        JsonObject obj = element.getAsJsonObject();
        return new ThresholdProposal(
                optString(obj, "proposal_id", ""),
                optString(oraclePayload, "equipment_id", equipmentId),
                optString(obj, "recipe_id", "-"),
                optString(obj, "rule_id", "-"),
                optString(obj, "metric", "-"),
                optNumberString(obj, "current_warning"),
                optNumberString(obj, "proposed_warning"),
                optString(obj, "basis", ""),
                optInt(obj, "lot_basis"),
                optString(obj, "status", "PENDING"),
                optNullableString(obj, "processed_by"),
                optNullableString(obj, "processed_at"),
                optNullableString(obj, "reason")
        );
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }

    public String getProposalId() {
        return proposalId;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public String getStatus() {
        return status;
    }

    public String getBannerTitle() {
        return "[임계값 제안] " + safe(recipeId, "-") + " " + safe(ruleId, "-");
    }

    public String getBannerReason() {
        String current = currentWarning == null || currentWarning.isEmpty() ? "-" : currentWarning;
        String proposed = proposedWarning == null || proposedWarning.isEmpty() ? "-" : proposedWarning;
        String lots = lotBasis > 0 ? " · " + lotBasis + " LOT" : "";
        return "현재 " + current + " → 제안 " + proposed + lots;
    }

    public String getBannerDetail() {
        if (basis != null && !basis.trim().isEmpty()) return "근거: " + basis.trim();
        return "지표: " + safe(metric, "-");
    }

    public String getResultMessage() {
        if (isApproved()) {
            String by = processedBy == null || processedBy.trim().isEmpty() ? "" : " (승인자 " + processedBy.trim() + ")";
            return "임계값 제안이 승인되었습니다" + by;
        }
        if (isRejected()) {
            String suffix = reason == null || reason.trim().isEmpty() ? "" : " (사유 " + reason.trim() + ")";
            return "임계값 제안이 거부되었습니다" + suffix;
        }
        return "";
    }

    private static String optString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return fallback;
        String value = obj.get(key).getAsString();
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String optNullableString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        String value = obj.get(key).getAsString();
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static int optInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0;
        try {
            return obj.get(key).getAsInt();
        } catch (Exception ignore) {
            return 0;
        }
    }

    private static String optNumberString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        try {
            double value = obj.get(key).getAsDouble();
            if (Math.abs(value - Math.rint(value)) < 0.00001d) {
                return String.format(Locale.US, "%.0f", value);
            }
            return String.format(Locale.US, "%.1f", value);
        } catch (Exception ignore) {
            return optString(obj, key, "");
        }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
