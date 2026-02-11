package com.softclass.fingerprint;

public class AttendanceRecord {
    private final String employeeName;
    private final String timestamp;
    private final String type;
    private boolean active;

    public AttendanceRecord(String employeeName, String timestamp, String type, boolean active) {
        this.employeeName = employeeName;
        this.timestamp = timestamp;
        this.type = type;
        this.active = active;
    }

    public String getEmployeeName() { return employeeName; }
    public String getTimestamp() { return timestamp; }
    public String getType() { return type; }
}
