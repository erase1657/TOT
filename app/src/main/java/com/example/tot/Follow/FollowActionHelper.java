package com.example.tot.Follow;

import android.util.Log;

import androidx.annotation.NonNull;

/**
 * 팔로우 관련 공통 동작을 도와주는 헬퍼 클래스
 * ✅ Firestore 알림 저장 제거
 * ✅ NotificationManager가 실시간 감지하므로 별도 알림 전송 불필요
 */
public final class FollowActionHelper {

    private static final String TAG = "FollowActionHelper";

    private FollowActionHelper() {
        // no-op
    }

    /**
     * ✅ 팔로우 알림 전송 (더 이상 사용하지 않음)
     * NotificationManager가 follower 컬렉션을 실시간 감지하므로
     * 팔로우 관계만 Firestore에 저장하면 자동으로 알림 생성됨
     */
    @Deprecated
    public static void sendFollowNotification(@NonNull String recipientUserId,
                                              @NonNull String senderUserId) {
        // ✅ 더 이상 Firestore에 알림을 저장하지 않음
        // NotificationManager의 실시간 리스너가 자동으로 감지
        Log.d(TAG, "⚠️ sendFollowNotification은 더 이상 필요하지 않습니다 (실시간 감지)");
    }
}