package com.smartfactory.visioninspection.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.StatusUpdateEvent;
import com.smartfactory.visioninspection.models.ControlRecommendation;
import com.smartfactory.visioninspection.models.DashboardLineState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InspectionCardAdapter extends RecyclerView.Adapter<InspectionCardAdapter.ViewHolder> {

    public interface OnLineCardClickListener {
        void onLineCardClick(DashboardLineState item);
    }

    public enum LotOutcome {
        PASS, MARGINAL, FAIL, UNKNOWN
    }

    private static final boolean DEMO_PROGRESS_MODE = true;
    private static final int DEMO_LOT_TARGET_UNITS = 40;

    private final List<DashboardLineState> lineList = new ArrayList<>();
    private final Map<String, DashboardLineState> lineMap = new HashMap<>();
    private final OnLineCardClickListener clickListener;

    public InspectionCardAdapter(OnLineCardClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inspection_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DashboardLineState item = lineList.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onLineCardClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return lineList.size();
    }

    public List<DashboardLineState> getCurrentItems() {
        return new ArrayList<>(lineList);
    }

    public void upsertFromStatus(StatusUpdateEvent event) {
        if (event == null || event.getEquipmentId() == null) return;

        DashboardLineState state = getOrCreate(event.getEquipmentId());

        state.setLotId(emptyToDash(event.getLotId()));
        state.setRecipeId(emptyToDash(event.getRecipeId()));
        state.setOperatorId(emptyToDash(event.getOperatorId()));
        state.setTimeText(toTimeText(event.getTimestamp()));

        int currentRaw = event.getCurrentUnitCount() != null ? event.getCurrentUnitCount() : state.getCurrentUnits();
        int expectedRaw = event.getExpectedTotalUnits() != null && event.getExpectedTotalUnits() > 0
                ? event.getExpectedTotalUnits() : Math.max(state.getExpectedUnits(), 4000);

        int displayCurrent;
        int displayExpected;
        if (DEMO_PROGRESS_MODE) {
            // 시연 모드 고정: 서버에서 1/2792가 와도 화면은 1/40 기준으로 표시
            displayExpected = DEMO_LOT_TARGET_UNITS;
            displayCurrent = Math.min(Math.max(currentRaw, 0), DEMO_LOT_TARGET_UNITS);
        } else {
            displayExpected = Math.max(expectedRaw, 1);
            displayCurrent = Math.max(currentRaw, 0);
        }

        state.setCurrentUnits(displayCurrent);
        state.setExpectedUnits(displayExpected);

        String equipmentStatus = safeUpper(event.getEquipmentStatus());
        if ("RUN".equals(equipmentStatus)) {
            state.setResult(DashboardLineState.LineResult.PASS);
            state.setEventMessage("정상 운전");
        } else if ("IDLE".equals(equipmentStatus)) {
            state.setResult(DashboardLineState.LineResult.IDLE);
            state.setEventMessage("대기");
        } else if ("STOP".equals(equipmentStatus)) {
            state.setResult(DashboardLineState.LineResult.STOP);
            state.setEventMessage("장비 정지");
        }

        state.touch(System.currentTimeMillis());
        sortAndNotify();
    }

    public void applyAlarmEvent(String equipmentId, String alarmLevel, String alarmCode, String alarmDetail, String timestamp) {
        DashboardLineState state = getOrCreate(equipmentId);
        String level = safeUpper(alarmLevel);

        if ("CRITICAL".equals(level)) {
            state.setResult(DashboardLineState.LineResult.FAIL);
        } else {
            state.setResult(DashboardLineState.LineResult.MARGINAL);
        }

        String msg = (alarmCode != null && !alarmCode.isEmpty()) ? alarmCode : "HW 알람";
        if (alarmDetail != null && !alarmDetail.isEmpty()) msg += " · " + alarmDetail;
        state.setEventMessage(msg);
        state.setLastErrorCode(alarmCode);
        state.setTimeText(toTimeText(timestamp));
        state.touch(System.currentTimeMillis());
        sortAndNotify();
    }

    public void applyOracleEvent(String equipmentId, String judgment, String aiComment, String timestamp) {
        DashboardLineState state = getOrCreate(equipmentId);
        String j = safeUpper(judgment);

        if ("DANGER".equals(j)) {
            state.setResult(DashboardLineState.LineResult.FAIL);
        } else if ("WARNING".equals(j)) {
            state.setResult(DashboardLineState.LineResult.MARGINAL);
        } else if ("NORMAL".equals(j) && state.getResult() == DashboardLineState.LineResult.IDLE) {
            state.setResult(DashboardLineState.LineResult.PASS);
        }

        state.setEventMessage((aiComment != null && !aiComment.isEmpty()) ? aiComment : "Oracle 분석 결과 " + j);
        state.setTimeText(toTimeText(timestamp));
        state.touch(System.currentTimeMillis());
        sortAndNotify();
    }

    public void applyRecommendationEvent(String equipmentId, ControlRecommendation recommendation) {
        if (equipmentId == null || recommendation == null) return;

        DashboardLineState state = getOrCreate(equipmentId);
        state.setRecommendation(recommendation.isOpen() ? recommendation : null);
        state.touch(System.currentTimeMillis());
        sortAndNotify();
    }

    public LotOutcome applyLotEndEvent(String equipmentId,
                                       String lotId,
                                       int totalUnits,
                                       int passCount,
                                       int failCount,
                                       String timestamp) {
        DashboardLineState state = getOrCreate(equipmentId);

        state.setLotId(emptyToDash(lotId));
        state.setTimeText(toTimeText(timestamp));

        int safeTotal = Math.max(totalUnits, 1);
        int safePass = Math.max(passCount, 0);
        int safeFail = Math.max(failCount, 0);

        // LOT 종료 시에는 즉시 100% 보여줌 (시연 모드 40 고정)
        if (DEMO_PROGRESS_MODE) {
            state.setCurrentUnits(DEMO_LOT_TARGET_UNITS);
            state.setExpectedUnits(DEMO_LOT_TARGET_UNITS);
        } else {
            state.setCurrentUnits(safeTotal);
            state.setExpectedUnits(safeTotal);
        }

        double yieldPct = (safePass * 100.0) / safeTotal;
        LotOutcome outcome;
        if (safeFail <= 0) {
            outcome = LotOutcome.PASS;
            state.setResult(DashboardLineState.LineResult.PASS);
        } else if (yieldPct >= 95.0) {
            outcome = LotOutcome.MARGINAL;
            state.setResult(DashboardLineState.LineResult.MARGINAL);
        } else {
            outcome = LotOutcome.FAIL;
            state.setResult(DashboardLineState.LineResult.FAIL);
        }

        state.setEventMessage(String.format("LOT 완료 · 수율 %.1f%%", yieldPct));
        state.touch(System.currentTimeMillis());
        sortAndNotify();
        return outcome;
    }

    public void setBlinkState(String equipmentId, LotOutcome outcome, boolean on) {
        if (equipmentId == null || outcome == null || outcome == LotOutcome.UNKNOWN) return;
        DashboardLineState state = getOrCreate(equipmentId);

        if (on) {
            if (outcome == LotOutcome.FAIL) {
                state.setResult(DashboardLineState.LineResult.FAIL);
            } else if (outcome == LotOutcome.MARGINAL) {
                state.setResult(DashboardLineState.LineResult.MARGINAL);
            }
        } else {
            state.setResult(DashboardLineState.LineResult.PASS);
        }

        state.touch(System.currentTimeMillis());
        sortAndNotify();
    }

    public void moveToNextLot(String equipmentId) {
        if (equipmentId == null) return;
        DashboardLineState state = getOrCreate(equipmentId);
        state.setCurrentUnits(0);
        state.setExpectedUnits(DEMO_PROGRESS_MODE ? DEMO_LOT_TARGET_UNITS : Math.max(state.getExpectedUnits(), 1));
        state.setResult(DashboardLineState.LineResult.PASS);
        state.setEventMessage("다음 LOT 시작");
        state.touch(System.currentTimeMillis());
        sortAndNotify();
    }

    private DashboardLineState getOrCreate(String equipmentId) {
        DashboardLineState state = lineMap.get(equipmentId);
        if (state != null) return state;

        int lineNo = parseLineNo(equipmentId);
        DashboardLineState created = new DashboardLineState(equipmentId, lineNo);
        created.setLotId("LOT-UNKNOWN");
        created.setRecipeId("RCP-UNKNOWN");
        created.setOperatorId("-");
        created.setTimeText("--:--:--");
        created.setEventMessage("데이터 수신 대기");
        created.setCurrentUnits(0);
        created.setExpectedUnits(DEMO_PROGRESS_MODE ? DEMO_LOT_TARGET_UNITS : 4000);
        created.setResult(DashboardLineState.LineResult.IDLE);
        created.touch(System.currentTimeMillis());

        lineMap.put(equipmentId, created);
        lineList.add(created);
        return created;
    }

    private void sortAndNotify() {
        lineList.sort(
                Comparator.comparingInt((DashboardLineState s) -> priorityOf(s.getResult()))
                        .thenComparingInt(DashboardLineState::getLineNo)
                        .thenComparingLong(DashboardLineState::getUpdatedAt)
        );
        notifyDataSetChanged();
    }

    private int priorityOf(DashboardLineState.LineResult result) {
        if (result == DashboardLineState.LineResult.FAIL) return 0;
        if (result == DashboardLineState.LineResult.MARGINAL) return 1;
        if (result == DashboardLineState.LineResult.PASS) return 2;
        if (result == DashboardLineState.LineResult.STOP) return 3;
        return 3;
    }

    private int parseLineNo(String equipmentId) {
        if (equipmentId == null) return 999;
        String digits = equipmentId.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 999;
        if (digits.length() >= 3) {
            digits = digits.substring(digits.length() - 3);
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 999;
        }
    }

    private String emptyToDash(String v) {
        return (v == null || v.trim().isEmpty()) ? "-" : v;
    }

    private String safeUpper(String v) {
        return v == null ? "" : v.trim().toUpperCase();
    }

    private String toTimeText(String iso) {
        if (iso == null || iso.isEmpty()) return "--:--:--";
        if (iso.length() >= 19 && iso.contains("T")) {
            return iso.substring(11, 19);
        }
        return iso;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final View dot;
        private final TextView tvLineName;
        private final TextView tvLineAlias;
        private final TextView tvStatus;
        private final TextView tvEvent;
        private final View recommendationLayout;
        private final TextView tvRecommendationTitle;
        private final TextView tvRecommendationReason;
        private final TextView tvRecommendationActions;
        private final ProgressBar pbProgress;
        private final TextView tvProgress;
        private final TextView tvLot;
        private final TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_line);
            dot = itemView.findViewById(R.id.view_status_dot);
            tvLineName = itemView.findViewById(R.id.tv_line_name);
            tvLineAlias = itemView.findViewById(R.id.tv_line_alias);
            tvStatus = itemView.findViewById(R.id.tv_status_badge);
            tvEvent = itemView.findViewById(R.id.tv_event_message);
            recommendationLayout = itemView.findViewById(R.id.layout_recommendation);
            tvRecommendationTitle = itemView.findViewById(R.id.tv_recommendation_title);
            tvRecommendationReason = itemView.findViewById(R.id.tv_recommendation_reason);
            tvRecommendationActions = itemView.findViewById(R.id.tv_recommendation_actions);
            pbProgress = itemView.findViewById(R.id.pb_progress);
            tvProgress = itemView.findViewById(R.id.tv_progress);
            tvLot = itemView.findViewById(R.id.tv_lot_id);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(DashboardLineState item) {
            int stroke;
            int dotColor;
            int textColor;
            int badgeBg;
            String statusLabel;

            if (item.getResult() == DashboardLineState.LineResult.FAIL) {
                stroke = Color.parseColor("#D94B4B");
                dotColor = Color.parseColor("#FF5A5A");
                textColor = Color.parseColor("#FF6C6C");
                badgeBg = Color.parseColor("#35F85149");
                statusLabel = "불합격";
                card.setCardBackgroundColor(Color.parseColor("#2A0B10"));
            } else if (item.getResult() == DashboardLineState.LineResult.MARGINAL) {
                stroke = Color.parseColor("#C89322");
                dotColor = Color.parseColor("#D29922");
                textColor = Color.parseColor("#E8B142");
                badgeBg = Color.parseColor("#35D29922");
                statusLabel = "경계";
                card.setCardBackgroundColor(Color.parseColor("#231A08"));
            } else if (item.getResult() == DashboardLineState.LineResult.PASS) {
                stroke = Color.parseColor("#2D874A");
                dotColor = Color.parseColor("#3FB950");
                textColor = Color.parseColor("#48CC66");
                badgeBg = Color.parseColor("#263FB950");
                statusLabel = "운전중";
                card.setCardBackgroundColor(Color.parseColor("#141F2A"));
            } else {
                stroke = Color.parseColor("#354154");
                dotColor = Color.parseColor("#7D8590");
                textColor = Color.parseColor("#9CA6B3");
                badgeBg = Color.parseColor("#267D8590");
                statusLabel = item.getResult() == DashboardLineState.LineResult.STOP ? "정지" : "대기";
                card.setCardBackgroundColor(Color.parseColor("#151B25"));
            }

            card.setStrokeColor(stroke);
            dot.setBackgroundColor(dotColor);

            tvLineName.setText(item.getLineNo() + "라인");
            tvLineAlias.setText(item.getLineNo() + "라인 비전센서");
            tvStatus.setText(statusLabel);
            tvStatus.setTextColor(textColor);
            tvStatus.setBackgroundColor(badgeBg);
            tvEvent.setText(item.getEventMessage());
            bindRecommendation(item.getRecommendation());

            int current = Math.max(item.getCurrentUnits(), 0);
            int expected = Math.max(item.getExpectedUnits(), 1);
            int progress = Math.min(100, (int) ((current * 100f) / expected));
            pbProgress.setProgress(progress);
            tvProgress.setText(String.format("%,d / %,d (%d%%)", current, expected, progress));
            tvProgress.setTextColor(textColor);

            tvLot.setText(item.getLotId());
            tvTime.setText(item.getTimeText());
        }

        private void bindRecommendation(ControlRecommendation recommendation) {
            if (recommendation == null || !recommendation.isOpen()) {
                recommendationLayout.setVisibility(View.GONE);
                return;
            }

            int titleColor;
            if (recommendation.isDisplayCritical()) {
                recommendationLayout.setBackgroundResource(R.drawable.bg_recommendation_critical);
                titleColor = Color.parseColor("#FF6C6C");
            } else {
                recommendationLayout.setBackgroundResource(R.drawable.bg_recommendation_warning);
                titleColor = Color.parseColor("#E8B142");
            }

            recommendationLayout.setVisibility(View.VISIBLE);
            tvRecommendationTitle.setText(recommendation.getBannerTitle());
            tvRecommendationTitle.setTextColor(titleColor);
            tvRecommendationReason.setText(recommendation.getReason());
            tvRecommendationActions.setText(recommendation.getSuggestedActionLabel());
        }
    }
}
