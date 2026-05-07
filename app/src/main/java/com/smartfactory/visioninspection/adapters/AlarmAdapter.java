package com.smartfactory.visioninspection.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.models.LotAlarm;

import java.util.List;

/**
 * LOT 알람 타임라인 어댑터 (DetailBottomSheet 로트 알람 탭 대응)
 * CRITICAL(빨강) / WARNING(노랑) / INFO(파랑) 심각도별 색상
 */
public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

    private final List<LotAlarm> alarms;

    public AlarmAdapter(List<LotAlarm> alarms) {
        this.alarms = alarms;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.bind(alarms.get(position));
    }

    @Override public int getItemCount() { return alarms.size(); }

    // ── ViewHolder ───────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        View     severityDot;
        TextView tvTime, tvSeverity, tvType, tvTitle, tvDesc;

        ViewHolder(@NonNull View v) {
            super(v);
            severityDot = v.findViewById(R.id.alarm_severity_dot);
            tvTime      = v.findViewById(R.id.tv_alarm_time);
            tvSeverity  = v.findViewById(R.id.tv_alarm_severity);
            tvType      = v.findViewById(R.id.tv_alarm_type);
            tvTitle     = v.findViewById(R.id.tv_alarm_title);
            tvDesc      = v.findViewById(R.id.tv_alarm_desc);
        }

        void bind(LotAlarm alarm) {
            tvTime.setText(alarm.getTime());
            tvType.setText(alarm.getTypeLabel());
            tvTitle.setText(alarm.getTitle());
            tvDesc.setText(alarm.getDescription());

            int color;
            String sevLabel;
            switch (alarm.getSeverity()) {
                case CRITICAL:
                    color    = Color.parseColor("#F85149");
                    sevLabel = "CRITICAL";
                    break;
                case WARNING:
                    color    = Color.parseColor("#D29922");
                    sevLabel = "WARNING";
                    break;
                default:
                    color    = Color.parseColor("#58A6FF");
                    sevLabel = "INFO";
                    break;
            }

            severityDot.setBackgroundColor(color);
            tvSeverity.setText(sevLabel);
            tvSeverity.setTextColor(color);
            tvTitle.setTextColor(color);
        }
    }
}
