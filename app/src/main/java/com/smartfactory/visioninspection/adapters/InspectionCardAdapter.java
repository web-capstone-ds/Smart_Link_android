package com.smartfactory.visioninspection.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.StatusUpdateEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InspectionCardAdapter extends RecyclerView.Adapter<InspectionCardAdapter.ViewHolder> {

    private final List<StatusUpdateEvent> eventList;

    public InspectionCardAdapter(List<StatusUpdateEvent> eventList) {
        this.eventList = eventList;
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
        StatusUpdateEvent event = eventList.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    // 💡 새로운 데이터가 서버에서 도착했을 때 리스트 맨 위에 끼워 넣는 함수
    public void addEventToTop(StatusUpdateEvent newEvent) {
        eventList.add(0, newEvent); // 0번째(맨 위)에 추가
        notifyItemInserted(0);      // 애니메이션과 함께 추가됨을 알림
        // 리스트가 너무 길어지면 메모리 관리를 위해 맨 밑에 있는 오래된 데이터 삭제 (선택 사항)
        if (eventList.size() > 50) {
            eventList.remove(eventList.size() - 1);
            notifyItemRemoved(eventList.size());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View viewResultIndicator;
        TextView tvLotId, tvResult, tvUnitId, tvEquipment, tvErrorCodes, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewResultIndicator = itemView.findViewById(R.id.view_result_indicator);
            tvLotId = itemView.findViewById(R.id.tv_lot_id);
            tvResult = itemView.findViewById(R.id.tv_result);
            tvUnitId = itemView.findViewById(R.id.tv_unit_id);
            tvEquipment = itemView.findViewById(R.id.tv_equipment);
            tvErrorCodes = itemView.findViewById(R.id.tv_error_codes);
            tvTime = itemView.findViewById(R.id.tv_time);
        }

        void bind(StatusUpdateEvent event) {
            // 1. 기본 정보 세팅
            tvLotId.setText(event.getLotId() != null ? event.getLotId() : "LOT-UNKNOWN");
            tvEquipment.setText(event.getEquipmentId() + " · " + (event.getRecipeId() != null ? event.getRecipeId() : "UNKNOWN_RECIPE"));

            // 현재 시간을 "HH:mm:ss" 포맷으로 변환
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            tvTime.setText(currentTime);

            // TODO: 나중에 백엔드에서 Unit ID도 보내준다면 연결
            tvUnitId.setText("UNIT-DATA-REQUIRED");

            // 2. 장비 상태(Status)에 따른 색상 및 피그마 디자인(PASS/FAIL/MARGINAL) 세팅
            String status = event.getStatus();
            if (status == null) status = "UNKNOWN";

            if (status.equalsIgnoreCase("ERROR") || status.equalsIgnoreCase("FAIL")) {
                // 불량(FAIL)일 경우: 피그마의 1번, 5번 빨간색 카드 디자인
                tvResult.setText("FAIL");
                tvResult.setTextColor(Color.parseColor("#F85149")); // 빨간 텍스트
                viewResultIndicator.setBackgroundColor(Color.parseColor("#F85149")); // 빨간 띠

                // 에러 사유 텍스트 보이기 (TODO: 백엔드에서 에러 코드 목록을 받아서 연결해야 함)
                tvErrorCodes.setVisibility(View.VISIBLE);
                tvErrorCodes.setText("설비 오류 발생 (상세코드 수신 필요)");

            } else if (status.equalsIgnoreCase("WARNING") || status.equalsIgnoreCase("MARGINAL")) {
                // 경고(MARGINAL)일 경우: 피그마의 3번 노란색 카드 디자인
                tvResult.setText("MARGINAL");
                tvResult.setTextColor(Color.parseColor("#D29922"));
                viewResultIndicator.setBackgroundColor(Color.parseColor("#D29922"));
                tvErrorCodes.setVisibility(View.VISIBLE);
                tvErrorCodes.setText("수율 저하 경고");

            } else {
                // 정상(PASS/RUNNING)일 경우: 피그마의 2, 4번 초록색 카드 디자인
                tvResult.setText("PASS");
                tvResult.setTextColor(Color.parseColor("#3FB950"));
                viewResultIndicator.setBackgroundColor(Color.parseColor("#3FB950"));
                tvErrorCodes.setVisibility(View.GONE); // 정상일 땐 에러 사유 숨김
            }
        }
    }
}