package com.example.tot.Notification;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 알림 관리 싱글톤 클래스
 * ✅ 1단계: 실시간 리스너 제거, get()으로 변경
 * ✅ 2단계: 로컬 캐시 추가 (SharedPreferences)
 * ✅ Firestore 쓰기 최소화 (읽음 처리는 로컬만)
 */
public class NotificationManager {

    private static final String TAG = "NotificationManager";

    private static NotificationManager instance;
    private List<NotificationDTO> todayNotifications;
    private List<NotificationDTO> recentNotifications;
    private List<UnreadCountListener> listeners;

    // ✅ 로컬 캐시
    private NotificationCache cache;
    private Context appContext;

    // Firestore
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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
     * ✅ 1단계: 실시간 리스너 제거, 필요할 때만 로드
     * ✅ 2단계: 로컬 캐시 먼저 로드 후 Firestore에서 새 알림만 가져오기
     */
    public void loadNotificationsFromCache() {
        if (cache == null) {
            Log.w(TAG, "⚠️ Cache가 초기화되지 않았습니다");
            return;
        }

        // 1. 로컬 캐시에서 먼저 로드 (빠른 UI 표시)
        List<NotificationDTO> cached = cache.loadNotifications();
        splitNotifications(cached);
        notifyUnreadCountChanged();

        Log.d(TAG, "📱 로컬 캐시 로드: " + cached.size() + "개");
    }

