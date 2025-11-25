package com.example.tot.Notification;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.example.tot.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotificationManager {

    private static final String TAG = "NotificationManager";
    private static final int MAX_CACHED_NOTIFICATIONS = 100;

    private static NotificationManager instance;
    private List<NotificationDTO> todayNotifications;
    private List<NotificationDTO> recentNotifications;
    private List<UnreadCountListener> listeners;

    private NotificationCache cache;
    private Context appContext;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ListenerRegistration followerListener;
    private ListenerRegistration commentListener;
    private ListenerRegistration postListener;  // ✅ 게시글 알림 리스너 추가
    private boolean isListening = false;

    // ✅ 게시글별 읽지 않은 댓글 수 추적
    private Map<String, Integer> unreadCommentCountByPost = new HashMap<>();

    // ✅ 사용자별 읽지 않은 게시글 수 추적
    private Map<String, Integer> unreadPostCountByUser = new HashMap<>();

    public interface UnreadCountListener {
        void onUnreadCountChanged(int count);
    }

    private NotificationManager() {
        todayNotifications = new ArrayList<>();
        recentNotifications = new ArrayList<>();
        listeners = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void init(Context context) {
        this.appContext = context.getApplicationContext();
        this.cache = new NotificationCache(appContext);
        Log.d(TAG, "✅ NotificationManager 초기화");
    }

    public void initialLoad() {
        if (cache == null) {
            Log.w(TAG, "⚠️ Cache가 초기화되지 않았습니다");
            return;
        }

        Log.d(TAG, "🚀 초기 알림 로드 시작");

        loadNotificationsFromCache();
        startListeningForFollowers();
        startListeningForComments();
        startListeningForPosts();  // ✅ 게시글 알림 리스너 시작
    }

    private void loadNotificationsFromCache() {
        if (cache == null) {
            Log.w(TAG, "⚠️ Cache가 초기화되지 않았습니다");
            return;
        }

        List<NotificationDTO> cached = cache.loadNotifications();

        if (cached.size() > MAX_CACHED_NOTIFICATIONS) {
            cached = cached.subList(0, MAX_CACHED_NOTIFICATIONS);
            cache.saveNotifications(cached);
            Log.d(TAG, "🗑️ 오래된 알림 자동 삭제");
        }

        // ✅ 게시글별 읽지 않은 댓글 수 계산 및 사용자별 게시글 수 계산
        recalculateUnreadCounts(cached);

        splitNotifications(cached);
        notifyUnreadCountChanged();

        Log.d(TAG, "📱 로컬 캐시 로드: " + cached.size() + "개");
    }

    /**
     * ✅ 읽지 않은 댓글/게시글 수 재계산
     */
    private void recalculateUnreadCounts(List<NotificationDTO> notifications) {
        unreadCommentCountByPost.clear();
        unreadPostCountByUser.clear();

        // 댓글 알림 그룹화 (게시글별)
        Map<String, List<NotificationDTO>> commentsByPost = new HashMap<>();

        // 게시글 알림 그룹화 (사용자별)
        Map<String, List<NotificationDTO>> postsByUser = new HashMap<>();

        for (NotificationDTO notif : notifications) {
            if (notif.getType() == NotificationDTO.NotificationType.COMMENT && !notif.isRead()) {
                String postId = notif.getPostId();
                if (postId != null && !postId.isEmpty()) {
                    if (!commentsByPost.containsKey(postId)) {
                        commentsByPost.put(postId, new ArrayList<>());
                    }
                    commentsByPost.get(postId).add(notif);
                }
            } else if (notif.getType() == NotificationDTO.NotificationType.POST && !notif.isRead()) {
                String userId = notif.getUserId();
                if (userId != null && !userId.isEmpty()) {
                    if (!postsByUser.containsKey(userId)) {
                        postsByUser.put(userId, new ArrayList<>());
                    }
                    postsByUser.get(userId).add(notif);
                }
            }
        }

        // 댓글: 게시글별 카운트 설정
        for (Map.Entry<String, List<NotificationDTO>> entry : commentsByPost.entrySet()) {
            String postId = entry.getKey();
            List<NotificationDTO> postComments = entry.getValue();

            postComments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

            int unreadCount = postComments.size();
            unreadCommentCountByPost.put(postId, unreadCount);

            if (!postComments.isEmpty()) {
                postComments.get(0).setUnreadCount(unreadCount);
            }

            for (int i = 1; i < postComments.size(); i++) {
                postComments.get(i).setUnreadCount(0);
            }
        }

        // ✅ 게시글: 사용자별 카운트 설정
        for (Map.Entry<String, List<NotificationDTO>> entry : postsByUser.entrySet()) {
            String userId = entry.getKey();
            List<NotificationDTO> userPosts = entry.getValue();

            userPosts.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

            int unreadCount = userPosts.size();
            unreadPostCountByUser.put(userId, unreadCount);

            if (!userPosts.isEmpty()) {
                userPosts.get(0).setUnreadCount(unreadCount);
            }

            for (int i = 1; i < userPosts.size(); i++) {
                userPosts.get(i).setUnreadCount(0);
            }
        }

        Log.d(TAG, "✅ 댓글 카운트 계산 완료: " + commentsByPost.size() + "개 게시글");
        Log.d(TAG, "✅ 게시글 카운트 계산 완료: " + postsByUser.size() + "명 사용자");
    }

    private void startListeningForFollowers() {
        if (isListening || mAuth.getCurrentUser() == null) {
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        long lastCheck = cache.getLastCheckTime();

        Log.d(TAG, "👂 follower 컬렉션 실시간 감지 시작 (마지막 확인: " + lastCheck + ")");

        followerListener = db.collection("user")
                .document(userId)
                .collection("follower")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ follower 리스너 오류", error);
                        return;
                    }

                    if (snapshots == null || snapshots.getDocumentChanges().isEmpty()) {
                        return;
                    }

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = change.getDocument();
                            Long followedAt = doc.getLong("followedAt");

                            if (followedAt != null && followedAt > lastCheck) {
                                String followerId = doc.getId();
                                createLocalFollowNotification(followerId, followedAt);
                            }
                        }
                    }

                    cache.setLastCheckTime(System.currentTimeMillis());
                });

        isListening = true;
    }

    private void startListeningForComments() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        long lastCheck = cache.getLastCheckTime();

        Log.d(TAG, "👂 commentNotifications 컬렉션 실시간 감지 시작");

        commentListener = db.collection("user")
                .document(userId)
                .collection("commentNotifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ commentNotifications 리스너 오류", error);
                        return;
                    }

                    if (snapshots == null || snapshots.getDocumentChanges().isEmpty()) {
                        return;
                    }

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = change.getDocument();
                            Long timestamp = doc.getLong("timestamp");

                            if (timestamp != null && timestamp > lastCheck) {
                                String postId = doc.getString("postId");
                                String commenterId = doc.getString("commenterId");
                                String commenterName = doc.getString("commenterName");
                                String commentContent = doc.getString("commentContent");

                                createLocalCommentNotification(
                                        doc.getId(),
                                        postId,
                                        commenterId,
                                        commenterName,
                                        commentContent,
                                        timestamp
                                );

                                doc.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ 처리된 댓글 알림 삭제: " + doc.getId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ 댓글 알림 삭제 실패", e);
                                        });
                            }
                        }
                    }

                    cache.setLastCheckTime(System.currentTimeMillis());
                });
    }

    /**
     * ✅ 친구 게시글 알림 리스너 시작
     */
    private void startListeningForPosts() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        long lastCheck = cache.getLastCheckTime();

        Log.d(TAG, "👂 postNotifications 컬렉션 실시간 감지 시작");

        postListener = db.collection("user")
                .document(userId)
                .collection("postNotifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ postNotifications 리스너 오류", error);
                        return;
                    }

                    if (snapshots == null || snapshots.getDocumentChanges().isEmpty()) {
                        return;
                    }

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = change.getDocument();
                            Long timestamp = doc.getLong("timestamp");

                            if (timestamp != null && timestamp > lastCheck) {
                                String postId = doc.getString("postId");
                                String authorId = doc.getString("authorId");
                                String authorName = doc.getString("authorName");
                                String postTitle = doc.getString("postTitle");

                                createLocalPostNotification(
                                        doc.getId(),
                                        postId,
                                        authorId,
                                        authorName,
                                        postTitle,
                                        timestamp
                                );

                                doc.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ 처리된 게시글 알림 삭제: " + doc.getId());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ 게시글 알림 삭제 실패", e);
                                        });
                            }
                        }
                    }

                    cache.setLastCheckTime(System.currentTimeMillis());
                });
    }
    // Part 1에서 이어짐...

    private void createLocalFollowNotification(String followerId, long followedAt) {
        db.collection("user")
                .document(followerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String nickname = doc.getString("nickname");
                    if (nickname == null || nickname.isEmpty()) {
                        nickname = "사용자";
                    }

                    NotificationDTO notification = NotificationDTO.createFollow(
                            "follow_" + followerId + "_" + followedAt,
                            nickname,
                            getTimeDisplay(followedAt),
                            false,
                            R.drawable.ic_user_add,
                            followerId,
                            followedAt
                    );

                    addNotificationToCache(notification);

                    Log.d(TAG, "✅ 로컬 팔로우 알림 생성: " + nickname);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로워 정보 조회 실패", e);
                });
    }

    private void createLocalCommentNotification(String notificationId, String postId,
                                                String commenterId, String commenterName,
                                                String commentContent, long timestamp) {
        int currentCount = unreadCommentCountByPost.getOrDefault(postId, 0);
        int newCount = currentCount + 1;
        unreadCommentCountByPost.put(postId, newCount);

        NotificationDTO notification = NotificationDTO.createComment(
                "comment_" + notificationId,
                commenterName,
                commentContent,
                getTimeDisplay(timestamp),
                false,
                newCount,
                R.drawable.ic_comment,
                commenterId,
                timestamp
        );

        notification.setPostId(postId);

        addCommentNotificationAndUpdateCounts(notification, postId);

        Log.d(TAG, "✅ 로컬 댓글 알림 생성: " + commenterName + " - " + commentContent + " (카운트: " + newCount + ")");
    }

    /**
     * ✅ 친구 게시글 알림 생성
     */
    private void createLocalPostNotification(String notificationId, String postId,
                                             String authorId, String authorName,
                                             String postTitle, long timestamp) {
        int currentCount = unreadPostCountByUser.getOrDefault(authorId, 0);
        int newCount = currentCount + 1;
        unreadPostCountByUser.put(authorId, newCount);

        NotificationDTO notification = NotificationDTO.createPost(
                "post_" + notificationId,
                authorName,
                postTitle,
                getTimeDisplay(timestamp),
                false,
                newCount,
                R.drawable.ic_community,
                authorId,
                postId,
                timestamp
        );

        addPostNotificationAndUpdateCounts(notification, authorId);

        Log.d(TAG, "✅ 로컬 게시글 알림 생성: " + authorName + " - " + postTitle + " (카운트: " + newCount + ")");
    }

    /**
     * ✅ 댓글 알림 추가 및 같은 게시글의 이전 알림 카운트 업데이트
     */
    private void addCommentNotificationAndUpdateCounts(NotificationDTO newNotification, String postId) {
        List<NotificationDTO> current = cache.loadNotifications();

        for (NotificationDTO notif : current) {
            if (notif.getType() == NotificationDTO.NotificationType.COMMENT &&
                    postId.equals(notif.getPostId()) &&
                    !notif.isRead()) {
                notif.setUnreadCount(0);
            }
        }

        current.add(0, newNotification);

        if (current.size() > MAX_CACHED_NOTIFICATIONS) {
            current = current.subList(0, MAX_CACHED_NOTIFICATIONS);
        }

        cache.saveNotifications(current);

        splitNotifications(current);
        notifyUnreadCountChanged();
    }

    /**
     * ✅ 게시글 알림 추가 및 같은 사용자의 이전 알림 카운트 업데이트
     */
    private void addPostNotificationAndUpdateCounts(NotificationDTO newNotification, String authorId) {
        List<NotificationDTO> current = cache.loadNotifications();

        for (NotificationDTO notif : current) {
            if (notif.getType() == NotificationDTO.NotificationType.POST &&
                    authorId.equals(notif.getUserId()) &&
                    !notif.isRead()) {
                notif.setUnreadCount(0);
            }
        }

        current.add(0, newNotification);

        if (current.size() > MAX_CACHED_NOTIFICATIONS) {
            current = current.subList(0, MAX_CACHED_NOTIFICATIONS);
        }

        cache.saveNotifications(current);

        splitNotifications(current);
        notifyUnreadCountChanged();
    }

    private void addNotificationToCache(NotificationDTO notification) {
        List<NotificationDTO> current = cache.loadNotifications();
        current.add(0, notification);

        if (current.size() > MAX_CACHED_NOTIFICATIONS) {
            current = current.subList(0, MAX_CACHED_NOTIFICATIONS);
        }

        cache.saveNotifications(current);

        splitNotifications(current);
        notifyUnreadCountChanged();
    }

    private String getTimeDisplay(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "방금";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (hours < 24) {
            return hours + "시간 전";
        } else if (days < 7) {
            return days + "일 전";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    private void splitNotifications(List<NotificationDTO> notifications) {
        todayNotifications.clear();
        recentNotifications.clear();

        long now = System.currentTimeMillis();
        long oneDayAgo = now - (24 * 60 * 60 * 1000);

        for (NotificationDTO notification : notifications) {
            long createdAt = notification.getCreatedAt();

            if (createdAt >= oneDayAgo) {
                todayNotifications.add(notification);
            } else {
                recentNotifications.add(notification);
            }
        }

        Log.d(TAG, "📊 분류 완료: 오늘 " + todayNotifications.size() + "개, 최근 " + recentNotifications.size() + "개");
    }

    public void addListener(UnreadCountListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            Log.d(TAG, "✅ 리스너 등록: " + listeners.size() + "개");
        }
    }

    public void removeListener(UnreadCountListener listener) {
        listeners.remove(listener);
        Log.d(TAG, "✅ 리스너 해제: " + listeners.size() + "개 남음");
    }

    private void notifyUnreadCountChanged() {
        int count = getUnreadCount();
        Log.d(TAG, "📬 읽지 않은 알림: " + count + "개");
        for (UnreadCountListener listener : listeners) {
            listener.onUnreadCountChanged(count);
        }
    }

    /**
     * ✅ 읽음 처리 - 댓글/게시글 알림의 경우 카운트 업데이트
     */
    public void markAsRead(String notificationId) {
        if (cache != null) {
            cache.markAsReadLocal(notificationId);
        }

        NotificationDTO targetNotif = null;
        String postId = null;
        String userId = null;

        for (NotificationDTO notif : todayNotifications) {
            if (notif.getId().equals(notificationId)) {
                notif.setRead(true);
                targetNotif = notif;
                postId = notif.getPostId();
                userId = notif.getUserId();
                break;
            }
        }

        if (targetNotif == null) {
            for (NotificationDTO notif : recentNotifications) {
                if (notif.getId().equals(notificationId)) {
                    notif.setRead(true);
                    targetNotif = notif;
                    postId = notif.getPostId();
                    userId = notif.getUserId();
                    break;
                }
            }
        }

        if (targetNotif != null) {
            if (targetNotif.getType() == NotificationDTO.NotificationType.COMMENT &&
                    postId != null && !postId.isEmpty()) {
                updateCommentCountsAfterRead(postId);
            } else if (targetNotif.getType() == NotificationDTO.NotificationType.POST &&
                    userId != null && !userId.isEmpty()) {
                updatePostCountsAfterRead(userId);
            }
        }

        notifyUnreadCountChanged();
        Log.d(TAG, "✅ 로컬 알림 읽음 처리: " + notificationId);
    }

    /**
     * ✅ 댓글 읽음 처리 후 다음 최신 알림으로 카운트 이동
     */
    private void updateCommentCountsAfterRead(String postId) {
        List<NotificationDTO> allNotifications = cache.loadNotifications();
        List<NotificationDTO> unreadComments = new ArrayList<>();

        for (NotificationDTO notif : allNotifications) {
            if (notif.getType() == NotificationDTO.NotificationType.COMMENT &&
                    postId.equals(notif.getPostId()) &&
                    !notif.isRead()) {
                unreadComments.add(notif);
            }
        }

        unreadComments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        int newCount = unreadComments.size();
        unreadCommentCountByPost.put(postId, newCount);

        if (!unreadComments.isEmpty()) {
            unreadComments.get(0).setUnreadCount(newCount);

            for (int i = 1; i < unreadComments.size(); i++) {
                unreadComments.get(i).setUnreadCount(0);
            }

            cache.saveNotifications(allNotifications);
            splitNotifications(allNotifications);
        }

        Log.d(TAG, "✅ 게시글 " + postId + "의 읽지 않은 댓글 수: " + newCount);
    }

    /**
     * ✅ 게시글 읽음 처리 후 다음 최신 알림으로 카운트 이동
     */
    private void updatePostCountsAfterRead(String authorId) {
        List<NotificationDTO> allNotifications = cache.loadNotifications();
        List<NotificationDTO> unreadPosts = new ArrayList<>();

        for (NotificationDTO notif : allNotifications) {
            if (notif.getType() == NotificationDTO.NotificationType.POST &&
                    authorId.equals(notif.getUserId()) &&
                    !notif.isRead()) {
                unreadPosts.add(notif);
            }
        }

        unreadPosts.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        int newCount = unreadPosts.size();
        unreadPostCountByUser.put(authorId, newCount);

        if (!unreadPosts.isEmpty()) {
            unreadPosts.get(0).setUnreadCount(newCount);

            for (int i = 1; i < unreadPosts.size(); i++) {
                unreadPosts.get(i).setUnreadCount(0);
            }

            cache.saveNotifications(allNotifications);
            splitNotifications(allNotifications);
        }

        Log.d(TAG, "✅ 사용자 " + authorId + "의 읽지 않은 게시글 수: " + newCount);
    }

    public void deleteNotification(String notificationId) {
        if (cache != null) {
            cache.deleteNotification(notificationId);
        }

        String deletedPostId = null;
        String deletedUserId = null;
        NotificationDTO.NotificationType deletedType = null;

        for (NotificationDTO notif : todayNotifications) {
            if (notif.getId().equals(notificationId)) {
                deletedPostId = notif.getPostId();
                deletedUserId = notif.getUserId();
                deletedType = notif.getType();
                break;
            }
        }
        if (deletedPostId == null && deletedUserId == null) {
            for (NotificationDTO notif : recentNotifications) {
                if (notif.getId().equals(notificationId)) {
                    deletedPostId = notif.getPostId();
                    deletedUserId = notif.getUserId();
                    deletedType = notif.getType();
                    break;
                }
            }
        }

        todayNotifications.removeIf(notif -> notif.getId().equals(notificationId));
        recentNotifications.removeIf(notif -> notif.getId().equals(notificationId));

        if (deletedType == NotificationDTO.NotificationType.COMMENT &&
                deletedPostId != null && !deletedPostId.isEmpty()) {
            updateCommentCountsAfterRead(deletedPostId);
        } else if (deletedType == NotificationDTO.NotificationType.POST &&
                deletedUserId != null && !deletedUserId.isEmpty()) {
            updatePostCountsAfterRead(deletedUserId);
        }

        notifyUnreadCountChanged();
        Log.d(TAG, "🗑️ 알림 삭제: " + notificationId);
    }

    public int getUnreadCount() {
        int count = 0;
        for (NotificationDTO notif : todayNotifications) {
            if (!notif.isRead()) count++;
        }
        for (NotificationDTO notif : recentNotifications) {
            if (!notif.isRead()) count++;
        }
        return count;
    }

    public List<NotificationDTO> getTodayNotifications() {
        return new ArrayList<>(todayNotifications);
    }

    public List<NotificationDTO> getRecentNotifications() {
        return new ArrayList<>(recentNotifications);
    }

    public void clearAll() {
        todayNotifications.clear();
        recentNotifications.clear();
        unreadCommentCountByPost.clear();
        unreadPostCountByUser.clear();
        if (cache != null) {
            cache.clearCache();
        }
        notifyUnreadCountChanged();
        Log.d(TAG, "🗑️ 모든 알림 삭제");
    }

    public void refresh() {
        Log.d(TAG, "🔄 알림 새로고침 시작");
        loadNotificationsFromCache();
    }

    public void stopListening() {
        if (followerListener != null) {
            followerListener.remove();
            followerListener = null;
            Log.d(TAG, "🛑 follower 리스너 해제");
        }
        if (commentListener != null) {
            commentListener.remove();
            commentListener = null;
            Log.d(TAG, "🛑 commentNotifications 리스너 해제");
        }
        if (postListener != null) {
            postListener.remove();
            postListener = null;
            Log.d(TAG, "🛑 postNotifications 리스너 해제");
        }
        isListening = false;
    }
}