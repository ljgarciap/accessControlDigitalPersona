package com.softclass.fingerprint;

public class AttendanceRecord {
    private final String employeeName;
    private final String timestamp;
    private final String type;

    public AttendanceRecord(String employeeName, String timestamp, String type) {
        this.employeeName = employeeName;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }
}
