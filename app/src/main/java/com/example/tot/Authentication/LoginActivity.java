package com.example.tot.Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tot.MainActivity;
import com.example.tot.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private Button GoRegisterBtn, findPasswordBtn, LoginBtn, GoogleBtn;
    private EditText EmailEt, PasswordEt;
    private FirebaseAuth Auth;
    private GoogleAuthManager authManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Auth = FirebaseAuth.getInstance();

        GoRegisterBtn = findViewById(R.id.btn_register);
        findPasswordBtn = findViewById(R.id.btn_findpassword);
        LoginBtn = findViewById(R.id.btn_login);
        GoogleBtn = findViewById(R.id.btn_google);

        EmailEt = findViewById(R.id.et_email);
        PasswordEt = findViewById(R.id.et_password);

        String serverClientId = getString(R.string.default_web_client_id);

        authManager = GoogleAuthManager.create(getApplicationContext(), serverClientId)
                .setFilterByAuthorizedAccounts(false); // 필요 시 false (모든 구글 계정 선택 허용)

        authManager.setCallback(new GoogleAuthManager.AuthCallback() {
            @Override
            public void onSignInSuccess(@NonNull FirebaseUser user) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this,
                            "구글 로그인 성공: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                });
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onSignInError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this,
                            "구글 로그인 실패: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });          }

            @Override
            public void onSignOut() {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this,
                            "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });

        // 앱 시작 시 현재 사용자 체크(옵션)
        authManager.checkCurrentUser();

        // 예: 버튼으로 로그인 실행
        GoogleBtn.setOnClickListener(v -> authManager.signIn(LoginActivity.this));

        GoRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        LoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = EmailEt.getText().toString().trim();
                String password = PasswordEt.getText().toString().trim();

                if (email.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                signIn(email,password);
            }
        });
        findPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, FindAccountActivity.class);
                startActivity(intent);
            }
        });
    }

    private void signIn(String email, String password){
        Auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            Toast.makeText(LoginActivity.this, "로그인 성공",
                                    Toast.LENGTH_SHORT).show();

                            FirebaseUser user = Auth.getCurrentUser();
                            updateUI(user);
                        } else {


                            Toast.makeText(LoginActivity.this, "이메일과 비밀번호를 다시 확인해주세요.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });

    }

    private void updateUI(FirebaseUser user){
        if(user !=null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
}