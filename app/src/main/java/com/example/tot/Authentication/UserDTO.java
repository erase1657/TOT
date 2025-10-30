package com.example.tot.Authentication;

import com.google.firebase.Timestamp;

/**
 * 유저 정보 DTO(데이터 객체)
 * uid,email,password 항목은 파이어베이스 시스템 내부에서 저장/관리
 */
public class UserDTO {
    private String nickname;
    private String profileImageUrl;
    private String comment;
    private String address;
    private Timestamp createAt;

    public UserDTO() {};
    public UserDTO(String nickname, String profileImageUrl, String comment, String address, Timestamp createAt) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.comment = comment;
        this.address = address;
        this.createAt = createAt;
    }

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
