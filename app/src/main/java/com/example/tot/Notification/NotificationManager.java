package com.example.tot.Notification;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 알림 관리 싱글톤 클래스
 * 앱 전역에서 알림 데이터를 관리하고 읽지 않은 알림 수를 추적합니다.
 * ✅ Firestore 실시간 리스너 추가
 */
public class NotificationManager {

    private static final String TAG = "NotificationManager";

    private static NotificationManager instance;
    private List<NotificationDTO> todayNotifications;
    private List<NotificationDTO> recentNotifications;
    private List<UnreadCountListener> listeners;

    // ✅ Firestore 리스너
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration notificationListener;

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
     * ✅ Firestore에서 실시간으로 알림 수신 시작
     */
    public void startListeningForNotifications() {
        // ✅ 이미 리스너가 활성화되어 있으면 중복 실행 방지
        if (notificationListener != null) {
            Log.d(TAG, "⚠️ 알림 리스너가 이미 실행 중입니다");
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "⚠️ 사용자가 로그인하지 않았습니다");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "🔔 알림 리스너 시작: " + userId);

        // ✅ Firestore 실시간 리스너 등록
        notificationListener = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ 알림 수신 실패", error);
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "⚠️ 알림 스냅샷이 null입니다");
                        return;
                    }

                    // 기존 Firestore 알림 초기화
                    todayNotifications.clear();
                    recentNotifications.clear();

                    long now = System.currentTimeMillis();
                    long oneDayAgo = now - (24 * 60 * 60 * 1000);

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        NotificationDTO notification = parseNotification(doc);
                        if (notification != null) {
                            long createdAt = doc.getLong("createdAt") != null ?
                                    doc.getLong("createdAt") : 0;

                            if (createdAt >= oneDayAgo) {
                                todayNotifications.add(notification);
                            } else {
                                recentNotifications.add(notification);
                            }
                        }
                    }

                    Log.d(TAG, "✅ 알림 로드 완료: 오늘 " + todayNotifications.size() +
                            "개, 최근 " + recentNotifications.size() + "개");

                    notifyUnreadCountChanged();
                });
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
     * ✅ 리스너 중지
     */
    public void stopListeningForNotifications() {
        if (notificationListener != null) {
            Log.d(TAG, "🔕 알림 리스너 중지");
            notificationListener.remove();
            notificationListener = null;
        }
    }

    /**
     * ✅ Firestore에 팔로우 알림 추가
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

    public void setTodayNotifications(List<NotificationDTO> notifications) {
        todayNotifications.clear();
        if (notifications != null) {
            todayNotifications.addAll(notifications);
        }
        notifyUnreadCountChanged();
    }

    public void setRecentNotifications(List<NotificationDTO> notifications) {
        recentNotifications.clear();
        if (notifications != null) {
            recentNotifications.addAll(notifications);
        }
        notifyUnreadCountChanged();
    }

    public void addNotification(NotificationDTO notification, boolean isToday) {
        if (isToday) {
            todayNotifications.add(0, notification);
        } else {
            recentNotifications.add(0, notification);
        }
        notifyUnreadCountChanged();
    }

    public void markAsRead(String notificationId) {
        // Firestore에도 읽음 상태 업데이트
        if (mAuth.getCurrentUser() != null) {
            db.collection("notifications")
                    .document(notificationId)
                    .update("isRead", true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ 알림 읽음 처리 성공: " + notificationId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ 알림 읽음 처리 실패", e);
                    });
        }

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
        notifyUnreadCountChanged();
    }

    /**
     * ✅ 새로고침 (Firestore에서 다시 로드)
     */
    public void refresh() {
        Log.d(TAG, "🔄 알림 새로고침 시작");
        // 기존 리스너 중지 후 재시작
        stopListeningForNotifications();
        startListeningForNotifications();
    }
}