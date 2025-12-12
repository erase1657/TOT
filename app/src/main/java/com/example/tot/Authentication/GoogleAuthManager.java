package com.example.tot.Authentication;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 🔐 Google Credential Manager + Firebase Auth 통합 로그인 매니저
 *  - 최초 로그인 시 Firestore에 UserDTO 저장
 *  - 이후 로그인은 Firestore 덮어쓰기 없이 그대로 통과
 *  - ✅ 계정 필터링 실패 시 자동 재시도 (fallback)
 */
public class GoogleAuthManager {

    private static final String TAG = "GoogleAuthManager";

    private final FirebaseAuth mAuth;
    private final CredentialManager credentialManager;
    private final String webClientId;
    private final Executor executor;
    private final Activity activity;

    @Nullable
    private AuthCallback callback;

    public interface AuthCallback {
        @MainThread
        void onSignInSuccess(@NonNull FirebaseUser user);
        @MainThread
        void onSignInError(@NonNull Exception e);
        @MainThread
        void onSignOut();
    }

    public GoogleAuthManager(@NonNull Activity activity, @NonNull String webClientId) {
        this.activity = activity;
        this.webClientId = webClientId;
        this.mAuth = FirebaseAuth.getInstance();
        this.credentialManager = CredentialManager.create(activity);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setCallback(@Nullable AuthCallback callback) {
        this.callback = callback;
    }

    /** ✅ 로그인 실행 - 기존 계정 필터링 시도 */
    public void signIn() {
        Log.d(TAG, "📸 signIn() called - 기존 계정 우선 시도");
        signInWithFilter(true);
    }

    /** ✅ 계정 필터링 옵션을 지정하여 로그인 시도 */
    private void signInWithFilter(boolean filterByAuthorized) {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(filterByAuthorized)
                .setServerClientId(webClientId)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                executor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "❌ getCredentialAsync error (filterByAuthorized=" + filterByAuthorized + "): " + e.getLocalizedMessage(), e);

                        // ✅ 기존 계정 필터링 실패 시 모든 계정으로 재시도
                        if (filterByAuthorized) {
                            Log.d(TAG, "🔄 재시도: 모든 Google 계정 표시");
                            signInWithFilter(false);
                        } else {
                            if (callback != null) callback.onSignInError(e);
                        }
                    }
                }
        );
    }

    private void handleSignInResult(Credential credential) {
        if (credential instanceof CustomCredential
                && TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            try {
                Bundle data = ((CustomCredential) credential).getData();
                GoogleIdTokenCredential googleCred = GoogleIdTokenCredential.createFrom(data);
                firebaseAuthWithGoogle(googleCred.getIdToken());
            } catch (Exception e) {
                Log.w(TAG, "⚠️ Failed to parse GoogleIdTokenCredential", e);
                if (callback != null) callback.onSignInError(e);
            }
        } else {
            Log.w(TAG, "⚠️ Credential is not Google ID type");
            if (callback != null)
                callback.onSignInError(new IllegalStateException("Not a Google ID credential"));
        }
    }

    /** ✅ Firebase 로그인 + Firestore 신규 사용자 생성 */
    private void firebaseAuthWithGoogle(@NonNull String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "❌ Firebase login failed", task.getException());
                        if (callback != null) callback.onSignInError(task.getException());
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) return;

                    Log.d(TAG, "✅ Firebase login success: " + user.getEmail());

                    // Firestore에서 유저 문서 존재 여부 확인 후 신규 생성
                    createUserIfNotExists(user);
                });
    }

    /** ✅ Firestore 문서 존재하지 않을 경우만 생성 */
    private void createUserIfNotExists(@NonNull FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("user")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "📂 기존 사용자: Firestore 문서 존재 → 저장 스킵");
                        if (callback != null) callback.onSignInSuccess(user);
                    } else {
                        Log.d(TAG, "🆕 신규 사용자: Firestore 문서 없음 → 새로 생성");
                        saveNewUserToFirestore(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore 조회 실패", e);
                    if (callback != null) callback.onSignInError(e);
                });
    }

    /** ✅ 신규 유저 문서 생성 - RegisterActivity와 동일한 기본 이미지 URL 사용 */
    private void saveNewUserToFirestore(@NonNull FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ✅ RegisterActivity와 동일한 기본 프로필 이미지 URL 사용
        String defaultProfileImageUrl =
                "https://firebasestorage.googleapis.com/v0/b/trickortrip-71733.firebasestorage.app/o/defaultProfile%2Fic_profile_default.png?alt=media&token=94b6cdbe-53a1-46fb-a453-00860c81cd4f";

        // ✅ UserDTO 생성: backgroundImageUrl 포함
        UserDTO dto = new UserDTO(
                user.getDisplayName() != null ? user.getDisplayName() : "사용자",
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : defaultProfileImageUrl,
                "",  // backgroundImageUrl (빈 값)
                "",  // comment
                "",  // address
                Timestamp.now()
        );

        db.collection("user")
                .document(user.getUid())
                .set(dto)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ 신규 사용자 Firestore 저장 완료: " + user.getUid());
                    if (callback != null) callback.onSignInSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore 저장 실패", e);
                    if (callback != null) callback.onSignInError(e);
                });
    }

    /** 로그아웃 */
    public void signOut() {
        mAuth.signOut();
        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();

        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                executor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(@NonNull Void result) {
                        Log.d(TAG, "✅ Credential cleared");
                        if (callback != null) callback.onSignOut();
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e(TAG, "⚠️ Credential clear failed: " + e.getLocalizedMessage());
                        if (callback != null) callback.onSignOut();
                    }
                }
        );
    }
}