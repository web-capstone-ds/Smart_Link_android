package com.smartfactory.visioninspection.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.models.LotDefect;

import java.util.List;
import java.util.Locale;

/**
 * 주요 불량 유형 어댑터 (DetailBottomSheet + ReportScreen topDefects 대응)
 */
public class DefectAdapter extends RecyclerView.Adapter<DefectAdapter.ViewHolder> {

    private final List<LotDefect> defects;

    public DefectAdapter(List<LotDefect> defects) {
        this.defects = defects;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_defect, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.bind(defects.get(position), position);
    }

    @Override public int getItemCount() { return defects.size(); }

    // ── ViewHolder ───────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvRank, tvDesc, tvCode, tvRatio, tvCount;
        ProgressBar progressBar;

        ViewHolder(@NonNull View v) {
            super(v);
            tvRank      = v.findViewById(R.id.tv_defect_rank);
            tvDesc      = v.findViewById(R.id.tv_defect_desc);
            tvCode      = v.findViewById(R.id.tv_defect_code);
            tvRatio     = v.findViewById(R.id.tv_defect_ratio);
            tvCount     = v.findViewById(R.id.tv_defect_count);
            progressBar = v.findViewById(R.id.progress_defect);
        }

        void bind(LotDefect d, int position) {
            tvRank.setText(String.valueOf(position + 1));
            tvDesc.setText(d.getDescription());
            tvCode.setText(d.getCode());
            tvRatio.setText(String.format(Locale.getDefault(), "%.1f%%", d.getRatio()));
            tvCount.setText(String.format(Locale.getDefault(), "%,d건", d.getCount()));
            progressBar.setProgress((int) d.getRatio());

            int color = position == 0 ? Color.parseColor("#F85149")
                    : position == 1 ? Color.parseColor("#D29922")
                    : Color.parseColor("#7D8590");
            tvRank.setTextColor(color);
            tvRatio.setTextColor(color);
        }
    }
}
