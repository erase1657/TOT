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

    public void uploadProfileImage(@NonNull String userId, @NonNull Uri imageUri, @NonNull UploadCallback callback) {
        String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
        StorageReference profileRef = storage.getReference()
                .child("profiles")
                .child(userId)
                .child(fileName);

        Log.d(TAG, "🔄 프로필 이미지 업로드 시작: " + imageUri.toString());

        profileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    profileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                Log.d(TAG, "✅ 프로필 이미지 업로드 성공: " + downloadUrl);
                                callback.onSuccess(downloadUrl);
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

    public void uploadBackgroundImage(@NonNull String userId, @NonNull Uri imageUri, @NonNull UploadCallback callback) {
        String fileName = "background_" + System.currentTimeMillis() + ".jpg";
        StorageReference bgRef = storage.getReference()
                .child("backgrounds")
                .child(userId)
                .child(fileName);

        Log.d(TAG, "🔄 배경 이미지 업로드 시작: " + imageUri.toString());

        bgRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    bgRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();
                                Log.d(TAG, "✅ 배경 이미지 업로드 성공: " + downloadUrl);
                                callback.onSuccess(downloadUrl);
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
            Log.d(TAG, "📝 프로필 이미지 URL 업데이트: " + profileImageUrl);
        }

        if (backgroundImageUrl != null) {
            updates.put("backgroundImageUrl", backgroundImageUrl);
            Log.d(TAG, "📝 배경 이미지 URL 업데이트: " + backgroundImageUrl);
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

    public void uploadAndSaveProfile(@NonNull String userId,
                                     @NonNull String nickname,
                                     @NonNull String comment,
                                     @NonNull String address,
                                     Uri profileImageUri,
                                     Uri backgroundImageUri,
                                     String currentProfileUrl,
                                     String currentBackgroundUrl,
                                     @NonNull SaveCallback callback) {

        Log.d(TAG, "💾 프로필 저장 시작");
        Log.d(TAG, "- 프로필 이미지: " + (profileImageUri != null ? "있음" : "없음"));
        Log.d(TAG, "- 배경 이미지: " + (backgroundImageUri != null ? "있음" : "없음"));

        // 업로드할 이미지가 없으면 바로 저장
        if (profileImageUri == null && backgroundImageUri == null) {
            Log.d(TAG, "⚡ 이미지 업로드 없이 텍스트만 저장");
            saveProfileText(userId, nickname, comment, address, currentProfileUrl, currentBackgroundUrl, callback);
            return;
        }

        // 업로드 카운터
        final int[] uploadCount = {0};
        final int totalUploads = (profileImageUri != null ? 1 : 0) + (backgroundImageUri != null ? 1 : 0);
        final String[] newProfileUrl = {currentProfileUrl};
        final String[] newBackgroundUrl = {currentBackgroundUrl};
        final boolean[] hasError = {false};

        // 프로필 이미지 업로드
        if (profileImageUri != null) {
            uploadProfileImage(userId, profileImageUri, new UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    if (hasError[0]) return;

                    newProfileUrl[0] = downloadUrl;
                    uploadCount[0]++;
                    Log.d(TAG, "✅ 프로필 업로드 완료 (" + uploadCount[0] + "/" + totalUploads + ")");

                    if (uploadCount[0] == totalUploads) {
                        saveProfileText(userId, nickname, comment, address, newProfileUrl[0], newBackgroundUrl[0], callback);
                    }
                }

                @Override
                public void onFailure(String message) {
                    if (!hasError[0]) {
                        hasError[0] = true;
                        Log.e(TAG, "❌ 프로필 업로드 실패: " + message);
                        callback.onFailure(message);
                    }
                }
            });
        }

        // 배경 이미지 업로드
        if (backgroundImageUri != null) {
            uploadBackgroundImage(userId, backgroundImageUri, new UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    if (hasError[0]) return;

                    newBackgroundUrl[0] = downloadUrl;
                    uploadCount[0]++;
                    Log.d(TAG, "✅ 배경 업로드 완료 (" + uploadCount[0] + "/" + totalUploads + ")");

                    if (uploadCount[0] == totalUploads) {
                        saveProfileText(userId, nickname, comment, address, newProfileUrl[0], newBackgroundUrl[0], callback);
                    }
                }

                @Override
                public void onFailure(String message) {
                    if (!hasError[0]) {
                        hasError[0] = true;
                        Log.e(TAG, "❌ 배경 업로드 실패: " + message);
                        callback.onFailure(message);
                    }
                }
            });
        }
    }
}