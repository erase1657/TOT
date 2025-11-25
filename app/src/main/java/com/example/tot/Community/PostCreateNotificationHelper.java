package com.example.tot.Community;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ✅ 게시글 생성 시 팔로워에게 알림을 전송하는 헬퍼 클래스
 * PostCreateActivity에서 게시글 생성 후 호출
 */
public class PostCreateNotificationHelper {

    private static final String TAG = "PostCreateNotifHelper";

    /**
     * 게시글 생성 시 모든 팔로워에게 알림 전송
     *
     * @param authorId 게시글 작성자 UID
     * @param authorName 게시글 작성자 닉네임
     * @param postId 생성된 게시글 ID
     * @param postTitle 게시글 제목
     */
    public static void notifyFollowers(String authorId, String authorName, String postId, String postTitle) {
        if (authorId == null || authorName == null || postId == null || postTitle == null) {
            Log.w(TAG, "⚠️ 필수 파라미터가 누락되어 알림 전송 중단");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 작성자의 팔로워 목록 조회
        db.collection("user")
                .document(authorId)
                .collection("follower")
                .get()
                .addOnSuccessListener(followerSnapshot -> {
                    if (followerSnapshot.isEmpty()) {
                        Log.d(TAG, "📭 팔로워가 없어 알림을 전송하지 않음");
                        return;
                    }

                    int followerCount = followerSnapshot.size();
                    Log.d(TAG, "📬 " + followerCount + "명의 팔로워에게 알림 전송 시작");

                    long timestamp = System.currentTimeMillis();

                    // 각 팔로워에게 알림 전송
                    for (DocumentSnapshot followerDoc : followerSnapshot.getDocuments()) {
                        String followerId = followerDoc.getId();

                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("postId", postId);
                        notificationData.put("authorId", authorId);
                        notificationData.put("authorName", authorName);
                        notificationData.put("postTitle", postTitle);
                        notificationData.put("timestamp", timestamp);

                        // 각 팔로워의 postNotifications 컬렉션에 알림 추가
                        db.collection("user")
                                .document(followerId)
                                .collection("postNotifications")
                                .add(notificationData)
                                .addOnSuccessListener(docRef -> {
                                    Log.d(TAG, "✅ 알림 전송 성공: " + followerId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ 알림 전송 실패 (" + followerId + ")", e);
                                });
                    }

                    Log.d(TAG, "✅ 게시글 알림 전송 완료: " + followerCount + "명");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로워 목록 조회 실패", e);
                });
    }
}