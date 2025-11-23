package com.example.tot.MyPage;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

/**
 * MyPage 프로필 관리 헬퍼 클래스
 * - 이미지 업로드
 * - 프로필 데이터 저장
 * - 코드 분할로 유지보수성 향상
 */
public class MyPageProfileManager {

    private static final String TAG = "MyPageProfileManager";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String message);
    }

    public interface SaveCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public MyPageProfileManager() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * 프로필 이미지 업로드
     */
    public void uploadProfileImage(@NonNull String userId, @NonNull Uri imageUri, @NonNull UploadCallback callback) {
        String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
        StorageReference profileRef = storage.getReference()
                .child("profiles")
                .child(userId)
                .child(fileName);

        profileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    profileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                Log.d(TAG, "✅ 프로필 이미지 업로드 성공: " + uri.toString());
                                callback.onSuccess(uri.toString());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 프로필 URL 가져오기 실패", e);
                                callback.onFailure("프로필 이미지 URL을 가져올 수 없습니다");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 프로필 이미지 업로드 실패", e);
                    callback.onFailure("프로필 이미지 업로드 실패");
                });
    }

    /**
     * 배경 이미지 업로드
     */
    public void uploadBackgroundImage(@NonNull String userId, @NonNull Uri imageUri, @NonNull UploadCallback callback) {
        String fileName = "background_" + System.currentTimeMillis() + ".jpg";
        StorageReference bgRef = storage.getReference()
                .child("backgrounds")
                .child(userId)
                .child(fileName);

        bgRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    bgRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                Log.d(TAG, "✅ 배경 이미지 업로드 성공: " + uri.toString());
                                callback.onSuccess(uri.toString());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ 배경 URL 가져오기 실패", e);
                                callback.onFailure("배경 이미지 URL을 가져올 수 없습니다");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 배경 이미지 업로드 실패", e);
                    callback.onFailure("배경 이미지 업로드 실패");
                });
    }

    /**
     * 프로필 텍스트 정보 저장
     */
    public void saveProfileText(@NonNull String userId,
                                @NonNull String nickname,
                                @NonNull String comment,
                                @NonNull String address,
                                String profileImageUrl,
                                String backgroundImageUrl,
                                @NonNull SaveCallback callback) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("nickname", nickname);
        updates.put("comment", comment.isEmpty() ? "" : comment);
        updates.put("address", address.isEmpty() ? "" : address);

        if (profileImageUrl != null) {
            updates.put("profileImageUrl", profileImageUrl);
        }

        if (backgroundImageUrl != null) {
            updates.put("backgroundImageUrl", backgroundImageUrl);
        }

        db.collection("user")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ 프로필 업데이트 성공");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 프로필 업데이트 실패", e);
                    callback.onFailure("저장 중 오류가 발생했습니다");
                });
    }

    /**
     * 이미지 업로드 후 프로필 저장 (통합 메서드)
     */
    public void uploadAndSaveProfile(@NonNull String userId,
                                     @NonNull String nickname,
                                     @NonNull String comment,
                                     @NonNull String address,
                                     Uri profileImageUri,
                                     Uri backgroundImageUri,
                                     String currentProfileUrl,
                                     String currentBackgroundUrl,
                                     @NonNull SaveCallback callback) {

        // 업로드할 이미지가 없으면 바로 저장
        if (profileImageUri == null && backgroundImageUri == null) {
            saveProfileText(userId, nickname, comment, address, currentProfileUrl, currentBackgroundUrl, callback);
            return;
        }

        // 업로드 카운터
        final int[] uploadCount = {0};
        final int totalUploads = (profileImageUri != null ? 1 : 0) + (backgroundImageUri != null ? 1 : 0);
        final String[] newProfileUrl = {currentProfileUrl};
        final String[] newBackgroundUrl = {currentBackgroundUrl};

        // 프로필 이미지 업로드
        if (profileImageUri != null) {
            uploadProfileImage(userId, profileImageUri, new UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    newProfileUrl[0] = downloadUrl;
                    uploadCount[0]++;
                    if (uploadCount[0] == totalUploads) {
                        saveProfileText(userId, nickname, comment, address, newProfileUrl[0], newBackgroundUrl[0], callback);
                    }
                }

                @Override
                public void onFailure(String message) {
                    callback.onFailure(message);
                }
            });
        }

        // 배경 이미지 업로드
        if (backgroundImageUri != null) {
            uploadBackgroundImage(userId, backgroundImageUri, new UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    newBackgroundUrl[0] = downloadUrl;
                    uploadCount[0]++;
                    if (uploadCount[0] == totalUploads) {
                        saveProfileText(userId, nickname, comment, address, newProfileUrl[0], newBackgroundUrl[0], callback);
                    }
                }

                @Override
                public void onFailure(String message) {
                    callback.onFailure(message);
                }
            });
        }
    }
}