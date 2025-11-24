package com.example.tot.Community;

import com.google.firebase.Timestamp;

public class CommentDTO {
    private String commentId;
    private String postId;
    private String uid;
    private String nickname;
    private String profileImageUrl;
    private String content;
    private Timestamp timestamp;

    // Firestore용 기본 생성자
    public CommentDTO() {}

    public CommentDTO(String postId, String uid, String nickname, String profileImageUrl, String content, Timestamp timestamp) {
        this.postId = postId;
        this.uid = uid;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters
    public String getCommentId() { return commentId; }
    public String getPostId() { return postId; }
    public String getUid() { return uid; }
    public String getNickname() { return nickname; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getContent() { return content; }
    public Timestamp getTimestamp() { return timestamp; }

    // Setters
    public void setCommentId(String commentId) { this.commentId = commentId; }
    public void setPostId(String postId) { this.postId = postId; }
    public void setUid(String uid) { this.uid = uid; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}