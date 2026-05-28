package com.smartfactory.visioninspection.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.models.FeedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    public enum LotResult {
        PASS, MARGINAL, FAIL, NONE
    }

    private final List<FeedEvent> items = new ArrayList<>();

    public void submitList(List<FeedEvent> events) {
        items.clear();
        items.addAll(events);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static LotResult computeLotResult(FeedEvent event) {
        if (event == null || event.getEventType() != FeedEvent.EventType.LOT_END) {
            return LotResult.NONE;
        }

        int total = Math.max(1, event.getTotalUnits());
        int fail = Math.max(0, event.getFailUnits());
        float yield = event.getYieldRate();

        if (fail <= 0) return LotResult.PASS;
        if (yield >= 95f) return LotResult.MARGINAL;
        return LotResult.FAIL;
    }

    private static int parseLineNo(String equipmentId) {
        if (equipmentId == null) return 0;
        String digits = equipmentId.replaceAll("[^0-9]", "");
        if (digits.length() >= 3) {
            digits = digits.substring(digits.length() - 3);
        }
        try {
            return Integer.parseInt(digits);
        } catch (Exception ignore) {
            return 0;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final View dot;
        private final TextView tvBadge;
        private final TextView tvTime;
        private final TextView tvLine;
        private final TextView tvTitle;
        private final TextView tvBody;
        private final TextView tvMeta;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_feed_event);
            dot = itemView.findViewById(R.id.feed_dot);
            tvBadge = itemView.findViewById(R.id.tv_feed_badge);
            tvTime = itemView.findViewById(R.id.tv_feed_time);
            tvLine = itemView.findViewById(R.id.tv_feed_line);
            tvTitle = itemView.findViewById(R.id.tv_feed_title);
            tvBody = itemView.findViewById(R.id.tv_feed_body);
            tvMeta = itemView.findViewById(R.id.tv_feed_meta);
        }

        void bind(FeedEvent event) {
            int lineNo = parseLineNo(event.getEquipmentId());
            tvLine.setText(lineNo > 0 ? (lineNo + "라인 비전센서") : event.getEquipmentId());
            tvTime.setText(event.getTime());

            if (event.getEventType() == FeedEvent.EventType.LOT_END) {
                bindLot(event);
            } else if (event.getEventType() == FeedEvent.EventType.HW_ALARM) {
                bindAlarm(event);
            } else {
                bindOracle(event);
            }
        }

        private void bindLot(FeedEvent event) {
            LotResult result = computeLotResult(event);
            int textColor;
            int strokeColor;
            int cardBg;
            String badge;

            if (result == LotResult.FAIL) {
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.color_fail);
                strokeColor = textColor;
                cardBg = ContextCompat.getColor(itemView.getContext(), R.color.color_fail_bg);
                badge = "불합격";
            } else if (result == LotResult.MARGINAL) {
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.color_marginal);
                strokeColor = textColor;
                cardBg = ContextCompat.getColor(itemView.getContext(), R.color.color_marginal_bg);
                badge = "경계";
            } else {
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.color_pass);
                strokeColor = textColor;
                cardBg = ContextCompat.getColor(itemView.getContext(), R.color.color_pass_bg);
                badge = "합격";
            }

            card.setStrokeColor(strokeColor);
            card.setCardBackgroundColor(cardBg);
            dot.setBackgroundColor(textColor);

            tvBadge.setText("LOT 완료");
            tvBadge.setTextColor(textColor);
            tvTitle.setText("결과: " + badge + "  ·  " + safe(event.getLotId(), "LOT-UNKNOWN"));

            String body = String.format(Locale.getDefault(),
                    "수율 %.1f%%  (합격 %,d / 불합격 %,d / 전체 %,d)",
                    event.getYieldRate(),
                    event.getPassUnits(),
                    event.getFailUnits(),
                    Math.max(1, event.getTotalUnits()));
            tvBody.setText(body);
            tvMeta.setText("작업자 " + safe(event.getOperator(), "-") + "  ·  레시피 " + safe(event.getRecipeId(), "-"));
        }

        private void bindAlarm(FeedEvent event) {
            boolean critical = event.getAlarmLevel() == FeedEvent.AlarmLevel.CRITICAL;
            int color = ContextCompat.getColor(itemView.getContext(), critical ? R.color.color_fail : R.color.color_marginal);

            card.setStrokeColor(color);
            card.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), critical ? R.color.color_fail_bg : R.color.color_marginal_bg));
            dot.setBackgroundColor(color);

            tvBadge.setText(critical ? "HW 알람 · CRITICAL" : "HW 알람 · WARNING");
            tvBadge.setTextColor(color);
            tvTitle.setText(safe(event.getAlarmCode(), "HW_ALARM"));
            tvBody.setText(safe(event.getAlarmDescription(), "장비 알람 발생"));
            tvMeta.setText("burst_id: " + safe(event.getBurstId(), "-"));
        }

        private void bindOracle(FeedEvent event) {
            boolean danger = event.getAnalysisLevel() == FeedEvent.AnalysisLevel.DANGER;
            int color = ContextCompat.getColor(itemView.getContext(), danger ? R.color.color_fail : R.color.color_marginal);

            card.setStrokeColor(color);
            card.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), danger ? R.color.color_fail_bg : R.color.color_marginal_bg));
            dot.setBackgroundColor(color);

            tvBadge.setText(danger ? "오라클 분석 · 위험" : "오라클 분석 · 경고");
            tvBadge.setTextColor(color);
            tvTitle.setText("분석 결과");
            tvBody.setText(safe(event.getAnalysisMessage(), "분석 코멘트 없음"));

            String[] codes = event.getErrorCodes();
            if (codes != null && codes.length > 0) {
                StringBuilder sb = new StringBuilder("관련 코드: ");
                for (int i = 0; i < codes.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(codes[i]);
                }
                tvMeta.setText(sb.toString());
            } else {
                tvMeta.setText("관련 코드 없음");
            }
        }

        private String safe(String value, String fallback) {
            if (value == null || value.trim().isEmpty()) return fallback;
            return value;
        }
    }
}
