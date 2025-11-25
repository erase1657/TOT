package com.example.tot.Notification;

public class NotificationDTO {

    public enum NotificationType {
        SCHEDULE_INVITE,
        FOLLOW,
        COMMENT,
        POST  // ✅ 친구 게시글 알림 타입 추가
    }

    private String id;
    private NotificationType type;
    private String title;
    private String content;
    private String timestamp;
    private String timeDisplay;
    private boolean isRead;
    private int unreadCount;
    private String userName;
    private String userId;
    private int iconResId;
    private long createdAt;
    private String postId;

    private NotificationDTO(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.title = builder.title;
        this.content = builder.content;
        this.timeDisplay = builder.timeDisplay;
        this.isRead = builder.isRead;
        this.unreadCount = builder.unreadCount;
        this.userName = builder.userName;
        this.userId = builder.userId;
        this.iconResId = builder.iconResId;
        this.createdAt = builder.createdAt;
        this.postId = builder.postId;
    }

    /**
     * ✅ 스케줄 초대 알림 생성
     */
    public static NotificationDTO createScheduleInvite(String id, String scheduleName,
                                                       String content, String timeDisplay,
                                                       boolean isRead, int unreadCount,
                                                       int iconResId, String userId,
                                                       long createdAt) {
        return new Builder(id, NotificationType.SCHEDULE_INVITE)
                .title(scheduleName + " 여행 일정에 초대되었습니다")
                .content(content)
                .timeDisplay(timeDisplay)
                .isRead(isRead)
                .unreadCount(unreadCount)
                .iconResId(iconResId)
                .userId(userId)
                .createdAt(createdAt)
                .build();
    }

    /**
     * ✅ 팔로우 알림 생성
     */
    public static NotificationDTO createFollow(String id, String userName,
                                               String timeDisplay, boolean isRead,
                                               int iconResId, String userId,
                                               long createdAt) {
        return new Builder(id, NotificationType.FOLLOW)
                .userName(userName)
                .userId(userId)
                .title(userName + " 님이 회원님을 팔로우했습니다")
                .content("프로필을 확인해 주세요")
                .timeDisplay(timeDisplay)
                .isRead(isRead)
                .iconResId(iconResId)
                .createdAt(createdAt)
                .build();
    }

    /**
     * ✅ 댓글 알림 생성
     */
    public static NotificationDTO createComment(String id, String userName,
                                                String content, String timeDisplay,
                                                boolean isRead, int unreadCount,
                                                int iconResId, String userId,
                                                long createdAt) {
        return new Builder(id, NotificationType.COMMENT)
                .userName(userName)
                .userId(userId)
                .title(userName + " 님이 게시물에 댓글을 남겼습니다")
                .content(content)
                .timeDisplay(timeDisplay)
                .isRead(isRead)
                .unreadCount(unreadCount)
                .iconResId(iconResId)
                .createdAt(createdAt)
                .build();
    }

    /**
     * ✅ 친구 게시글 알림 생성
     */
    public static NotificationDTO createPost(String id, String userName,
                                             String postTitle, String timeDisplay,
                                             boolean isRead, int unreadCount,
                                             int iconResId, String userId,
                                             String postId, long createdAt) {
        return new Builder(id, NotificationType.POST)
                .userName(userName)
                .userId(userId)
                .postId(postId)
                .title(userName + " 님이 새로운 게시글을 올렸습니다")
                .content(postTitle)
                .timeDisplay(timeDisplay)
                .isRead(isRead)
                .unreadCount(unreadCount)
                .iconResId(iconResId)
                .createdAt(createdAt)
                .build();
    }

    public static class Builder {
        private final String id;
        private final NotificationType type;
        private String title;
        private String content;
        private String timeDisplay;
        private boolean isRead;
        private int unreadCount;
        private String userName;
        private String userId;
        private int iconResId;
        private long createdAt;
        private String postId;

        public Builder(String id, NotificationType type) {
            this.id = id;
            this.type = type;
            this.createdAt = System.currentTimeMillis();
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder timeDisplay(String timeDisplay) {
            this.timeDisplay = timeDisplay;
            return this;
        }

        public Builder isRead(boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public Builder unreadCount(int unreadCount) {
            this.unreadCount = unreadCount;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder iconResId(int iconResId) {
            this.iconResId = iconResId;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder postId(String postId) {
            this.postId = postId;
            return this;
        }

        public NotificationDTO build() {
            return new NotificationDTO(this);
        }
    }

    // Getters
    public String getId() { return id; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
    public String getTimeDisplay() { return timeDisplay; }
    public boolean isRead() { return isRead; }
    public int getUnreadCount() { return unreadCount; }
    public String getUserName() { return userName; }
    public String getUserId() { return userId; }
    public int getIconResId() { return iconResId; }
    public long getCreatedAt() { return createdAt; }
    public String getPostId() { return postId; }

    // Setters
    public void setRead(boolean read) { isRead = read; }
    public void setUnreadCount(int count) { unreadCount = count; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setPostId(String postId) { this.postId = postId; }
}