package com.example.tot.Schedule.ScheduleSetting;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

public class ScheduleItemDTO {

    private String title;
    private Timestamp startTime; //스케줄 시간(시작), 몇시부터 할지
    private Timestamp endTime; //스케줄 시간(끝), 몇시까지 할지
    private GeoPoint place;
    private String placeName;
    private Boolean alarm;

    public ScheduleItemDTO() {
    }

    public ScheduleItemDTO(String title, Timestamp startTime, Timestamp endTime, GeoPoint place, String placeName, Boolean alarm) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.place = place;
        this.placeName = placeName;
        this.alarm = alarm;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public GeoPoint getPlace() {
        return place;
    }

    public void setPlace(GeoPoint place) {
        this.place = place;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public Boolean getAlarm() {
        return alarm;
    }

    public void setAlarm(Boolean alarm) {
        this.alarm = Boolean.valueOf(alarm);
    }
}
