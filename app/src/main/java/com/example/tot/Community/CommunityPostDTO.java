package com.example.tot.Community;

public class CommunityPostDTO {
    private String postId;
    private String userId;
    private String scheduleId;
    private String authorUid;
    private String userName;
    private int userProfileImage;
    private String profileImageUrl; // ✅ Firestore에서 가져온 프로필 이미지 URL
    private String title;
    private int postImage;
    private int heartCount;
    private int commentCount;
    private String regionTag;
    private String provinceCode;
    private String cityCode;
    private long createdAt;
    private boolean isFriend;
    private boolean isLiked;

    public CommunityPostDTO() {
    }

    public CommunityPostDTO(String postId, String userId, String userName, int userProfileImage,
                            String title, int postImage, int heartCount, int commentCount,
                            String regionTag, String provinceCode, String cityCode,
                            long createdAt, boolean isFriend) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.userProfileImage = userProfileImage;
        this.title = title;
        this.postImage = postImage;
        this.heartCount = heartCount;
        this.commentCount = commentCount;
        this.regionTag = regionTag;
        this.provinceCode = provinceCode;
        this.cityCode = cityCode;
        this.createdAt = createdAt;
        this.isFriend = isFriend;
        this.isLiked = false;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getAuthorUid() {
        return authorUid;
    }

    public void setAuthorUid(String authorUid) {
        this.authorUid = authorUid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getUserProfileImage() {
        return userProfileImage;
    }

    public void setUserProfileImage(int userProfileImage) {
        this.userProfileImage = userProfileImage;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPostImage() {
        return postImage;
    }

    public void setPostImage(int postImage) {
        this.postImage = postImage;
    }

    public int getHeartCount() {
        return heartCount;
    }

    public void setHeartCount(int heartCount) {
        this.heartCount = heartCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public String getRegionTag() {
        return regionTag;
    }

    public void setRegionTag(String regionTag) {
        this.regionTag = regionTag;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public void setFriend(boolean friend) {
        isFriend = friend;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public void toggleLike() {
        if (isLiked) {
            heartCount--;
            isLiked = false;
        } else {
            heartCount++;
            isLiked = true;
        }
    }
}