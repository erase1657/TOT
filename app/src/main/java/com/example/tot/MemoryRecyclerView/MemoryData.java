package com.example.tot.MemoryRecyclerView;

public class MemoryData {
    private String title;
    private String date;
    private String room;
    private String startTime;
    private String endTime;
    private int locationIconResId; // 위치 아이콘 리소스 (선택 사항)

    public MemoryData(String title, String date, String room, String startTime, String endTime, int locationIconResId) {
        this.title = title;
        this.date = date;
        this.room = room;
        this.startTime = startTime;
        this.endTime = endTime;
        this.locationIconResId = locationIconResId;
    }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getRoom() { return room; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getLocationIconResId() { return locationIconResId; }

    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setRoom(String room) { this.room = room; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setLocationIconResId(int locationIconResId) { this.locationIconResId = locationIconResId; }
}
