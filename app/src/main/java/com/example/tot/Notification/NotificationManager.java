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
import java.util.List;
import java.util.Locale;

/**
 * 알림 관리 싱글톤 클래스
 * ✅ Firestore 알림 컬렉션 완전 제거
 * ✅ follower 컬렉션 변경사항을 실시간 감지하여 로컬 알림 생성
 * ✅ 로컬 캐시만 사용 (SharedPreferences)
 */
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

    // ✅ follower 컬렉션 실시간 리스너
    private ListenerRegistration followerListener;
    private boolean isListening = false;

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

    /**
     * ✅ Context 초기화 (Application에서 호출)
     */
    public void init(Context context) {
        this.appContext = context.getApplicationContext();
        this.cache = new NotificationCache(appContext);
        Log.d(TAG, "✅ NotificationManager 초기화");
    }

    /**
     * ✅ 초기 로드 (앱 시작시 한 번만 호출)
     */
    public void initialLoad() {
        if (cache == null) {
            Log.w(TAG, "⚠️ Cache가 초기화되지 않았습니다");
            return;
        }

        Log.d(TAG, "🚀 초기 알림 로드 시작");

        // 1. 로컬 캐시 먼저 로드
        loadNotificationsFromCache();

        // 2. follower 컬렉션 실시간 감지 시작
        startListeningForFollowers();
    }

    /**
     * ✅ 로컬 캐시에서 알림 로드
     */
    private void loadNotificationsFromCache() {
        if (cache == null) {
            Log.w(TAG, "⚠️ Cache가 초기화되지 않았습니다");
            return;
        }

        List<NotificationDTO> cached = cache.loadNotifications();

        // ✅ 최대 개수 제한
        if (cached.size() > MAX_CACHED_NOTIFICATIONS) {
            cached = cached.subList(0, MAX_CACHED_NOTIFICATIONS);
            cache.saveNotifications(cached);
            Log.d(TAG, "🗑️ 오래된 알림 자동 삭제");
        }

        splitNotifications(cached);
        notifyUnreadCountChanged();

        Log.d(TAG, "📱 로컬 캐시 로드: " + cached.size() + "개");
    }

    /**
     * ✅ follower 컬렉션 실시간 감지 (새 팔로워 → 로컬 알림 생성)
     */
    private void startListeningForFollowers() {
        if (isListening || mAuth.getCurrentUser() == null) {
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        // ✅ 마지막 확인 시간 이후의 팔로워만 감지
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

                    // ✅ 새로 추가된 팔로워만 처리
                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = change.getDocument();
                            Long followedAt = doc.getLong("followedAt");

                            // 마지막 확인 시간 이후의 팔로워만 알림 생성
                            if (followedAt != null && followedAt > lastCheck) {
                                String followerId = doc.getId();
                                createLocalFollowNotification(followerId, followedAt);
                            }
                        }
                    }

                    // 마지막 확인 시간 업데이트
                    cache.setLastCheckTime(System.currentTimeMillis());
                });

        isListening = true;
    }

    /**
     * ✅ 로컬 팔로우 알림 생성 (Firestore 쓰기 없음)
     */
    private void createLocalFollowNotification(String followerId, long followedAt) {
        // 팔로워 정보 조회 (닉네임)
        db.collection("user")
                .document(followerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String nickname = doc.getString("nickname");
                    if (nickname == null || nickname.isEmpty()) {
                        nickname = "사용자";
                    }

                    // ✅ 로컬 알림 생성
                    NotificationDTO notification = NotificationDTO.createFollow(
                            "follow_" + followerId + "_" + followedAt,
                            nickname,
                            getTimeDisplay(followedAt),
                            false,
                            R.drawable.ic_user_add,
                            followerId,
                            followedAt
                    );

                    // 캐시에 추가
                    List<NotificationDTO> current = cache.loadNotifications();
                    current.add(0, notification);

                    // 최대 개수 제한
                    if (current.size() > MAX_CACHED_NOTIFICATIONS) {
                        current = current.subList(0, MAX_CACHED_NOTIFICATIONS);
                    }

                    cache.saveNotifications(current);

                    // UI 업데이트
                    splitNotifications(current);
                    notifyUnreadCountChanged();

                    Log.d(TAG, "✅ 로컬 팔로우 알림 생성: " + nickname);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로워 정보 조회 실패", e);
                });
    }

    /**
     * ✅ 시간 표시 문자열 생성
     */
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

    /**
     * ✅ 알림 목록을 오늘/최근으로 분류
     */
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
     * ✅ 읽음 처리 - 로컬만 업데이트
     */
    public void markAsRead(String notificationId) {
        if (cache != null) {
            cache.markAsReadLocal(notificationId);
        }

        boolean found = false;
        for (NotificationDTO notif : todayNotifications) {
            if (notif.getId().equals(notificationId)) {
                notif.setRead(true);
                found = true;
                break;
            }
        }

        if (!found) {
            for (NotificationDTO notif : recentNotifications) {
                if (notif.getId().equals(notificationId)) {
                    notif.setRead(true);
                    break;
                }
            }
        }

        notifyUnreadCountChanged();
        Log.d(TAG, "✅ 로컬 알림 읽음 처리: " + notificationId);
    }

    /**
     * ✅ 특정 알림 삭제 (스와이프 삭제 지원)
     */
    public void deleteNotification(String notificationId) {
        if (cache != null) {
            cache.deleteNotification(notificationId);
        }

        todayNotifications.removeIf(notif -> notif.getId().equals(notificationId));
        recentNotifications.removeIf(notif -> notif.getId().equals(notificationId));

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
        if (cache != null) {
            cache.clearCache();
        }
        notifyUnreadCountChanged();
        Log.d(TAG, "🗑️ 모든 알림 삭제");
    }

    /**
     * ✅ 새로고침 (로컬 캐시 재로드)
     */
    public void refresh() {
        Log.d(TAG, "🔄 알림 새로고침 시작");
        loadNotificationsFromCache();
    }

    /**
     * ✅ 리스너 정리
     */
    public void stopListening() {
        if (followerListener != null) {
            followerListener.remove();
            followerListener = null;
            isListening = false;
            Log.d(TAG, "🛑 follower 리스너 해제");
        }
    }
}