package com.smartfactory.visioninspection.adapters;

import android.graphics.Color;
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
    private final OnLotClickListener onLotClickListener;

    public interface OnLotClickListener {
        void onLotClick(FeedEvent event);
    }

    public FeedAdapter(OnLotClickListener onLotClickListener) {
        this.onLotClickListener = onLotClickListener;
    }

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
        holder.bind(items.get(position), onLotClickListener);
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

        void bind(FeedEvent event, OnLotClickListener onLotClickListener) {
            int lineNo = parseLineNo(event.getEquipmentId());
            tvLine.setText(lineNo > 0 ? (lineNo + "\uB77C\uC778 \uBE44\uC804\uC13C\uC11C") : event.getEquipmentId());
            tvTime.setText(event.getTime());

            if (event.getEventType() == FeedEvent.EventType.LOT_END) {
                bindLot(event);
                itemView.setOnClickListener(v -> {
                    if (onLotClickListener != null) {
                        onLotClickListener.onLotClick(event);
                    }
                });
            } else if (event.getEventType() == FeedEvent.EventType.HW_ALARM) {
                bindAlarm(event);
                itemView.setOnClickListener(null);
            } else {
                bindOracle(event);
                itemView.setOnClickListener(null);
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
                badge = "\uBD88\uD569\uACA9";
            } else if (result == LotResult.MARGINAL) {
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.color_marginal);
                strokeColor = textColor;
                cardBg = ContextCompat.getColor(itemView.getContext(), R.color.color_marginal_bg);
                badge = "\uACBD\uACC4";
            } else {
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.color_pass);
                strokeColor = textColor;
                cardBg = ContextCompat.getColor(itemView.getContext(), R.color.color_pass_bg);
                badge = "\uD569\uACA9";
            }

            card.setStrokeColor(strokeColor);
            card.setCardBackgroundColor(cardBg);
            dot.setBackgroundColor(textColor);

            tvBadge.setText("LOT \uC644\uB8CC");
            tvBadge.setTextColor(textColor);
            tvTitle.setText("\uACB0\uACFC: " + badge + " \u00B7 " + safe(event.getLotId(), "LOT-UNKNOWN"));

            String body = String.format(
                    Locale.getDefault(),
                    "\uC218\uC728 %.1f%%  (\uD569\uACA9 %,d / \uBD88\uD569\uACA9 %,d / \uC804\uCCB4 %,d)",
                    event.getYieldRate(),
                    event.getPassUnits(),
                    event.getFailUnits(),
                    Math.max(1, event.getTotalUnits())
            );
            tvBody.setText(body);
            tvMeta.setText("\uC791\uC5C5\uC790 " + safe(event.getOperator(), "-") + " \u00B7 \uB808\uC2DC\uD53C " + safe(event.getRecipeId(), "-"));
        }

        private void bindAlarm(FeedEvent event) {
            int color = Color.parseColor("#9CA6B3");
            int bgColor = Color.parseColor("#151B25");
            int strokeColor = Color.parseColor("#354154");
            int dotColor = Color.parseColor("#7D8590");
            boolean recipeNotice = "RECIPE_CHANGED_NOTICE".equalsIgnoreCase(event.getAlarmCode());

            card.setStrokeColor(strokeColor);
            card.setCardBackgroundColor(bgColor);
            dot.setBackgroundColor(dotColor);

            tvBadge.setText(recipeNotice ? "운영 알림 · 레시피 변경" : (event.getAlarmLevel() == FeedEvent.AlarmLevel.CRITICAL ? "HW \uC54C\uB78C \u00B7 CRITICAL" : "HW \uC54C\uB78C \u00B7 WARNING"));
            tvBadge.setTextColor(color);
            tvTitle.setText(recipeNotice ? "레시피 변경" : safe(event.getAlarmCode(), "HW_ALARM"));
            tvBody.setText(safe(event.getAlarmDescription(), "\uC7A5\uBE44 \uC54C\uB78C \uBC1C\uC0DD"));
            tvMeta.setText("burst_id: " + safe(event.getBurstId(), "-"));
        }

        private void bindOracle(FeedEvent event) {
            FeedEvent.AnalysisLevel level = event.getAnalysisLevel();
            boolean danger = level == FeedEvent.AnalysisLevel.DANGER;
            boolean warning = level == FeedEvent.AnalysisLevel.WARNING;

            int color;
            int bgColor;
            String badge;

            if (danger) {
                color = ContextCompat.getColor(itemView.getContext(), R.color.color_fail);
                bgColor = ContextCompat.getColor(itemView.getContext(), R.color.color_fail_bg);
                badge = "\uC624\uB77C\uD074 \uBD84\uC11D \u00B7 \uBD88\uD569\uACA9";
            } else if (warning) {
                color = ContextCompat.getColor(itemView.getContext(), R.color.color_marginal);
                bgColor = ContextCompat.getColor(itemView.getContext(), R.color.color_marginal_bg);
                badge = "\uC624\uB77C\uD074 \uBD84\uC11D \u00B7 \uACBD\uACC4";
            } else {
                color = ContextCompat.getColor(itemView.getContext(), R.color.color_pass);
                bgColor = ContextCompat.getColor(itemView.getContext(), R.color.color_pass_bg);
                badge = "\uC624\uB77C\uD074 \uBD84\uC11D \u00B7 \uD569\uACA9";
            }

            card.setStrokeColor(color);
            card.setCardBackgroundColor(bgColor);
            dot.setBackgroundColor(color);

            tvBadge.setText(badge);
            tvBadge.setTextColor(color);
            tvTitle.setText("\uBD84\uC11D \uACB0\uACFC");
            tvBody.setText(safe(event.getAnalysisMessage(), "\uBD84\uC11D \uCF54\uBA58\uD2B8 \uC5C6\uC74C"));

            String[] codes = event.getErrorCodes();
            if (codes != null && codes.length > 0) {
                StringBuilder sb = new StringBuilder("\uAD00\uB828 \uCF54\uB4DC: ");
                for (int i = 0; i < codes.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(codes[i]);
                }
                tvMeta.setText(sb.toString());
            } else {
                tvMeta.setText("\uAD00\uB828 \uCF54\uB4DC \uC5C6\uC74C");
            }
        }

        private String safe(String value, String fallback) {
            if (value == null || value.trim().isEmpty()) return fallback;
            return value;
        }
    }
}

