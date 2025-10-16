package com.example.tot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tot.Authentication.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splash = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);


        // 2) Firebase 현재 유저 확인 (동기)
        final long start = SystemClock.uptimeMillis();
        splash.setKeepOnScreenCondition(() -> {
            long elapsed = SystemClock.uptimeMillis() - start;
            // 최소 200ms, 최대 400ms 유지
            return elapsed < 1000;
        });
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        final Intent next;
        if (currentUser != null) {
            // 로그인 기록 있음 → 메인으로
            next = new Intent(this, MainActivity.class);
        } else {
            // 로그인 기록 없음 → 로그인으로
            next = new Intent(this, LoginActivity.class);
        }
        next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(next);
            finish();
        }, 1000); // 1초 뒤 화면 전환
    }
}