package com.smartfactory.visioninspection.bottomsheets;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.smartfactory.visioninspection.R;
import com.smartfactory.visioninspection.adapters.AlarmAdapter;
import com.smartfactory.visioninspection.adapters.DefectAdapter;
import com.smartfactory.visioninspection.models.*;
import com.smartfactory.visioninspection.utils.MockDataUtil;
import com.smartfactory.visioninspection.views.DonutChartView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * LOT 筌욌쵌??獄쏅????쀫뱜 (DetailBottomSheet.tsx ?袁⑹읈 ??곷뻼)
 *
 * ?닌듼?
 * ???????????????????????????????????????????????????????????????????????????????
 * ?? LOT ID ??삳쐭 + 野껉퀗??獄쏄퀣?             ??
 * ?? ?貫??/ ?臾믩씜??/ ??됰뻻??/ ??볦퍟 (4?? ??
 * ?? ????????????????????????????????????????????????????????????????????  ??
 * ?? TabLayout: [??륁몛 ?袁れ넺] [嚥≪뮉?????뿺]   ??
 * ?? ???? ??륁몛 ?袁れ넺 ??????????????????????????????????????  ??
 * ??   ?袁④퐢 筌△뫂??+ ??륁몛%                  ??
 * ??   4揶?????(?μ빘????븍뜇鍮野???륁몛/?븍뜄?양몴? ??
 * ??   雅뚯눘???븍뜄???醫륁굨 RecyclerView        ??
 * ?? ???? 嚥≪뮉?????뿺 ??????????????????????????????????????  ??
 * ??   CRITICAL/WARNING/INFO 燁삳똻???      ??
 * ??   ???뿺 ???袁⑥뵬??RecyclerView         ??
 * ?遺?????????????????????????????????????????????????????????????????????????????
 */
public class LotDetailBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "LotDetailBottomSheet";

    // ???? Bundle ????????????????????????????????????????????????????????????????????????????????????????????
    private static final String K_LOT_ID      = "lotId";
    private static final String K_EQ_ID       = "eqId";
    private static final String K_RECIPE      = "recipe";
    private static final String K_TIME        = "time";
    private static final String K_RESULT      = "result";
    private static final String K_CODES       = "errorCodes";
    private static final String K_OPERATOR    = "operator";
    private static final String K_USER_NAME   = "userName";
    private static final String K_USER_EMP_ID = "userEmpId";
    private static final String K_ORACLE_LEVEL = "oracleLevel";
    private static final String K_ORACLE_MESSAGE = "oracleMessage";
    private static final String K_ORACLE_CODES = "oracleCodes";

    // ???? ??븍꽅??????????????????????????????????????????????????????????????????????????????????????????????????
    public static LotDetailBottomSheet newInstance(InspectionEvent event, User user) {
        LotDetailBottomSheet sheet = new LotDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(K_LOT_ID,   event.getLotId());
        args.putString(K_EQ_ID,    event.getEquipmentId());
        args.putString(K_RECIPE,   event.getRecipeId());
        args.putString(K_TIME,     event.getTime());
        args.putString(K_RESULT,   event.getResult().name());
        args.putStringArray(K_CODES, event.getErrorCodes());
        args.putString(K_OPERATOR, event.getOperator());
        if (user != null) {
            args.putString(K_USER_NAME,   user.getName());
            args.putString(K_USER_EMP_ID, user.getEmployeeId());
        }
        sheet.setArguments(args);
        return sheet;
    }

    public static LotDetailBottomSheet newInstance(InspectionEvent event, User user, @Nullable FeedEvent oracleEvent) {
        LotDetailBottomSheet sheet = newInstance(event, user);
        Bundle args = sheet.getArguments();
        if (args != null && oracleEvent != null) {
            if (oracleEvent.getAnalysisLevel() != null) {
                args.putString(K_ORACLE_LEVEL, oracleEvent.getAnalysisLevel().name());
            }
            args.putString(K_ORACLE_MESSAGE, oracleEvent.getAnalysisMessage());
            args.putStringArray(K_ORACLE_CODES, oracleEvent.getErrorCodes());
        }
        sheet.setArguments(args);
        return sheet;
    }


    // ???? ???춳: ??쎄쾿 獄쏅????쀫뱜 ????????????????????????????????????????????????????????????????????
    @Override
    public int getTheme() {
        return R.style.Theme_SmartFactory_BottomSheet;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_lot_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        // ???? ??源?????????????????????????????????????????????????????????????????????????????????
        InspectionEvent.Result result =
                InspectionEvent.Result.valueOf(
                        args.getString(K_RESULT, "PASS"));

        String[] codes = args.getStringArray(K_CODES);
        InspectionEvent event = new InspectionEvent.Builder(
                "SHEET-EVT",
                args.getString(K_LOT_ID, ""),
                args.getString(K_TIME, "08:00:00"),
                result)
                .equipmentId(args.getString(K_EQ_ID, "DS-VIS-001"))
                .recipeId(args.getString(K_RECIPE, "RCP-BGA-0.5mm"))
                .operator(args.getString(K_OPERATOR, "ENG-KIM"))
                .errorCodes(codes != null ? codes : new String[0])
                .build();

        // LOT 筌욌쵌????밴쉐
        LotSummary lot = MockDataUtil.generateLotSummary(event);

        // ???? ??삳쐭 獄쏅뗄?????????????????????????????????????????????????????????????????????????????????
        bindHeader(view, lot, result,
                args.getString(K_USER_NAME),
                args.getString(K_USER_EMP_ID));

        // ???? ????됱뵠?袁⑹뜍 ??????????????????????????????????????????????????????????????????????????????
        View yieldContent = view.findViewById(R.id.content_yield);
        View alarmContent = view.findViewById(R.id.content_alarms);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_yield)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_alarms)));

        yieldContent.setVisibility(View.VISIBLE);
        alarmContent.setVisibility(View.GONE);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    yieldContent.setVisibility(View.VISIBLE);
                    alarmContent.setVisibility(View.GONE);
                } else {
                    yieldContent.setVisibility(View.GONE);
                    alarmContent.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // ???? ???꾩꼹?쀯㎘?獄쏅뗄?????????????????????????????????????????????????????????????????????
        bindYieldTab(view, lot);
        bindAlarmTab(view, lot, args);
    }

    // ???? ??삳쐭 獄쏅뗄???????????????????????????????????????????????????????????????????????????????????????
    private void bindHeader(View view, LotSummary lot,
                            InspectionEvent.Result result,
                            String userName, String userEmpId) {
        TextView tvSubtitle    = view.findViewById(R.id.tv_sheet_subtitle);
        TextView tvLotId       = view.findViewById(R.id.tv_sheet_lot_id);
        TextView tvResultBadge = view.findViewById(R.id.tv_result_badge);

        tvSubtitle.setText(getString(R.string.sheet_subtitle));
        tvLotId.setText(lot.getLotId());

        int    resultColor;
        String resultLabel;
        int    badgeBg;
        switch (result) {
            case FAIL:
                resultColor = Color.parseColor("#F85149");
                resultLabel = "\uBD88\uD569\uACA9 FAIL";
                badgeBg     = R.drawable.bg_badge_fail;
                break;
            case MARGINAL:
                resultColor = Color.parseColor("#D29922");
                resultLabel = "\uACBD\uACC4 MARGINAL";
                badgeBg     = R.drawable.bg_badge_marginal;
                break;
            default:
                resultColor = Color.parseColor("#3FB950");
                resultLabel = "\uD569\uACA9 PASS";
                badgeBg     = R.drawable.bg_badge_pass;
                break;
        }
        tvResultBadge.setText(resultLabel);
        tvResultBadge.setTextColor(resultColor);
        tvResultBadge.setBackgroundResource(badgeBg);

        // ???? 筌롫?? 4??(?貫??/ ?臾믩씜??/ ??됰뻻??/ ??볦퍟) ????????????????????
        TextView tvMetaEquipment = view.findViewById(R.id.tv_meta_equipment);
        TextView tvMetaOperator  = view.findViewById(R.id.tv_meta_operator);
        TextView tvMetaRecipe    = view.findViewById(R.id.tv_meta_recipe);
        TextView tvMetaTime      = view.findViewById(R.id.tv_meta_time);

        tvMetaEquipment.setText(lot.getEquipmentId());

        // ?袁⑹삺 嚥≪뮄???????癒? ??깊뒄??롢늺 ?λ뜄以??揶쏅벡??
        if (userName != null && userEmpId != null) {
            tvMetaOperator.setText(userName + " (" + userEmpId + ")");
            tvMetaOperator.setTextColor(Color.parseColor("#3FB950"));
            tvMetaOperator.setBackgroundResource(R.drawable.bg_meta_chip_operator);
        } else {
            tvMetaOperator.setText(lot.getOperator());
            tvMetaOperator.setTextColor(Color.parseColor("#E6EDF3"));
            tvMetaOperator.setBackgroundResource(R.drawable.bg_meta_chip);
        }

        tvMetaRecipe.setText(lot.getRecipeId());
        tvMetaTime.setText(lot.getStartTime() + " ~ " + lot.getEndTime());
    }

    // ???? ??륁몛 ?袁れ넺 ????????????????????????????????????????????????????????????????????????????????????
    private void bindYieldTab(View view, LotSummary lot) {
        // ?袁④퐢 筌△뫂??
        DonutChartView donut = view.findViewById(R.id.donut_chart);
        donut.setData(lot.getPassUnits(), lot.getMarginalUnits(),
                lot.getFailUnits(), lot.getTotalUnits());

        // ??륁몛 % ??용뮞??(筌△뫂???袁⑥삋)
        TextView tvYieldPct = view.findViewById(R.id.tv_yield_pct);
        float    yield      = lot.getYieldRate();
        tvYieldPct.setText(String.format(Locale.getDefault(), "%.1f%%", yield));
        int yColor = yield >= 95f ? Color.parseColor("#3FB950")
                : yield >= 85f ? Color.parseColor("#D29922")
                : Color.parseColor("#F85149");
        tvYieldPct.setTextColor(yColor);

        // 4揶???????
        TextView tvStatTotal   = view.findViewById(R.id.tv_stat_total);
        TextView tvStatFail    = view.findViewById(R.id.tv_stat_fail);
        TextView tvStatYield   = view.findViewById(R.id.tv_stat_yield);
        TextView tvStatFailRate = view.findViewById(R.id.tv_stat_fail_rate);

        tvStatTotal.setText(String.format(Locale.getDefault(), "%,d", lot.getTotalUnits()));
        tvStatFail.setText(String.format(Locale.getDefault(), "%,d", lot.getFailUnits()));
        tvStatYield.setText(String.format(Locale.getDefault(), "%.1f%%", lot.getYieldRate()));
        tvStatYield.setTextColor(yColor);
        tvStatFailRate.setText(String.format(Locale.getDefault(), "%.2f%%", lot.getFailRate()));

        // 雅뚯눘???븍뜄???醫륁굨 RecyclerView
        RecyclerView rvDefects = view.findViewById(R.id.rv_defects);
        rvDefects.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDefects.setNestedScrollingEnabled(false);
        rvDefects.setAdapter(new DefectAdapter(lot.getTopDefects()));
    }

    // ???? 嚥≪뮉?????뿺 ????????????????????????????????????????????????????????????????????????????????????
    private void bindAlarmTab(View view, LotSummary lot, Bundle args) {
        // ??而?袁⑦?燁삳똻???
        List<LotAlarm> alarms = new ArrayList<>(lot.getAlarms());

        String oracleMessage = args.getString(K_ORACLE_MESSAGE, "");
        String oracleLevel = args.getString(K_ORACLE_LEVEL, "");
        String[] oracleCodes = args.getStringArray(K_ORACLE_CODES);
        if (oracleMessage != null && !oracleMessage.trim().isEmpty()) {
            LotAlarm.Severity severity;
            if ("DANGER".equalsIgnoreCase(oracleLevel)) {
                severity = LotAlarm.Severity.CRITICAL;
            } else if ("NORMAL".equalsIgnoreCase(oracleLevel)) {
                severity = LotAlarm.Severity.INFO;
            } else {
                severity = LotAlarm.Severity.WARNING;
            }

            StringBuilder oracleTitle = new StringBuilder("\uC624\uB77C\uD074 \uBD84\uC11D");
            if (oracleCodes != null && oracleCodes.length > 0) {
                oracleTitle.append(" \u00B7 ").append(oracleCodes[0]);
            }

            alarms.add(0, new LotAlarm(
                    "oracle-linked",
                    lot.getEndTime(),
                    LotAlarm.Type.ORACLE,
                    severity,
                    oracleTitle.toString(),
                    oracleMessage
            ));
        }

        int critical = 0, warning = 0, info = 0;
        for (LotAlarm a : alarms) {
            switch (a.getSeverity()) {
                case CRITICAL: critical++; break;
                case WARNING:  warning++;  break;
                default:       info++;     break;
            }
        }
        TextView tvCritical = view.findViewById(R.id.tv_alarm_critical_count);
        TextView tvWarning  = view.findViewById(R.id.tv_alarm_warning_count);
        TextView tvInfo     = view.findViewById(R.id.tv_alarm_info_count);
        tvCritical.setText(String.valueOf(critical));
        tvWarning.setText(String.valueOf(warning));
        tvInfo.setText(String.valueOf(info));

        // ???뿺 ??곸벉 筌롫뗄?놅쭪?
        View tvNoAlarms = view.findViewById(R.id.tv_no_alarms);
        if (tvNoAlarms != null) {
            tvNoAlarms.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
        }

        // ???뿺 ???袁⑥뵬??RecyclerView
        RecyclerView rvAlarms = view.findViewById(R.id.rv_alarms);
        rvAlarms.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlarms.setNestedScrollingEnabled(false);
        rvAlarms.setAdapter(new AlarmAdapter(alarms));
    }
}







