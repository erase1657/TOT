/**
 * Copyright 2021 Google Inc.
 * Licensed under the Apache License, Version 2.0
 *
 * NOTE:
 *  - 이 클래스는 Firebase Authentication + AndroidX Credential Manager + Google Identity(One Tap/GID)
 *    조합을 재사용 가능한 형태로 래핑합니다.
 *  - UI 반영은 외부(Activity/Fragment)에서 AuthCallback을 통해 처리하세요.
 */

package com.example.tot.Authentication;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;
import android.os.Bundle;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 재사용 가능한 Firebase 기반 Google 로그인 매니저.
 *
 * 사용 흐름:
 *   1) GoogleAuthManager manager = GoogleAuthManager.create(appContext, serverClientId);
 *   2) manager.setCallback(callback); // 로그인 결과 수신
 *   3) manager.checkCurrentUser();    // 앱 시작 시 현재 유저 전달(옵션)
 *   4) manager.signIn();              // 구글 로그인 UI 실행
 *   5) manager.signOut();             // 로그아웃
 */
public class GoogleAuthManager {

    private static final String TAG = "GoogleAuthManager";

    public interface AuthCallback {
        /** Firebase 로그인 성공 시 호출됩니다. */
        @MainThread
        void onSignInSuccess(@NonNull FirebaseUser user);

        /** Firebase 로그인 실패 또는 Credential 조회 실패 시 호출됩니다. */
        @MainThread
        void onSignInError(@NonNull Exception e);

        /** 로그아웃 완료(자격 상태 초기화 포함) 시 호출됩니다. */
        @MainThread
        void onSignOut();
    }

    // Firebase
    private final FirebaseAuth firebaseAuth;

    // AndroidX Credential Manager
    private final CredentialManager credentialManager;

    // Google ID 옵션 구성에 필요한 웹 클라이언트 ID
    private final String serverClientId;

    // 비동기 실행용 Executor (기본: single thread)
    private final Executor executor;

    // Application Context (Activity context 불필요)
    private final Context appContext;

    // 취소 제어
    @Nullable
    private CancellationSignal getCredCancellation;

    @Nullable
    private AuthCallback callback;

    // 구성 옵션
    private boolean filterByAuthorizedAccounts = true;

    private GoogleAuthManager(@NonNull Context appContext,
                              @NonNull String serverClientId,
                              @NonNull FirebaseAuth auth,
                              @NonNull CredentialManager cm,
                              @NonNull Executor executor) {
        this.appContext = appContext.getApplicationContext();
        this.serverClientId = Objects.requireNonNull(serverClientId, "serverClientId required");
        this.firebaseAuth = auth;
        this.credentialManager = cm;
        this.executor = executor;
    }

    /** 가장 간편한 팩토리: FirebaseAuth/CredentialManager/Executor를 내부에서 준비. */
    public static GoogleAuthManager create(@NonNull Context context,
                                           @NonNull String serverClientId) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        CredentialManager cm = CredentialManager.create(context.getApplicationContext());
        Executor executor = Executors.newSingleThreadExecutor();
        return new GoogleAuthManager(context, serverClientId, auth, cm, executor);
    }

    /** 고급 사용자용 팩토리: 커스텀 Executor 등을 주입하고 싶을 때. */
    public static GoogleAuthManager create(@NonNull Context context,
                                           @NonNull String serverClientId,
                                           @NonNull FirebaseAuth auth,
                                           @NonNull CredentialManager cm,
                                           @NonNull Executor executor) {
        return new GoogleAuthManager(context, serverClientId, auth, cm, executor);
    }

    /** 기본 동작(본인 계정만 필터) 변경이 필요하면 false로 지정. */
    public GoogleAuthManager setFilterByAuthorizedAccounts(boolean enabled) {
        this.filterByAuthorizedAccounts = enabled;
        return this;
    }

    /** 콜백 등록(액티비티/프래그먼트에서 UI 업데이트 담당). */
    public void setCallback(@Nullable AuthCallback callback) {
        this.callback = callback;
    }

    /** 앱 시작 시 현재 로그인 유저가 있으면 콜백으로 전달(옵션). */
    public void checkCurrentUser() {
        FirebaseUser current = firebaseAuth.getCurrentUser();
        if (current != null && callback != null) {
            // 이미 메인스레드 컨텍스트라 가정. 필요 시 Handler로 main posting 가능.
            callback.onSignInSuccess(current);
        }
    }

    /** 구글 로그인 플로우 시작(One Tap/Credential Manager UI 호출). */
    public void signIn(@NonNull Activity activity) {
        // 1) Google ID 옵션 구성
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                .setServerClientId(serverClientId)
                .build();

        // 2) Credential 요청 만들기
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // 3) 취소 시그널 관리
        cancelOngoingGetCredential(); // 중복 요청 방지
        getCredCancellation = new CancellationSignal();

        // 4) Credential Manager UI 실행
        credentialManager.getCredentialAsync(
                activity,
                request,
                getCredCancellation,
                executor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignInResult(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "getCredentialAsync error: " + e.getLocalizedMessage(), e);
                        if (callback != null) callback.onSignInError(e);
                    }
                }
        );
    }

    /** 현재 진행 중인 Credential 요청이 있으면 취소. */
    public void cancelOngoingGetCredential() {
        if (getCredCancellation != null && !getCredCancellation.isCanceled()) {
            getCredCancellation.cancel();
        }
        getCredCancellation = null;
    }

    /** 자격 증명 응답 처리 → Google ID Token → Firebase Auth 연동. */
    private void handleSignInResult(@NonNull Credential credential) {
        if (credential instanceof CustomCredential
                && TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            try {
                Bundle data = ((CustomCredential) credential).getData();
                GoogleIdTokenCredential gid = GoogleIdTokenCredential.createFrom(data);
                String idToken = gid.getIdToken();
                firebaseAuthWithGoogle(idToken);
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse GoogleIdTokenCredential", e);
                if (callback != null) callback.onSignInError(e);
            }
        } else {
            Log.w(TAG, "Credential is not Google ID token type.");
            if (callback != null) {
                callback.onSignInError(new IllegalStateException("Not a Google ID credential"));
            }
        }
    }

    /** Firebase Auth 연동 */
    private void firebaseAuthWithGoogle(@NonNull String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            if (callback != null) callback.onSignInSuccess(user);
                        } else {
                            if (callback != null) callback.onSignInError(
                                    new IllegalStateException("FirebaseUser is null after success"));
                        }
                    } else {
                        Exception e = (task.getException() != null)
                                ? task.getException()
                                : new RuntimeException("signInWithCredential failed");
                        Log.w(TAG, "signInWithCredential:failure", e);
                        if (callback != null) callback.onSignInError(e);
                    }
                });
    }

    /** Firebase Sign-out + Credential 상태 초기화 */
    public void signOut() {
        // 1) Firebase 로그아웃
        firebaseAuth.signOut();

        // 2) 각 Credential Provider의 현재 사용자 상태 초기화
        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                executor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(@NonNull Void result) {
                        if (callback != null) callback.onSignOut();
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e(TAG, "clearCredentialStateAsync error: " + e.getLocalizedMessage(), e);
                        // 에러가 있어도 Firebase 로그아웃은 이미 완료된 상태. 콜백은 알림 목적.
                        if (callback != null) callback.onSignOut();
                    }
                }
        );
    }

    /** 현재 로그인한 FirebaseUser 조회(없으면 null). */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
}