    /**
     * ✅ Firestore에서 새 알림만 가져오기 (실시간 리스너 제거)
     */
    public void loadNewNotificationsFromFirestore() {
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "⚠️ 사용자가 로그인하지 않았습니다");
            return;
        }

        if (cache == null) {
            Log.w(TAG, "⚠️ Cache가 초기화되지 않았습니다");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        long lastCheck = cache.getLastCheckTime();

        Log.d(TAG, "🔄 Firestore에서 새 알림 확인 (마지막 확인: " + lastCheck + ")");

        // ✅ get()으로 변경 (실시간 리스너 제거)
        db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereGreaterThan("createdAt", lastCheck)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()  // ✅ addSnapshotListener() → get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "✅ 새 알림 없음");
                        return;
                    }

                    List<NotificationDTO> newNotifications = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        NotificationDTO notification = parseNotification(doc);
                        if (notification != null) {
                            newNotifications.add(notification);
                        }
                    }

                    if (!newNotifications.isEmpty()) {
                        // 로컬 캐시에 새 알림 추가
                        cache.addNewNotifications(newNotifications);

                        // 메모리에도 반영
                        List<NotificationDTO> allNotifications = cache.loadNotifications();
                        splitNotifications(allNotifications);

                        // 마지막 확인 시간 업데이트
                        cache.setLastCheckTime(System.currentTimeMillis());

                        Log.d(TAG, "✅ 새 알림 " + newNotifications.size() + "개 추가");
                        notifyUnreadCountChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 알림 로드 실패", e);
                });
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
            // createdAt을 추가해야 하므로 임시로 현재 시간 사용
            // TODO: NotificationDTO에 createdAt 필드 추가
            long createdAt = now; // 임시

            if (createdAt >= oneDayAgo) {
                todayNotifications.add(notification);
            } else {
                recentNotifications.add(notification);
            }
        }

        Log.d(TAG, "📊 분류 완료: 오늘 " + todayNotifications.size() + "개, 최근 " + recentNotifications.size() + "개");
    }

    /**
     * ✅ Firestore 문서를 NotificationDTO로 변환
     */
    private NotificationDTO parseNotification(DocumentSnapshot doc) {
        try {
            String type = doc.getString("type");
            String id = doc.getId();
            String title = doc.getString("title");
            String content = doc.getString("content");
            String timeDisplay = doc.getString("timeDisplay");
            Boolean isRead = doc.getBoolean("isRead");
            String userName = doc.getString("userName");
            String userId = doc.getString("senderId");

            if (type == null) {
                Log.w(TAG, "⚠️ 알림 타입이 null입니다: " + id);
                return null;
            }

            NotificationDTO.NotificationType notifType;
            switch (type) {
                case "FOLLOW":
                    notifType = NotificationDTO.NotificationType.FOLLOW;
                    break;
                case "SCHEDULE_INVITE":
                    notifType = NotificationDTO.NotificationType.SCHEDULE_INVITE;
                    break;
                case "COMMENT":
                    notifType = NotificationDTO.NotificationType.COMMENT;
                    break;
                default:
                    Log.w(TAG, "⚠️ 알 수 없는 알림 타입: " + type);
                    return null;
            }

            return new NotificationDTO.Builder(id, notifType)
                    .title(title != null ? title : "")
                    .content(content != null ? content : "")
                    .timeDisplay(timeDisplay != null ? timeDisplay : "방금")
                    .isRead(isRead != null ? isRead : false)
                    .userName(userName != null ? userName : "")
                    .userId(userId != null ? userId : "")
                    .iconResId(getIconForType(notifType))
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "❌ 알림 파싱 실패", e);
            return null;
        }
    }

    /**
     * ✅ 타입별 아이콘 리소스 반환
     */
    private int getIconForType(NotificationDTO.NotificationType type) {
        switch (type) {
            case FOLLOW:
                return com.example.tot.R.drawable.ic_user_add;
            case SCHEDULE_INVITE:
                return com.example.tot.R.drawable.ic_schedule;
            case COMMENT:
                return com.example.tot.R.drawable.ic_comment;
            default:
                return com.example.tot.R.drawable.ic_alarm;
        }
    }

    /**
     * ✅ Firestore에 팔로우 알림 추가 (쓰기는 여전히 필요)
     */
    public void addFollowNotification(String recipientId, String senderName, String senderId) {
        if (recipientId == null || recipientId.isEmpty()) {
            Log.w(TAG, "⚠️ 수신자 ID가 null입니다");
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "FOLLOW");
        notification.put("recipientId", recipientId);
        notification.put("senderId", senderId);
        notification.put("userName", senderName);
        notification.put("title", senderName + " 님이 회원님을 팔로우했습니다");
        notification.put("content", "프로필을 확인해 주세요");
        notification.put("timeDisplay", "방금");
        notification.put("isRead", false);
        notification.put("createdAt", System.currentTimeMillis());

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ 팔로우 알림 전송 성공: " + recipientId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로우 알림 전송 실패", e);
                });
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
     * ✅ 읽음 처리 - 로컬만 업데이트 (Firestore 쓰기 제거)
     */
    public void markAsRead(String notificationId) {
        // 로컬 캐시에서만 읽음 처리
        if (cache != null) {
            cache.markAsReadLocal(notificationId);
        }

        // 메모리에서도 읽음 처리
        for (NotificationDTO notif : todayNotifications) {
            if (notif.getId().equals(notificationId)) {
                notif.setRead(true);
                notifyUnreadCountChanged();
                return;
            }
        }
        for (NotificationDTO notif : recentNotifications) {
            if (notif.getId().equals(notificationId)) {
                notif.setRead(true);
                notifyUnreadCountChanged();
                return;
            }
        }

        Log.d(TAG, "✅ 로컬 알림 읽음 처리: " + notificationId);
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
    }

    /**
     * ✅ 새로고침 - 로컬 캐시 먼저 로드 후 Firestore 확인
     */
    public void refresh() {
        Log.d(TAG, "🔄 알림 새로고침 시작");
        loadNotificationsFromCache();
        loadNewNotificationsFromFirestore();
    }

    /**
     * ✅ 초기 로드 (앱 시작시 호출)
     */
    public void initialLoad() {
        Log.d(TAG, "🚀 초기 알림 로드 시작");
        loadNotificationsFromCache();
        loadNewNotificationsFromFirestore();
    }
}