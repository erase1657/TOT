package com.example.tot.Follow;

public class FollowUserDTO {
    private String userId;              // 사용자 ID
    private String userName;            // 사용자 이름
    private String nickname;            // 내가 설정한 별명
    private String statusMessage;       // 상태메시지
    private int profileImage;           // 프로필 이미지
    private boolean isFollowing;        // 내가 팔로우 중인지 여부
    private boolean isFollower;         // 나를 팔로우하는지 여부
    private long followedAt;            // 팔로우한 시간 (타임스탬프)

    public FollowUserDTO() {
    }

    public FollowUserDTO(String userId, String userName, String nickname,
                         String statusMessage, int profileImage,
                         boolean isFollowing, boolean isFollower, long followedAt) {
        this.userId = userId;
        this.userName = userName;
        this.nickname = nickname;
        this.statusMessage = statusMessage;
        this.profileImage = profileImage;
        this.isFollowing = isFollowing;
        this.isFollower = isFollower;
        this.followedAt = followedAt;
    }

    // Getters and Setters
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public int getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(int profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }

    public boolean isFollower() {
        return isFollower;
    }

    public void setFollower(boolean follower) {
        isFollower = follower;
    }

    public long getFollowedAt() {
        return followedAt;
    }

    public void setFollowedAt(long followedAt) {
        this.followedAt = followedAt;
    }

    /**
     * 표시할 이름 반환 (별명 우선, 없으면 실제 이름)
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname;
        }
        return userName;
    }

    /**
     * 맞팔로우 여부
     */
    public boolean isMutualFollow() {
        return isFollowing && isFollower;
    }
}