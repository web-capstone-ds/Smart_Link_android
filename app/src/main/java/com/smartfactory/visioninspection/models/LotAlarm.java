package com.smartfactory.visioninspection.models;

/** LOT 알람 타임라인 항목 (DetailBottomSheet alarms 대응) */
public class LotAlarm {

    public enum Type     { HW_ALARM, THRESHOLD, ORACLE }
    public enum Severity { CRITICAL, WARNING, INFO }

    private final String   id;
    private final String   time;
    private final Type     type;
    private final Severity severity;
    private final String   title;
    private final String   description;

    public LotAlarm(String id, String time, Type type,
                    Severity severity, String title, String description) {
        this.id          = id;
        this.time        = time;
        this.type        = type;
        this.severity    = severity;
        this.title       = title;
        this.description = description;
    }

    public String   getId()          { return id; }
    public String   getTime()        { return time; }
    public Type     getType()        { return type; }
    public Severity getSeverity()    { return severity; }
    public String   getTitle()       { return title; }
    public String   getDescription() { return description; }

    public String getTypeLabel() {
        switch (type) {
            case HW_ALARM:  return "HW 알람";
            case ORACLE:    return "Oracle AI";
            default:        return "임계값";
        }
    }
}
