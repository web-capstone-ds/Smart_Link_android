package com.smartfactory.visioninspection.adapters;

import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.models.FeedEvent;

import java.util.List;
import java.util.Locale;

/**
 * 피드 이벤트 어댑터 (FeedEventCard.tsx 대응)
 * LOT_END / HW_ALARM / ORACLE_ANALYSIS 3종 이벤트 타입별 렌더링
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.ViewHolder> {

    private final List<FeedEvent> events;

    public FeedAdapter(List<FeedEvent> events) {
        this.events = events;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        h.bind(events.get(position));
    }

    @Override public int getItemCount() { return events.size(); }

    // ── ViewHolder ───────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        View     dot;
        TextView tvTime, tvEquipment, tvEventType, tvTitle, tvBody;

        ViewHolder(@NonNull View v) {
            super(v);
            dot          = v.findViewById(R.id.feed_dot);
            tvTime       = v.findViewById(R.id.tv_feed_time);
            tvEquipment  = v.findViewById(R.id.tv_feed_equipment);
            tvEventType  = v.findViewById(R.id.tv_feed_event_type);
            tvTitle      = v.findViewById(R.id.tv_feed_title);
            tvBody       = v.findViewById(R.id.tv_feed_body);
        }

        void bind(FeedEvent ev) {
            tvTime.setText(ev.getTime());
            tvEquipment.setText(ev.getEquipmentId());

            switch (ev.getEventType()) {

                // ── LOT_END ──────────────────────────────────
                case LOT_END: {
                    int dotColor = Color.parseColor("#3FB950");
                    dot.setBackgroundColor(dotColor);
                    tvEventType.setText("LOT 완료");
                    tvEventType.setTextColor(dotColor);
                    tvTitle.setText("LOT 종료 — " + ev.getLotId());
                    tvTitle.setTextColor(Color.parseColor("#E6EDF3"));
                    tvBody.setText(String.format(Locale.getDefault(),
                            "총 %,d유닛 · 합격 %,d · 불합격 %,d · 수율 %.1f%%\n작업자: %s · 레시피: %s",
                            ev.getTotalUnits(), ev.getPassUnits(), ev.getFailUnits(),
                            ev.getYieldRate(), ev.getOperator(), ev.getRecipeId()));
                    break;
                }

                // ── HW_ALARM ─────────────────────────────────
                case HW_ALARM: {
                    boolean isCritical = ev.getAlarmLevel() == FeedEvent.AlarmLevel.CRITICAL;
                    int dotColor = isCritical
                            ? Color.parseColor("#F85149")
                            : Color.parseColor("#D29922");
                    dot.setBackgroundColor(dotColor);
                    tvEventType.setText(isCritical ? "CRITICAL" : "WARNING");
                    tvEventType.setTextColor(dotColor);
                    tvTitle.setText("[HW] " + ev.getAlarmCode());
                    tvTitle.setTextColor(dotColor);
                    tvBody.setText(ev.getAlarmDescription());
                    break;
                }

                // ── ORACLE_ANALYSIS ───────────────────────────
                case ORACLE_ANALYSIS: {
                    boolean isDanger = ev.getAnalysisLevel() == FeedEvent.AnalysisLevel.DANGER;
                    int dotColor = isDanger
                            ? Color.parseColor("#F85149")
                            : Color.parseColor("#D29922");
                    dot.setBackgroundColor(dotColor);
                    tvEventType.setText("Oracle AI");
                    tvEventType.setTextColor(Color.parseColor("#58A6FF"));
                    tvTitle.setText(isDanger ? "⚠ DANGER 분석" : "⚠ WARNING 분석");
                    tvTitle.setTextColor(dotColor);

                    StringBuilder sb = new StringBuilder(ev.getAnalysisMessage());
                    String[] codes = ev.getErrorCodes();
                    if (codes != null && codes.length > 0) {
                        sb.append("\n관련 코드: ");
                        for (int i = 0; i < codes.length; i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(codes[i]);
                        }
                    }
                    tvBody.setText(sb.toString());
                    break;
                }
            }
        }
    }
}
