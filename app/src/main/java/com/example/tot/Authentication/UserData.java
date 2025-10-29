package com.example.tot.Authentication;

public class UserData {
    private String Email;
    private String Uid;
    private String profile;
    private String NickName;
    private String statusMessage;  // 상태메시지 추가
    private String location;       // 위치 추가

    // 기본 생성자
    public UserData() {
        this.statusMessage = "";  // 기본값 공백
        this.location = "";       // 기본값 공백
    }

    // Getter & Setter
    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getNickName() {
        return NickName;
    }

    public void setNickName(String nickName) {
        NickName = nickName;
    }

    public String getStatusMessage() {
        return statusMessage != null ? statusMessage : "";
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getLocation() {
        return location != null ? location : "";
    }

    public void setLocation(String location) {
        this.location = location;
    }
}