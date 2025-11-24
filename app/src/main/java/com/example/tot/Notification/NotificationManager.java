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
    private List<ListenerRegistration> inviteListeners = new ArrayList<>();
    private boolean isListening = false;

    // ✅ 게시글별 읽지 않은 댓글 수 추적
    private Map<String, Integer> unreadCommentCountByPost = new HashMap<>();

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
        startListeningForInvites();
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

        // ✅ 게시글별 읽지 않은 댓글 수 계산
        recalculateUnreadCommentCounts(cached);

        splitNotifications(cached);
        notifyUnreadCountChanged();

        Log.d(TAG, "📱 로컬 캐시 로드: " + cached.size() + "개");
    }

    /**
     * ✅ 게시글별 읽지 않은 댓글 수 재계산
     */
    private void recalculateUnreadCommentCounts(List<NotificationDTO> notifications) {
        unreadCommentCountByPost.clear();

        // 댓글 알림만 필터링하고 게시글별로 그룹화
        Map<String, List<NotificationDTO>> commentsByPost = new HashMap<>();

        for (NotificationDTO notif : notifications) {
            if (notif.getType() == NotificationDTO.NotificationType.COMMENT && !notif.isRead()) {
                String postId = notif.getPostId();
                if (postId != null && !postId.isEmpty()) {
                    if (!commentsByPost.containsKey(postId)) {
                        commentsByPost.put(postId, new ArrayList<>());
                    }
                    commentsByPost.get(postId).add(notif);
                }
            }
        }

        // 각 게시글별로 가장 최신 알림에만 카운트 설정
        for (Map.Entry<String, List<NotificationDTO>> entry : commentsByPost.entrySet()) {
            String postId = entry.getKey();
            List<NotificationDTO> postComments = entry.getValue();

            // 시간순 정렬 (최신순)
            postComments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

            int unreadCount = postComments.size();
            unreadCommentCountByPost.put(postId, unreadCount);

            // 가장 최신 알림에만 카운트 설정
            if (!postComments.isEmpty()) {
                postComments.get(0).setUnreadCount(unreadCount);
            }

            // 나머지 알림은 카운트 0
            for (int i = 1; i < postComments.size(); i++) {
                postComments.get(i).setUnreadCount(0);
            }
        }

        Log.d(TAG, "✅ 게시글별 읽지 않은 댓글 수 계산 완료: " + commentsByPost.size() + "개 게시글");
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
    private void startListeningForInvites() {

        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        long lastCheck = cache.getLastCheckTime();

        Log.d(TAG, "👂 inviteReceived 리스너 시작");

        ListenerRegistration reg = db.collection("user")
                .document(uid)
                .collection("inviteReceived")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "❌ inviteReceived 리스너 오류", error);
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {

                            DocumentSnapshot doc = change.getDocument();
                            Long createdAt = doc.getLong("createdAt");

                            if (createdAt != null && createdAt > lastCheck) {

                                String inviteId = doc.getId();
                                String senderUid = doc.getString("senderUid");
                                String scheduleId = doc.getString("scheduleId");

                                Log.d(TAG, "🎉 새로운 초대 감지: " + inviteId);

                                createLocalScheduleInviteNotification(
                                        inviteId,
                                        scheduleId,
                                        senderUid,
                                        createdAt
                                );
                            }
                        }
                    }

                    cache.setLastCheckTime(System.currentTimeMillis());
                });

        inviteListeners.add(reg);
    }
    private void createLocalScheduleInviteNotification(String inviteId,
                                                       String scheduleId,
                                                       String senderUid,
                                                       long createdAt) {

        db.collection("user")
                .document(senderUid)
                .get()
                .addOnSuccessListener(doc -> {

                    String nickname = doc.getString("nickname");
                    if (nickname == null) nickname = "사용자";

                    // ⭐ scheduleId 를 넣는 createScheduleInvite() 사용 (DTO 수정 필수)
                    NotificationDTO notification = NotificationDTO.createScheduleInvite(
                            "invite_" + inviteId,
                            nickname,
                            "여행 일정에 참여하고 싶으시다면 여기를 클릭해 여행 일정에 참가해주세요",
                            getTimeDisplay(createdAt),
                            false,
                            0,
                            R.drawable.ic_schedule,
                            senderUid,
                            createdAt,
                            scheduleId   // ← ⭐ 여기 추가됨
                    );

                    notification.setPostId(null);

                    addNotificationToCache(notification);

                    Log.d(TAG, "🎉 스케줄 초대 알림 생성됨!");
                });
    }
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
        // ✅ 해당 게시글의 읽지 않은 댓글 수 증가
        int currentCount = unreadCommentCountByPost.getOrDefault(postId, 0);
        int newCount = currentCount + 1;
        unreadCommentCountByPost.put(postId, newCount);

        // ✅ 새 알림 생성 (카운트 포함)
        NotificationDTO notification = NotificationDTO.createComment(
                "comment_" + notificationId,
                commenterName,
                commentContent,
                getTimeDisplay(timestamp),
                false,
                newCount,  // 현재 게시글의 총 읽지 않은 댓글 수
                R.drawable.ic_comment,
                commenterId,
                timestamp
        );

        notification.setPostId(postId);

        // ✅ 캐시에 추가하고 이전 알림들의 카운트 업데이트
        addCommentNotificationAndUpdateCounts(notification, postId);

        Log.d(TAG, "✅ 로컬 댓글 알림 생성: " + commenterName + " - " + commentContent + " (카운트: " + newCount + ")");
    }

    /**
     * ✅ 댓글 알림 추가 및 같은 게시글의 이전 알림 카운트 업데이트
     */
    private void addCommentNotificationAndUpdateCounts(NotificationDTO newNotification, String postId) {
        List<NotificationDTO> current = cache.loadNotifications();

        // 같은 게시글의 이전 댓글 알림들의 카운트를 0으로 설정
        for (NotificationDTO notif : current) {
            if (notif.getType() == NotificationDTO.NotificationType.COMMENT &&
                    postId.equals(notif.getPostId()) &&
                    !notif.isRead()) {
                notif.setUnreadCount(0);
            }
        }

        // 새 알림을 맨 앞에 추가
        current.add(0, newNotification);

        // 최대 개수 제한
        if (current.size() > MAX_CACHED_NOTIFICATIONS) {
            current = current.subList(0, MAX_CACHED_NOTIFICATIONS);
        }

        cache.saveNotifications(current);

        // UI 업데이트
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
     * ✅ 읽음 처리 - 댓글 알림의 경우 게시글별 카운트 업데이트
     */
    public void markAsRead(String notificationId) {
        if (cache != null) {
            cache.markAsReadLocal(notificationId);
        }

        NotificationDTO targetNotif = null;
        String postId = null;

        // 알림 찾기
        for (NotificationDTO notif : todayNotifications) {
            if (notif.getId().equals(notificationId)) {
                notif.setRead(true);
                targetNotif = notif;
                postId = notif.getPostId();
                break;
            }
        }

        if (targetNotif == null) {
            for (NotificationDTO notif : recentNotifications) {
                if (notif.getId().equals(notificationId)) {
                    notif.setRead(true);
                    targetNotif = notif;
                    postId = notif.getPostId();
                    break;
                }
            }
        }

        // ✅ 댓글 알림인 경우 카운트 업데이트
        if (targetNotif != null &&
                targetNotif.getType() == NotificationDTO.NotificationType.COMMENT &&
                postId != null && !postId.isEmpty()) {

            updateCommentCountsAfterRead(postId);
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

        // 해당 게시글의 읽지 않은 댓글 알림 수집
        for (NotificationDTO notif : allNotifications) {
            if (notif.getType() == NotificationDTO.NotificationType.COMMENT &&
                    postId.equals(notif.getPostId()) &&
                    !notif.isRead()) {
                unreadComments.add(notif);
            }
        }

        // 시간순 정렬 (최신순)
        unreadComments.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));

        int newCount = unreadComments.size();
        unreadCommentCountByPost.put(postId, newCount);

        // 가장 최신 알림에만 카운트 설정
        if (!unreadComments.isEmpty()) {
            unreadComments.get(0).setUnreadCount(newCount);

            // 나머지는 0
            for (int i = 1; i < unreadComments.size(); i++) {
                unreadComments.get(i).setUnreadCount(0);
            }

            // 캐시 업데이트
            cache.saveNotifications(allNotifications);
            splitNotifications(allNotifications);
        }

        Log.d(TAG, "✅ 게시글 " + postId + "의 읽지 않은 댓글 수: " + newCount);
    }

    public void deleteNotification(String notificationId) {
        if (cache != null) {
            cache.deleteNotification(notificationId);
        }

        // 삭제되는 알림의 postId 확인
        String deletedPostId = null;
        for (NotificationDTO notif : todayNotifications) {
            if (notif.getId().equals(notificationId)) {
                deletedPostId = notif.getPostId();
                break;
            }
        }
        if (deletedPostId == null) {
            for (NotificationDTO notif : recentNotifications) {
                if (notif.getId().equals(notificationId)) {
                    deletedPostId = notif.getPostId();
                    break;
                }
            }
        }

        todayNotifications.removeIf(notif -> notif.getId().equals(notificationId));
        recentNotifications.removeIf(notif -> notif.getId().equals(notificationId));

        // ✅ 댓글 알림 삭제 시 카운트 재계산
        if (deletedPostId != null && !deletedPostId.isEmpty()) {
            updateCommentCountsAfterRead(deletedPostId);
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

        isListening = false;
    }
}