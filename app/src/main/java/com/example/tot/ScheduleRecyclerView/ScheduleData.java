package com.example.tot.ScheduleRecyclerView;

public class ScheduleData {
    private String scheduleId;        // 스케줄 고유 ID (서버 연동용)
    private String location;          // 여행 지역 (예: "익산")
    private String dateRange;         // 날짜 범위 (예: "23-27")
    private String yearMonth;         // 년월 (예: "2020년 8월")
    private int backgroundImage;      // 배경 이미지 리소스
    private String fullStartDate;     // 전체 시작 날짜 (예: "2020-08-23")
    private String fullEndDate;       // 전체 종료 날짜 (예: "2020-08-27")

    // 기본 생성자
    public ScheduleData(String scheduleId, String location, String dateRange,
                        String yearMonth, int backgroundImage) {
        this.scheduleId = scheduleId;
        this.location = location;
        this.dateRange = dateRange;
        this.yearMonth = yearMonth;
        this.backgroundImage = backgroundImage;
    }

    // 전체 날짜 정보 포함 생성자
    public ScheduleData(String scheduleId, String location, String dateRange,
                        String yearMonth, int backgroundImage,
                        String fullStartDate, String fullEndDate) {
        this.scheduleId = scheduleId;
        this.location = location;
        this.dateRange = dateRange;
        this.yearMonth = yearMonth;
        this.backgroundImage = backgroundImage;
        this.fullStartDate = fullStartDate;
        this.fullEndDate = fullEndDate;
    }

    // Getter 메서드
    public String getScheduleId() {
        return scheduleId;
    }

    public String getLocation() {
        return location;
    }

    public String getDateRange() {
        return dateRange;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public int getBackgroundImage() {
        return backgroundImage;
    }

    public String getFullStartDate() {
        return fullStartDate;
    }

    public String getFullEndDate() {
        return fullEndDate;
    }

    // Setter 메서드
    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public void setBackgroundImage(int backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public void setFullStartDate(String fullStartDate) {
        this.fullStartDate = fullStartDate;
    }

    public void setFullEndDate(String fullEndDate) {
        this.fullEndDate = fullEndDate;
    }
}