package com.example.tot.Authentication;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tot.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Log;
import android.view.View;
import android.widget.*;


public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private Button RegisterBtn, GoLoginBtn;
    private EditText EmailEt, PasswordEt, NicknameEt;
    private FirebaseAuth mAuth;
    private String uid, email, nickname, profileImageUrl, comment, address;
    Timestamp createAt;
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
                String password = PasswordEt.getText().toString().trim();
                email =  EmailEt.getText().toString().trim();
                nickname = NicknameEt.getText().toString().trim();
                if(email.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                createAccount(email,password,nickname);//계정 생성으로 이동
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
    private void createAccount(String email, String password,String nickname) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            String defaultProfileImageUrl = "https://firebasestorage.googleapis.com/v0/b/trickortrip-71733.firebasestorage.app/o/defaultProfile%2Fic_profile_default.xml?alt=media&token=b2d8211d-ccf1-49de-b423-a7b659089702";
                            FirebaseUser user = task.getResult().getUser();
                            UserDTO dto = new UserDTO(
                                    nickname,
                                    defaultProfileImageUrl,
                                    "",
                                    "",
                                    Timestamp.now()
                            );

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            db.collection(("users"))
                                    .document(user.getUid())
                                    .set(dto)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this, "계정 생성 성공!", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "db저장 성공");
                                        updateUI(user);
                                    })
                                    .addOnFailureListener(e ->{
                                        Toast.makeText(RegisterActivity.this, "데이터 저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "db저장 실패", e);
                                    });

                        }
                        else {
                            Toast.makeText(RegisterActivity.this, "계정 생성 실패. 중복된 이메일입니다.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Firebase Auth 계정 생성 실패", task.getException());
                        }
                    }
                });
    }
    private void updateUI(FirebaseUser user){
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}