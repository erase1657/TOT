package com.example.tot.Community;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PostCreateActivity extends AppCompatActivity {

    private static final String TAG = "PostCreateActivity";

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

        btnPublish.setEnabled(false);

        // ✅ 통합 구조: posts/{postId} 안에 모든 정보 저장
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

        communityPostsRef
                .document(postId)
                .set(postData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ 게시글 등록 성공: " + postId);
                    copyScheduleToPost(uid, scheduleId, postId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 게시글 등록 실패", e);
                    Toast.makeText(this, "게시글 등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnPublish.setEnabled(true);
                });
    }

    /**
     * ✅ 최적화: posts/{postId}/scheduleDate로 직접 복사
     */
    private void copyScheduleToPost(String uid, String scheduleId, String postId) {

        Log.d(TAG, "📋 일정 데이터 복사 시작: " + scheduleId);

        CollectionReference sourceScheduleRef = db.collection("user")
                .document(uid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate");

        CollectionReference destScheduleRef = communityPostsRef
                .document(postId)
                .collection("scheduleDate");

        sourceScheduleRef.get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "⚠️ 복사할 일정 데이터가 없음");
                        Toast.makeText(this, "게시글이 등록되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    int[] pendingCopies = {0};

                    for (DocumentSnapshot dateDoc : querySnapshot.getDocuments()) {

                        String dateKey = dateDoc.getId();
                        Map<String, Object> dateData = dateDoc.getData();

                        if (dateData != null) {
                            batch.set(destScheduleRef.document(dateKey), dateData);
                        }

                        pendingCopies[0]++;

                        copyScheduleItems(uid, scheduleId, dateKey, postId, () -> {
                            pendingCopies[0]--;
                            if (pendingCopies[0] == 0) {
                                Log.d(TAG, "✅ 모든 일정 복사 완료");
                                Toast.makeText(this, "게시글이 등록되었습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });

                        pendingCopies[0]++;

                        copyAlbumData(uid, scheduleId, dateKey, postId, () -> {
                            pendingCopies[0]--;
                            if (pendingCopies[0] == 0) {
                                Log.d(TAG, "✅ 모든 앨범 복사 완료");
                            }
                        });
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid2 -> Log.d(TAG, "✅ scheduleDate 배치 저장 완료"))
                            .addOnFailureListener(e -> Log.e(TAG, "❌ scheduleDate 저장 실패", e));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ 일정 데이터 복사 실패", e);
                    Toast.makeText(this, "게시글이 등록되었습니다 (일정 복사 실패)", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void copyScheduleItems(String uid, String scheduleId, String dateKey, String postId, Runnable onComplete) {

        CollectionReference sourceItems = db.collection("user")
                .document(uid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem");

        CollectionReference destItems = communityPostsRef
                .document(postId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("scheduleItem");

        sourceItems.get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            batch.set(destItems.document(doc.getId()), data);
                        }
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ scheduleItem 복사 완료: " + dateKey);
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ scheduleItem 복사 실패: " + dateKey, e);
                                onComplete.run();
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ scheduleItem 로드 실패", e);
                    onComplete.run();
                });
    }

    private void copyAlbumData(String uid, String scheduleId, String dateKey, String postId, Runnable onComplete) {

        CollectionReference sourceAlbum = db.collection("user")
                .document(uid)
                .collection("schedule")
                .document(scheduleId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("album");

        CollectionReference destAlbum = communityPostsRef
                .document(postId)
                .collection("scheduleDate")
                .document(dateKey)
                .collection("album");

        sourceAlbum.get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            batch.set(destAlbum.document(doc.getId()), data);
                        }
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ album 복사 완료: " + dateKey);
                                onComplete.run();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ album 복사 실패", e);
                                onComplete.run();
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ album 로드 실패", e);
                    onComplete.run();
                });
    }
}
