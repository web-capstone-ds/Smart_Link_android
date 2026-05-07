package com.smartfactory.visioninspection.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.adapters.DefectAdapter;
import com.smartfactory.visioninspection.adapters.LineStatAdapter;
import com.smartfactory.visioninspection.models.LotDefect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 보고서 탭 (ReportScreen.tsx 대응)
 * 일일 / 주간 탭 전환 + 수율 지표 + 불량 분석 + 라인별 통계 + 서버 자동 코멘트
 * 데이터: mockReports.ts 의 mockDailyReport / mockWeeklyReport 완전 이식
 */
public class ReportFragment extends Fragment {

    private boolean isDailySelected = true;

    // ── 뷰 참조 ──────────────────────────────────────────────
    private Button      btnDaily, btnWeekly;
    private TextView    tvReportPeriod;
    private TextView    tvTotalLots, tvPassLots, tvFailLots, tvFailRate;
    private TextView    tvTotalUnits, tvUnitFailRate;
    private TextView    tvOracleComment;
    private RecyclerView rvDefects, rvLineStats;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnDaily       = view.findViewById(R.id.btn_daily);
        btnWeekly      = view.findViewById(R.id.btn_weekly);
        tvReportPeriod = view.findViewById(R.id.tv_report_period);
        tvTotalLots    = view.findViewById(R.id.tv_total_lots);
        tvPassLots     = view.findViewById(R.id.tv_pass_lots);
        tvFailLots     = view.findViewById(R.id.tv_fail_lots);
        tvFailRate     = view.findViewById(R.id.tv_fail_rate);
        tvTotalUnits   = view.findViewById(R.id.tv_total_units);
        tvUnitFailRate = view.findViewById(R.id.tv_unit_fail_rate);
        tvOracleComment = view.findViewById(R.id.tv_oracle_comment);
        rvDefects      = view.findViewById(R.id.rv_defects);
        rvLineStats    = view.findViewById(R.id.rv_line_stats);

        rvDefects.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDefects.setNestedScrollingEnabled(false);
        rvLineStats.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLineStats.setNestedScrollingEnabled(false);

        btnDaily.setOnClickListener(v -> {
            isDailySelected = true;
            updateUI();
        });
        btnWeekly.setOnClickListener(v -> {
            isDailySelected = false;
            updateUI();
        });

