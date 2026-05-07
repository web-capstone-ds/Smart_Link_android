package com.smartfactory.visioninspection.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.adapters.EquipmentAdapter;
import com.smartfactory.visioninspection.utils.MockDataUtil;
import com.smartfactory.visioninspection.StatusUpdateEvent;

import java.util.List;

/**
 * 장비 상태 탭 (EquipmentStatus.tsx 대응)
 * DS-VIS-001 ~ 010 장비 10라인 카드 목록
 * RUNNING(초록) / WARNING(노랑) / ERROR(빨강) / IDLE(회색) 상태 표시
 */
public class EquipmentFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_equipment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── 요약 카운트 ───────────────────────────────────────
        List<String[]> equipList = MockDataUtil.getMockEquipmentList();
        int running = 0, warning = 0, error = 0, idle = 0;
        for (String[] eq : equipList) {
            switch (eq[2]) {
                case "RUNNING": running++; break;
                case "WARNING": warning++; break;
                case "ERROR":   error++;   break;
                default:        idle++;    break;
            }
        }

        TextView tvRunning = view.findViewById(R.id.tv_eq_count_running);
        TextView tvWarning = view.findViewById(R.id.tv_eq_count_warning);
        TextView tvError   = view.findViewById(R.id.tv_eq_count_error);
        TextView tvIdle    = view.findViewById(R.id.tv_eq_count_idle);

        if (tvRunning != null) tvRunning.setText(String.valueOf(running));
        if (tvWarning != null) tvWarning.setText(String.valueOf(warning));
        if (tvError   != null) tvError.setText(String.valueOf(error));
        if (tvIdle    != null) tvIdle.setText(String.valueOf(idle));

        // ── 장비 목록 RecyclerView ────────────────────────────
        RecyclerView rv = view.findViewById(R.id.rv_equipment);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setHasFixedSize(true);
        rv.setAdapter(new EquipmentAdapter(equipList));
    }
    public void updateRealTimeData(StatusUpdateEvent event) {
        // TODO: 실시간 데이터로 장비 목록 UI 업데이트하기
    }
}
