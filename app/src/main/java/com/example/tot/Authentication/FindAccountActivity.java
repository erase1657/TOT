package com.example.tot.Authentication;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tot.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;

public class FindAccountActivity extends AppCompatActivity {
    private Button FindPasswordBtn;
    private EditText EmailEt;
    private FirebaseAuth Auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_account);

        Auth = FirebaseAuth.getInstance();

        FindPasswordBtn = findViewById(R.id.btn_findpassword);
        EmailEt = findViewById(R.id.et_email);

        FindPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email =  EmailEt.getText().toString().trim();
                Auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(FindAccountActivity.this, "재설정 링크를 보냈습니다. \n 이메일을 확인해주세요.", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(FindAccountActivity.this, "올바르지않은 이메일입니다.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}