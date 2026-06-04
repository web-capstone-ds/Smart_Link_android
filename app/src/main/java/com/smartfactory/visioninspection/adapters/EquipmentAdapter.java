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
import com.smartfactory.visioninspection.models.ControlRecommendation;

import java.util.ArrayList;
import java.util.List;
public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {

    public enum EquipmentState {
        RUN, IDLE, OFF
    }

    public static class EquipmentUiItem {
        public String equipmentId;
        public int lineNo;
        public EquipmentState state = EquipmentState.OFF;
        public int currentUnits;
        public int expectedUnits;
        public String latestMessage = "Latest: 데이터 수신 대기";
        public String timeText = "--:--:--";
        public ControlRecommendation recommendation;
    }

    public interface OnEquipmentClickListener {
        void onEquipmentClick(EquipmentUiItem item);
    }

    private final List<EquipmentUiItem> items = new ArrayList<>();
    private final OnEquipmentClickListener clickListener;

    public EquipmentAdapter(OnEquipmentClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<EquipmentUiItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EquipmentUiItem item = items.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onEquipmentClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final View dot;
        private final TextView tvLineTitle;
        private final TextView tvEquipmentId;
        private final TextView tvStatus;
        private final TextView tvLatest;
        private final View recommendationLayout;
        private final TextView tvRecommendationTitle;
        private final TextView tvRecommendationReason;
        private final TextView tvRecommendationActions;
        private final TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_equipment);
            dot = itemView.findViewById(R.id.eq_status_dot);
            tvLineTitle = itemView.findViewById(R.id.tv_eq_alias);
            tvEquipmentId = itemView.findViewById(R.id.tv_eq_id);
            tvStatus = itemView.findViewById(R.id.tv_eq_status);
            tvLatest = itemView.findViewById(R.id.tv_eq_latest);
            recommendationLayout = itemView.findViewById(R.id.layout_eq_recommendation);
            tvRecommendationTitle = itemView.findViewById(R.id.tv_eq_recommendation_title);
            tvRecommendationReason = itemView.findViewById(R.id.tv_eq_recommendation_reason);
            tvRecommendationActions = itemView.findViewById(R.id.tv_eq_recommendation_actions);
            tvTime = itemView.findViewById(R.id.tv_eq_last_check);
        }

        void bind(EquipmentUiItem item) {
            tvLineTitle.setText(item.lineNo + "라인 비전센서");
            tvEquipmentId.setText(item.equipmentId);
            tvLatest.setText(item.latestMessage);
            tvTime.setText(item.timeText);
            bindRecommendation(item.recommendation);

            if (item.state == EquipmentState.RUN) {
                setStatusStyle("RUN",
                        R.color.color_pass,
                        R.drawable.bg_status_run_chip,
                        R.color.color_pass_bg,
                        R.color.color_pass);
            } else if (item.state == EquipmentState.IDLE) {
                setStatusStyle("IDLE",
                        R.color.text_secondary,
                        R.drawable.bg_status_idle_chip,
                        R.color.bg_card,
                        R.color.border);
            } else {
                setStatusStyle("OFF",
                        R.color.color_fail,
                        R.drawable.bg_status_off_chip,
                        R.color.color_fail_bg,
                        R.color.color_fail);
            }
        }

        private void bindRecommendation(ControlRecommendation recommendation) {
            if (recommendation == null || !recommendation.isOpen()) {
                recommendationLayout.setVisibility(View.GONE);
                return;
            }

            recommendationLayout.setVisibility(View.VISIBLE);
            if (recommendation.isDisplayCritical()) {
                recommendationLayout.setBackgroundResource(R.drawable.bg_recommendation_critical);
                tvRecommendationTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_fail));
            } else {
                recommendationLayout.setBackgroundResource(R.drawable.bg_recommendation_warning);
                tvRecommendationTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_marginal));
            }
            tvRecommendationTitle.setText(recommendation.getBannerTitle());
            tvRecommendationReason.setText(recommendation.getReason());
            tvRecommendationActions.setText(recommendation.getSuggestedActionLabel());
        }

        private void setStatusStyle(String label,
                                    int textColorRes,
                                    int chipBgRes,
                                    int cardBgRes,
                                    int strokeRes) {
            int textColor = ContextCompat.getColor(itemView.getContext(), textColorRes);
            int cardBgColor = ContextCompat.getColor(itemView.getContext(), cardBgRes);
            int strokeColor = ContextCompat.getColor(itemView.getContext(), strokeRes);

            tvStatus.setText(label);
            tvStatus.setTextColor(textColor);
            tvStatus.setBackgroundResource(chipBgRes);
            dot.setBackgroundColor(textColor);
            card.setCardBackgroundColor(cardBgColor);
            card.setStrokeColor(strokeColor);
        }
    }
}
