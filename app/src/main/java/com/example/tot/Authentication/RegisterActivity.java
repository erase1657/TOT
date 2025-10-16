package com.example.tot.Authentication;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tot.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.view.View;
import android.widget.*;
public class RegisterActivity extends AppCompatActivity {
    private Button RegisterBtn, GoLoginBtn;
    private EditText EmailEt, PasswordEt, NicknameEt;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        RegisterBtn = findViewById(R.id.btn_register);
        GoLoginBtn = findViewById(R.id.btn_login);

        EmailEt = findViewById(R.id.et_email);
        PasswordEt = findViewById(R.id.et_password);
        NicknameEt = findViewById(R.id.et_nickname);

        RegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email =  EmailEt.getText().toString().trim();
                String password = PasswordEt.getText().toString().trim();
                String nickname = NicknameEt.getText().toString().trim();
                if(email.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "필수사항들을 입력해주세요", Toast.LENGTH_SHORT).show();
                    return;
                }
                createAccount(email,password,nickname);
            }
        });
        GoLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }
    private void createAccount(String email, String password, String nickname) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            FirebaseUser user = task.getResult().getUser();
                            Toast.makeText(RegisterActivity.this, "계정 생성 성공! 로그인 해주세요.",
                                    Toast.LENGTH_SHORT).show();

                            updateUI(user);

                            } else {
                            Toast.makeText(RegisterActivity.this, "계정 생성 실패. 중복된 이메일입니다.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void updateUI(FirebaseUser user){
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}