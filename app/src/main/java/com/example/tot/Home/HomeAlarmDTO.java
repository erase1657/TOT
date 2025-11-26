package com.example.tot.Home;

import com.google.firebase.Timestamp;

public class HomeAlarmDTO {
    // ✅ 기존 필드 (절대 수정 안함)
    private String ScheduledId;
    private String ScheduleItemId;
    private String title;
    private String date;
    private String place;
    private Timestamp startTime;
    private Timestamp endTime;

    // ✅ GPS 베어링 기능을 위한 새 필드 추가
    private double destLatitude;
    private double destLongitude;

    // ✅ 기존 생성자 (절대 수정 안함)
    public HomeAlarmDTO(String scheduledId, String scheduleItemId, String title, String date, String place, Timestamp startTime, Timestamp endTime) {
        ScheduledId = scheduledId;
        ScheduleItemId = scheduleItemId;
        this.title = title;
        this.date = date;
        this.place = place;
        this.startTime = startTime;
        this.endTime = endTime;
        this.destLatitude = 0.0;
        this.destLongitude = 0.0;
    }

    // ✅ GPS 좌표를 포함한 새 생성자 추가
    public HomeAlarmDTO(String scheduledId, String scheduleItemId, String title, String date, String place,
                        Timestamp startTime, Timestamp endTime, double destLatitude, double destLongitude) {
        ScheduledId = scheduledId;
        ScheduleItemId = scheduleItemId;
        this.title = title;
        this.date = date;
        this.place = place;
        this.startTime = startTime;
        this.endTime = endTime;
        this.destLatitude = destLatitude;
        this.destLongitude = destLongitude;
    }

    // ✅ 기존 Getter & Setter (절대 수정 안함)
    public String getScheduledId() {
        return ScheduledId;
    }

    public void setScheduledId(String scheduledId) {
        ScheduledId = scheduledId;
    }

    public String getScheduleItemId() {
        return ScheduleItemId;
    }

    public void setScheduleItemId(String scheduleItemId) {
        ScheduleItemId = scheduleItemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String room) {
        this.place = room;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    // ✅ GPS 좌표 Getter & Setter 추가
    public double getDestLatitude() {
        return destLatitude;
    }

    public void setDestLatitude(double destLatitude) {
        this.destLatitude = destLatitude;
    }

    public double getDestLongitude() {
        return destLongitude;
    }

    public void setDestLongitude(double destLongitude) {
        this.destLongitude = destLongitude;
    }

    // ✅ 좌표가 유효한지 확인하는 헬퍼 메서드 추가
    public boolean hasValidCoordinates() {
        return destLatitude != 0.0 && destLongitude != 0.0;
    }
}