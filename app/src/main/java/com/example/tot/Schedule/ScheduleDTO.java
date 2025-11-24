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
    private String backgroundImageUri;


    public ScheduleDTO() {
    }

    public ScheduleDTO(String scheduleId, String locationName, Timestamp startDate, Timestamp endDate, DocumentReference thumbnailRef, String inviteCode, int invitedCount, String backgroundImageUri) {
        this.scheduleId = scheduleId;
        this.locationName = locationName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailRef = thumbnailRef;
        this.inviteCode = inviteCode;
        this.invitedCount = invitedCount;
        this.backgroundImageUri = backgroundImageUri;

    }

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

    public String getBackgroundImageUri() {
        return backgroundImageUri;
    }

    public void setBackgroundImageUri(String backgroundImageUri) {
        this.backgroundImageUri = backgroundImageUri;
    }
}