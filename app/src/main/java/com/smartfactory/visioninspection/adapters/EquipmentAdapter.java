package com.smartfactory.visioninspection.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.smartfactory.visioninspection.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        private final TextView tvProgress;
        private final TextView tvTime;
        private final ProgressBar pbProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_equipment);
            dot = itemView.findViewById(R.id.eq_status_dot);
            tvLineTitle = itemView.findViewById(R.id.tv_eq_alias);
            tvEquipmentId = itemView.findViewById(R.id.tv_eq_id);
            tvStatus = itemView.findViewById(R.id.tv_eq_status);
            tvLatest = itemView.findViewById(R.id.tv_eq_latest);
            tvProgress = itemView.findViewById(R.id.tv_eq_progress);
            tvTime = itemView.findViewById(R.id.tv_eq_last_check);
            pbProgress = itemView.findViewById(R.id.progress_eq);
        }

        void bind(EquipmentUiItem item) {
            tvLineTitle.setText(item.lineNo + "라인 비전센서");
            tvEquipmentId.setText(item.equipmentId);
            tvLatest.setText(item.latestMessage);
            tvTime.setText(item.timeText);

            int expected = Math.max(item.expectedUnits, 1);
            int current = Math.max(item.currentUnits, 0);
            int progress = Math.min(100, (int) ((current * 100f) / expected));
            pbProgress.setProgress(progress);
            tvProgress.setText(String.format(Locale.getDefault(), "진행도 %d%%  %,d / %,d", progress, current, expected));

            if (item.state == EquipmentState.RUN) {
                setStatusStyle("RUN",
                        R.color.color_pass,
                        R.drawable.bg_status_run_chip,
                        R.color.color_pass_bg,
                        R.color.color_pass);
            } else if (item.state == EquipmentState.IDLE) {
                setStatusStyle("IDLE",
                        R.color.color_marginal,
                        R.drawable.bg_status_idle_chip,
                        R.color.color_marginal_bg,
                        R.color.color_marginal);
            } else {
                setStatusStyle("OFF",
                        R.color.text_secondary,
                        R.drawable.bg_status_off_chip,
                        R.color.bg_card,
                        R.color.border);
            }
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
