package com.example.tot.Follow;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.tot.Notification.NotificationManager;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * 팔로우 관련 공통 동작을 도와주는 헬퍼 클래스.
 * 현재는 팔로우 알림 전송 로직을 중앙집중화해서
 * 다양한 화면에서 재사용할 수 있도록 한다.
 */
public final class FollowActionHelper {

    private static final String TAG = "FollowActionHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private FollowActionHelper() {
        // no-op
    }

    /**
     * Firestore 알림 컬렉션에 팔로우 알림을 추가한다.
     * recipientUserId: 팔로우를 받은 사용자 (알림 수신자)
     * senderUserId: 팔로우를 수행한 사용자 (알림 발신자)
     */
    public static void sendFollowNotification(@NonNull String recipientUserId,
                                              @NonNull String senderUserId) {
        if (recipientUserId.isEmpty() || senderUserId.isEmpty()) {
            Log.w(TAG, "sendFollowNotification: invalid user ids");
            return;
        }

        db.collection("user")
                .document(senderUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    String nickname = doc.getString("nickname");
                    if (nickname == null || nickname.trim().isEmpty()) {
                        nickname = "사용자";
                    }

                    NotificationManager.getInstance()
                            .addFollowNotification(recipientUserId, nickname, senderUserId);

                    Log.d(TAG, "팔로우 알림 전송 성공: sender=" + senderUserId
                            + ", recipient=" + recipientUserId);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "팔로우 알림 전송 실패: 내 정보 로드 오류", e));
    }
}

