package com.smartfactory.visioninspection.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfactory.visioninspection.R;

import java.util.List;

/**
 * 라인별 통계 어댑터 (ReportScreen lineStats 대응)
 * data: String[] { lineNo, equipmentId, alias, totalLots, failLots, failRate, status }
 */
public class LineStatAdapter extends RecyclerView.Adapter<LineStatAdapter.ViewHolder> {

    private final List<String[]> lineStats;

    public LineStatAdapter(List<String[]> lineStats) {
        this.lineStats = lineStats;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_line_stat, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.bind(lineStats.get(position));
    }

    @Override public int getItemCount() { return lineStats.size(); }

    // ── ViewHolder ───────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlias, tvEqId, tvTotalLots, tvFailLots, tvFailRate, tvStatus;
        View     statusDot;

        ViewHolder(@NonNull View v) {
            super(v);
            tvAlias     = v.findViewById(R.id.tv_line_alias);
            tvEqId      = v.findViewById(R.id.tv_line_eq_id);
            tvTotalLots = v.findViewById(R.id.tv_line_total_lots);
            tvFailLots  = v.findViewById(R.id.tv_line_fail_lots);
            tvFailRate  = v.findViewById(R.id.tv_line_fail_rate);
            tvStatus    = v.findViewById(R.id.tv_line_status);
            statusDot   = v.findViewById(R.id.line_status_dot);
        }

        void bind(String[] d) {
            // d = [lineNo, equipmentId, alias, totalLots, failLots, failRate, status]
            tvAlias.setText(d[2]);
            tvEqId.setText(d[1]);
            tvTotalLots.setText(d[3] + " LOT");
            tvFailLots.setText("불합격 " + d[4] + " LOT");
            tvFailRate.setText(d[5] + "%");

            int color;
            switch (d[6]) {
                case "경고": color = Color.parseColor("#F85149"); break;
                case "주의": color = Color.parseColor("#D29922"); break;
                default:    color = Color.parseColor("#3FB950"); break;
            }
            tvStatus.setText(d[6]);
            tvStatus.setTextColor(color);
            statusDot.setBackgroundColor(color);
            tvFailRate.setTextColor(color);
        }
    }
}
