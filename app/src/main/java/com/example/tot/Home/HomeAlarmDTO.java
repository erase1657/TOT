package com.example.tot.Home;

import com.google.firebase.Timestamp;

public class HomeAlarmDTO {
    private String ScheduledId;
    private String ScheduleItemId;
    private String title;
    private String date;
    private String place;
    private Timestamp startTime;
    private Timestamp endTime;

    public HomeAlarmDTO(String scheduledId, String scheduleItemId, String title, String date, String place, Timestamp startTime, Timestamp endTime) {
        ScheduledId = scheduledId;
        ScheduleItemId = scheduleItemId;
        this.title = title;
        this.date = date;
        this.place = place;
        this.startTime = startTime;
        this.endTime = endTime;
    }

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
}
