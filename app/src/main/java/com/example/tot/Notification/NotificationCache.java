package com.example.tot.Notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 알림 로컬 캐시 관리 클래스
 * SharedPreferences를 사용하여 알림 데이터를 로컬에 저장/로드
 * ✅ 개별 삭제 기능 추가
 */
public class NotificationCache {

    private static final String TAG = "NotificationCache";
    private static final String PREF_NAME = "notification_cache";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_LAST_CHECK = "last_check_timestamp";

    private SharedPreferences prefs;
    private Gson gson;

    public NotificationCache(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * 알림 목록을 로컬에 저장
     */
    public void saveNotifications(List<NotificationDTO> notifications) {
        try {
            String json = gson.toJson(notifications);
            prefs.edit().putString(KEY_NOTIFICATIONS, json).apply();
            Log.d(TAG, "✅ 로컬에 알림 저장: " + notifications.size() + "개");
        } catch (Exception e) {
            Log.e(TAG, "❌ 알림 저장 실패", e);
        }
    }

    /**
     * 로컬에서 알림 목록 로드
     */
    public List<NotificationDTO> loadNotifications() {
        try {
            String json = prefs.getString(KEY_NOTIFICATIONS, "[]");
            Type listType = new TypeToken<List<NotificationDTO>>(){}.getType();
            List<NotificationDTO> notifications = gson.fromJson(json, listType);
            Log.d(TAG, "✅ 로컬에서 알림 로드: " + notifications.size() + "개");
            return notifications != null ? notifications : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "❌ 알림 로드 실패", e);
            return new ArrayList<>();
        }
    }

    /**
     * 마지막 확인 시간 저장
     */
    public void setLastCheckTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_CHECK, timestamp).apply();
        Log.d(TAG, "✅ 마지막 확인 시간 저장: " + timestamp);
    }

    /**
     * 마지막 확인 시간 가져오기
     */
    public long getLastCheckTime() {
        return prefs.getLong(KEY_LAST_CHECK, 0);
    }

    /**
     * 특정 알림의 읽음 상태 업데이트 (로컬만)
     */
    public void markAsReadLocal(String notificationId) {
        List<NotificationDTO> notifications = loadNotifications();
        boolean updated = false;

        for (NotificationDTO notification : notifications) {
            if (notification.getId().equals(notificationId)) {
                notification.setRead(true);
                updated = true;
                break;
            }
        }

        if (updated) {
            saveNotifications(notifications);
            Log.d(TAG, "✅ 로컬 알림 읽음 처리: " + notificationId);
        }
    }

    /**
     * ✅ 특정 알림 삭제 (스와이프 삭제)
     */
    public void deleteNotification(String notificationId) {
        List<NotificationDTO> notifications = loadNotifications();
        boolean removed = notifications.removeIf(n -> n.getId().equals(notificationId));

        if (removed) {
            saveNotifications(notifications);
            Log.d(TAG, "✅ 로컬 알림 삭제: " + notificationId);
        }
    }

    /**
     * 새 알림을 기존 목록에 추가
     */
    public void addNewNotifications(List<NotificationDTO> newNotifications) {
        List<NotificationDTO> existing = loadNotifications();

        // 중복 제거 (ID 기준)
        for (NotificationDTO newNotif : newNotifications) {
            boolean isDuplicate = false;
            for (NotificationDTO existingNotif : existing) {
                if (existingNotif.getId().equals(newNotif.getId())) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                existing.add(0, newNotif);
            }
        }

        saveNotifications(existing);
        Log.d(TAG, "✅ 새 알림 추가: " + newNotifications.size() + "개");
    }

    /**
     * 캐시 초기화
     */
    public void clearCache() {
        prefs.edit().clear().apply();
        Log.d(TAG, "✅ 알림 캐시 초기화");
    }
}