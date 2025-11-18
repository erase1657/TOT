package com.example.tot.Notification;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tot.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    private ImageView btnBack;
    private LinearLayout todaySection;
    private LinearLayout recentSection;
    private LinearLayout emptyView;
    private RecyclerView recyclerToday;
    private RecyclerView recyclerRecent;

    private NotificationAdapter todayAdapter;
    private NotificationAdapter recentAdapter;

    private List<NotificationDTO> todayNotifications = new ArrayList<>();
    private List<NotificationDTO> recentNotifications = new ArrayList<>();

    // Firestore
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerViews();
        loadNotifications();
        updateUI();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        todaySection = findViewById(R.id.today_section);
        recentSection = findViewById(R.id.recent_section);
        emptyView = findViewById(R.id.empty_view);
        recyclerToday = findViewById(R.id.recycler_today);
        recyclerRecent = findViewById(R.id.recycler_recent);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        recyclerToday.setLayoutManager(new LinearLayoutManager(this));
        todayAdapter = new NotificationAdapter(todayNotifications, new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(NotificationDTO notification) {
                handleNotificationClick(notification);
            }

            @Override
            public void onFollowBackClick(NotificationDTO notification) {
                handleFollowBack(notification);
            }
        });
        recyclerToday.setAdapter(todayAdapter);

        recyclerRecent.setLayoutManager(new LinearLayoutManager(this));
        recentAdapter = new NotificationAdapter(recentNotifications, new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(NotificationDTO notification) {
                handleNotificationClick(notification);
            }

            @Override
            public void onFollowBackClick(NotificationDTO notification) {
                handleFollowBack(notification);
            }
        });
        recyclerRecent.setAdapter(recentAdapter);
    }

    private void loadNotifications() {
        NotificationManager manager = NotificationManager.getInstance();
        todayNotifications.addAll(manager.getTodayNotifications());
        recentNotifications.addAll(manager.getRecentNotifications());
    }

    private void updateUI() {
        boolean hasToday = !todayNotifications.isEmpty();
        boolean hasRecent = !recentNotifications.isEmpty();

        if (hasToday || hasRecent) {
            emptyView.setVisibility(View.GONE);
            todaySection.setVisibility(hasToday ? View.VISIBLE : View.GONE);
            recentSection.setVisibility(hasRecent ? View.VISIBLE : View.GONE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
            todaySection.setVisibility(View.GONE);
            recentSection.setVisibility(View.GONE);
        }
    }

    private void handleNotificationClick(NotificationDTO notification) {
        notification.setRead(true);
        todayAdapter.notifyDataSetChanged();
        recentAdapter.notifyDataSetChanged();

        switch (notification.getType()) {
            case SCHEDULE_INVITE:
                Toast.makeText(this, "일정 상세 화면으로 이동", Toast.LENGTH_SHORT).show();
                break;
            case FOLLOW:
                Toast.makeText(this, notification.getUserName() + " 님의 프로필로 이동", Toast.LENGTH_SHORT).show();
                break;
            case COMMENT:
                Toast.makeText(this, "게시물 상세 화면으로 이동", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * ✅ 맞팔로우 버튼 클릭 처리 (Firestore 연동 - 양방향 처리)
     */
    private void handleFollowBack(NotificationDTO notification) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUid = mAuth.getCurrentUser().getUid();
        String targetUserId = notification.getUserId();

        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "사용자 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ 이미 팔로우 중인지 확인
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // 이미 팔로우 중인 경우 → 언팔로우
                        performUnfollow(myUid, targetUserId, notification);
                    } else {
                        // 팔로우하지 않은 경우 → 팔로우
                        performFollowBack(myUid, targetUserId, notification);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "팔로우 상태 확인 실패", e);
                    Toast.makeText(this, "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ 맞팔로우 실행 (양방향 처리)
     */
    private void performFollowBack(String myUid, String targetUserId, NotificationDTO notification) {
        Map<String, Object> followData = new HashMap<>();
        followData.put("followedAt", System.currentTimeMillis());

        // ✅ 1. 내 following에 추가
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .set(followData)
                .addOnSuccessListener(aVoid -> {
                    // ✅ 2. 상대방 follower에 추가
                    db.collection("user")
                            .document(targetUserId)
                            .collection("follower")
                            .document(myUid)
                            .set(followData)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, notification.getUserName() + " 님을 팔로우했습니다", Toast.LENGTH_SHORT).show();

                                // ✅ 알림 읽음 처리
                                notification.setRead(true);
                                todayAdapter.notifyDataSetChanged();
                                recentAdapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "상대방 팔로워 추가 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "팔로우 실패", e);
                    Toast.makeText(this, "팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * ✅ 언팔로우 실행 (양방향 처리)
     */
    private void performUnfollow(String myUid, String targetUserId, NotificationDTO notification) {
        // ✅ 1. 내 following에서 삭제
        db.collection("user")
                .document(myUid)
                .collection("following")
                .document(targetUserId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // ✅ 2. 상대방 follower에서 삭제
                    db.collection("user")
                            .document(targetUserId)
                            .collection("follower")
                            .document(myUid)
                            .delete()
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, notification.getUserName() + " 님을 언팔로우했습니다", Toast.LENGTH_SHORT).show();

                                // ✅ 알림 읽음 처리
                                notification.setRead(true);
                                todayAdapter.notifyDataSetChanged();
                                recentAdapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "상대방 follower 삭제 실패", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "언팔로우 실패", e);
                    Toast.makeText(this, "언팔로우 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
    }

    public int getTotalUnreadCount() {
        int count = 0;
        for (NotificationDTO notif : todayNotifications) {
            if (!notif.isRead()) count++;
        }
        for (NotificationDTO notif : recentNotifications) {
            if (!notif.isRead()) count++;
        }
        return count;
    }
}