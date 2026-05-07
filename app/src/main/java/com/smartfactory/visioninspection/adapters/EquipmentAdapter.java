package com.smartfactory.visioninspection.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.smartfactory.visioninspection.R;

import java.util.List;
import java.util.Locale;

/**
 * 장비 상태 목록 어댑터 (EquipmentStatus.tsx 대응)
 * data 형식: String[] { equipmentId, alias, status, yieldStr, lastCheck, currentUnit, totalUnit }
 */
public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {

    private final List<String[]> equipList;

    public EquipmentAdapter(List<String[]> equipList) {
        this.equipList = equipList;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.bind(equipList.get(position));
    }

    @Override public int getItemCount() { return equipList.size(); }

    // ── ViewHolder ───────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        View        statusDot;
        TextView    tvEqId, tvAlias, tvStatus, tvYield, tvLastCheck;
        ProgressBar progressBar;
        TextView    tvProgress;

        ViewHolder(@NonNull View v) {
            super(v);
            statusDot   = v.findViewById(R.id.eq_status_dot);
            tvEqId      = v.findViewById(R.id.tv_eq_id);
            tvAlias     = v.findViewById(R.id.tv_eq_alias);
            tvStatus    = v.findViewById(R.id.tv_eq_status);
            tvYield     = v.findViewById(R.id.tv_eq_yield);
            tvLastCheck = v.findViewById(R.id.tv_eq_last_check);
            progressBar = v.findViewById(R.id.progress_eq);
            tvProgress  = v.findViewById(R.id.tv_eq_progress);
        }

        void bind(String[] d) {
            // d = [equipmentId, alias, status, yieldStr, lastCheck, currentUnit, totalUnit]
            tvEqId.setText(d[0]);
            tvAlias.setText(d[1]);
            tvStatus.setText(statusLabel(d[2]));
            tvYield.setText("수율 " + d[3] + "%");
            tvLastCheck.setText("최종 점검: " + d[4]);

            int color = statusColor(d[2]);
            tvStatus.setTextColor(color);
            statusDot.setBackgroundColor(color);

            // 진행 바
            try {
                int cur   = Integer.parseInt(d[5]);
                int total = Integer.parseInt(d[6]);
                int pct   = total > 0 ? (int) ((cur / (float) total) * 100) : 0;
                progressBar.setProgress(pct);
                tvProgress.setText(String.format(Locale.getDefault(),
                        "%,d / %,d (%d%%)", cur, total, pct));
                progressBar.setVisibility(View.VISIBLE);
                tvProgress.setVisibility(View.VISIBLE);
            } catch (NumberFormatException e) {
                progressBar.setVisibility(View.GONE);
                tvProgress.setVisibility(View.GONE);
            }
        }

        private static int statusColor(String status) {
            switch (status) {
                case "ERROR":   return Color.parseColor("#F85149");
                case "WARNING": return Color.parseColor("#D29922");
                case "RUNNING": return Color.parseColor("#3FB950");
                default:        return Color.parseColor("#7D8590");
            }
        }

        private static String statusLabel(String status) {
            switch (status) {
                case "RUNNING": return "가동 중";
                case "WARNING": return "경고";
                case "ERROR":   return "오류";
                default:        return "대기";
            }
        }
    }
}
