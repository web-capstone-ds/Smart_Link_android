package com.smartfactory.visioninspection.models;

/** LOT 주요 불량 유형 (DetailBottomSheet topDefects 대응) */
public class LotDefect {
    private final String code;
    private final String description;
    private final int    count;
    private final float  ratio; // 전체 불합격 대비 %

    public LotDefect(String code, String description, int count, float ratio) {
        this.code        = code;
        this.description = description;
        this.count       = count;
        this.ratio       = ratio;
    }

    public String getCode()        { return code; }
    public String getDescription() { return description; }
    public int    getCount()       { return count; }
    public float  getRatio()       { return ratio; }
}
