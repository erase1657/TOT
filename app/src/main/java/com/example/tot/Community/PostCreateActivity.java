package com.example.tot.Community;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PostCreateActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvScheduleInfo;
    private EditText edtPostTitle;
    private Button btnPublish;

    private String scheduleId;
    private String locationName;
    private long startDate;
    private long endDate;

    private FirebaseFirestore db;
    private CollectionReference communityPostsRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_create);

        db = FirebaseFirestore.getInstance();
        communityPostsRef = db.collection("public")
                .document("community")
                .collection("posts");
        auth = FirebaseAuth.getInstance();

        scheduleId = getIntent().getStringExtra("scheduleId");
        locationName = getIntent().getStringExtra("locationName");
        startDate = getIntent().getLongExtra("startDate", 0);
        endDate = getIntent().getLongExtra("endDate", 0);

        initViews();
        displayScheduleInfo();

        btnBack.setOnClickListener(v -> finish());
        btnPublish.setOnClickListener(v -> publishPost());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvScheduleInfo = findViewById(R.id.tv_schedule_info);
        edtPostTitle = findViewById(R.id.edt_post_title);
        btnPublish = findViewById(R.id.btn_publish);
    }

    private void displayScheduleInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
        String start = sdf.format(new Date(startDate));
        String end = sdf.format(new Date(endDate));

        String info = locationName + " (" + start + " ~ " + end + ")";
        tvScheduleInfo.setText(info);
    }

    private void publishPost() {
        String title = edtPostTitle.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "게시글 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String postId = communityPostsRef.document().getId();

        Map<String, Object> postData = new HashMap<>();
        postData.put("postId", postId);
        postData.put("authorUid", uid);
        postData.put("scheduleId", scheduleId);
        postData.put("title", title);
        postData.put("locationName", locationName);
        postData.put("startDate", startDate);
        postData.put("endDate", endDate);
        postData.put("createdAt", System.currentTimeMillis());
        postData.put("heartCount", 0);
        postData.put("commentCount", 0);

        btnPublish.setEnabled(false);

        communityPostsRef
                .document(postId)
                .set(postData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "게시글이 등록되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "게시글 등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                });
    }
}