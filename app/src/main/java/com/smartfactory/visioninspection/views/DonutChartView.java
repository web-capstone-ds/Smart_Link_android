package com.smartfactory.visioninspection.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

/**
 * SVG 도넛 차트 → Android Canvas 커스텀 뷰 변환
 * PASS(초록) / MARGINAL(노랑) / FAIL(빨강) 3색 아크 + 중앙 수율 텍스트
 */
public class DonutChartView extends View {

    // ── 색상 상수 ────────────────────────────────────────────
    private static final int COLOR_PASS     = 0xFF3FB950;
    private static final int COLOR_MARGINAL = 0xFFD29922;
    private static final int COLOR_FAIL     = 0xFFF85149;
    private static final int COLOR_TRACK    = 0xFF1E2530;

    // ── Paint 객체 ───────────────────────────────────────────
    private  Paint arcPaint;
    private  Paint yieldPaint;
    private  Paint labelPaint;

    // ── 데이터 ───────────────────────────────────────────────
    private int   passUnits     = 0;
    private int   marginalUnits = 0;
    private int   failUnits     = 0;
    private int   totalUnits    = 1; // 0 나눔 방지
    private boolean dataSet     = false;

    // ── 생성자 ───────────────────────────────────────────────
    public DonutChartView(Context context) {
        super(context); init();
    }
    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs); init();
    }
    public DonutChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        yieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yieldPaint.setTextAlign(Paint.Align.CENTER);
        yieldPaint.setFakeBoldText(true);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(0xFF7D8590);
    }

    /** 데이터 주입 후 invalidate() 호출 */
    public void setData(int passUnits, int marginalUnits, int failUnits, int totalUnits) {
        this.passUnits     = passUnits;
        this.marginalUnits = marginalUnits;
        this.failUnits     = failUnits;
        this.totalUnits    = (totalUnits > 0) ? totalUnits : 1;
        this.dataSet       = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!dataSet) return;

        final int   w           = getWidth();
        final int   h           = getHeight();
        final int   size        = Math.min(w, h);
        final float cx          = w / 2f;
        final float cy          = h / 2f;
        final float strokeWidth = size * 0.115f;
        final float radius      = size / 2f - strokeWidth / 2f - 6;

        arcPaint.setStrokeWidth(strokeWidth);

        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        // ── 배경 트랙 ─────────────────────────────────────────
        arcPaint.setColor(COLOR_TRACK);
        canvas.drawOval(oval, arcPaint);

        float passAngle     = (passUnits     / (float) totalUnits) * 360f;
        float marginalAngle = (marginalUnits / (float) totalUnits) * 360f;
        float failAngle     = (failUnits     / (float) totalUnits) * 360f;
        float startAngle    = -90f;

        // ── PASS 아크 (초록) ─────────────────────────────────
        if (passUnits > 0) {
            arcPaint.setColor(COLOR_PASS);
            canvas.drawArc(oval, startAngle, passAngle, false, arcPaint);
            startAngle += passAngle;
        }
        // ── MARGINAL 아크 (노랑) ─────────────────────────────
        if (marginalUnits > 0) {
            arcPaint.setColor(COLOR_MARGINAL);
            canvas.drawArc(oval, startAngle, marginalAngle, false, arcPaint);
            startAngle += marginalAngle;
        }
        // ── FAIL 아크 (빨강) ─────────────────────────────────
        if (failUnits > 0) {
            arcPaint.setColor(COLOR_FAIL);
            canvas.drawArc(oval, startAngle, failAngle, false, arcPaint);
        }

        // ── 중앙 수율 텍스트 ──────────────────────────────────
        float yieldRate = (passUnits / (float) totalUnits) * 100f;
        int yieldColor  = yieldRate >= 95f ? COLOR_PASS
                : yieldRate >= 85f ? COLOR_MARGINAL
                : COLOR_FAIL;

        float yieldTextSize = size * 0.19f;
        yieldPaint.setTextSize(yieldTextSize);
        yieldPaint.setColor(yieldColor);
        canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", yieldRate),
                cx, cy + yieldTextSize * 0.37f, yieldPaint);

        // ── 중앙 하단 보조 레이블 "LOT 수율" ─────────────────
        float labelTextSize = size * 0.09f;
        labelPaint.setTextSize(labelTextSize);
        canvas.drawText("LOT 수율",
                cx, cy + yieldTextSize * 0.37f + labelTextSize * 1.4f, labelPaint);
    }
}
