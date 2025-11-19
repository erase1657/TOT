package com.example.tot.Community;

public class CommunityPostDTO {
    private String postId;           // 게시글 ID
    private String userId;           // 작성자 ID
    private String userName;         // 작성자 이름
    private int userProfileImage;    // 작성자 프로필 이미지
    private String title;            // 게시글 제목
    private int postImage;           // 게시글 이미지
    private int heartCount;          // 좋아요 수
    private int commentCount;        // 댓글 수
    private String regionTag;        // 지역 태그 (예: "서울", "부산")
    private String provinceCode;     // 시/도 코드 (서버 연동용)
    private String cityCode;         // 시군구 코드 (서버 연동용)
    private long createdAt;          // 작성 시간 (타임스탬프)
    private boolean isFriend;        // 친구 여부
    private boolean isLiked;         // 현재 사용자가 좋아요 했는지 여부

    // 기본 생성자
    public CommunityPostDTO() {
    }

    // 전체 생성자
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

    // Getter & Setter
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

    /**
     * 좋아요 토글 (증가/감소)
     */
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