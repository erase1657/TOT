package com.example.tot;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tot.Authentication.GoogleAuthManager;
import com.example.tot.Authentication.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class WelcomeActivity extends AppCompatActivity {
    private Button emailBtn, googleBtn;
    private FirebaseAuth Auth;
    private GoogleAuthManager authManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        emailBtn = findViewById(R.id.btn_email);
        googleBtn = findViewById(R.id.btn_google);
        String serverClientId = getString(R.string.default_web_client_id);
        authManager = GoogleAuthManager.create(getApplicationContext(), serverClientId)
                .setFilterByAuthorizedAccounts(false); // 필요 시 false (모든 구글 계정 선택 허용)
        authManager.setCallback(new GoogleAuthManager.AuthCallback() {
            @Override
            public void onSignInSuccess(@NonNull FirebaseUser user) {
                runOnUiThread(() -> {
                    Toast.makeText(WelcomeActivity.this,
                            "구글 로그인 성공: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                });
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onSignInError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(WelcomeActivity.this,
                            "구글 로그인 실패: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });          }

            @Override
            public void onSignOut() {

            }

        });
        emailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authManager.signIn(WelcomeActivity.this);
            }
        });


    }
}