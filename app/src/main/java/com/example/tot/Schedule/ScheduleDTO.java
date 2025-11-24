package com.example.tot.Schedule;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

public class ScheduleDTO{
    private String scheduleId;
    private String locationName;
    private Timestamp startDate;
    private Timestamp endDate;
    private DocumentReference thumbnailRef;
    private String inviteCode;
    private int invitedCount;
    private String background; // 배경 이미지 URL 필드 추가

    public ScheduleDTO() {
        // Firestore Deserialization을 위한 빈 생성자
    }

    public ScheduleDTO(String scheduleId, String locationName, Timestamp startDate, Timestamp endDate, DocumentReference thumbnailRef, String inviteCode, int invitedCount) {
        this.scheduleId = scheduleId;
        this.locationName = locationName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailRef = thumbnailRef;
        this.inviteCode = inviteCode;
        this.invitedCount = invitedCount;
    }

    // --- 기존 Getter/Setter ---

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public DocumentReference getThumbnailRef() {
        return thumbnailRef;
    }

    public void setThumbnailRef(DocumentReference thumbnailRef) {
        this.thumbnailRef = thumbnailRef;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public int getInvitedCount() {
        return invitedCount;
    }

    public void setInvitedCount(int invitedCount) {
        this.invitedCount = invitedCount;
    }

    // ▼▼▼ 새로 추가된 Getter/Setter ▼▼▼
    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }
}