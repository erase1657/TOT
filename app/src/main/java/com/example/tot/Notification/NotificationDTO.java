package com.example.tot.Notification;

public class NotificationDTO {

    public enum NotificationType {
        SCHEDULE_INVITE,  // 일정 초대
        FOLLOW,           // 팔로우
        COMMENT           // 댓글
    }

    private String id;
    private NotificationType type;
    private String title;
    private String content;
    private String timestamp;
    private String timeDisplay; // "9분전", "14일" 등
    private boolean isRead;
    private int unreadCount; // 읽지 않은 메시지 수 (SCHEDULE_INVITE, COMMENT용)
    private String userName; // 팔로우/댓글 사용자 이름
    private int iconResId;

    // Private 생성자 - Builder를 통해서만 생성
    private NotificationDTO(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.title = builder.title;
        this.content = builder.content;
        this.timeDisplay = builder.timeDisplay;
        this.isRead = builder.isRead;
        this.unreadCount = builder.unreadCount;
        this.userName = builder.userName;
        this.iconResId = builder.iconResId;
    }

    // 정적 팩토리 메서드들 - 따옴표 제거 및 말투 통일
    public static NotificationDTO createScheduleInvite(String id, String scheduleName,
                                                       String content, String timeDisplay,
                                                       boolean isRead, int unreadCount, int iconResId) {
        return new Builder(id, NotificationType.SCHEDULE_INVITE)
                .title(scheduleName + " 여행 일정에 초대되었습니다")
                .content(content)
                .timeDisplay(timeDisplay)
                .isRead(isRead)
                .unreadCount(unreadCount)
                .iconResId(iconResId)
                .build();
    }

    public static NotificationDTO createFollow(String id, String userName,
                                               String timeDisplay, boolean isRead, int iconResId) {
        return new Builder(id, NotificationType.FOLLOW)
                .userName(userName)
                .title(userName + " 님이 회원님을 팔로우했습니다")
                .content("프로필을 확인해 주세요")
                .timeDisplay(timeDisplay)
                .isRead(isRead)
                .iconResId(iconResId)
                .build();
    }

    public static NotificationDTO createComment(String id, String userName,
                                                String content, String timeDisplay,
                                                boolean isRead, int unreadCount, int iconResId) {
        return new Builder(id, NotificationType.COMMENT)
                .userName(userName)
                .title(userName + " 님이 게시물에 댓글을 남겼습니다")
                .content(content)
                .timeDisplay(timeDisplay)
                .isRead(isRead)
                .unreadCount(unreadCount)
                .iconResId(iconResId)
                .build();
    }

    // Builder 클래스
    public static class Builder {
        private final String id;
        private final NotificationType type;
        private String title;
        private String content;
        private String timeDisplay;
        private boolean isRead;
        private int unreadCount;
        private String userName;
        private int iconResId;

        public Builder(String id, NotificationType type) {
            this.id = id;
            this.type = type;
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

        public Builder iconResId(int iconResId) {
            this.iconResId = iconResId;
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
    public int getIconResId() { return iconResId; }

    // Setters
    public void setRead(boolean read) { isRead = read; }
    public void setUnreadCount(int count) { unreadCount = count; }
}