        updateUI();
    }

    // ── UI 갱신 ──────────────────────────────────────────────
    private void updateUI() {
        if (isDailySelected) {
            btnDaily.setBackgroundResource(R.drawable.bg_tab_selected);
            btnWeekly.setBackgroundResource(R.drawable.bg_tab_unselected);
            btnDaily.setTextColor(Color.parseColor("#58A6FF"));
            btnWeekly.setTextColor(Color.parseColor("#7D8590"));
            bindDailyReport();
        } else {
            btnDaily.setBackgroundResource(R.drawable.bg_tab_unselected);
            btnWeekly.setBackgroundResource(R.drawable.bg_tab_selected);
            btnDaily.setTextColor(Color.parseColor("#7D8590"));
            btnWeekly.setTextColor(Color.parseColor("#58A6FF"));
            bindWeeklyReport();
        }
    }

    // ── 일일 보고서 바인딩 (mockDailyReport 대응) ─────────────
    private void bindDailyReport() {
        tvReportPeriod.setText("2025-05-01 08:00 ~ 18:00");
        tvTotalLots.setText("48");
        tvPassLots.setText("43");
        tvFailLots.setText("5");
        tvFailRate.setText("10.4%");
        tvTotalUnits.setText(String.format(Locale.getDefault(), "%,d", 192000));
        tvUnitFailRate.setText("2.38%");

        // 주요 불량 유형
        List<LotDefect> defects = new ArrayList<>();
        defects.add(new LotDefect("CHIPPING_EXCEED", "엣지 치핑 초과",  1824, 39.8f));
        defects.add(new LotDefect("OFFSET_X",        "X축 오프셋 이탈", 1102, 24.1f));
        defects.add(new LotDefect("SOLDER_VOID",     "솔더 보이드",      889, 19.4f));
        defects.add(new LotDefect("MISSING_BALL",    "볼 누락",          512, 11.2f));
        defects.add(new LotDefect("COPLANARITY",     "공면도 불량",      242,  5.3f));
        rvDefects.setAdapter(new DefectAdapter(defects));

        // 라인별 통계
        List<String[]> lineStats = new ArrayList<>();
        lineStats.add(new String[]{"1", "DS-VIS-001", "1라인",  "8",  "2", "25.0", "경고"});
        lineStats.add(new String[]{"2", "DS-VIS-002", "2라인",  "8",  "1", "12.5", "주의"});
        lineStats.add(new String[]{"3", "DS-VIS-003", "3라인",  "7",  "1", "14.3", "주의"});
        lineStats.add(new String[]{"4", "DS-VIS-004", "4라인",  "7",  "0",  "0.0", "정상"});
        lineStats.add(new String[]{"5", "DS-VIS-005", "5라인",  "6",  "1", "16.7", "주의"});
        lineStats.add(new String[]{"6", "DS-VIS-006", "6라인",  "6",  "0",  "0.0", "정상"});
        lineStats.add(new String[]{"7", "DS-VIS-007", "7라인",  "3",  "0",  "0.0", "정상"});
        lineStats.add(new String[]{"8", "DS-VIS-008", "8라인",  "3",  "0",  "0.0", "정상"});
        rvLineStats.setAdapter(new LineStatAdapter(lineStats));

        tvOracleComment.setText(
                "【오늘(2025-05-01) 자동 분석 요약】\n\n" +
                        "금일 총 48 LOT 중 5 LOT에서 불합격이 발생하여 LOT 불합격률 10.4%를 기록하였습니다." +
                        " 이는 이번 주 평균(8.2%) 대비 2.2%p 높은 수치입니다.\n\n" +
                        "가장 빈번한 불량 유형은 '엣지 치핑 초과(CHIPPING_EXCEED)'로 전체 불량의 39.8%를 차지하였습니다." +
                        " 1라인(DS-VIS-001)에서 LOT 불합격률 25.0%가 발생하여 즉각 점검이 권고됩니다.\n\n" +
                        "조치 권고: 1라인 가공 지그 마모 상태 점검 및 레시피 파라미터(치핑 허용 임계값) 재검토 필요.");
    }

    // ── 주간 보고서 바인딩 (mockWeeklyReport 대응) ────────────
    private void bindWeeklyReport() {
        tvReportPeriod.setText("2025년 18주차 · 2025-04-26 ~ 2025-05-01");
        tvTotalLots.setText("284");
        tvPassLots.setText("261");
        tvFailLots.setText("23");
        tvFailRate.setText("8.1%");
        tvTotalUnits.setText(String.format(Locale.getDefault(), "%,d", 1136000));
        tvUnitFailRate.setText("1.94%");

        // 주요 불량 유형
        List<LotDefect> defects = new ArrayList<>();
        defects.add(new LotDefect("CHIPPING_EXCEED", "엣지 치핑 초과",  8940, 35.1f));
        defects.add(new LotDefect("OFFSET_X",        "X축 오프셋 이탈", 6320, 24.8f));
        defects.add(new LotDefect("SOLDER_VOID",     "솔더 보이드",     5210, 20.5f));
        defects.add(new LotDefect("MISSING_BALL",    "볼 누락",         3180, 12.5f));
        defects.add(new LotDefect("COPLANARITY",     "공면도 불량",     1800,  7.1f));
        rvDefects.setAdapter(new DefectAdapter(defects));

        // 라인별 통계
        List<String[]> lineStats = new ArrayList<>();
        lineStats.add(new String[]{"1", "DS-VIS-001", "1라인", "48", "8", "16.7", "경고"});
        lineStats.add(new String[]{"2", "DS-VIS-002", "2라인", "46", "4",  "8.7", "주의"});
        lineStats.add(new String[]{"3", "DS-VIS-003", "3라인", "45", "5", "11.1", "주의"});
        lineStats.add(new String[]{"4", "DS-VIS-004", "4라인", "44", "2",  "4.5", "정상"});
        lineStats.add(new String[]{"5", "DS-VIS-005", "5라인", "42", "3",  "7.1", "주의"});
        lineStats.add(new String[]{"6", "DS-VIS-006", "6라인", "38", "1",  "2.6", "정상"});
        lineStats.add(new String[]{"7", "DS-VIS-007", "7라인", "22", "0",  "0.0", "정상"});
        lineStats.add(new String[]{"8", "DS-VIS-008", "8라인", "22", "0",  "0.0", "정상"});
        rvLineStats.setAdapter(new LineStatAdapter(lineStats));

        tvOracleComment.setText(
                "【18주차(2025-04-26 ~ 2025-05-01) 주간 분석 요약】\n\n" +
                        "금주 총 284 LOT 처리 중 23 LOT 불합격, LOT 불합격률 8.1%를 달성하였습니다." +
                        " 전주(17주차) 9.7% 대비 1.6%p 개선되어 목표치(8.0%) 근접 수준을 유지하고 있습니다.\n\n" +
                        "주간 최대 불량 유형은 '엣지 치핑 초과(CHIPPING_EXCEED)'로 35.1%를 기록하였으며," +
                        " 주 후반(수~목)으로 갈수록 발생 빈도가 증가하는 추세입니다." +
                        " 1라인의 주간 불합격률(16.7%)이 전체 평균의 2배를 초과하여 집중 관리가 필요합니다.\n\n" +
                        "목표 달성 권고사항: ① 1라인 스핀들 마모 점검(주 1회 → 격일 전환)" +
                        " ② 치핑 불량 임계값 상향 조정(현행 0.15mm → 0.12mm)" +
                        " ③ 5라인 레시피 파라미터 재검증 요망.");
    }
}
