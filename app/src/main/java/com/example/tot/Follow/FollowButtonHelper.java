package com.example.tot.Follow;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * 팔로우 버튼 통합 관리 헬퍼
 * - 팔로우 상태에 따른 버튼 UI 업데이트
 * - 팔로우/언팔로우 동작 처리
 */
public class FollowButtonHelper {

    private static final String TAG = "FollowButtonHelper";

    public interface FollowStatusCallback {
        void onStatusChecked(boolean isFollowing, boolean isFollower);
    }

    public interface FollowActionCallback {
        void onSuccess(boolean isFollowing);
        void onFailure(String message);
    }

    /**
     * Firestore에서 팔로우 상태 확인
     */
    public static void checkFollowStatus(@NonNull String targetUserId,
                                         @NonNull FollowStatusCallback callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            callback.onStatusChecked(false, false);
            return;
        }

        String myUid = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 내가 팔로잉 중인지 확인
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(followingDoc -> {
                    boolean isFollowing = followingDoc.exists();

                    // 상대가 나를 팔로워 중인지 확인
                    db.collection("user")
                            .document(targetUserId)
                            .collection("following")
                            .document(myUid)
                            .get()
                            .addOnSuccessListener(followerDoc -> {
                                boolean isFollower = followerDoc.exists();
                                callback.onStatusChecked(isFollowing, isFollower);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "팔로워 상태 확인 실패", e);
                                callback.onStatusChecked(isFollowing, false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "팔로잉 상태 확인 실패", e);
                    callback.onStatusChecked(false, false);
                });
    }

    /**
     * 팔로우 버튼 UI 업데이트
     * 로직:
     * - 팔로잉 X, 팔로워 X → "팔로우" (Primary)
     * - 팔로잉 X, 팔로워 O → "맞 팔로우" (Primary)
     * - 팔로잉 O → "팔로우 중" (Secondary)
     */
    public static void updateFollowButton(@NonNull TextView button,
                                          boolean isFollowing,
                                          boolean isFollower) {
        if (isFollowing) {
            // 팔로잉 중
            button.setText("팔로우 중");
            button.setBackgroundResource(R.drawable.bg_follow_button_secondary);
            button.setTextColor(0xFF666666);
        } else if (isFollower) {
            // 상대가 나를 팔로우 중 → 맞팔로우
            button.setText("맞 팔로우");
            button.setBackgroundResource(R.drawable.bg_follow_button_primary);
            button.setTextColor(0xFFFFFFFF);
        } else {
            // 아무 관계 없음
            button.setText("팔로우");
            button.setBackgroundResource(R.drawable.bg_follow_button_primary);
            button.setTextColor(0xFFFFFFFF);
        }
    }

    /**
     * 팔로우 버튼 클릭 처리
     */
    public static void handleFollowButtonClick(@NonNull Context context,
                                               @NonNull String targetUserId,
                                               boolean isFollowing,
                                               boolean isFollower,
                                               @NonNull FollowActionCallback callback) {
        if (isFollowing) {
            // 언팔로우 확인 다이얼로그
            showUnfollowDialog(context, targetUserId, isFollower, callback);
        } else {
            // 팔로우 실행
            performFollow(context, targetUserId, isFollower, callback);
        }
    }

    /**
     * 언팔로우 확인 다이얼로그
     */
    private static void showUnfollowDialog(@NonNull Context context,
                                           @NonNull String targetUserId,
                                           boolean isFollower,
                                           @NonNull FollowActionCallback callback) {
        new AlertDialog.Builder(context)
                .setTitle("언팔로우")
                .setMessage("정말 언팔로우하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    performUnfollow(context, targetUserId, isFollower, callback);
                })
                .setNegativeButton("아니오", null)
                .show();
    }

    /**
     * 팔로우 실행
     */
    private static void performFollow(@NonNull Context context,
                                      @NonNull String targetUserId,
                                      boolean isFollower,
                                      @NonNull FollowActionCallback callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("로그인이 필요합니다");
            return;
        }

        String myUid = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> followData = new HashMap<>();
        followData.put("followedAt", System.currentTimeMillis());

        // 1. 내 following에 추가
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .set(followData)
                .addOnSuccessListener(aVoid -> {
                    // 2. 상대방 follower에 추가
                    db.collection("user")
                            .document(targetUserId)
                            .collection("follower")
                            .document(myUid)
                            .set(followData)
                            .addOnSuccessListener(aVoid2 -> {
                                String message = isFollower ? "맞팔로우했습니다" : "팔로우했습니다";
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                callback.onSuccess(true);

                                Log.d(TAG, "✅ 팔로우 성공: " + myUid + " → " + targetUserId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 상대방 follower 추가 실패", e);
                                callback.onFailure("팔로우 중 오류가 발생했습니다");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 팔로우 실패", e);
                    callback.onFailure("팔로우 중 오류가 발생했습니다");
                });
    }

    /**
     * 언팔로우 실행
     */
    private static void performUnfollow(@NonNull Context context,
                                        @NonNull String targetUserId,
                                        boolean isFollower,
                                        @NonNull FollowActionCallback callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("로그인이 필요합니다");
            return;
        }

        String myUid = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. 내 following에서 삭제
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // 2. 상대방 follower에서 삭제
                    db.collection("user")
                            .document(targetUserId)
                            .collection("follower")
                            .document(myUid)
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(context, "언팔로우했습니다", Toast.LENGTH_SHORT).show();
                                callback.onSuccess(false);

                                Log.d(TAG, "✅ 언팔로우 성공: " + targetUserId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 상대방 follower 삭제 실패", e);
                                callback.onFailure("언팔로우 중 오류가 발생했습니다");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 언팔로우 실패", e);
                    callback.onFailure("언팔로우 중 오류가 발생했습니다");
                });
    }
}