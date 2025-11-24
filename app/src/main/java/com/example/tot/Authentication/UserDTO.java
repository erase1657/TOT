package com.example.tot.Authentication;

import com.google.firebase.Timestamp;

/**
 * 유저 정보 DTO(데이터 객체)
 * ✅ backgroundImageUrl 필드 추가
 */
public class UserDTO {
    private String nickname;
    private String profileImageUrl;
    private String backgroundImageUrl;  // ✅ 배경 이미지 URL 추가
    private String comment;
    private String address;
    private Timestamp createAt;

    public UserDTO() {}

    public UserDTO(String nickname, String profileImageUrl, String backgroundImageUrl,
                   String comment, String address, Timestamp createAt) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.backgroundImageUrl = backgroundImageUrl;
        this.comment = comment;
        this.address = address;
        this.createAt = createAt;
    }

    // Getters and Setters
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Timestamp getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Timestamp createAt) {
        this.createAt = createAt;
    }
}