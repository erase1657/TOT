package com.example.tot.Schedule.ScheduleSetting.Invite;

import com.google.firebase.Timestamp;

public class InviteDTO {
    private String scheduleID;
    private Timestamp invitedAt;
    private String senderUID;
    private String receiverUID;
    private String status;
    public InviteDTO() {}
    public InviteDTO(String scheduleID, String senderUID, String receiverUID, String status,Timestamp invitedAt) {
        this.scheduleID = scheduleID;
        this.invitedAt = invitedAt;
        this.senderUID = senderUID;
        this.receiverUID = receiverUID;
        this.status = status;
    }

    public String getScheduleID() {
        return scheduleID;
    }

    public void setScheduleID(String scheduleID) {
        this.scheduleID = scheduleID;
    }

    public Timestamp getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Timestamp invitedAt) {
        this.invitedAt = invitedAt;
    }

    public String getSenderUID() {
        return senderUID;
    }

    public void setSenderUID(String senderUID) {
        this.senderUID = senderUID;
    }

    public String getReceiverUID() {
        return receiverUID;
    }

    public void setReceiverUID(String receiverUID) {
        this.receiverUID = receiverUID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
