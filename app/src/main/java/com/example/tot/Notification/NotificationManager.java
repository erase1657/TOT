package com.example.tot.Notification;

import java.util.ArrayList;
import java.util.List;

/**
 * 알림 관리 싱글톤 클래스
 * 앱 전역에서 알림 데이터를 관리하고 읽지 않은 알림 수를 추적합니다.
 */
public class NotificationManager {

    private static NotificationManager instance;
    private List<NotificationDTO> todayNotifications;
    private List<NotificationDTO> recentNotifications;
    private List<UnreadCountListener> listeners;

    public interface UnreadCountListener {
        void onUnreadCountChanged(int count);
    }

    private NotificationManager() {
        todayNotifications = new ArrayList<>();
        recentNotifications = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void addListener(UnreadCountListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(UnreadCountListener listener) {
        listeners.remove(listener);
    }

    private void notifyUnreadCountChanged() {
        int count = getUnreadCount();
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
}